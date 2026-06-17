package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
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
    val totalApprovedAmount: BigDecimal?,
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
 *
 * 배송 5필드(기사명/차량/연락처/예정시간/완료시간) 는 배송중/배송완료 라인 탭 팝업용
 * (레거시 `view.jsp:141-158`). 빈 값/`'000000'` sentinel 은 `null` 로 매핑하며,
 * 시각은 F18 내 주문 상세(`OrderRequestDetailMapper`)와 동일하게 `HHmmss → HH:mm` 포맷.
 */
data class ClientOrderItemResponse(
    val productCode: String?,
    val productName: String?,
    val deliveredQuantity: String,
    val deliveryStatus: DeliveryStatus,
    val driverName: String?,
    val vehicle: String?,
    val driverPhone: String?,
    val scheduleTime: String?,
    val completeTime: String?
) {
    companion object {
        private const val ZERO_TIME: String = "000000"

        fun from(product: ErpOrderProduct): ClientOrderItemResponse {
            return ClientOrderItemResponse(
                productCode = product.productCode,
                productName = product.productName,
                deliveredQuantity = formatDeliveredQuantity(product.shippingQuantityBox, product.unit),
                deliveryStatus = DeliveryStatus.fromKoreanLabel(product.deliveryStatus),
                driverName = nullIfBlank(product.shippingDriverName),
                vehicle = nullIfBlank(product.shippingVehicle),
                driverPhone = nullIfBlank(product.shippingDriverPhone),
                scheduleTime = formatHHmm(product.shippingScheduleTime),
                completeTime = formatHHmm(product.shippingCompleteTime)
            )
        }

        private fun nullIfBlank(s: String?): String? = if (s.isNullOrBlank()) null else s

        /** SAP `HHmmss` → `HH:mm`. `'000000'` sentinel 또는 빈 값은 `null`. */
        private fun formatHHmm(s: String?): String? {
            if (s.isNullOrBlank() || s == ZERO_TIME) return null
            if (s.length < 4) return s
            return "${s.substring(0, 2)}:${s.substring(2, 4)}"
        }

        private fun formatDeliveredQuantity(quantityBox: BigDecimal?, unit: String?): String {
            val qty = quantityBox ?: BigDecimal.ZERO
            val stripped = qty.stripTrailingZeros()
            val qtyStr = if (stripped.scale() <= 0) stripped.toBigInteger().toString() else stripped.toPlainString()
            return if (unit.isNullOrBlank()) qtyStr else "$qtyStr $unit"
        }
    }
}
