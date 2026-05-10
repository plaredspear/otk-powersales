package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.order.service.ErpOrderUpsertService
import com.otoki.powersales.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderFailure
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderItemDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequestItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP ERP 주문 인바운드 어댑터. (Spec #561 / 어댑터-도메인 분리: #635 P3-B)
 *
 * 책임:
 * - SAP 페이로드 [ErpOrderRequestItem] (헤더 + ItemDetailList) → 도메인 커맨드 [ErpOrderUpsertCommand] 매핑
 * - 도메인 서비스 [ErpOrderUpsertService.upsert] 호출 (헤더+라인 다단 saveAll, 단일 트랜잭션)
 * - 도메인 결과 [ErpOrderUpsertResult] → SAP 응답 [ErpOrderDetail] 매핑 (헤더 카운트만 응답)
 * - [SapInboundAuditService] 감사 기록
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 * 라인 ConstraintViolation 시 도메인 측 트랜잭션 전체 롤백 → 어댑터에서 catch 후 실패 audit + 재전파.
 */
@Service
class SapErpOrderService(
    private val erpOrderUpsertService: ErpOrderUpsertService,
    private val auditService: SapInboundAuditService
) {

    fun upsert(items: List<ErpOrderRequestItem>): ErpOrderDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            erpOrderUpsertService.upsert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        recordAccepted(items.size, success = result.headerSuccessCount, failure = result.failures.size)
        return ErpOrderDetail(
            successCount = result.headerSuccessCount,
            failureCount = result.failures.size,
            failures = result.failures.map { ErpOrderFailure(it.identifier, it.reason) }
        )
    }

    private fun ErpOrderRequestItem.toCommand(): ErpOrderUpsertCommand = ErpOrderUpsertCommand(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = sapAccountCode,
        sapAccountName = sapAccountName,
        deliveryRequestDate = deliveryRequestDate,
        orderDate = orderDate,
        employeeCode = employeeCode,
        employeeName = employeeName,
        orderSalesAmount = orderSalesAmount,
        orderChannel = orderChannel,
        orderChannelNm = orderChannelNm,
        orderType = orderType,
        orderTypeNm = orderTypeNm,
        lines = itemDetailList.orEmpty().map { it.toLineCommand() }
    )

    private fun ErpOrderItemDetail.toLineCommand(): ErpOrderLineCommand = ErpOrderLineCommand(
        sapOrderNumber = sapOrderNumber,
        lineNumber = lineNumber,
        productCode = productCode,
        productName = productName,
        orderQuantity = orderQuantity,
        unit = unit,
        confirmQuantityBox = confirmQuantityBox,
        confirmQuantity = confirmQuantity,
        confirmUnit = confirmUnit,
        defaultReason = defaultReason,
        lineItemStatus = lineItemStatus,
        shippingDriverName = shippingDriverName,
        shippingVehicle = shippingVehicle,
        shippingDriverPhone = shippingDriverPhone,
        shippingScheduleTime = shippingScheduleTime,
        shippingCompleteTime = shippingCompleteTime,
        shippingQuantityBox = shippingQuantityBox,
        shippingQuantity = shippingQuantity,
        orderSalesLineAmount = orderSalesLineAmount,
        shippingAmount = shippingAmount,
        plant = plant,
        plantNm = plantNm,
        releaseQuantity = releaseQuantity,
        releaseAmount = releaseAmount
    )

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
}
