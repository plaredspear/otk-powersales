package com.otoki.powersales.domain.activity.order.sap

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import org.springframework.stereotype.Component

/**
 * 주문 취소 SAP 송신 페이로드 팩토리 (Spec #597).
 *
 * 레거시 `IF_REST_SAP_OrderChange` 송신 본문 동등 (`IF_REST_MOBILE_OrderCancelRequest.cls:84-99`).
 * `LineChangeType` 은 모든 라인에 고정 `"X"`.
 *
 * sap-integration.md §11.2 — 도메인 → SAP 페이로드 변환은 `<domain>/sap/` 위치 의무.
 */
@Component
class OrderRequestCancelPayloadFactory {

    fun build(orderRequest: OrderRequest, products: List<OrderRequestProduct>): Map<String, Any?> {
        return mapOf(
            "RequestNumber" to orderRequest.orderRequestNumber,
            "reqItemList" to products.map { p ->
                mapOf(
                    "LineNumber" to p.lineNumber.toString(),
                    "ProductCode" to p.productCode,
                    "LineChangeType" to LINE_CHANGE_TYPE_CANCEL,
                )
            },
        )
    }

    companion object {
        const val LINE_CHANGE_TYPE_CANCEL: String = "X"
    }
}
