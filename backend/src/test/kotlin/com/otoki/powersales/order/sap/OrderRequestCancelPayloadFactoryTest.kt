package com.otoki.powersales.order.sap

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.entity.OrderRequestStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("OrderRequestCancelPayloadFactory 테스트 (#597)")
class OrderRequestCancelPayloadFactoryTest {

    private val factory = OrderRequestCancelPayloadFactory()

    @Test
    @DisplayName("페이로드 — RequestNumber + reqItemList[LineNumber, ProductCode, LineChangeType=X]")
    fun payloadStructure() {
        val orderRequest = orderRequest()
        val products = listOf(
            product(id = 101, lineNumber = 10, productCode = "P001", orderRequest = orderRequest),
            product(id = 102, lineNumber = 20, productCode = "P002", orderRequest = orderRequest),
        )

        val payload = factory.build(orderRequest, products)

        assertThat(payload["RequestNumber"]).isEqualTo("ORD-20260504-000001")
        @Suppress("UNCHECKED_CAST")
        val items = payload["reqItemList"] as List<Map<String, Any?>>
        assertThat(items).hasSize(2)
        assertThat(items[0]["LineNumber"]).isEqualTo("10")
        assertThat(items[0]["ProductCode"]).isEqualTo("P001")
        assertThat(items[0]["LineChangeType"]).isEqualTo("X")
        assertThat(items[1]["LineNumber"]).isEqualTo("20")
        assertThat(items[1]["ProductCode"]).isEqualTo("P002")
        assertThat(items[1]["LineChangeType"]).isEqualTo("X")
    }

    @Test
    @DisplayName("빈 라인 → 빈 reqItemList")
    fun emptyLines() {
        val payload = factory.build(orderRequest(), emptyList())
        @Suppress("UNCHECKED_CAST")
        val items = payload["reqItemList"] as List<Map<String, Any?>>
        assertThat(items).isEmpty()
    }

    private fun orderRequest(
        id: Long = 1L,
        number: String = "ORD-20260504-000001",
    ) = OrderRequest(
        id = id,
        orderRequestNumber = number,
        orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
        deliveryDate = LocalDate.of(2026, 5, 6),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = OrderRequestStatus.APPROVED,
        employee = Employee(id = 1L, employeeCode = "E001", name = "tester", role = UserRole.WOMAN),
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(
        id: Long,
        lineNumber: Int,
        productCode: String,
        orderRequest: OrderRequest,
    ) = OrderRequestProduct(
        id = id,
        lineNumber = lineNumber,
        productCode = productCode,
        productName = "P-$productCode",
        unit = "EA",
        orderRequest = orderRequest,
    )
}
