package com.otoki.powersales.order.dto.response

import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 거래처별 출하 주문 상세 응답 (Spec #593).
 *
 * 레거시 SF `IF_REST_MOBILE_ClientOrderDetail` 응답 19개 라인 필드 중
 * 모바일이 실제로 사용하는 4개 라인 필드(`productCode`/`productName`/`deliveredQuantity`/`deliveryStatus`)만 노출.
 *
 * `clientDeadlineTime` 은 모바일 화면(`ClientOrderInfoHeader`)의 정적 표시값(`"13:50"`).
 * 마감 시각 판정(`deadlineType`/`cancellable`)은 본 스펙 비범위 — 단일 권위는 #598.
 */
data class ClientOrderDetailResponse(
    val sapOrderNumber: String,
    val sapAccountCode: String?,
    val sapAccountName: String?,
    val clientDeadlineTime: String?,
    val orderDate: LocalDate?,
    val deliveryDate: LocalDate?,
    val totalApprovedAmount: Long?,
    val orderedItemCount: Int,
    val orderedItems: List<ClientOrderItemResponse>
) {
    companion object {
        /**
         * 모바일 `ClientOrderInfoHeader` 가 표시하는 정적 마감 시각 라벨.
         * 레거시 JSP `view.jsp:58` 의 정적 문자열과 동등 (Q1 옵션 3 정합).
         */
        const val CLIENT_DEADLINE_TIME: String = "13:50"

        fun from(order: ErpOrder, products: List<ErpOrderProduct>): ClientOrderDetailResponse {
            val items = products.map(ClientOrderItemResponse::from)
            return ClientOrderDetailResponse(
                sapOrderNumber = order.sapOrderNumber,
                sapAccountCode = order.sapAccountCode,
                sapAccountName = order.sapAccountName,
                clientDeadlineTime = CLIENT_DEADLINE_TIME,
                orderDate = order.orderDate,
                deliveryDate = order.deliveryRequestDate,
                totalApprovedAmount = order.orderSalesAmount,
                orderedItemCount = items.size,
                orderedItems = items
            )
        }
    }
}

/**
 * 거래처 출하 주문 상세 - 라인 응답.
 *
 * `deliveredQuantity` 는 `shippingQuantityBox` + `unit` 조립한 표시 문자열 (예: `"10 BOX"`).
 * `deliveryStatus` 는 DB 한글 라벨을 [DeliveryStatus] enum 으로 변환한 영문 코드.
 */
data class ClientOrderItemResponse(
    val productCode: String?,
    val productName: String?,
    val deliveredQuantity: String,
    val deliveryStatus: DeliveryStatus
) {
    companion object {
        fun from(product: ErpOrderProduct): ClientOrderItemResponse {
            return ClientOrderItemResponse(
                productCode = product.productCode,
                productName = product.productName,
                deliveredQuantity = formatDeliveredQuantity(product.shippingQuantityBox, product.unit),
                deliveryStatus = DeliveryStatus.fromKoreanLabel(product.deliveryStatus)
            )
        }

        private fun formatDeliveredQuantity(quantityBox: BigDecimal?, unit: String?): String {
            val qty = quantityBox ?: BigDecimal.ZERO
            val stripped = qty.stripTrailingZeros()
            val qtyStr = if (stripped.scale() <= 0) stripped.toBigInteger().toString() else stripped.toPlainString()
            return if (unit.isNullOrBlank()) qtyStr else "$qtyStr $unit"
        }
    }
}
