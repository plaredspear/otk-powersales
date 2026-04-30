package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderFailure
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderItemDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequestItem
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP ERP 주문 인바운드 UPSERT 서비스. (Spec #561)
 *
 * - 헤더 UPSERT 키: [ErpOrder.sapOrderNumber]
 * - 라인 UPSERT 키: [ErpOrderProduct.externalKey] = `SAPOrderNumber(선두 0 1자 제거) + LineNumber`
 * - 단일 트랜잭션: 헤더 saveAllAndFlush 후 라인 saveAll. 라인 ConstraintViolation 발생 시
 *   전체 트랜잭션 롤백 → 호출자에게 [Exception] 전파 → 500 INTERNAL_ERROR (D3 결정)
 * - Account 룩업: 페이로드 SAPAccountCode 로 [com.otoki.powersales.account.entity.Account.externalKey]
 *   일괄 조회. 미존재 시 해당 헤더 행 failure (라인도 함께 누락). FK 컬럼은 ErpOrder 에 없으므로
 *   검증 용도로만 사용한다.
 * - 부분 실패: Account 매칭 실패 / 필수 필드 누락만 행 단위 failure 처리. saveAll 도중 예외는
 *   트랜잭션 전체를 롤백시킨다.
 */
@Service
class SapErpOrderService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
    private val accountRepository: AccountRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<ErpOrderRequestItem>): ErpOrderDetail {
        val accountCodes = items.mapNotNull { it.sapAccountCode?.takeIf { c -> c.isNotBlank() } }.distinct()
        val accountKeySet: Set<String> = if (accountCodes.isEmpty()) {
            emptySet()
        } else {
            accountRepository.findByExternalKeyIn(accountCodes)
                .mapNotNull { it.externalKey }
                .toHashSet()
        }

        val orderNumbers = items.mapNotNull { it.sapOrderNumber?.takeIf { n -> n.isNotBlank() } }.distinct()
        val existingHeaders: MutableMap<String, ErpOrder> = orderNumbers
            .mapNotNull { erpOrderRepository.findBySapOrderNumber(it) }
            .associateBy { it.sapOrderNumber }
            .toMutableMap()

        val failures = mutableListOf<ErpOrderFailure>()
        val acceptedHeaders = mutableListOf<ErpOrder>()
        val acceptedLinesByHeader = mutableMapOf<String, List<ErpOrderItemDetail>>()

        items.forEach { item ->
            val sapOrderNumber = item.sapOrderNumber?.takeIf { it.isNotBlank() }
            val sapAccountCode = item.sapAccountCode?.takeIf { it.isNotBlank() }
            if (sapOrderNumber == null) {
                failures += ErpOrderFailure(item.sapOrderNumber, "SAPOrderNumber 필수")
                return@forEach
            }
            if (sapAccountCode == null) {
                failures += ErpOrderFailure(sapOrderNumber, "SAPAccountCode 필수")
                return@forEach
            }
            if (sapAccountCode !in accountKeySet) {
                failures += ErpOrderFailure(sapOrderNumber, "account not found")
                return@forEach
            }

            val entity = existingHeaders[sapOrderNumber]?.also { applyHeaderFields(it, item) }
                ?: ErpOrder(sapOrderNumber = sapOrderNumber).also {
                    applyHeaderFields(it, item)
                    existingHeaders[sapOrderNumber] = it
                }
            acceptedHeaders += entity
            acceptedLinesByHeader[sapOrderNumber] = item.itemDetailList.orEmpty()
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

        recordAccepted(items.size, savedHeaders.size, failures.size)

        return ErpOrderDetail(
            successCount = savedHeaders.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun applyHeaderFields(entity: ErpOrder, item: ErpOrderRequestItem) {
        entity.sapAccountCode = item.sapAccountCode
        entity.sapAccountName = item.sapAccountName
        entity.deliveryRequestDate = item.deliveryRequestDate
        entity.orderDate = item.orderDate
        entity.employeeCode = item.employeeCode
        entity.employeeName = item.employeeName
        entity.orderSalesAmount = parseAmount(item.orderSalesAmount)
        entity.orderChannel = item.orderChannel ?: ""
        entity.orderChannelNm = item.orderChannelNm ?: ""
        entity.orderType = item.orderType ?: ""
        entity.orderTypeNm = item.orderTypeNm ?: ""
    }

    private fun buildOrUpdateLine(
        header: ErpOrder,
        line: ErpOrderItemDetail,
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

    private fun applyLineFields(entity: ErpOrderProduct, line: ErpOrderItemDetail) {
        entity.productCode = line.productCode
        entity.productName = line.productName
        entity.orderQuantity = parseAmount(line.orderQuantity)
        entity.unit = line.unit
        entity.confirmQuantityBox = parseAmount(line.confirmQuantityBox)
        entity.confirmQuantity = parseAmount(line.confirmQuantity)
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
        entity.shippingQuantityBox = parseAmount(line.shippingQuantityBox)
        entity.shippingQuantity = parseAmount(line.shippingQuantity)
        entity.orderSalesLineAmount = parseAmount(line.orderSalesLineAmount)
        entity.shippingAmount = parseAmount(line.shippingAmount)
        entity.plant = line.plant ?: ""
        entity.plantNm = line.plantNm ?: ""
        entity.releaseQuantity = parseAmount(line.releaseQuantity)
        entity.releaseAmount = parseAmount(line.releaseAmount)
    }

    private fun computeExternalKey(line: ErpOrderItemDetail): String? {
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

    private fun computeDeliveryStatus(line: ErpOrderItemDetail): String {
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

    private fun recordAccepted(received: Int, success: Int, failure: Int) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = "success=$success failure=$failure"
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }

    companion object {
        const val STATUS_OUT_OF_STOCK: String = "결품"
        const val STATUS_DELIVERED: String = "배송 완료"
        const val STATUS_SHIPPING: String = "배송중"
        const val STATUS_PENDING: String = "대기"
        private const val EMPTY_TIME = "000000"

        fun parseAmount(value: String?): Double? {
            val trimmed = value?.trim()
            if (trimmed.isNullOrEmpty()) return 0.0
            return try {
                trimmed.toDouble()
            } catch (_: NumberFormatException) {
                0.0
            }
        }
    }
}
