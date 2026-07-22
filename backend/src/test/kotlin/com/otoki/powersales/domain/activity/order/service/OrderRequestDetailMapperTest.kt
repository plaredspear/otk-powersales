package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.external.sap.outbound.sender.SapOrderRequestDetailLine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("OrderRequestDetailMapper 테스트 (#595 P1-B)")
class OrderRequestDetailMapperTest {

    private val mapper = OrderRequestDetailMapper()
    private val requestNumber = "OR-0001234"

    @Test
    @DisplayName("정상 — 단일 SAPOrderNumber, derived = DELIVERED + BOX→EA 환산(boxReceivingQuantity), 시각 무가공")
    fun singleGroupDelivered() {
        val sap = listOf(
            line(
                productCode = "1000023",
                sapOrderNumber = "0300004993",
                shippingScheduleTime = "120000",
                shippingCompleteTime = "143000",
                shippingQuantityBox = "10",
            ),
        )
        val crm = mapOf("1000023" to product("1000023", "진라면 매운맛", boxReceivingQuantity = 30))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.processingGroups).hasSize(1)
        val group = result.processingGroups[0]
        assertThat(group.sapOrderNumber).isEqualTo("0300004993")
        assertThat(group.items).hasSize(1)
        val item = group.items[0]
        assertThat(item.productCode).isEqualTo("1000023")
        assertThat(item.productName).isEqualTo("진라면 매운맛")
        assertThat(item.deliveryStatus).isEqualTo(DeliveryStatus.DELIVERED)
        assertThat(item.deliveredQuantity).isEqualTo("10 BOX (300 EA)")
        // 레거시 cls:141-142 무가공 — HHmmss 그대로
        assertThat(item.scheduleTime).isEqualTo("120000")
        assertThat(item.completeTime).isEqualTo("143000")
        assertThat(result.rejectedItems).isEmpty()
    }

    @Test
    @DisplayName("derived = SHIPPING — schedule 채워짐 + complete '000000', 시각 무가공('000000'은 빈값 아님→유지)")
    fun deliveryStatusShipping() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingScheduleTime = "120000",
                shippingCompleteTime = "000000",
                shippingQuantityBox = "5",
            ),
        )
        val result = mapper.map(requestNumber, sap, mapOf("P1" to product("P1", "P1Name", 10)))
        val item = result.processingGroups[0].items[0]
        assertThat(item.deliveryStatus).isEqualTo(DeliveryStatus.SHIPPING)
        // 레거시 무가공 — '120000' 그대로, '000000' 도 빈 문자열이 아니라 그대로 유지
        assertThat(item.scheduleTime).isEqualTo("120000")
        assertThat(item.completeTime).isEqualTo("000000")
    }

    @Test
    @DisplayName("derived = PENDING — DefaultReason 비어있음 + LineItemStatus 비어있음 + schedule '000000'")
    fun deliveryStatusPending() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                lineItemStatus = "",
                shippingScheduleTime = "000000",
                shippingCompleteTime = "000000",
                shippingQuantityBox = "0",
            ),
        )
        val result = mapper.map(requestNumber, sap, mapOf("P1" to product("P1", "P1Name", 10)))
        val item = result.processingGroups[0].items[0]
        assertThat(item.deliveryStatus).isEqualTo(DeliveryStatus.PENDING)
    }

    @Test
    @DisplayName("derived = UNKNOWN — 정상 라인이나 LineItemStatus 채워짐 + 시각 없음 (레거시 cls:155-157 status='')")
    fun deliveryStatusUnknown() {
        // 결품(DefaultReason 없음)·반려(SAPOrderNumber 있음) 아닌 정상 라인인데, LineItemStatus 만
        // 채워지고 배차/완료 시각이 없으면 레거시는 어느 if 에도 안 걸려 status='' → UNKNOWN.
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                lineItemStatus = "SOMESTATUS",
                shippingScheduleTime = "000000",
                shippingCompleteTime = "000000",
                shippingQuantityBox = "0",
            ),
        )
        val result = mapper.map(requestNumber, sap, mapOf("P1" to product("P1", "P1Name", 10)))
        val item = result.processingGroups[0].items[0]
        // 과거엔 '대기'(PENDING)로 뭉갰던 케이스 — SF 정합상 빈 상태(UNKNOWN).
        assertThat(item.deliveryStatus).isEqualTo(DeliveryStatus.UNKNOWN)
        assertThat(item.deliveryStatus.koreanLabel).isEqualTo("")
    }

    @Test
    @DisplayName("DELIVERED koreanLabel 은 거래처주문 inbound 정합 '배송 완료'(공백) — 주문상세 표기는 클라이언트가 별도 매핑")
    fun deliveredKoreanLabelBelongsToClientOrderDomain() {
        // koreanLabel 은 erp_order_product.delivery_status 저장/매칭 키(거래처주문, SF inbound cls:158)라
        // 공백 있는 '배송 완료'. 주문상세(SF 조회 cls:157 공백 없음)의 화면 표기는 mobile 위젯이 담당한다.
        assertThat(DeliveryStatus.DELIVERED.koreanLabel).isEqualTo("배송 완료")
    }

    @Test
    @DisplayName("isFullyDelivered — 전 라인 CompleteTime 채워짐 → true (취소 버튼 비활성화 대상)")
    fun fullyDeliveredAllComplete() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000"),
            line(productCode = "P2", sapOrderNumber = "S1", shippingCompleteTime = "150000"),
        )
        assertThat(mapper.isFullyDelivered(sap)).isTrue()
    }

    @Test
    @DisplayName("isFullyDelivered — 일부 라인만 CompleteTime → false (부분 납품은 취소 가능 유지)")
    fun fullyDeliveredPartial() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000"),
            line(productCode = "P2", sapOrderNumber = "S1", shippingCompleteTime = "000000"),
        )
        assertThat(mapper.isFullyDelivered(sap)).isFalse()
    }

    @Test
    @DisplayName("isFullyDelivered — 결품/반려 라인은 제외하고 나머지 정상 라인 전부 완료면 true")
    fun fullyDeliveredExcludesOutOfStockAndRejected() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000"),
            // 결품(DefaultReason 채워짐) — 배송 대상 아님, 판정에서 제외
            line(productCode = "P2", sapOrderNumber = "S1", defaultReason = "재고부족", shippingCompleteTime = "000000"),
            // 반려(SAPOrderNumber 빈 + LineItemStatus 채워짐) — 제외
            line(productCode = "P3", sapOrderNumber = "", lineItemStatus = "반려사유", shippingCompleteTime = "000000"),
        )
        assertThat(mapper.isFullyDelivered(sap)).isTrue()
    }

    @Test
    @DisplayName("isFullyDelivered — 정상 라인이 하나도 없으면(전부 결품/반려) false")
    fun fullyDeliveredNoDeliverableLines() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "S1", defaultReason = "재고부족"),
            line(productCode = "P2", sapOrderNumber = "", lineItemStatus = "반려사유"),
        )
        assertThat(mapper.isFullyDelivered(sap)).isFalse()
    }

    @Test
    @DisplayName("isFullyDelivered — 응답 없음/빈 리스트 → false")
    fun fullyDeliveredNullOrEmpty() {
        assertThat(mapper.isFullyDelivered(null)).isFalse()
        assertThat(mapper.isFullyDelivered(emptyList())).isFalse()
    }

    @Test
    @DisplayName("결품 — SAPOrderNumber 있는 라인은 처리현황 그룹에 OUT_OF_STOCK 로 포함 + outOfStockReasons 수집 (레거시 view.jsp:523/414 동등)")
    fun outOfStockIncludedInProcessingGroup() {
        val sap = listOf(
            line(
                productCode = "P_REJ",
                sapOrderNumber = "S1",
                lineItemStatus = "OK",
                defaultReason = "L1",
                shippingCompleteTime = "143000",
                shippingQuantityBox = "5",
            ),
        )
        val crm = mapOf("P_REJ" to product("P_REJ", "참기름", boxReceivingQuantity = 30, quantityBoxes = BigDecimal("5")))

        val result = mapper.map(requestNumber, sap, crm)

        // 레거시 처리현황 테이블(view.jsp:523)은 반려만 제외 — SAP 주문번호 있는 결품 라인은
        // 상태 '결품' 으로 테이블에 표시된다. CompleteTime 채워져 있어도 결품이 우선 (cls:158 마지막 매칭).
        assertThat(result.processingGroups).hasSize(1)
        assertThat(result.processingGroups[0].sapOrderNumber).isEqualTo("S1")
        assertThat(result.processingGroups[0].items[0].deliveryStatus).isEqualTo(DeliveryStatus.OUT_OF_STOCK)
        assertThat(result.rejectedItems).isEmpty()
        // 동시에 orderedItems 회색+사유 표시용으로도 수집 (레거시는 두 곳 모두 표시). 사유 = "{코드} {설명}".
        assertThat(result.outOfStockReasons).containsEntry("P_REJ", "L1 [물류] 재고부족")
    }

    @Test
    @DisplayName("2맵 분리 (#845) — 결품(L1) + 취소(S2) 라인 혼재 → outOfStockReasons=[L1], cancelledReasons=[S2]")
    fun defaultReasonSplitIntoTwoMaps() {
        val sap = listOf(
            line(productCode = "P_OOS", sapOrderNumber = "S1", defaultReason = "L1", shippingCompleteTime = "000000"),
            line(productCode = "P_CAN", sapOrderNumber = "S1", defaultReason = "S2", shippingCompleteTime = "000000"),
        )
        val crm = mapOf("P_OOS" to product("P_OOS", "A", 10), "P_CAN" to product("P_CAN", "B", 10))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.outOfStockReasons).containsOnlyKeys("P_OOS")
        assertThat(result.outOfStockReasons).containsEntry("P_OOS", "L1 [물류] 재고부족")
        assertThat(result.cancelledReasons).containsOnlyKeys("P_CAN")
        assertThat(result.cancelledReasons).containsEntry("P_CAN", "S2 [영업] 고객사정에 의한 취소")
        // 취소 라인도 처리현황 그룹에 결품과 동일하게 OUT_OF_STOCK 로 포함 (D-1 결정).
        assertThat(result.processingGroups[0].items).allMatch { it.deliveryStatus == DeliveryStatus.OUT_OF_STOCK }
        assertThat(result.rejectedItems).isEmpty()
    }

    @Test
    @DisplayName("derived = 반려 — SAPOrderNumber 빈 값 + LineItemStatus 채워짐 → rejectedItems (rejectionReason = LineItemStatus)")
    fun rejectedAsReject() {
        val sap = listOf(
            line(
                productCode = "P_REJ",
                sapOrderNumber = "",
                lineItemStatus = "REJ",
                defaultReason = "",
            ),
        )
        val crm = mapOf("P_REJ" to product("P_REJ", "참기름", boxReceivingQuantity = 30, quantityBoxes = BigDecimal("5")))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).hasSize(1)
        assertThat(result.rejectedItems[0].rejectionReason).isEqualTo("REJ")
        assertThat(result.outOfStockReasons).isEmpty()
    }

    @Test
    @DisplayName("마지막 매칭 우선 — DefaultReason + LineItemStatus + ShippingCompleteTime 모두 채워져도 결품 (평가 5) → outOfStockReasons")
    fun derivedLastMatchWins() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "",
                lineItemStatus = "REJ",
                defaultReason = "L1",
                shippingCompleteTime = "143000",
            ),
        )
        val result = mapper.map(requestNumber, sap, mapOf("P1" to product("P1", "P1Name", 10)))
        // DefaultReason 매칭이 최종(평가 5) — 반려가 아니라 결품으로 분류.
        // SAPOrderNumber 빈 값이라 처리현황 그룹에서는 제외 (view.jsp:494, 500 빈 키 그룹 제외 동등).
        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).isEmpty()
        assertThat(result.outOfStockReasons).containsEntry("P1", "L1 [물류] 재고부족")
    }

    @Test
    @DisplayName("미납 — SAPOrderNumber 있음 + LineItemStatus 채워짐 && != OK → unfulfilledItems 수집 + 처리현황 그룹에도 유지 (신규 정책)")
    fun unfulfilledCollected() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                lineItemStatus = "배차 미확정",
                shippingScheduleTime = "000000",
                shippingCompleteTime = "000000",
            ),
        )
        val crm = mapOf("P1" to product("P1", "진라면", boxReceivingQuantity = 30, quantityBoxes = BigDecimal("7")))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.unfulfilledItems).hasSize(1)
        assertThat(result.unfulfilledItems[0].productCode).isEqualTo("P1")
        assertThat(result.unfulfilledItems[0].reason).isEqualTo("배차 미확정")
        assertThat(result.unfulfilledItems[0].orderQuantityBoxes).isEqualByComparingTo("7")
        // 미납 라인은 처리현황 그룹에도 그대로 남는다 (이중 표시 — 여기선 시각 없음+상태 채움 → UNKNOWN).
        assertThat(result.processingGroups).hasSize(1)
        assertThat(result.processingGroups[0].items[0].deliveryStatus).isEqualTo(DeliveryStatus.UNKNOWN)
        assertThat(result.rejectedItems).isEmpty()
    }

    @Test
    @DisplayName("미납 제외 — OK/빈 값/반려(SAPOrderNumber 빈)/결품(DefaultReason) 라인은 unfulfilledItems 에 미수집")
    fun unfulfilledExclusions() {
        val sap = listOf(
            // 정상(OK) — 제외
            line(productCode = "P_OK", sapOrderNumber = "S1", lineItemStatus = "OK", shippingCompleteTime = "143000"),
            // 빈 값(정상 대기) — 제외
            line(productCode = "P_EMPTY", sapOrderNumber = "S1", lineItemStatus = ""),
            // 반려(SAPOrderNumber 빈) — rejectedItems 로 분류, 미납 아님
            line(productCode = "P_REJ", sapOrderNumber = "", lineItemStatus = "납품일자 오류"),
            // 결품(DefaultReason) — 기존 결품 표시 유지, LineItemStatus non-OK 라도 미납 아님
            line(productCode = "P_OOS", sapOrderNumber = "S1", lineItemStatus = "이상", defaultReason = "L1"),
        )
        val crm = mapOf(
            "P_OK" to product("P_OK", "A", 10),
            "P_EMPTY" to product("P_EMPTY", "B", 10),
            "P_REJ" to product("P_REJ", "C", 10),
            "P_OOS" to product("P_OOS", "D", 10),
        )

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.unfulfilledItems).isEmpty()
        assertThat(result.rejectedItems).extracting("productCode").containsExactly("P_REJ")
        assertThat(result.outOfStockReasons).containsOnlyKeys("P_OOS")
    }

    @Test
    @DisplayName("sumApprovedAmount — 전 라인 OrderSalesAmount 단순 합산, 반려/결품 제외 없음 (레거시 view.jsp:343-348 동등)")
    fun sumApprovedAmountAllLines() {
        val sap = listOf(
            // 정상 라인
            line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000")
                .copy(orderSalesAmount = "120000"),
            // 반려 라인도 합산 포함 (레거시 EL 은 전 라인 누적)
            line(productCode = "P2", sapOrderNumber = "", lineItemStatus = "반려사유")
                .copy(orderSalesAmount = "30000"),
            // 결품 라인도 합산 포함
            line(productCode = "P3", sapOrderNumber = "S1", defaultReason = "재고부족")
                .copy(orderSalesAmount = "5000"),
        )
        assertThat(mapper.sumApprovedAmount(sap)).isEqualByComparingTo("155000")
    }

    @Test
    @DisplayName("sumApprovedAmount — null/비숫자 값은 0 취급, null/빈 리스트는 0 (레거시 EL null→0 동등)")
    fun sumApprovedAmountNullSafe() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "S1").copy(orderSalesAmount = null),
            line(productCode = "P2", sapOrderNumber = "S1").copy(orderSalesAmount = "-"),
            line(productCode = "P3", sapOrderNumber = "S1").copy(orderSalesAmount = "1000"),
        )
        assertThat(mapper.sumApprovedAmount(sap)).isEqualByComparingTo("1000")
        assertThat(mapper.sumApprovedAmount(null)).isEqualByComparingTo("0")
        assertThat(mapper.sumApprovedAmount(emptyList())).isEqualByComparingTo("0")
    }

    @Test
    @DisplayName("다중 SAP 주문 분할 — LinkedHashMap 자연 순서 유지 (Q7)")
    fun multipleGroupsOrder() {
        val sap = listOf(
            line(productCode = "P_A1", sapOrderNumber = "AAA", shippingScheduleTime = "120000", shippingCompleteTime = "143000", shippingQuantityBox = "1"),
            line(productCode = "P_B1", sapOrderNumber = "BBB", shippingScheduleTime = "120000", shippingCompleteTime = "143000", shippingQuantityBox = "1"),
            line(productCode = "P_A2", sapOrderNumber = "AAA", shippingScheduleTime = "120000", shippingCompleteTime = "143000", shippingQuantityBox = "1"),
        )
        val crm = mapOf(
            "P_A1" to product("P_A1", "A1", 10),
            "P_A2" to product("P_A2", "A2", 10),
            "P_B1" to product("P_B1", "B1", 10),
        )
        val result = mapper.map(requestNumber, sap, crm)
        // 순서 = SAP 응답 자연 순서. AAA 가 먼저 등장 → 첫 그룹.
        assertThat(result.processingGroups).hasSize(2)
        assertThat(result.processingGroups[0].sapOrderNumber).isEqualTo("AAA")
        assertThat(result.processingGroups[0].items).extracting("productCode").containsExactly("P_A1", "P_A2")
        assertThat(result.processingGroups[1].sapOrderNumber).isEqualTo("BBB")
        assertThat(result.processingGroups[1].items).extracting("productCode").containsExactly("P_B1")
    }

    @Test
    @DisplayName("동일 ProductCode 라인 중복 — 모두 보존 (Q3, 레거시 동등)")
    fun duplicateProductCodePreserved() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "AAA", shippingCompleteTime = "143000", shippingQuantityBox = "5"),
            line(productCode = "P1", sapOrderNumber = "AAA", shippingCompleteTime = "143000", shippingQuantityBox = "3"),
        )
        val crm = mapOf("P1" to product("P1", "진라면 매운맛", 30))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups).hasSize(1)
        assertThat(result.processingGroups[0].items).hasSize(2)
        assertThat(result.processingGroups[0].items.map { it.productCode }).containsExactly("P1", "P1")
    }

    @Test
    @DisplayName("빈 SAPOrderNumber 정상 라인 — 응답에서 제외 (Q4)")
    fun emptyGroupExcluded() {
        val sap = listOf(
            line(productCode = "P1", sapOrderNumber = "AAA", shippingCompleteTime = "143000", shippingQuantityBox = "1"),
            line(productCode = "P2", sapOrderNumber = "", shippingCompleteTime = "143000", lineItemStatus = ""),
        )
        val crm = mapOf("P1" to product("P1", "P1", 10), "P2" to product("P2", "P2", 10))
        val result = mapper.map(requestNumber, sap, crm)
        // AAA 그룹만 살아남음. 빈 sapOrderNumber + lineItemStatus="" 는 반려도 아님 → 그룹에서 제외
        assertThat(result.processingGroups).hasSize(1)
        assertThat(result.processingGroups[0].sapOrderNumber).isEqualTo("AAA")
        assertThat(result.rejectedItems).isEmpty()
    }

    @Test
    @DisplayName("응답 라인 상한 초과 — 1001건 → 첫 1000건만 매핑")
    fun lineOverflow() {
        val sap = (1..1001).map { idx ->
            line(productCode = "P$idx", sapOrderNumber = "S$idx", shippingCompleteTime = "143000", shippingQuantityBox = "1")
        }
        val crm = (1..1001).associate { idx -> "P$idx" to product("P$idx", "n", 10) }
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups).hasSize(1000)
    }

    @Test
    @DisplayName("boxReceivingQuantity = 0 → EA 환산 생략, BOX 만 표시")
    fun boxReceivingQuantityZeroSkipsEa() {
        val sap = listOf(line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000", shippingQuantityBox = "10"))
        val crm = mapOf("P1" to product("P1", "P1", boxReceivingQuantity = 0))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("10 BOX")
    }

    @Test
    @DisplayName("ShippingQuantity_Box 비숫자 → 주문수량 폴백 (여기선 총수량도 0 이라 0 BOX (0 EA))")
    fun invalidShippingQty() {
        val sap = listOf(line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000", shippingQuantityBox = "-"))
        val crm = mapOf("P1" to product("P1", "P1", 30))
        val result = mapper.map(requestNumber, sap, crm)
        // shippingQuantityBox 파싱 실패 → 주문수량(totalQuantityBox/totalQuantity, 헬퍼 기본 "0") 폴백.
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("0 BOX (0 EA)")
    }

    @Test
    @DisplayName("출하수량 null → 주문수량(TotalQuantity_Box/TotalQuantity) 폴백 (레거시 view.jsp:532-535 동등)")
    fun shippingQtyNullFallsBackToOrderQty() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingCompleteTime = "143000",
                shippingQuantityBox = null,
                totalQuantityBox = "10",
                totalQuantity = "300",
            ),
        )
        val crm = mapOf("P1" to product("P1", "P1", boxReceivingQuantity = 30))
        val result = mapper.map(requestNumber, sap, crm)
        // 출하수량이 없으면 0 BOX 가 아니라 주문수량(10 BOX, EA 는 SAP TotalQuantity 300 그대로).
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("10 BOX (300 EA)")
    }

    @Test
    @DisplayName("출하수량 빈문자 → 주문수량 폴백 (빈문자도 미도래로 간주)")
    fun shippingQtyBlankFallsBackToOrderQty() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingCompleteTime = "143000",
                shippingQuantityBox = "",
                totalQuantityBox = "7",
                totalQuantity = "210",
            ),
        )
        val crm = mapOf("P1" to product("P1", "P1", boxReceivingQuantity = 30))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("7 BOX (210 EA)")
    }

    @Test
    @DisplayName("출하수량 null + 주문 EA 없음 → BOX 만 표시")
    fun shippingQtyNullOrderEaMissingShowsBoxOnly() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingCompleteTime = "143000",
                shippingQuantityBox = null,
                totalQuantityBox = "4",
                totalQuantity = null,
            ),
        )
        val crm = mapOf("P1" to product("P1", "P1", boxReceivingQuantity = 30))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("4 BOX")
    }

    @Test
    @DisplayName("출하수량 있음 → 기존대로 box × 박스입수량 EA 환산 (폴백 미적용)")
    fun shippingQtyPresentUsesShippingNotFallback() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingCompleteTime = "143000",
                shippingQuantityBox = "2",
                totalQuantityBox = "10",
                totalQuantity = "300",
            ),
        )
        val crm = mapOf("P1" to product("P1", "P1", boxReceivingQuantity = 30))
        val result = mapper.map(requestNumber, sap, crm)
        // 출하수량 2 BOX 가 우선 — 주문수량(10/300) 폴백 아님. EA = 2 × 30 = 60.
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("2 BOX (60 EA)")
    }

    @Test
    @DisplayName("차량/기사 5필드 매핑 — 빈 문자열은 null, '000000' sentinel 은 null")
    fun driverFields() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "S1",
                shippingDriverName = "홍길동",
                shippingVehicle = "12가3456",
                shippingDriverPhone = "010-1234-5678",
                shippingScheduleTime = "120000",
                shippingCompleteTime = "143000",
                shippingQuantityBox = "1",
            ),
        )
        val empty = listOf(
            line(
                productCode = "P2",
                sapOrderNumber = "S1",
                shippingDriverName = "",
                shippingVehicle = "",
                shippingDriverPhone = "",
                shippingScheduleTime = "000000",
                shippingCompleteTime = "000000",
                shippingQuantityBox = "1",
            ),
        )
        val crm = mapOf("P1" to product("P1", "P1", 10), "P2" to product("P2", "P2", 10))

        val result1 = mapper.map(requestNumber, sap, crm)
        val item1 = result1.processingGroups[0].items[0]
        assertThat(item1.driverName).isEqualTo("홍길동")
        assertThat(item1.vehicle).isEqualTo("12가3456")
        assertThat(item1.driverPhone).isEqualTo("010-1234-5678")
        // 레거시 무가공 — HHmmss 그대로
        assertThat(item1.scheduleTime).isEqualTo("120000")
        assertThat(item1.completeTime).isEqualTo("143000")

        val result2 = mapper.map(requestNumber, empty, crm)
        val item2 = result2.processingGroups[0].items[0]
        // 빈 문자열 driver 필드는 null, 시각 '000000' 은 빈값이 아니라 그대로 유지 (레거시 무가공)
        assertThat(item2.driverName).isNull()
        assertThat(item2.vehicle).isNull()
        assertThat(item2.driverPhone).isNull()
        assertThat(item2.scheduleTime).isEqualTo("000000")
        assertThat(item2.completeTime).isEqualTo("000000")
    }

    @Test
    @DisplayName("빈 SAP 응답 → 빈 결과")
    fun emptyInput() {
        val result = mapper.map(requestNumber, emptyList(), emptyMap())
        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).isEmpty()
    }

    // ---- 헬퍼 ----

    private fun line(
        productCode: String,
        sapOrderNumber: String?,
        productName: String? = null,
        lineItemStatus: String? = "OK",
        shippingScheduleTime: String? = "000000",
        shippingCompleteTime: String? = "000000",
        shippingQuantityBox: String? = "0",
        totalQuantityBox: String? = "0",
        totalQuantity: String? = "0",
        defaultReason: String? = "",
        shippingDriverName: String? = "",
        shippingVehicle: String? = "",
        shippingDriverPhone: String? = "",
    ): SapOrderRequestDetailLine = SapOrderRequestDetailLine(
        lineNumber = "00001",
        productCode = productCode,
        productName = productName,
        lineItemStatus = lineItemStatus,
        totalQuantity = totalQuantity,
        unit = "BOX",
        sapOrderNumber = sapOrderNumber,
        orderSalesAmount = "0",
        deliveryRequestDate = "20260506",
        orderDate = "20260504",
        shippingDriverName = shippingDriverName,
        shippingVehicle = shippingVehicle,
        shippingDriverPhone = shippingDriverPhone,
        shippingScheduleTime = shippingScheduleTime,
        shippingCompleteTime = shippingCompleteTime,
        totalQuantityBox = totalQuantityBox,
        shippingQuantityBox = shippingQuantityBox,
        defaultReason = defaultReason,
    )

    private fun product(
        productCode: String,
        productName: String,
        boxReceivingQuantity: Int = 30,
        quantityBoxes: BigDecimal = BigDecimal.ZERO,
    ): OrderRequestProduct {
        val account = Account(id = 1, name = "ACC")
        val employee = Employee(id = 1L, employeeCode = "20030117", name = "Test")
        val order = OrderRequest(
            id = 100L,
            orderRequestNumber = "OR-0001234",
            orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
            deliveryDate = LocalDate.of(2026, 5, 6),
            totalAmount = BigDecimal.ZERO,
            orderRequestStatus = OrderRequestStatus.APPROVED,
            employee = employee,
            account = account,
        )
        // 레거시 EA 환산 계수 = 제품 마스터 Product.BoxReceivingQuantity__c.
        val productEntity = Product(
            id = 1L,
            productCode = productCode,
            name = productName,
            boxReceivingQuantity = BigDecimal.valueOf(boxReceivingQuantity.toLong()),
        )
        return OrderRequestProduct(
            id = 1L,
            lineNumber = BigDecimal.valueOf(1L),
            productCode = productCode,
            quantityBoxes = quantityBoxes,
            quantityPieces = BigDecimal.valueOf(0L),
            unit = "BOX",
            unitPrice = BigDecimal.ZERO,
            amount = BigDecimal.ZERO,
            piecesPerBox = 1,
            orderRequest = order,
            product = productEntity,
        )
    }
}
