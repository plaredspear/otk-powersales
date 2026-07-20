package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
import java.math.BigDecimal
import java.text.DecimalFormat
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
    val ordererName: String?,
    val ordererCode: String?,
    val orderedItemCount: Int,
    val orderedItems: List<ClientOrderItemResponse>
) {
    companion object {
        /**
         * 모바일 `ClientOrderInfoHeader` 가 표시하는 정적 마감 시각 라벨.
         * 레거시 JSP `view.jsp:58` 의 정적 문자열과 동등 (Q1 옵션 3 정합).
         */
        const val CLIENT_DEADLINE_TIME: String = "13:50"

        /**
         * @param ordererName 주문자 사번으로 Employee 마스터에서 해석한 실제 주문자명.
         *   `erp_order.employee_name`(SF `EmployeeName__c`)은 시스템 계정명이 적재되는 경우가 있어 신뢰하지 않고,
         *   서비스가 `employee_code` 로 조회한 이름을 주입한다. 미해석 시 `order.employeeName` 로 폴백.
         */
        fun from(
            order: ErpOrder,
            products: List<ErpOrderProduct>,
            ordererName: String?,
        ): ClientOrderDetailResponse {
            val items = products.map(ClientOrderItemResponse::from)
            return ClientOrderDetailResponse(
                sapOrderNumber = order.sapOrderNumber,
                sapAccountCode = order.sapAccountCode,
                sapAccountName = order.sapAccountName,
                clientDeadlineTime = CLIENT_DEADLINE_TIME,
                orderDate = order.orderDate,
                deliveryDate = order.deliveryRequestDate,
                totalApprovedAmount = order.orderSalesAmount,
                // 주문자(사원) — 거래처별 주문은 담당자 무관 전체 노출이라 라인마다 주문자가 다를 수 있어 헤더에 명시.
                ordererName = ordererName,
                ordererCode = order.employeeCode,
                orderedItemCount = items.size,
                orderedItems = items
            )
        }
    }
}

/**
 * 거래처 출하 주문 상세 - 라인 응답.
 *
 * `deliveredQuantity` 는 `confirmQuantityBox`(총납품수량 Box환산치) 를 표시 문자열로 조립 (예: `"10 BOX"`).
 * 레거시 `view.jsp:89` 가 `${data.ConfirmQuantity_Box}` 를 표시하며, 배송수량(`shippingQuantityBox`)이 아님 —
 * 배송 전(대기/결품) 라인은 배송수량이 0 이므로 배송수량을 쓰면 전 라인이 `0 BOX` 로 오표시됨.
 * `deliveryStatus` 는 DB 한글 라벨을 [DeliveryStatus] enum 으로 변환한 영문 코드.
 *
 * `shippedQuantity` 는 실제 출하량 `"N BOX (M EA)"` (신규 추가, 2026-07-20 사용자 결정 — 레거시는 화면
 * 미표시 hidden input 만 보유). BOX = `shippingQuantityBox`(SAP `ShippingQuantity_Box`), EA =
 * `shippingQuantity`(SAP `ShippingQuantity`, 최소주문단위) 둘 다 SAP 원본 적재값이라 제품마스터 재환산 없음.
 * 배송 전 라인은 두 값 모두 null/0 이라 `"0 BOX (0 EA)"` 로 표기(납품수량 `deliveredQuantity` 와 별도 행).
 *
 * 배송 5필드(기사명/차량/연락처/예정시간/완료시간) 는 배송중/배송완료 라인 탭 팝업용
 * (레거시 `view.jsp:141-158`). 빈 값/`'000000'` sentinel 은 `null` 로 매핑하며,
 * 시각은 F18 내 주문 상세(`OrderRequestDetailMapper`)와 동일하게 `HHmmss → HH:mm` 포맷.
 */
data class ClientOrderItemResponse(
    val productCode: String?,
    val productName: String?,
    val deliveredQuantity: String,
    val shippedQuantity: String,
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
                deliveredQuantity = formatDeliveredQuantity(product.confirmQuantityBox),
                shippedQuantity = formatShippedQuantity(product.shippingQuantityBox, product.shippingQuantity),
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

        /**
         * 레거시 `view.jsp:89` 정합: `<fmt:formatNumber value="${data.ConfirmQuantity_Box}" pattern="#,###.##"/> BOX`.
         * 천단위 구분 + 소수 최대 2자리(후행 0 제거) 로 포맷한 뒤 리터럴 `"BOX"` 를 붙인다.
         * 레거시가 동적 단위(`Confirm_Unit`)를 주석 처리하고 `"BOX"` 를 하드코딩하므로 단위는 항상 `"BOX"` 로 고정.
         * `DecimalFormat` 은 thread-safe 하지 않으므로 호출마다 생성.
         */
        private fun formatDeliveredQuantity(quantityBox: BigDecimal?): String {
            val qty = quantityBox ?: BigDecimal.ZERO
            return "${DecimalFormat("#,###.##").format(qty)} BOX"
        }

        /**
         * 배송수량 `"N BOX (M EA)"` (신규, 2026-07-20 사용자 결정).
         * BOX/EA 모두 SAP 원본 적재값(`shipping_quantity_box`/`shipping_quantity`) 사용 — 제품마스터 재환산 없음.
         * 배송 전(대기/결품) 라인은 두 값 모두 null → `"0 BOX (0 EA)"` (배송수량은 confirm 과 달리 실제 출하량이라 0 이 정상).
         */
        private fun formatShippedQuantity(box: BigDecimal?, ea: BigDecimal?): String {
            val fmt = DecimalFormat("#,###.##")
            val boxStr = fmt.format(box ?: BigDecimal.ZERO)
            val eaStr = fmt.format(ea ?: BigDecimal.ZERO)
            return "$boxStr BOX ($eaStr EA)"
        }
    }
}
