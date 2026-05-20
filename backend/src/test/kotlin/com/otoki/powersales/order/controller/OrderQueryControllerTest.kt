/* Order 모듈 전체 비활성화로 인해 주석 처리
package com.otoki.powersales.order.controller

import com.otoki.powersales.dto.response.CreditBalanceResponse
import com.otoki.powersales.order.dto.response.OrderHistoryProductResponse
import com.otoki.powersales.order.dto.response.ProductOrderInfoResponse
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.exception.ClientNotFoundException
import com.otoki.powersales.exception.ProductNotFoundException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.service.OrderQueryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import io.mockk.every
import io.mockk.verify
import com.ninjasquad.springmockk.MockkBean
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

    @MockkBean
    private lateinit var orderQueryService: OrderQueryService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== 주문이력 제품 조회 ==========

    @Nested
    @DisplayName("주문이력 제품 조회 - GET /api/v1/mobile/me/order-history/products")
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
            every { orderQueryService.getOrderHistoryProducts(
                eq(1L), null, null, null, null
            ) } returns page

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/me/order-history/products")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.content[0].productName").value("갈릭 아이올리소스 240g"))
                .andExpect(jsonPath("$.data.content[0].barcode").value("8801045570716"))
                .andExpect(jsonPath("$.data.content[0].storageType").value("냉장"))
                .andExpect(jsonPath("$.data.content[0].lastOrderDate").value("2026-02-08"))
                .andExpect(jsonPath("$.data.content[0].totalOrderCount").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(1))
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
            every { orderQueryService.getOrderHistoryProducts(
                eq(1L), any(), any(), null, null
            ) } returns page

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/me/order-history/products")
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
    @DisplayName("거래처 여신잔액 조회 - GET /api/v1/mobile/clients/{clientId}/credit-balance")
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
            every { orderQueryService.getClientCreditBalance(1L) } returns response

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/clients/1/credit-balance")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.clientId").value(1))
                .andExpect(jsonPath("$.data.clientName").value("롯데마트 응암점"))
                .andExpect(jsonPath("$.data.creditLimit").value(100000000))
                .andExpect(jsonPath("$.data.usedCredit").value(45000000))
                .andExpect(jsonPath("$.data.availableCredit").value(55000000))
                .andExpect(jsonPath("$.data.lastUpdatedAt").value("2026-02-10T10:00:00"))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("거래처 없음 - 404 CLIENT_NOT_FOUND")
        fun getClientCreditBalance_notFound_returns404() {
            // Given
            every { orderQueryService.getClientCreditBalance(999L) } throws ClientNotFoundException()

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/clients/999/credit-balance")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
        }
    }

    // ========== 제품 주문정보 조회 ==========

    @Nested
    @DisplayName("제품 주문정보 조회 - GET /api/v1/mobile/products/{productCode}/order-info")
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
            every { orderQueryService.getProductOrderInfo("01101123") } returns response

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/products/01101123/order-info")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("01101123"))
                .andExpect(jsonPath("$.data.productName").value("갈릭 아이올리소스 240g"))
                .andExpect(jsonPath("$.data.piecesPerBox").value(50))
                .andExpect(jsonPath("$.data.minOrderUnit").value(10))
                .andExpect(jsonPath("$.data.supplyQuantity").value(1000))
                .andExpect(jsonPath("$.data.dcQuantity").value(500))
                .andExpect(jsonPath("$.data.unitPrice").value(5000))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("제품 없음 - 404 PRODUCT_NOT_FOUND")
        fun getProductOrderInfo_notFound_returns404() {
            // Given
            every { orderQueryService.getProductOrderInfo("INVALID") } throws ProductNotFoundException("INVALID")

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/products/INVALID/order-info")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
        }
    }
}
*/