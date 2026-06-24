package com.otoki.powersales.domain.activity.order.sap

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
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
    @DisplayName("레거시 body 동등 — reqItemList={RequestNumber}, ItemList=[{LineNumber(숫자), ProductCode, LineChangeType=X}]")
    fun payloadStructure() {
        val orderRequest = orderRequest()
        val products = listOf(
            product(id = 101, lineNumber = BigDecimal.valueOf(10L), productCode = "P001", orderRequest = orderRequest),
            product(id = 102, lineNumber = BigDecimal.valueOf(20L), productCode = "P002", orderRequest = orderRequest),
        )

        val payload = factory.build(orderRequest, products)

        // 헤더: reqItemList 키에 { RequestNumber } 객체 (cls:76)
        @Suppress("UNCHECKED_CAST")
        val header = payload["reqItemList"] as Map<String, Any?>
        assertThat(header["RequestNumber"]).isEqualTo("OR00000001")
        assertThat(payload).doesNotContainKey("RequestNumber") // 최상위 평면 키 아님

        // 라인: ItemList 키 (cls:95), LineNumber 는 숫자(BigDecimal) — 레거시 Decimal 동등
        @Suppress("UNCHECKED_CAST")
        val items = payload["ItemList"] as List<Map<String, Any?>>
        assertThat(items).hasSize(2)
        assertThat(items[0]["LineNumber"]).isEqualTo(BigDecimal.valueOf(10L))
        assertThat(items[0]["ProductCode"]).isEqualTo("P001")
        assertThat(items[0]["LineChangeType"]).isEqualTo("X")
        assertThat(items[1]["LineNumber"]).isEqualTo(BigDecimal.valueOf(20L))
        assertThat(items[1]["ProductCode"]).isEqualTo("P002")
        assertThat(items[1]["LineChangeType"]).isEqualTo("X")
    }

    @Test
    @DisplayName("빈 라인 → 빈 ItemList (헤더 reqItemList 는 유지)")
    fun emptyLines() {
        val payload = factory.build(orderRequest(), emptyList())
        @Suppress("UNCHECKED_CAST")
        val header = payload["reqItemList"] as Map<String, Any?>
        assertThat(header["RequestNumber"]).isEqualTo("OR00000001")
        @Suppress("UNCHECKED_CAST")
        val items = payload["ItemList"] as List<Map<String, Any?>>
        assertThat(items).isEmpty()
    }

    private fun orderRequest(
        id: Long = 1L,
        number: String = "OR00000001",
    ) = OrderRequest(
        id = id,
        orderRequestNumber = number,
        orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
        deliveryDate = LocalDate.of(2026, 5, 6),
        totalAmount = BigDecimal("100000"),
        orderRequestStatus = OrderRequestStatus.APPROVED,
        employee = Employee(id = 1L, employeeCode = "E001", name = "tester", role = AppAuthority.WOMAN),
        account = Account(id = 1, name = "A1", externalKey = "EXT-1"),
    )

    private fun product(
        id: Long,
        lineNumber: BigDecimal,
        productCode: String,
        orderRequest: OrderRequest,
    ) = OrderRequestProduct(
        id = id,
        lineNumber = lineNumber,
        productCode = productCode,
        unit = "EA",
        orderRequest = orderRequest,
    )
}
