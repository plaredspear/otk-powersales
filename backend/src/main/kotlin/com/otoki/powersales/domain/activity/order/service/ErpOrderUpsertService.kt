package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertFailedRow
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertResult
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
 * 2. cross-domain lookup: [AccountRepository.findByExternalKeyIn] (Account 매칭 검증, 미존재 → 행 단위 failure).
 * 3. 헤더 UPSERT 키 ([ErpOrder.sapOrderNumber]) 로 [ErpOrderRepository.findBySapOrderNumber] 조회 후 entity 갱신/생성.
 * 4. 행 단위 검증 (필수값 누락 / Account 매칭 실패) → failures 누적, 적재 제외 (트랜잭션 롤백 없음).
 * 5. 단일 `@Transactional` 안에서 다단 saveAll:
 *    - [ErpOrderRepository.saveAllAndFlush] (헤더)
 *    - 라인 entity 빌드 (헤더 ID 참조 + externalKey 도출)
 *    - [ErpOrderProductRepository.saveAll] (라인)
 * 6. 라인 ConstraintViolation 등 적재 도중 throw → 트랜잭션 전체 롤백 (헤더까지 미반영).
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * cross-domain 의존: [AccountRepository] (Account 매칭 lookup) — Q3 옵션 1 정합 (lookup 용도 read-only).
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
        val accountKeySet: Set<String> = if (accountCodes.isEmpty()) {
            emptySet()
        } else {
            accountRepository.findByExternalKeyIn(accountCodes)
                .mapNotNull { it.externalKey }
                .toHashSet()
        }

        val orderNumbers = commands.mapNotNull { it.sapOrderNumber?.takeIf { n -> n.isNotBlank() } }.distinct()
        val existingHeaders: MutableMap<String, ErpOrder> = orderNumbers
            .mapNotNull { erpOrderRepository.findBySapOrderNumber(it) }
            .associateBy { it.sapOrderNumber }
            .toMutableMap()

        val failures = mutableListOf<ErpOrderUpsertFailedRow>()
        val acceptedHeaders = mutableListOf<ErpOrder>()
        val acceptedLinesByHeader = mutableMapOf<String, List<ErpOrderLineCommand>>()

        commands.forEach { command ->
            val sapOrderNumber = command.sapOrderNumber?.takeIf { it.isNotBlank() }
            val sapAccountCode = command.sapAccountCode?.takeIf { it.isNotBlank() }
            if (sapOrderNumber == null) {
                failures += ErpOrderUpsertFailedRow(command.sapOrderNumber, "SAPOrderNumber 필수")
                return@forEach
            }
            if (sapAccountCode == null) {
                failures += ErpOrderUpsertFailedRow(sapOrderNumber, "SAPAccountCode 필수")
                return@forEach
            }
            if (sapAccountCode !in accountKeySet) {
                failures += ErpOrderUpsertFailedRow(sapOrderNumber, "account not found")
                return@forEach
            }

            val entity = existingHeaders[sapOrderNumber]?.also { applyHeaderFields(it, command) }
                ?: ErpOrder(sapOrderNumber = sapOrderNumber).also {
                    applyHeaderFields(it, command)
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

        val lineExternalKeys = mutableSetOf<String>()
        val lineEntities = mutableListOf<ErpOrderProduct>()
        acceptedLinesByHeader.forEach { (sapOrderNumber, lines) ->
            val header = headerByNumber[sapOrderNumber] ?: return@forEach
            lines.forEach { line ->
                val key = computeExternalKey(line) ?: return@forEach
                if (!lineExternalKeys.add(key)) return@forEach
                lineEntities += buildOrUpdateLine(header, line, key)
            }
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

    private fun applyHeaderFields(entity: ErpOrder, command: ErpOrderUpsertCommand) {
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
        externalKey: String
    ): ErpOrderProduct {
        val existing = erpOrderProductRepository.findByExternalKey(externalKey)
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
     * 라인 externalKey 도출 — `SAPOrderNumber(선행 0 한 자리 제거) + LineNumber`.
     *
     * 레거시 의도적 deviation (유지보수 우선): SF `IF_REST_SAP_ClientOrderReceive` 는 키 말미에
     * `ShippingVehicle`(배송 차량번호) 도 조건부로 붙였다(non-blank 시). 그 결과 동일 (주문번호+라인번호)
     * 라도 차량번호가 달라지면 별개 row 로 insert 되어 같은 주문 라인이 여러 레코드로 쪼개지는
     * 부작용이 있었다(레거시 분석상 orphan/중복 라인 발생 경로). 신규는 ShippingVehicle 을 키에서
     * 제외해 (주문번호+라인번호) 단일 키로 upsert 하므로, 차량 변경은 같은 라인의 갱신으로 수렴한다.
     * ShippingVehicle 값 자체는 [applyLineFields] 에서 컬럼에 보존된다 (키에서만 뺀 것).
     * 선행 0 제거는 한 자리만 — 레거시 `substring(1)` 동작 그대로 (0이 2개면 1개만 제거).
     */
    private fun computeExternalKey(line: ErpOrderLineCommand): String? {
        val orderNumber = line.sapOrderNumber?.takeIf { it.isNotBlank() } ?: return null
        val lineNumber = line.lineNumber?.takeIf { it.isNotBlank() } ?: return null
        val trimmed = if (orderNumber.startsWith("0")) orderNumber.substring(1) else orderNumber
        return trimmed + lineNumber
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
            if (trimmed.isNullOrEmpty()) return null
            return try {
                LocalDate.parse(trimmed, YYYYMMDD)
            } catch (_: Exception) {
                null
            }
        }
    }
}
