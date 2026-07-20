package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.OrderProcessingStatusResponse
import com.otoki.powersales.domain.activity.order.dto.response.ProcessingItemResponse
import com.otoki.powersales.domain.activity.order.dto.response.RejectedItemResponse
import com.otoki.powersales.domain.activity.order.dto.response.UnfulfilledItemResponse
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
import com.otoki.powersales.external.sap.outbound.sender.SapOrderRequestDetailLine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * SAP `OrderRequestDetail` 응답 + CRM `OrderRequestProduct` 라인을 결합하여
 * 주문 상세 응답의 `orderProcessingStatusList[]` / `rejectedItems[]` 를 빌드한다 (Spec #595).
 *
 * 레거시 `IF_REST_MOBILE_OrderRequestDetail.cls` 의 `Item` 파생 규칙을 동등 구현한다:
 * - 라인 derived 상태 5분기는 레거시 `cls:153-159` 의 **독립 if 5개** (`else if` 아님). **마지막 매칭 우선**.
 *   결품(평가 5, `DefaultReason` 채워짐)은 **반려와 달리 별도 분리하지 않고** 정상 라인 그룹에
 *   `deliveryStatus = OUT_OF_STOCK` 로 포함한다 (레거시 처리현황 테이블 `view.jsp:523` 이 반려만
 *   제외 — SAP 주문번호 있는 결품 라인은 상태 '결품' 으로 표시). 동시에 `outOfStockReasons` 로도
 *   수집되어 "주문한 제품" 리스트의 회색+사유 표시에 쓰인다 (`view.jsp:414` 동등 — 레거시는 두 곳
 *   모두에 표시). 반려(평가 1)만 `rejectedItems` 로 분리.
 * - SAP 주문번호별 그룹핑은 `LinkedHashMap` 기반으로 SAP 응답 자연 순서 유지 (Q7 강화).
 * - 빈 `SAPOrderNumber` 정상 라인은 응답 그룹에서 제외 (Q4, 레거시 JSP `view.jsp:494, 500` 동등).
 * - BOX→EA 환산은 레거시 `cls:147,151` 동등 — `ShippingQuantity_Box × Product.BoxReceivingQuantity__c`
 *   (제품 마스터 실측값). `OrderRequestProduct` 등록 시점 스냅샷이 아니다.
 * - 시각(`scheduleTime`/`completeTime`)은 레거시 `cls:141-142` 동등 — SAP 응답 문자열을 **무가공**
 *   으로 전달한다 (`HHmmss→HH:mm` 변환·`'000000'` sentinel 제거 없음).
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
        // 미납(신규 정책): SAPOrderNumber 있는 라인 중 LineItemStatus 채워짐 && != "OK".
        val unfulfilled = mutableListOf<UnfulfilledItemResponse>()
        // 결품(DefaultReason 채워짐) productCode → 사유. 레거시 `cls:158`/`view.jsp:414` 동등 —
        // 결품은 처리현황 그룹/반려가 아니라 "주문한 제품"(orderedItems) 에 회색 표시되므로,
        // service 가 orderedItems 빌드 시 본 맵으로 결품 플래그를 주입한다.
        val outOfStockReasons = LinkedHashMap<String, String>()
        // LinkedHashMap → SAP 응답 자연 순서 유지 (Q7).
        val groups = LinkedHashMap<String, MutableList<ProcessingItemResponse>>()
        var emptyKeyCount = 0

        for (sapLine in effective) {
            val productCode = sapLine.productCode.orEmpty()
            val crmProduct = crmProductsByCode[productCode]

            val productName = crmProduct?.product?.name ?: sapLine.productName.orEmpty()

            // 결품(평가 5, 마지막 매칭 우선): DefaultReason 채워짐 → 반려 분리 대상에서 제외하고
            // (레거시 cls:158 이 cls:154 반려 판정을 덮어씀) orderedItems 결품 플래그용으로 수집
            // (레거시 화면은 "주문한 제품" 리스트에 회색 표시, view.jsp:414). 처리현황 그룹에는
            // 상태 '결품' 으로 **포함**한다 — 레거시 처리현황 테이블(view.jsp:523)은 반려만 제외.
            val isOutOfStock = !sapLine.defaultReason.isNullOrEmpty()
            if (isOutOfStock) {
                outOfStockReasons.putIfAbsent(productCode, sapLine.defaultReason.orEmpty())
            } else if (sapLine.sapOrderNumber.isNullOrEmpty() && !sapLine.lineItemStatus.isNullOrEmpty()) {
                // 반려(평가 1): SAPOrderNumber 빈 값 + LineItemStatus 채워짐 → 별도 섹션 분리.
                // 레거시 `view.jsp:449-486` "주문 반려 제품" 별도 영역 동등.
                rejected += RejectedItemResponse(
                    productCode = productCode,
                    productName = productName,
                    orderQuantityBoxes = crmProduct?.quantityBoxes ?: BigDecimal.ZERO,
                    rejectionReason = sapLine.lineItemStatus,
                )
                continue
            }

            // 그룹 키 결정 — 빈 SAPOrderNumber 라인(결품 포함)은 그룹에서 제외 (view.jsp:494, 500 동등).
            val groupKey = sapLine.sapOrderNumber ?: ""
            if (groupKey.isEmpty()) {
                emptyKeyCount++
                continue
            }

            // 미납(신규 정책, 2026-07-20 사용자 결정 — SF 레거시엔 없던 분류): SAP 주문번호 있는 라인 중
            // LineItemStatus 채워짐 && != "OK". 결품(DefaultReason)은 제외(기존 결품 표시 유지),
            // 빈 값(정상 대기 라인)도 제외. 라인 자체는 처리현황 그룹에도 그대로 남는다(이중 표시).
            if (!isOutOfStock &&
                !sapLine.lineItemStatus.isNullOrEmpty() &&
                sapLine.lineItemStatus != NORMAL_LINE_ITEM_STATUS
            ) {
                unfulfilled += UnfulfilledItemResponse(
                    productCode = productCode,
                    productName = productName,
                    orderQuantityBoxes = crmProduct?.quantityBoxes ?: BigDecimal.ZERO,
                    reason = sapLine.lineItemStatus,
                )
            }

            val deliveryStatus = if (isOutOfStock) DeliveryStatus.OUT_OF_STOCK else computeDeliveryStatus(sapLine)
            val deliveredQuantity = formatDeliveredQuantity(sapLine, crmProduct, requestNumber)

            val item = ProcessingItemResponse(
                productCode = productCode,
                productName = productName,
                deliveredQuantity = deliveredQuantity,
                deliveryStatus = deliveryStatus,
                driverName = nullIfBlank(sapLine.shippingDriverName),
                vehicle = nullIfBlank(sapLine.shippingVehicle),
                driverPhone = nullIfBlank(sapLine.shippingDriverPhone),
                // 레거시 `cls:141-142` 동등 — SAP 응답 시각 문자열 무가공 전달 ('000000' 포함, HH:mm 변환 없음).
                scheduleTime = nullIfBlank(sapLine.shippingScheduleTime),
                completeTime = nullIfBlank(sapLine.shippingCompleteTime),
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

        return MapResult(
            processingGroups = processingGroups,
            rejectedItems = rejected.toList(),
            outOfStockReasons = outOfStockReasons.toMap(),
            unfulfilledItems = unfulfilled.toList(),
        )
    }

    /**
     * 레거시 `cls:153-159` 의 **독립 if 5개** 중 처리현황 그룹에 들어오는 라인의 배송 차원만 매핑한다.
     * 결품(평가 5)은 [map] 에서 `OUT_OF_STOCK` 으로 직접 지정되고 반려(평가 1)는 분리되었으므로
     * 여기서는 대기/배송중/배송완료 + 빈상태만 판정.
     *
     * 레거시는 `String status=''` 에서 시작해 5개 독립 if 를 순차 실행하고 **아래 if 일수록 우선**(마지막
     * 참 조건이 최종값)이다. 여기서는 결품(158)/반려(154)가 이미 분리됐으므로 155~157 만 재현한다.
     * SF 원문 (조회 클래스, 공백 없는 '배송완료'):
     *  - cls:155 (대기): `DefaultReason 빈 && LineItemStatus 빈 && ScheduleTime(빈|000000)`
     *      (DefaultReason 빈은 정상 라인이라 이미 성립 → LineItemStatus 빈 && ScheduleTime 미도래)
     *  - cls:156 (배송중): `ScheduleTime≠000000 && CompleteTime(빈|000000)`
     *  - cls:157 (배송완료): `CompleteTime≠000000`
     *  - 어느 것도 아니면 status='' → [DeliveryStatus.UNKNOWN] (예: LineItemStatus 만 채워지고 시각 없음).
     *    과거 신규는 이 케이스를 '대기'로 뭉갰으나 SF 는 빈 문자열로 남긴다 → 정합 위해 UNKNOWN 구분.
     */
    private fun computeDeliveryStatus(line: SapOrderRequestDetailLine): DeliveryStatus {
        val scheduleFilled = !line.shippingScheduleTime.isNullOrEmpty() && line.shippingScheduleTime != ZERO_TIME
        val completeFilled = !line.shippingCompleteTime.isNullOrEmpty() && line.shippingCompleteTime != ZERO_TIME
        val lineItemStatusFilled = !line.lineItemStatus.isNullOrEmpty()

        // cls:157 (배송완료) — 최우선
        if (completeFilled) return DeliveryStatus.DELIVERED
        // cls:156 (배송중)
        if (scheduleFilled) return DeliveryStatus.SHIPPING
        // cls:155 (대기) — LineItemStatus 빈 && ScheduleTime 미도래. (DefaultReason 빈은 정상 라인 전제로 성립)
        if (!lineItemStatusFilled) return DeliveryStatus.PENDING
        // 어느 조건에도 안 걸림 → 레거시 status='' 정합.
        return DeliveryStatus.UNKNOWN
    }

    /**
     * `"<box> BOX (<ea> EA)"` 또는 EA 미포함 시 `"<box> BOX"`.
     *
     * **출하수량 폴백 (레거시 `view.jsp:527-536` 동등)**: SAP `ShippingQuantity_Box` 가
     * 없으면(null/빈문자/파싱불가) 출하수량 대신 **주문수량**(`TotalQuantity_Box` / `TotalQuantity`)
     * 으로 대체 표시한다. 레거시는 출하수량이 아직 안 온 라인을 `0 BOX` 가 아니라 주문한 만큼으로
     * 보여줬다(납품문서 생성 전 가정). 과거 신규 구현은 이 폴백을 누락해 완료 건도 `0 BOX` 로
     * 오표시하는 회귀가 있었다.
     *
     * EA 환산:
     *  - 출하수량 경로: 레거시 `cls:147,151` 동등 — `ShippingQuantity_Box` × 제품 마스터
     *    `Product.BoxReceivingQuantity__c`(박스입수량 실측값). 등록 시점 스냅샷이 아니라 조회 시점 값.
     *  - 주문수량 폴백 경로: 레거시 `view.jsp:533` 동등 — SAP 응답 `TotalQuantity`(EA)를 그대로 사용
     *    (박스입수량 재환산이 아니라 SAP 가 준 EA 값).
     */
    private fun formatDeliveredQuantity(
        line: SapOrderRequestDetailLine,
        crmProduct: OrderRequestProduct?,
        requestNumber: String,
    ): String {
        val shippingBox = parseDecimalOrNull(line.shippingQuantityBox)
        if (shippingBox != null) {
            // 출하수량 있음 — box × 박스입수량으로 EA 환산 (레거시 cls:147,151).
            val boxReceivingQuantity = crmProduct?.product?.boxReceivingQuantity
            if (boxReceivingQuantity == null || boxReceivingQuantity <= BigDecimal.ZERO) {
                log.warn(
                    "order-request.detail.invalid-box-receiving-quantity requestNumber={} productCode={} box_receiving_quantity={}",
                    requestNumber, line.productCode, boxReceivingQuantity,
                )
                return "${shippingBox.stripTrailingZeros().toPlainString()} BOX"
            }
            val ea = shippingBox.multiply(boxReceivingQuantity)
            return "${shippingBox.stripTrailingZeros().toPlainString()} BOX (${ea.stripTrailingZeros().toPlainString()} EA)"
        }

        // 출하수량 없음 — 주문수량(TotalQuantity_Box / TotalQuantity)으로 폴백 (레거시 view.jsp:532-535).
        if (!line.shippingQuantityBox.isNullOrEmpty()) {
            log.warn(
                "sap.outbound.detail.invalid-shipping-qty requestNumber={} value={}",
                requestNumber, line.shippingQuantityBox,
            )
        }
        val orderBox = parseDecimalOrNull(line.totalQuantityBox) ?: BigDecimal.ZERO
        val orderEa = parseDecimalOrNull(line.totalQuantity)
        return if (orderEa != null) {
            "${orderBox.stripTrailingZeros().toPlainString()} BOX (${orderEa.stripTrailingZeros().toPlainString()} EA)"
        } else {
            "${orderBox.stripTrailingZeros().toPlainString()} BOX"
        }
    }

    /**
     * SD03052 응답 기준 **주문 전체가 납품완료** 인지 판정한다 (취소 버튼 비활성화용).
     *
     * 취소 시점(마감 전)에는 처리현황 그룹이 응답에서 null 로 강제되지만, SAP 호출 자체는 마감 여부와
     * 무관하게 수행되므로([OrderRequestService] `fetchDetail`) 원본 [sapLines] 로 납품완료를 판정할 수 있다.
     *
     * 판정 규칙:
     *  - 결품(`DefaultReason` 채워짐)·반려(`SAPOrderNumber` 빈값 && `LineItemStatus` 채워짐) 라인은 제외.
     *  - 남은 **정상 배송 대상 라인이 1개 이상** 있고, 그 **전부가 배송완료**(`ShippingCompleteTime`
     *    ≠null/빈/'000000') 이면 true.
     *  - 정상 라인이 하나도 없으면(전부 결품/반려, 또는 응답 없음) false — 납품완료로 단정하지 않는다.
     *
     * 레거시엔 없던 신규 취소 가드 — SAP 가 이미 납품완료된 주문의 OrderChange(취소)를 거부하는데,
     * 로컬 상태(order_request)로는 이를 알 수 없어 취소 버튼이 떴다가 클릭 시 실패하던 문제를 선제 차단.
     */
    fun isFullyDelivered(sapLines: List<SapOrderRequestDetailLine>?): Boolean {
        if (sapLines.isNullOrEmpty()) return false
        val deliverableLines = sapLines.filter { line ->
            val isOutOfStock = !line.defaultReason.isNullOrEmpty()
            val isRejected = line.sapOrderNumber.isNullOrEmpty() && !line.lineItemStatus.isNullOrEmpty()
            !isOutOfStock && !isRejected
        }
        if (deliverableLines.isEmpty()) return false
        return deliverableLines.all { line ->
            !line.shippingCompleteTime.isNullOrEmpty() && line.shippingCompleteTime != ZERO_TIME
        }
    }

    /**
     * 총 승인 금액 — SAP 응답 **전 라인**의 `OrderSalesAmount` 단순 합산 (레거시 `view.jsp:343-348` 동등).
     *
     * 레거시는 반려/결품 제외 없이 itemList 전 라인을 EL 로 누적했고, null 값은 0 으로 취급됐다
     * (Apex `String.valueOf(null)` 은 null → JSP EL 산술에서 0). 여기서도 null/비숫자 값은 0 으로 간주.
     * SAP 호출 실패([sapLines] null)/빈 응답이면 0 — 레거시도 `resultCode != 'S'` 시 itemList 미생성으로
     * sum=0 이 표시됐다. DB `order_request.total_approved_amount` 컬럼은 사용하지 않는다 (레거시 정합).
     */
    fun sumApprovedAmount(sapLines: List<SapOrderRequestDetailLine>?): BigDecimal {
        if (sapLines.isNullOrEmpty()) return BigDecimal.ZERO
        return sapLines.fold(BigDecimal.ZERO) { acc, line ->
            acc + (parseDecimalOrNull(line.orderSalesAmount) ?: BigDecimal.ZERO)
        }
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

    data class MapResult(
        val processingGroups: List<OrderProcessingStatusResponse>,
        val rejectedItems: List<RejectedItemResponse>,
        /** 결품 productCode → DefaultReason. orderedItems 결품 플래그 주입용 (레거시 화면 "주문한 제품" 회색 표시 동등). */
        val outOfStockReasons: Map<String, String> = emptyMap(),
        /** 미납 라인 (SAPOrderNumber 있음 && LineItemStatus 채워짐 && != "OK") — 신규 정책, 별도 섹션 표시용. */
        val unfulfilledItems: List<UnfulfilledItemResponse> = emptyList(),
    )

    companion object {
        const val MAX_LINES: Int = 1000
        private const val ZERO_TIME: String = "000000"

        /** SAP `LineItemStatus` 정상 값 — 이 값이 아니면서 채워져 있으면 미납 (신규 정책). */
        private const val NORMAL_LINE_ITEM_STATUS: String = "OK"
    }
}
