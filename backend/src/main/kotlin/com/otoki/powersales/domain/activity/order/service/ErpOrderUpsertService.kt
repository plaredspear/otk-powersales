package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertFailedRow
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ERP 주문 UPSERT 도메인 서비스 (헤더 + 라인 다단 saveAll).
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapErpOrderService]
 * - origin spec: #561 (SAP ERP 주문 인바운드) — 어댑터/도메인 분리: #635 P3-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<ErpOrderUpsertCommand>` (헤더 + 라인 중첩 모델).
 * 2. cross-domain lookup: [AccountRepository.findByExternalKeyIn] (Account 매칭 — 미존재 시 FK null 로 적재, 거부 안 함).
 * 3. 헤더/라인 기존 행은 UPSERT 키로 **IN 절 일괄 조회** 후 메모리 맵으로 매칭한다
 *    ([ErpOrderRepository.findBySapOrderNumberIn] / [ErpOrderProductRepository.findByExternalKeyIn]).
 *    건별 개별 조회는 대량 페이로드에서 N+1 로 처리 시간이 폭증해 전송단 timeout(연결 끊김)을 유발했다.
 * 4. 레거시 정합 — 수신 필드 명시 필수/형식 검증으로 행을 거부하지 않는다 (레거시 IF_REST_SAP_ClientOrderReceive
 *    에 그런 게이트 없음). 헤더 upsert 키 [ErpOrder.sapOrderNumber](NOT NULL+UNIQUE, SF `Name` 정합)만
 *    누락 행이 saveAllAndFlush 단계 DB 제약에서 행 격리된다 (allOrNone=false 동등).
 * 5. 단일 `@Transactional` 안에서 다단 saveAll:
 *    - [ErpOrderRepository.saveAllAndFlush] (헤더)
 *    - 라인 entity 빌드 (헤더 ID 참조 + externalKey 도출)
 *    - [ErpOrderProductRepository.saveAll] (라인)
 * 6. 라인 ConstraintViolation 등 적재 도중 throw → 트랜잭션 전체 롤백 (헤더까지 미반영).
 *
 * ## 레거시 정합 (의도적 deviation 외 동등)
 * - Account FK: SAPAccountCode → [AccountRepository.findByExternalKeyIn] resolve 거래처를 헤더의
 *   [ErpOrder.account] (account_id) + [ErpOrder.accountSfid] 에 연결 (레거시 `AccountId__c` MasterDetail 정합).
 * - 날짜: 빈값/`00000000`/파싱 실패 → `2999-12-31` 센티넬 ([parseDate], 레거시 `Util.convertStringToDate` 정합).
 * - 라인 externalKey: `주문번호(선행 0 한 자리 제거)+라인번호 (+ShippingVehicle non-blank 시)` — 레거시 키 규격 정합 ([computeExternalKey] 참조).
 *
 * cross-domain 의존: [AccountRepository] (Account 매칭 lookup) — lookup 용도 read-only.
 * `sap.*` 패키지 의존 0건.
 */
@Service
class ErpOrderUpsertService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
    private val accountRepository: AccountRepository
) {

    @Transactional
    fun upsert(commands: List<ErpOrderUpsertCommand>): ErpOrderUpsertResult {
        val accountCodes = commands.mapNotNull { it.sapAccountCode?.takeIf { c -> c.isNotBlank() } }.distinct()
        val accountByCode: Map<String, Account> = if (accountCodes.isEmpty()) {
            emptyMap()
        } else {
            accountRepository.findByExternalKeyIn(accountCodes)
                .mapNotNull { account -> account.externalKey?.let { it to account } }
                .toMap()
        }

        // 헤더 기존 행 일괄 조회 (IN 절 1회) — 건별 findBySapOrderNumber 반복(N+1) 제거.
        val orderNumbers = commands.mapNotNull { it.sapOrderNumber?.takeIf { n -> n.isNotBlank() } }.distinct()
        val existingHeaders: MutableMap<String, ErpOrder> = if (orderNumbers.isEmpty()) {
            mutableMapOf()
        } else {
            erpOrderRepository.findBySapOrderNumberIn(orderNumbers)
                .associateBy { it.sapOrderNumber }
                .toMutableMap()
        }

        val failures = mutableListOf<ErpOrderUpsertFailedRow>()
        val acceptedHeaders = mutableListOf<ErpOrder>()
        val acceptedLinesByHeader = mutableMapOf<String, List<ErpOrderLineCommand>>()

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_ClientOrderReceive 정합 — 수신 필드에 대한 명시적 필수/형식 검증으로
            // 행을 거부하는 코드가 레거시에 전무하다. SAPOrderNumber/SAPAccountCode 누락도 그대로 적재하고,
            // 거래처 미존재 시에도 (레거시는 NPE 로 배치 전체가 ERROR 가 되는 우발 동작이나, 그 버그성 전체
            // 실패는 재현하지 않고) account FK 를 null 로 둔 채 헤더를 적재해 행 격리를 유지한다.
            // 단 헤더 upsert 키(sap_order_number, NOT NULL+UNIQUE)는 SF `Name` upsert 키 정합으로
            // 누락 행만 DB 제약에서 격리되도록 둔다 — 빈 키 헤더는 saveAllAndFlush 단계에서 행 단위 실패.
            val sapOrderNumber = command.sapOrderNumber?.takeIf { it.isNotBlank() }
            if (sapOrderNumber == null) {
                failures += ErpOrderUpsertFailedRow(command.sapOrderNumber, "SAPOrderNumber 필수 (헤더 upsert 키)")
                return@forEach
            }
            val sapAccountCode = command.sapAccountCode?.takeIf { it.isNotBlank() }
            val account = sapAccountCode?.let { accountByCode[it] }

            val entity = existingHeaders[sapOrderNumber]?.also { applyHeaderFields(it, command, account) }
                ?: ErpOrder(sapOrderNumber = sapOrderNumber).also {
                    applyHeaderFields(it, command, account)
                    existingHeaders[sapOrderNumber] = it
                }
            acceptedHeaders += entity
            acceptedLinesByHeader[sapOrderNumber] = command.lines
        }

        val savedHeaders = if (acceptedHeaders.isNotEmpty()) {
            erpOrderRepository.saveAllAndFlush(acceptedHeaders)
        } else {
            emptyList()
        }
        val headerByNumber = savedHeaders.associateBy { it.sapOrderNumber }

        // 1차 패스: 적재 대상 라인 (header + line + externalKey) 을 수집하며 요청 내 중복 키를 제거한다.
        // (기존엔 라인마다 findByExternalKey 개별 조회 → 대량 페이로드에서 N+1 로 처리 시간 폭증)
        data class PendingLine(val header: ErpOrder, val line: ErpOrderLineCommand, val key: String)
        val lineExternalKeys = mutableSetOf<String>()
        val pendingLines = mutableListOf<PendingLine>()
        acceptedLinesByHeader.forEach { (sapOrderNumber, lines) ->
            val header = headerByNumber[sapOrderNumber] ?: return@forEach
            lines.forEach { line ->
                val key = computeExternalKey(line) ?: return@forEach
                if (!lineExternalKeys.add(key)) return@forEach
                pendingLines += PendingLine(header, line, key)
            }
        }

        // 라인 기존 행 일괄 조회 (IN 절 1회) — 건별 findByExternalKey 반복(N+1) 제거.
        val existingLinesByKey: Map<String, ErpOrderProduct> = if (lineExternalKeys.isEmpty()) {
            emptyMap()
        } else {
            erpOrderProductRepository.findByExternalKeyIn(lineExternalKeys)
                .mapNotNull { entity -> entity.externalKey?.let { it to entity } }
                .toMap()
        }

        val lineEntities = pendingLines.map { (header, line, key) ->
            buildOrUpdateLine(header, line, key, existingLinesByKey[key])
        }

        if (lineEntities.isNotEmpty()) {
            erpOrderProductRepository.saveAll(lineEntities)
        }

        return ErpOrderUpsertResult(
            headerSuccessCount = savedHeaders.size,
            lineSuccessCount = lineEntities.size,
            failures = failures
        )
    }

    private fun applyHeaderFields(entity: ErpOrder, command: ErpOrderUpsertCommand, account: Account?) {
        // 레거시 ERP_Order__c.AccountId__c (lookup→Account) 정합:
        // SAPAccountCode → Account.externalKey 로 resolve 한 거래처를 FK + SF Id 양쪽에 연결.
        // 거래처 미존재(또는 SAPAccountCode 누락) 시 account=null → FK/SFId 미연결로 적재 (레거시 검증 부재 정합).
        entity.account = account
        entity.accountSfid = account?.sfid
        entity.sapAccountCode = command.sapAccountCode
        entity.sapAccountName = command.sapAccountName
        entity.deliveryRequestDate = parseDate(command.deliveryRequestDate)
        entity.orderDate = parseDate(command.orderDate)
        entity.employeeCode = command.employeeCode
        entity.employeeName = command.employeeName
        entity.orderSalesAmount = parseAmountLong(command.orderSalesAmount)
        entity.orderChannel = command.orderChannel ?: ""
        entity.orderChannelNm = command.orderChannelNm ?: ""
        entity.orderType = command.orderType ?: ""
        entity.orderTypeNm = command.orderTypeNm ?: ""
    }

    private fun buildOrUpdateLine(
        header: ErpOrder,
        line: ErpOrderLineCommand,
        externalKey: String,
        existing: ErpOrderProduct?
    ): ErpOrderProduct {
        val entity = existing?.also { it.erpOrder = header } ?: ErpOrderProduct(
            erpOrder = header,
            sapOrderNumber = line.sapOrderNumber!!,
            lineNumber = line.lineNumber!!,
            externalKey = externalKey
        )
        applyLineFields(entity, line)
        return entity
    }

    private fun applyLineFields(entity: ErpOrderProduct, line: ErpOrderLineCommand) {
        entity.productCode = line.productCode
        entity.productName = line.productName
        entity.orderQuantity = parseAmountLong(line.orderQuantity)
        entity.unit = line.unit
        entity.confirmQuantityBox = parseDecimal(line.confirmQuantityBox, scale = 4)
        entity.confirmQuantity = parseDecimal(line.confirmQuantity, scale = 3)
        entity.confirmUnit = line.confirmUnit
        entity.defaultReason = line.defaultReason
        entity.lineItemStatus = line.lineItemStatus
        // 레거시 `if(status != '') OrderStatus__c = status;` 동등 — null(=상태 미산출)이면 기존 값 유지.
        computeDeliveryStatus(line)?.let { entity.deliveryStatus = it }
        entity.shippingDriverName = line.shippingDriverName
        if (!line.shippingVehicle.isNullOrBlank()) {
            entity.shippingVehicle = line.shippingVehicle
        }
        entity.shippingDriverPhone = line.shippingDriverPhone
        entity.shippingScheduleTime = sanitizeTime(line.shippingScheduleTime)
        entity.shippingCompleteTime = sanitizeTime(line.shippingCompleteTime)
        entity.shippingQuantityBox = parseDecimal(line.shippingQuantityBox, scale = 2)
        entity.shippingQuantity = parseAmountLong(line.shippingQuantity)
        entity.orderSalesLineAmount = parseAmountLong(line.orderSalesLineAmount)
        entity.shippingAmount = parseAmountLong(line.shippingAmount)
        entity.plant = line.plant ?: ""
        entity.plantNm = line.plantNm ?: ""
        entity.releaseQuantity = parseAmountLong(line.releaseQuantity)
        entity.releaseAmount = parseAmountLong(line.releaseAmount)
    }

    /**
     * 라인 externalKey 도출 — `SAPOrderNumber(선행 0 한 자리 제거) + LineNumber (+ ShippingVehicle)`.
     *
     * 레거시 `IF_REST_SAP_ClientOrderReceive.cls:141-142` 정합 — 기본 키는 `주문번호+라인번호` 이고,
     * `ShippingVehicle`(배송 차량번호) 이 비어있지 않으면 키 말미에 차량번호를 덧붙인다 (사전 협의된 키 규격).
     * 선행 0 제거는 한 자리만 — 레거시 `substring(1)` 동작 그대로 (0이 2개면 1개만 제거).
     * ShippingVehicle 값 자체는 [applyLineFields] 에서 컬럼에도 적재된다.
     */
    private fun computeExternalKey(line: ErpOrderLineCommand): String? {
        val orderNumber = line.sapOrderNumber?.takeIf { it.isNotBlank() } ?: return null
        val lineNumber = line.lineNumber?.takeIf { it.isNotBlank() } ?: return null
        val trimmed = if (orderNumber.startsWith("0")) orderNumber.substring(1) else orderNumber
        val shippingVehicle = line.shippingVehicle?.takeIf { it.isNotBlank() }.orEmpty()
        return trimmed + lineNumber + shippingVehicle
    }

    private fun sanitizeTime(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMPTY_TIME) return null
        return raw
    }

    /**
     * 라인 배송 상태 산출 — 레거시 `IF_REST_SAP_ClientOrderReceive.cls:146,156-160` 동등.
     *
     * 레거시는 4개 if 를 **독립적으로 순차 평가**(when 의 first-match 가 아니라 last-match-wins)하고,
     * 마지막에 `if(status != '') product.OrderStatus__c = status;` 로 **공백이면 적재 자체를 건너뛴다**.
     * 따라서:
     *  - 대기 조건은 `DefaultReason 공백 AND LineItemStatus 공백 AND ScheduleTime 미설정` 세 가지를 모두
     *    요구한다 (LineItemStatus 조건 포함). 이 조건을 빠뜨리면 LineItemStatus 만 채워진 라인이 잘못 '대기'로 분류된다.
     *  - 배송완료 평가는 결품 평가보다 **앞**에 있어, `DefaultReason 채워짐 + CompleteTime 채워짐 + ScheduleTime 미설정`
     *    같은 라인은 결품(마지막 if)이 배송완료를 덮어쓴다 (last-match-wins).
     *  - 어느 if 도 성립하지 않으면 `null` 을 반환하고, 호출부는 `deliveryStatus` 를 건드리지 않는다
     *    (레거시 `status == ''` → OrderStatus__c 미설정 동등).
     */
    private fun computeDeliveryStatus(line: ErpOrderLineCommand): String? {
        val scheduleBlank = line.shippingScheduleTime.isBlankOrEmptyTime()
        val completeBlank = line.shippingCompleteTime.isBlankOrEmptyTime()
        val defaultReasonBlank = line.defaultReason.isNullOrBlank()
        val lineItemStatusBlank = line.lineItemStatus.isNullOrBlank()

        var status: String? = null
        // cls:156 — 대기: DefaultReason 공백 AND LineItemStatus 공백 AND ScheduleTime 미설정
        if (defaultReasonBlank && lineItemStatusBlank && scheduleBlank) status = STATUS_PENDING
        // cls:157 — 배송중: ScheduleTime 설정 AND CompleteTime 미설정
        if (!scheduleBlank && completeBlank) status = STATUS_SHIPPING
        // cls:158 — 배송 완료: CompleteTime 설정
        if (!completeBlank) status = STATUS_DELIVERED
        // cls:159 — 결품: DefaultReason 설정 AND ScheduleTime 미설정 (마지막 평가 — 배송완료를 덮어씀)
        if (!defaultReasonBlank && scheduleBlank) status = STATUS_OUT_OF_STOCK
        return status
    }

    /** `null`/공백/`'000000'`(미설정 마커) 을 모두 "미설정"으로 본다 (레거시 `== '000000'` 동등). */
    private fun String?.isBlankOrEmptyTime(): Boolean {
        val raw = this?.trim().orEmpty()
        return raw.isEmpty() || raw == EMPTY_TIME
    }

    companion object {
        const val STATUS_OUT_OF_STOCK: String = "결품"
        const val STATUS_DELIVERED: String = "배송 완료"
        const val STATUS_SHIPPING: String = "배송중"
        const val STATUS_PENDING: String = "대기"
        private const val EMPTY_TIME = "000000"
        private val YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd")

        /**
         * 레거시 `Util.convertStringToDate` 센티넬 정합 — 빈값/`00000000`/파싱 실패 시
         * `2999-12-31` 을 반환한다 (날짜 미정 표현). SF 다운스트림이 이 센티넬을
         * "미정" 으로 가정하므로 null 대신 동일 센티넬을 저장해 동작을 일치시킨다.
         */
        private val DATE_SENTINEL: LocalDate = LocalDate.of(2999, 12, 31)

        fun parseAmount(value: String?): Double? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) return 0.0
            return try {
                trimmed.toDouble()
            } catch (_: NumberFormatException) {
                0.0
            }
        }

        /**
         * SF Number 컬럼용 — 빈 입력 → 0, parse 실패 → 0. V168 이후 numeric 컬럼은 NUMERIC 으로
         * 통일되어 소수점 보존 (이전: scale=0 가정으로 BIGINT 반환).
         */
        fun parseAmountLong(value: String?): BigDecimal? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) return BigDecimal.ZERO
            return try {
                BigDecimal(trimmed)
            } catch (_: NumberFormatException) {
                BigDecimal.ZERO
            }
        }

        /**
         * SF `double` scale>0 소수점 정밀도 컬럼용. 빈 입력 → 0, 지정 scale 로 반올림 (HALF_UP).
         */
        fun parseDecimal(value: String?, scale: Int): BigDecimal? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) return BigDecimal.ZERO.setScale(scale)
            return try {
                BigDecimal(trimmed).setScale(scale, RoundingMode.HALF_UP)
            } catch (_: NumberFormatException) {
                BigDecimal.ZERO.setScale(scale)
            }
        }

        fun parseDate(value: String?): LocalDate? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty() || trimmed == "00000000") return DATE_SENTINEL
            return try {
                LocalDate.parse(trimmed, YYYYMMDD)
            } catch (_: Exception) {
                DATE_SENTINEL
            }
        }
    }
}
