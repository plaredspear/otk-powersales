package com.otoki.powersales.domain.activity.order.sap.sender

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.external.sap.outbox.SapInterfaceRegistry
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 레거시 `IF_Util.registOrder` (IF_Util.cls:154-235) SAP SD03050 payload 동등 검증.
 *
 * 레거시 wrapper: `REQUEST_List_header` / `REQUEST_List_item` (IF_Util.cls:80-92).
 * header 4필드 (RequestNumber, SAPAccountCode, OrderDate=yyyyMMdd HHmm, DeliveryRequestDate=yyyyMMdd),
 * item 4필드 (LineNumber, ProductCode, TotalQuantity, Unit) 만 송신.
 */
@DisplayName("OrderRequestRegisterSender.buildPayload — 레거시 SD03050 동등")
class OrderRequestRegisterSenderPayloadTest {

    private val sender = OrderRequestRegisterSender(
        outboxRepository = mockk<SapOutboxRepository>(),
        interfaceRegistry = mockk<SapInterfaceRegistry>(),
        objectMapper = mockk<ObjectMapper>(),
    )

    @Test
    @DisplayName("header — REQUEST_List_header 4필드, OrderDate=yyyyMMdd HHmm(공백 포함), DeliveryRequestDate=yyyyMMdd")
    fun header() {
        val order = orderRequest()
        val payload = sender.buildPayload(order, order.account!!, order.employee!!, emptyList())

        @Suppress("UNCHECKED_CAST")
        val header = payload["REQUEST_List_header"] as Map<String, Any?>
        assertThat(header.keys).containsExactlyInAnyOrder(
            "RequestNumber", "SAPAccountCode", "OrderDate", "DeliveryRequestDate",
        )
        assertThat(header["RequestNumber"]).isEqualTo("ORD-20260504-000001")
        assertThat(header["SAPAccountCode"]).isEqualTo("EXT-1")
        // 레거시 IF_Util.cls:169 = yyyyMMdd HHmm (날짜·시각 사이 공백 1칸)
        assertThat(header["OrderDate"]).isEqualTo("20260504 1430")
        assertThat(header["DeliveryRequestDate"]).isEqualTo("20260506")
    }

    @Test
    @DisplayName("header — 레거시 미존재 필드(EmployeeCode/InterfaceType/TotalOrderAmount)는 송신하지 않는다")
    fun headerOmitsNonLegacyFields() {
        val order = orderRequest()
        @Suppress("UNCHECKED_CAST")
        val header = sender.buildPayload(order, order.account!!, order.employee!!, emptyList())["REQUEST_List_header"] as Map<String, Any?>
        assertThat(header).doesNotContainKeys("EmployeeCode", "InterfaceType", "TotalOrderAmount")
    }

    @Test
    @DisplayName("item — REQUEST_List_item[LineNumber, ProductCode, TotalQuantity(=주문단위수량), Unit]")
    fun items() {
        val order = orderRequest()
        val products = listOf(
            // BOX 주문 → TotalQuantity = quantityBoxes (레거시 OrderQuantity 동등)
            product(id = 101, lineNumber = BigDecimal.valueOf(10L), productCode = "P001", boxes = BigDecimal("3"), pieces = BigDecimal("30"), unit = "BOX", orderRequest = order),
            // EA 주문 → TotalQuantity = quantityPieces (quantityBoxes=0 이므로 박스수량 송신 시 0 누락 — 레거시는 낱개수량 송신)
            product(id = 102, lineNumber = BigDecimal.valueOf(20L), productCode = "P002", boxes = BigDecimal.ZERO, pieces = BigDecimal("5"), unit = "EA", orderRequest = order),
        )

        @Suppress("UNCHECKED_CAST")
        val items = sender.buildPayload(order, order.account!!, order.employee!!, products)["REQUEST_List_item"] as List<Map<String, Any?>>

        assertThat(items).hasSize(2)
        assertThat(items[0].keys).containsExactlyInAnyOrder("LineNumber", "ProductCode", "TotalQuantity", "Unit")
        assertThat(items[0]["LineNumber"]).isEqualTo(BigDecimal.valueOf(10L))
        assertThat(items[0]["ProductCode"]).isEqualTo("P001")
        assertThat(items[0]["TotalQuantity"]).isEqualTo(BigDecimal("3"))
        assertThat(items[0]["Unit"]).isEqualTo("BOX")
        // EA 주문은 낱개수량(quantityPieces)을 송신 — 박스수량(0)이 아님
        assertThat(items[1]["TotalQuantity"]).isEqualTo(BigDecimal("5"))
        assertThat(items[1]["Unit"]).isEqualTo("EA")
    }

    @Test
    @DisplayName("item — EA 주문(quantityBoxes=0) 은 TotalQuantity 에 낱개수량을 송신한다 (0 누락 방지)")
    fun eaUnitSendsPieces() {
        val order = orderRequest()
        val products = listOf(
            product(id = 201, lineNumber = BigDecimal.ONE, productCode = "P010", boxes = BigDecimal.ZERO, pieces = BigDecimal("7"), unit = "EA", orderRequest = order),
        )
        @Suppress("UNCHECKED_CAST")
        val items = sender.buildPayload(order, order.account!!, order.employee!!, products)["REQUEST_List_item"] as List<Map<String, Any?>>
        assertThat(items[0]["TotalQuantity"]).isEqualTo(BigDecimal("7"))
    }

    @Test
    @DisplayName("item — 레거시 미존재 필드(OrderQuantity/TotalQuantity_Each/TotalQuantity_Box)는 송신하지 않는다")
    fun itemOmitsNonLegacyFields() {
        val order = orderRequest()
        val products = listOf(product(id = 101, lineNumber = BigDecimal.ONE, productCode = "P001", boxes = BigDecimal.ZERO, pieces = BigDecimal("1"), unit = "EA", orderRequest = order))
        @Suppress("UNCHECKED_CAST")
        val items = sender.buildPayload(order, order.account!!, order.employee!!, products)["REQUEST_List_item"] as List<Map<String, Any?>>
        assertThat(items[0]).doesNotContainKeys("OrderQuantity", "TotalQuantity_Each", "TotalQuantity_Box")
    }

    @Test
    @DisplayName("빈 라인 → 빈 REQUEST_List_item")
    fun emptyLines() {
        val order = orderRequest()
        @Suppress("UNCHECKED_CAST")
        val items = sender.buildPayload(order, order.account!!, order.employee!!, emptyList())["REQUEST_List_item"] as List<Map<String, Any?>>
        assertThat(items).isEmpty()
    }

    private fun orderRequest() = OrderRequest(
        id = 1L,
        orderRequestNumber = "ORD-20260504-000001",
        orderDate = LocalDateTime.of(2026, 5, 4, 14, 30),
        deliveryDate = LocalDate.of(2026, 5, 6),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = OrderRequestStatus.SENT,
        employee = Employee(id = 1L, employeeCode = "E001", name = "tester", role = AppAuthority.WOMAN),
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(
        id: Long,
        lineNumber: BigDecimal,
        productCode: String,
        boxes: BigDecimal,
        pieces: BigDecimal,
        unit: String,
        orderRequest: OrderRequest,
    ) = OrderRequestProduct(
        id = id,
        lineNumber = lineNumber,
        productCode = productCode,
        quantityBoxes = boxes,
        quantityPieces = pieces,
        unit = unit,
        orderRequest = orderRequest,
    )
}
