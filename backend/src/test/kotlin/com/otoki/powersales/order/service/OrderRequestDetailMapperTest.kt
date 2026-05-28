package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.sap.outbound.sender.SapOrderRequestDetailLine
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
    @DisplayName("정상 — 단일 SAPOrderNumber, derived = DELIVERED + BOX→EA 환산")
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
        val crm = mapOf("1000023" to product("1000023", "진라면 매운맛", piecesPerBox = 30))

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
        assertThat(item.scheduleTime).isEqualTo("12:00")
        assertThat(item.completeTime).isEqualTo("14:30")
        assertThat(result.rejectedItems).isEmpty()
    }

    @Test
    @DisplayName("derived = SHIPPING — schedule 채워짐 + complete 비어있음")
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
        assertThat(item.scheduleTime).isEqualTo("12:00")
        assertThat(item.completeTime).isNull()
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
    @DisplayName("derived = 결품 — DefaultReason 채워짐 → rejectedItems")
    fun rejectedAsOutOfStock() {
        val sap = listOf(
            line(
                productCode = "P_REJ",
                sapOrderNumber = "S1",
                lineItemStatus = "OK",
                defaultReason = "재고부족",
                shippingCompleteTime = "143000",
            ),
        )
        val crm = mapOf("P_REJ" to product("P_REJ", "참기름", piecesPerBox = 30, quantityBoxes = BigDecimal("5")))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).hasSize(1)
        val rej = result.rejectedItems[0]
        assertThat(rej.productCode).isEqualTo("P_REJ")
        assertThat(rej.productName).isEqualTo("참기름")
        assertThat(rej.rejectionReason).isEqualTo("재고부족")
        assertThat(rej.orderQuantityBoxes).isEqualByComparingTo("5")
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
        val crm = mapOf("P_REJ" to product("P_REJ", "참기름", piecesPerBox = 30, quantityBoxes = BigDecimal("5")))

        val result = mapper.map(requestNumber, sap, crm)

        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).hasSize(1)
        assertThat(result.rejectedItems[0].rejectionReason).isEqualTo("REJ")
    }

    @Test
    @DisplayName("마지막 매칭 우선 — DefaultReason + LineItemStatus + ShippingCompleteTime 모두 채워져도 결품 (평가 5)")
    fun derivedLastMatchWins() {
        val sap = listOf(
            line(
                productCode = "P1",
                sapOrderNumber = "",
                lineItemStatus = "REJ",
                defaultReason = "재고부족",
                shippingCompleteTime = "143000",
            ),
        )
        val result = mapper.map(requestNumber, sap, mapOf("P1" to product("P1", "P1Name", 10)))
        // DefaultReason 매칭이 최종 — `결품` 으로 분리됨
        assertThat(result.processingGroups).isEmpty()
        assertThat(result.rejectedItems).hasSize(1)
        assertThat(result.rejectedItems[0].rejectionReason).isEqualTo("재고부족")
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
    @DisplayName("pieces_per_box = 0 → EA 환산 생략, BOX 만 표시")
    fun piecesPerBoxZeroSkipsEa() {
        val sap = listOf(line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000", shippingQuantityBox = "10"))
        val crm = mapOf("P1" to product("P1", "P1", piecesPerBox = 0))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("10 BOX")
    }

    @Test
    @DisplayName("ShippingQuantity_Box 비숫자 → 0 BOX (catch)")
    fun invalidShippingQty() {
        val sap = listOf(line(productCode = "P1", sapOrderNumber = "S1", shippingCompleteTime = "143000", shippingQuantityBox = "-"))
        val crm = mapOf("P1" to product("P1", "P1", 30))
        val result = mapper.map(requestNumber, sap, crm)
        assertThat(result.processingGroups[0].items[0].deliveredQuantity).isEqualTo("0 BOX (0 EA)")
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
        assertThat(item1.scheduleTime).isEqualTo("12:00")
        assertThat(item1.completeTime).isEqualTo("14:30")

        val result2 = mapper.map(requestNumber, empty, crm)
        val item2 = result2.processingGroups[0].items[0]
        assertThat(item2.driverName).isNull()
        assertThat(item2.vehicle).isNull()
        assertThat(item2.driverPhone).isNull()
        assertThat(item2.scheduleTime).isNull()
        assertThat(item2.completeTime).isNull()
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
        defaultReason: String? = "",
        shippingDriverName: String? = "",
        shippingVehicle: String? = "",
        shippingDriverPhone: String? = "",
    ): SapOrderRequestDetailLine = SapOrderRequestDetailLine(
        lineNumber = "00001",
        productCode = productCode,
        productName = productName,
        lineItemStatus = lineItemStatus,
        totalQuantity = "0",
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
        totalQuantityBox = "0",
        shippingQuantityBox = shippingQuantityBox,
        defaultReason = defaultReason,
    )

    private fun product(
        productCode: String,
        productName: String,
        piecesPerBox: Int = 30,
        quantityBoxes: BigDecimal = BigDecimal.ZERO,
    ): OrderRequestProduct {
        val account = Account(id = 1, name = "ACC")
        val employee = Employee(id = 1L, employeeCode = "20030117", name = "Test")
        val order = OrderRequest(
            id = 100L,
            orderRequestNumber = "OR-0001234",
            orderDate = java.time.LocalDateTime.of(2026, 5, 4, 10, 0),
            deliveryDate = LocalDate.of(2026, 5, 6),
            totalAmount = BigDecimal.ZERO,
            orderRequestStatus = OrderRequestStatus.APPROVED,
            employee = employee,
            account = account,
        )
        val productEntity = com.otoki.powersales.product.entity.Product(
            id = 1L,
            productCode = productCode,
            name = productName,
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
            piecesPerBox = piecesPerBox,
            orderRequest = order,
            product = productEntity,
        )
    }
}
