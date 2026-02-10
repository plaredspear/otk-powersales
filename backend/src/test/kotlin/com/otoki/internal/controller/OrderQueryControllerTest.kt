package com.otoki.internal.controller

import com.otoki.internal.dto.response.CreditBalanceResponse
import com.otoki.internal.dto.response.OrderHistoryProductResponse
import com.otoki.internal.dto.response.ProductOrderInfoResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderQueryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(OrderQueryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderQueryController 테스트")
class OrderQueryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var orderQueryService: OrderQueryService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== 주문이력 제품 조회 ==========

    @Nested
    @DisplayName("주문이력 제품 조회 - GET /api/v1/me/order-history/products")
    inner class GetOrderHistoryProducts {

        @Test
        @DisplayName("기본 조회 - 200 OK")
        fun getOrderHistoryProducts_default_returnsOk() {
            // Given
            val products = listOf(
                OrderHistoryProductResponse(
                    productCode = "P001",
                    productName = "갈릭 아이올리소스 240g",
                    barcode = "8801045570716",
                    storageType = "냉장",
                    categoryMid = "소스",
                    categorySub = "양념소스",
                    lastOrderDate = "2026-02-08",
                    totalOrderCount = 3
                )
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 1)
            whenever(orderQueryService.getOrderHistoryProducts(
                eq(1L), isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/order-history/products")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.content[0].product_name").value("갈릭 아이올리소스 240g"))
                .andExpect(jsonPath("$.data.content[0].barcode").value("8801045570716"))
                .andExpect(jsonPath("$.data.content[0].storage_type").value("냉장"))
                .andExpect(jsonPath("$.data.content[0].last_order_date").value("2026-02-08"))
                .andExpect(jsonPath("$.data.content[0].total_order_count").value(3))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("날짜 범위 필터 전달")
        fun getOrderHistoryProducts_withDateRange_passesParams() {
            // Given
            val page = PageImpl(
                emptyList<OrderHistoryProductResponse>(),
                PageRequest.of(0, 20),
                0
            )
            whenever(orderQueryService.getOrderHistoryProducts(
                eq(1L), any(), any(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/order-history/products")
                    .param("orderDateFrom", "2026-02-01")
                    .param("orderDateTo", "2026-02-10")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    // ========== 거래처 여신잔액 조회 ==========

    @Nested
    @DisplayName("거래처 여신잔액 조회 - GET /api/v1/clients/{clientId}/credit-balance")
    inner class GetClientCreditBalance {

        @Test
        @DisplayName("여신잔액 조회 성공 - 200 OK")
        fun getClientCreditBalance_success_returnsOk() {
            // Given
            val response = CreditBalanceResponse(
                clientId = 1L,
                clientName = "롯데마트 응암점",
                creditLimit = 100_000_000,
                usedCredit = 45_000_000,
                availableCredit = 55_000_000,
                lastUpdatedAt = "2026-02-10T10:00:00"
            )
            whenever(orderQueryService.getClientCreditBalance(1L)).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/clients/1/credit-balance")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client_id").value(1))
                .andExpect(jsonPath("$.data.client_name").value("롯데마트 응암점"))
                .andExpect(jsonPath("$.data.credit_limit").value(100000000))
                .andExpect(jsonPath("$.data.used_credit").value(45000000))
                .andExpect(jsonPath("$.data.available_credit").value(55000000))
                .andExpect(jsonPath("$.data.last_updated_at").value("2026-02-10T10:00:00"))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("거래처 없음 - 404 CLIENT_NOT_FOUND")
        fun getClientCreditBalance_notFound_returns404() {
            // Given
            whenever(orderQueryService.getClientCreditBalance(999L))
                .thenThrow(ClientNotFoundException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/clients/999/credit-balance")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
        }
    }

    // ========== 제품 주문정보 조회 ==========

    @Nested
    @DisplayName("제품 주문정보 조회 - GET /api/v1/products/{productCode}/order-info")
    inner class GetProductOrderInfo {

        @Test
        @DisplayName("제품 주문정보 조회 성공 - 200 OK")
        fun getProductOrderInfo_success_returnsOk() {
            // Given
            val response = ProductOrderInfoResponse(
                productCode = "01101123",
                productName = "갈릭 아이올리소스 240g",
                piecesPerBox = 50,
                minOrderUnit = 10,
                supplyQuantity = 1000,
                dcQuantity = 500,
                unitPrice = 5000
            )
            whenever(orderQueryService.getProductOrderInfo("01101123")).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/01101123/order-info")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_code").value("01101123"))
                .andExpect(jsonPath("$.data.product_name").value("갈릭 아이올리소스 240g"))
                .andExpect(jsonPath("$.data.pieces_per_box").value(50))
                .andExpect(jsonPath("$.data.min_order_unit").value(10))
                .andExpect(jsonPath("$.data.supply_quantity").value(1000))
                .andExpect(jsonPath("$.data.dc_quantity").value(500))
                .andExpect(jsonPath("$.data.unit_price").value(5000))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("제품 없음 - 404 PRODUCT_NOT_FOUND")
        fun getProductOrderInfo_notFound_returns404() {
            // Given
            whenever(orderQueryService.getProductOrderInfo("INVALID"))
                .thenThrow(ProductNotFoundException("INVALID"))

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/INVALID/order-info")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
        }
    }
}
