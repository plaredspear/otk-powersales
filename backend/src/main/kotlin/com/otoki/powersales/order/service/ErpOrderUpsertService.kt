package com.otoki.powersales.order.service

import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import com.otoki.powersales.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertFailedRow
import com.otoki.powersales.order.service.dto.ErpOrderUpsertResult
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
        entity.deliveryStatus = computeDeliveryStatus(line)
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

    private fun computeDeliveryStatus(line: ErpOrderLineCommand): String {
        val schedule = line.shippingScheduleTime?.trim().orEmpty()
        val complete = line.shippingCompleteTime?.trim().orEmpty()
        val defaultReason = line.defaultReason?.trim().orEmpty()
        val scheduleEffective = schedule.isNotEmpty() && schedule != EMPTY_TIME
        val completeEffective = complete.isNotEmpty() && complete != EMPTY_TIME

        return when {
            defaultReason.isNotEmpty() && !scheduleEffective -> STATUS_OUT_OF_STOCK
            completeEffective -> STATUS_DELIVERED
            scheduleEffective && !completeEffective -> STATUS_SHIPPING
            else -> STATUS_PENDING
        }
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
