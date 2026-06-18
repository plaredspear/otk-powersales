package com.otoki.powersales.domain.activity.order.sap

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import org.springframework.stereotype.Component

/**
 * 주문 취소 SAP 송신 페이로드 팩토리 (Spec #597).
 *
 * 레거시 `IF_REST_SAP_OrderChange` 송신 본문 동등 (`IF_REST_MOBILE_OrderCancelRequest.cls:76, 95-96`).
 *
 * 레거시 `jsonBody` 구조 — 최상위 2키:
 *  - `reqItemList`: 헤더 객체 `{ RequestNumber }` (cls:76 `new REQUEST_List_header`)
 *  - `ItemList`: 라인 배열 `[{ LineNumber, ProductCode, LineChangeType }]` (cls:95)
 *
 * `LineNumber` 는 레거시 `Decimal` 동등으로 숫자(BigDecimal) 그대로 직렬화 (따옴표 없음).
 * `LineChangeType` 은 모든 라인에 고정 `"X"` — 레거시도 첫 취소 시 `'X'` 로 귀결.
 *
 * sap-integration.md §11.2 — 도메인 → SAP 페이로드 변환은 `<domain>/sap/` 위치 의무.
 */
@Component
class OrderRequestCancelPayloadFactory {

    fun build(orderRequest: OrderRequest, products: List<OrderRequestProduct>): Map<String, Any?> {
        return mapOf(
            "reqItemList" to mapOf(
                "RequestNumber" to orderRequest.orderRequestNumber,
            ),
            "ItemList" to products.map { p ->
                mapOf(
                    "LineNumber" to p.lineNumber,
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
