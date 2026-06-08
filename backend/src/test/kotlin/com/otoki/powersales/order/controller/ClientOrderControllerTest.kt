package com.otoki.powersales.order.controller

import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.ClientOrderItemResponse
import com.otoki.powersales.order.dto.response.ClientOrderSummaryResponse
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.exception.ClientNotFoundException
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
    @DisplayName("GET /api/v1/mobile/client-orders")
    inner class GetClientOrdersTests {

        @Test
        @DisplayName("성공 - 거래처별 주문 목록 페이지 200 OK")
        fun success() {
            val page = PageImpl(
                listOf(
                    ClientOrderSummaryResponse(
                        sapOrderNumber = "0300011396",
                        clientId = 10L,
                        clientName = "홍길동마트",
                        totalAmount = 1_250_000L
                    )
                ),
                PageRequest.of(0, 20),
                1
            )
            every {
                clientOrderQueryService.getClientOrders(eq(10L), eq(LocalDate.of(2026, 5, 6)), null, null)
            } returns page

            mockMvc.perform(
                get("/api/v1/mobile/client-orders")
                    .param("clientId", "10")
                    .param("deliveryDate", "2026-05-06")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].sapOrderNumber").value("0300011396"))
                .andExpect(jsonPath("$.data.content[0].clientId").value(10))
                .andExpect(jsonPath("$.data.content[0].clientName").value("홍길동마트"))
                .andExpect(jsonPath("$.data.content[0].totalAmount").value(1_250_000))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
        }

        @Test
        @DisplayName("실패 - 거래처 미존재 시 404 CLIENT_NOT_FOUND")
        fun clientNotFound() {
            every {
                clientOrderQueryService.getClientOrders(eq(99L), any(), null, null)
            } throws ClientNotFoundException()

            mockMvc.perform(
                get("/api/v1/mobile/client-orders").param("clientId", "99")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - clientId 누락 시 400")
        fun missingClientId() {
            mockMvc.perform(get("/api/v1/mobile/client-orders"))
                .andExpect(status().isBadRequest)
        }
    }

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
}
