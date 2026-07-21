package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderDetail
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderFailure
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderItemDetail
import com.otoki.powersales.external.sap.inbound.dto.order.ErpOrderRequestItem
import com.otoki.powersales.domain.activity.order.service.ErpOrderUpsertService
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertCommand
import org.springframework.stereotype.Service

/**
 * SAP ERP 주문 인바운드 어댑터. (Spec #561 / 어댑터-도메인 분리: #635 P3-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [ErpOrderRequestItem] (헤더 + ItemDetailList) → 도메인 커맨드 [ErpOrderUpsertCommand] 매핑
 * - 도메인 서비스 [ErpOrderUpsertService.upsert] 호출 (헤더+라인 다단 saveAll, 단일 트랜잭션)
 * - 도메인 결과 → SAP 응답 [ErpOrderDetail] 매핑 (헤더 카운트만 응답)
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639). 라인 ConstraintViolation 시
 * 도메인 측 트랜잭션 전체 롤백 → Aspect 가 catch 후 실패 audit + 재전파.
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 */
@Service
class SapErpOrderService(
    private val erpOrderUpsertService: ErpOrderUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<ErpOrderRequestItem>): ErpOrderDetail {
        val commands = items.map { it.toCommand() }
        val result = erpOrderUpsertService.upsert(commands)
        return ErpOrderDetail(
            successCount = result.headerSuccessCount,
            failureCount = result.failures.size,
            failures = result.failures.map { ErpOrderFailure(it.identifier, it.reason) }
        )
    }

    private fun ErpOrderRequestItem.toCommand(): ErpOrderUpsertCommand = ErpOrderUpsertCommand(
        sapOrderNumber = sapOrderNumber,
        refSapOrderNumber = refSapOrderNumber,
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
        refSapOrderNumber = refSapOrderNumber,
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
}
