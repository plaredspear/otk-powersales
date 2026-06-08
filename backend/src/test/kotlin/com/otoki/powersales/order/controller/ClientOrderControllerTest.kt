package com.otoki.powersales.order.controller

import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.ClientOrderItemResponse
import com.otoki.powersales.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.order.dto.response.OrderHistoryProductResponse
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.service.ClientOrderQueryService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import io.mockk.every
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.math.BigDecimal

@WebMvcTest(ClientOrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientOrderController 테스트 (#593)")
class ClientOrderControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var clientOrderQueryService: ClientOrderQueryService

    @Nested
    @DisplayName("GET /api/v1/mobile/client-orders/{sapOrderNumber}")
    inner class GetClientOrderDetailTests {

        private val sapOrderNumber = "0300011396"

        @Test
        @DisplayName("성공 - 거래처 출하 상세 응답 200 OK")
        fun success() {
            val response = ClientOrderDetailResponse(
                sapOrderNumber = sapOrderNumber,
                sapAccountCode = "0001234567",
                sapAccountName = "홍길동마트",
                clientDeadlineTime = "13:50",
                orderDate = LocalDate.of(2026, 5, 4),
                deliveryDate = LocalDate.of(2026, 5, 6),
                totalApprovedAmount = BigDecimal.valueOf(1_250_000L),
                orderedItemCount = 1,
                orderedItems = listOf(
                    ClientOrderItemResponse(
                        productCode = "P001",
                        productName = "예시 상품",
                        deliveredQuantity = "10 BOX",
                        deliveryStatus = DeliveryStatus.DELIVERED
                    )
                )
            )
            every { clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)) } returns response

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", sapOrderNumber))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sapOrderNumber").value(sapOrderNumber))
                .andExpect(jsonPath("$.data.sapAccountCode").value("0001234567"))
                .andExpect(jsonPath("$.data.sapAccountName").value("홍길동마트"))
                .andExpect(jsonPath("$.data.clientDeadlineTime").value("13:50"))
                .andExpect(jsonPath("$.data.orderedItemCount").value(1))
                .andExpect(jsonPath("$.data.orderedItems[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.orderedItems[0].deliveredQuantity").value("10 BOX"))
                .andExpect(jsonPath("$.data.orderedItems[0].deliveryStatus").value("DELIVERED"))
        }

        @Test
        @DisplayName("실패 - 형식 오류 시 400 ORD_INVALID_SAP_NUMBER")
        fun invalidFormat() {
            every { clientOrderQueryService.getClientOrderDetail(eq(1L), eq("abc")) } throws InvalidSapOrderNumberException()

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", "abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORD_INVALID_SAP_NUMBER"))
        }

        @Test
        @DisplayName("실패 - 권한 없음 시 403 ORD_FORBIDDEN")
        fun forbidden() {
            every { clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)) } throws ClientOrderForbiddenException()

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", sapOrderNumber))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("ORD_FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - SAP 주문번호 미존재 시 404 ORD_SAP_NOT_FOUND")
        fun notFound() {
            every { clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)) } throws SapOrderNotFoundException()

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", sapOrderNumber))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ORD_SAP_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/client-orders/product-history")
    inner class GetAccountOrderHistoryTests {

        @Test
        @DisplayName("성공 - 주문일별 제품 그룹 200 OK")
        fun success() {
            val groups = listOf(
                OrderHistoryGroupResponse(
                    orderDate = "2026-05-06",
                    products = listOf(
                        OrderHistoryProductResponse("P001", "참깨라면"),
                        OrderHistoryProductResponse("P002", "진라면순한맛")
                    )
                ),
                OrderHistoryGroupResponse(
                    orderDate = "2026-05-04",
                    products = listOf(OrderHistoryProductResponse("P003", "열라면"))
                )
            )
            every {
                clientOrderQueryService.getAccountOrderHistory(
                    eq(1L), eq("0001234567"),
                    eq(LocalDate.of(2026, 5, 4)), eq(LocalDate.of(2026, 5, 6))
                )
            } returns groups

            mockMvc.perform(
                get("/api/v1/mobile/client-orders/product-history")
                    .param("accountCode", "0001234567")
                    .param("startDate", "2026-05-04")
                    .param("endDate", "2026-05-06")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderDate").value("2026-05-06"))
                .andExpect(jsonPath("$.data[0].products[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data[0].products[0].productName").value("참깨라면"))
                .andExpect(jsonPath("$.data[1].orderDate").value("2026-05-04"))
        }

        @Test
        @DisplayName("실패 - 필수 파라미터(accountCode) 누락 시 400")
        fun missingAccountCode() {
            mockMvc.perform(
                get("/api/v1/mobile/client-orders/product-history")
                    .param("startDate", "2026-05-04")
                    .param("endDate", "2026-05-06")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 권한 없음(사번 없음) 시 403 ORD_FORBIDDEN")
        fun forbidden() {
            every {
                clientOrderQueryService.getAccountOrderHistory(eq(1L), any(), any(), any())
            } throws ClientOrderForbiddenException()

            mockMvc.perform(
                get("/api/v1/mobile/client-orders/product-history")
                    .param("accountCode", "0001234567")
                    .param("startDate", "2026-05-04")
                    .param("endDate", "2026-05-06")
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("ORD_FORBIDDEN"))
        }
    }
}
