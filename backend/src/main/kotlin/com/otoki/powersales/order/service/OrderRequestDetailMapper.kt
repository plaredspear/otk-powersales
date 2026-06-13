package com.otoki.powersales.order.service

import com.otoki.powersales.order.dto.response.OrderProcessingStatusResponse
import com.otoki.powersales.order.dto.response.ProcessingItemResponse
import com.otoki.powersales.order.dto.response.RejectedItemResponse
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.external.sap.outbound.sender.SapOrderRequestDetailLine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * SAP `OrderRequestDetail` 응답 + CRM `OrderRequestProduct` 라인을 결합하여
 * 주문 상세 응답의 `orderProcessingStatusList[]` / `rejectedItems[]` 를 빌드한다 (Spec #595).
 *
 * - 라인 derived 상태 5분기는 레거시 `IF_REST_MOBILE_OrderRequestDetail.cls:153-159` 의 **독립 if 5개**
 *   (`else if` 아님) 를 동등 구현. **마지막 매칭 우선**.
 * - SAP 주문번호별 그룹핑은 `LinkedHashMap` 기반으로 SAP 응답 자연 순서 유지 (Q7 강화).
 * - 빈 `SAPOrderNumber` 정상 라인은 응답 그룹에서 제외 (Q4, 레거시 JSP `view.jsp:494, 500` 동등).
 * - BOX→EA 환산은 `OrderRequestProduct.pieces_per_box` 등록 시점 스냅샷 사용 (Q `bx-ea-correction-policy` TODO).
 */
@Component
class OrderRequestDetailMapper {

    private val log = LoggerFactory.getLogger(OrderRequestDetailMapper::class.java)

    /**
     * SAP 응답 라인 + CRM 라인을 결합하여 그룹/반려 결과를 산출한다.
     */
    fun map(
        requestNumber: String,
        sapLines: List<SapOrderRequestDetailLine>,
        crmProductsByCode: Map<String, OrderRequestProduct>,
    ): MapResult {
        if (sapLines.isEmpty()) {
            return MapResult(processingGroups = emptyList(), rejectedItems = emptyList())
        }

        val truncated = sapLines.size > MAX_LINES
        val effective = if (truncated) sapLines.subList(0, MAX_LINES) else sapLines
        if (truncated) {
            log.warn(
                "sap.outbound.detail.line-overflow requestNumber={} total_lines={}",
                requestNumber, sapLines.size,
            )
        }

        val rejected = mutableListOf<RejectedItemResponse>()
        // LinkedHashMap → SAP 응답 자연 순서 유지 (Q7).
        val groups = LinkedHashMap<String, MutableList<ProcessingItemResponse>>()
        var emptyKeyCount = 0

        for (sapLine in effective) {
            val productCode = sapLine.productCode.orEmpty()
            val crmProduct = crmProductsByCode[productCode]

            val productName = crmProduct?.product?.name ?: sapLine.productName.orEmpty()

            // 결품: DefaultReason 채워짐 (평가 5 — 마지막 매칭 우선)
            if (!sapLine.defaultReason.isNullOrEmpty()) {
                rejected += RejectedItemResponse(
                    productCode = productCode,
                    productName = productName,
                    orderQuantityBoxes = crmProduct?.quantityBoxes ?: BigDecimal.ZERO,
                    rejectionReason = sapLine.defaultReason,
                )
                continue
            }

            // 반려: SAPOrderNumber 빈 값 + LineItemStatus 채워짐 (평가 1, 결품 매칭 안 됐을 때)
            if (sapLine.sapOrderNumber.isNullOrEmpty() && !sapLine.lineItemStatus.isNullOrEmpty()) {
                rejected += RejectedItemResponse(
                    productCode = productCode,
                    productName = productName,
                    orderQuantityBoxes = crmProduct?.quantityBoxes ?: BigDecimal.ZERO,
                    rejectionReason = sapLine.lineItemStatus,
                )
                continue
            }

            // 정상 라인 — 그룹 키 결정
            val groupKey = sapLine.sapOrderNumber ?: ""
            if (groupKey.isEmpty()) {
                emptyKeyCount++
                continue
            }

            val deliveryStatus = computeDeliveryStatus(sapLine)
            val deliveredQuantity = formatDeliveredQuantity(sapLine, crmProduct, requestNumber)

            val item = ProcessingItemResponse(
                productCode = productCode,
                productName = productName,
                deliveredQuantity = deliveredQuantity,
                deliveryStatus = deliveryStatus,
                driverName = nullIfBlank(sapLine.shippingDriverName),
                vehicle = nullIfBlank(sapLine.shippingVehicle),
                driverPhone = nullIfBlank(sapLine.shippingDriverPhone),
                scheduleTime = formatHHmm(sapLine.shippingScheduleTime),
                completeTime = formatHHmm(sapLine.shippingCompleteTime),
            )

            groups.computeIfAbsent(groupKey) { mutableListOf() } += item
        }

        if (emptyKeyCount > 0) {
            log.warn(
                "sap.outbound.detail.empty-sap-order-number requestNumber={} empty_line_count={}",
                requestNumber, emptyKeyCount,
            )
        }

        // 동일 ProductCode 라인 중복 INFO 로그 (Q3 — 그룹 내 중복 보존, 운영 빈도 모니터링)
        groups.forEach { (sapOrderNumber, items) ->
            items.groupingBy { it.productCode }.eachCount()
                .filterValues { it > 1 }
                .forEach { (productCode, count) ->
                    log.info(
                        "sap.outbound.detail.duplicate-product requestNumber={} sapOrderNumber={} productCode={} line_count={}",
                        requestNumber, sapOrderNumber, productCode, count,
                    )
                }
        }

        if (groups.size > 1) {
            log.info(
                "sap.outbound.detail.multi-split requestNumber={} distinct_sap_order_count={}",
                requestNumber, groups.size,
            )
        }

        val processingGroups = groups.map { (sapOrderNumber, items) ->
            OrderProcessingStatusResponse(sapOrderNumber = sapOrderNumber, items = items.toList())
        }

        return MapResult(processingGroups = processingGroups, rejectedItems = rejected.toList())
    }

    /**
     * 레거시 `cls:153-159` 의 **독립 if 5개** 동등. 평가 순서 = 소스 순서이며 마지막 매칭이 우선.
     * 결품(평가 5) / 반려(평가 1) 는 [map] 에서 분리 처리하므로 여기서는 배송 차원 3개만 매핑.
     */
    private fun computeDeliveryStatus(line: SapOrderRequestDetailLine): DeliveryStatus {
        val scheduleFilled = !line.shippingScheduleTime.isNullOrEmpty() && line.shippingScheduleTime != ZERO_TIME
        val completeFilled = !line.shippingCompleteTime.isNullOrEmpty() && line.shippingCompleteTime != ZERO_TIME

        // 평가 4 (마지막 매칭 — 배송완료 우선)
        if (completeFilled) return DeliveryStatus.DELIVERED
        // 평가 3
        if (scheduleFilled) return DeliveryStatus.SHIPPING
        // 평가 2 (대기) — 그 외 모든 경우 fallback
        return DeliveryStatus.PENDING
    }

    /**
     * `"<box> BOX (<ea> EA)"` 또는 EA 미포함 시 `"<box> BOX"`.
     *
     * EA 환산: SAP `ShippingQuantity_Box` × `OrderRequestProduct.pieces_per_box` (스냅샷, Spec #592).
     */
    private fun formatDeliveredQuantity(
        line: SapOrderRequestDetailLine,
        crmProduct: OrderRequestProduct?,
        requestNumber: String,
    ): String {
        val boxRaw = line.shippingQuantityBox.orEmpty()
        val box = parseDecimalOrNull(boxRaw) ?: run {
            if (boxRaw.isNotEmpty()) {
                log.warn(
                    "sap.outbound.detail.invalid-shipping-qty requestNumber={} value={}",
                    requestNumber, boxRaw,
                )
            }
            BigDecimal.ZERO
        }

        val piecesPerBox = crmProduct?.piecesPerBox ?: 0
        if (piecesPerBox <= 0) {
            log.warn(
                "order-request.detail.invalid-pieces-per-box requestNumber={} productCode={} pieces_per_box={}",
                requestNumber, line.productCode, piecesPerBox,
            )
            return "${box.stripTrailingZeros().toPlainString()} BOX"
        }

        val ea = box.multiply(BigDecimal(piecesPerBox))
        return "${box.stripTrailingZeros().toPlainString()} BOX (${ea.stripTrailingZeros().toPlainString()} EA)"
    }

    private fun parseDecimalOrNull(s: String?): BigDecimal? {
        if (s.isNullOrBlank()) return null
        return try {
            BigDecimal(s)
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun nullIfBlank(s: String?): String? = if (s.isNullOrBlank()) null else s

    /**
     * SAP `HHmmss` 6자리 → `HH:mm`. `'000000'` sentinel 또는 빈 값은 `null`.
     */
    private fun formatHHmm(s: String?): String? {
        if (s.isNullOrBlank() || s == ZERO_TIME) return null
        // 6자리 길이 가정. 유연 처리 — 짧으면 그대로 반환.
        if (s.length < 4) return s
        return "${s.substring(0, 2)}:${s.substring(2, 4)}"
    }

    data class MapResult(
        val processingGroups: List<OrderProcessingStatusResponse>,
        val rejectedItems: List<RejectedItemResponse>,
    )

    companion object {
        const val MAX_LINES: Int = 1000
        private const val ZERO_TIME: String = "000000"
    }
}
