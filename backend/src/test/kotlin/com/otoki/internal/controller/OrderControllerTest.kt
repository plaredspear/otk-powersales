package com.otoki.internal.controller

import com.otoki.internal.dto.response.OrderSummaryResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.InvalidDateRangeException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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

@WebMvcTest(OrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var orderService: OrderService

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

    // ========== 성공 케이스 ==========

    @Nested
    @DisplayName("조회 성공 케이스")
    inner class SuccessCases {

        @Test
        @DisplayName("기본 조회 - 200 OK, 주문 목록 반환")
        fun getMyOrders_defaultParams_returnsOk() {
            // Given
            val orders = listOf(
                createOrderResponse(1L, "OP00000074", "천사푸드", "APPROVED"),
                createOrderResponse(2L, "OP00000075", "행복식품", "PENDING")
            )
            val page = PageImpl(orders, PageRequest.of(0, 20), 2)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].order_request_number").value("OP00000074"))
                .andExpect(jsonPath("$.data.content[0].client_id").value(100))
                .andExpect(jsonPath("$.data.content[0].client_name").value("천사푸드"))
                .andExpect(jsonPath("$.data.content[0].approval_status").value("APPROVED"))
                .andExpect(jsonPath("$.data.content[0].order_date").value("2026-02-01"))
                .andExpect(jsonPath("$.data.content[0].delivery_date").value("2026-02-04"))
                .andExpect(jsonPath("$.data.content[0].total_amount").value(612000000))
                .andExpect(jsonPath("$.data.content[0].is_closed").value(false))
                .andExpect(jsonPath("$.data.total_elements").value(2))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("거래처 필터 - clientId 파라미터 전달")
        fun getMyOrders_withClientId_passesParam() {
            // Given
            val orders = listOf(
                createOrderResponse(1L, "OP00000074", "천사푸드", "APPROVED")
            )
            val page = PageImpl(orders, PageRequest.of(0, 20), 1)
            whenever(orderService.getMyOrders(
                eq(1L), eq(123L), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("clientId", "123")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
        }

        @Test
        @DisplayName("상태 필터 - status 파라미터 전달")
        fun getMyOrders_withStatus_passesParam() {
            // Given
            val orders = listOf(
                createOrderResponse(1L, "OP00000074", "천사푸드", "APPROVED")
            )
            val page = PageImpl(orders, PageRequest.of(0, 20), 1)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), eq("APPROVED"), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("status", "APPROVED")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("납기일 범위 필터 - deliveryDateFrom/To 전달")
        fun getMyOrders_withDateRange_passesParams() {
            // Given
            val page = PageImpl(emptyList<OrderSummaryResponse>(), PageRequest.of(0, 20), 0)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("deliveryDateFrom", "2026-02-01")
                    .param("deliveryDateTo", "2026-02-28")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("정렬 변경 - sortBy/sortDir 전달")
        fun getMyOrders_withSort_passesParams() {
            // Given
            val page = PageImpl(emptyList<OrderSummaryResponse>(), PageRequest.of(0, 20), 0)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                eq("totalAmount"), eq("DESC"), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("sortBy", "totalAmount")
                    .param("sortDir", "DESC")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("페이지네이션 - page/size 전달")
        fun getMyOrders_withPagination_passesParams() {
            // Given
            val orders = listOf(
                createOrderResponse(3L, "OP00000076", "미래식품", "PENDING")
            )
            val page = PageImpl(orders, PageRequest.of(1, 10), 25)
            whenever(orderService.getMyOrders(
                any(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), anyOrNull(), anyOrNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("page", "1")
                    .param("size", "10")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total_elements").value(25))
                .andExpect(jsonPath("$.data.total_pages").value(3))
        }

        @Test
        @DisplayName("빈 결과 - 200 OK, 빈 목록")
        fun getMyOrders_noResults_returnsEmptyList() {
            // Given
            val page = PageImpl(emptyList<OrderSummaryResponse>(), PageRequest.of(0, 20), 0)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }
    }

    // ========== 에러 케이스 ==========

    @Nested
    @DisplayName("조회 에러 케이스")
    inner class ErrorCases {

        @Test
        @DisplayName("잘못된 상태값 - 400 INVALID_PARAMETER")
        fun getMyOrders_invalidStatus_returnsBadRequest() {
            // Given
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), eq("INVALID"), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenThrow(InvalidOrderParameterException(
                "승인상태는 APPROVED, PENDING, SEND_FAILED, RESEND 중 하나여야 합니다"
            ))

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("status", "INVALID")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.error.message").value("승인상태는 APPROVED, PENDING, SEND_FAILED, RESEND 중 하나여야 합니다"))
        }

        @Test
        @DisplayName("잘못된 날짜 범위 - 400 INVALID_DATE_RANGE")
        fun getMyOrders_invalidDateRange_returnsBadRequest() {
            // Given
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), any(), any(),
                isNull(), isNull(), isNull(), isNull()
            )).thenThrow(InvalidDateRangeException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("deliveryDateFrom", "2026-02-28")
                    .param("deliveryDateTo", "2026-02-01")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
                .andExpect(jsonPath("$.error.message").value("납기일 종료일은 시작일 이후여야 합니다"))
        }

        @Test
        @DisplayName("잘못된 sortBy - 400 INVALID_PARAMETER")
        fun getMyOrders_invalidSortBy_returnsBadRequest() {
            // Given
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                eq("invalidField"), isNull(), isNull(), isNull()
            )).thenThrow(InvalidOrderParameterException(
                "정렬 기준은 orderDate, deliveryDate, totalAmount 중 하나여야 합니다"
            ))

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .param("sortBy", "invalidField")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }

    // ========== clientDeadlineTime 필드 검증 ==========

    @Nested
    @DisplayName("clientDeadlineTime 필드")
    inner class ClientDeadlineTime {

        @Test
        @DisplayName("마감시간이 있는 경우 응답에 포함")
        fun getMyOrders_withDeadlineTime_includesInResponse() {
            // Given
            val order = OrderSummaryResponse(
                id = 1L,
                orderRequestNumber = "OP00000074",
                clientId = 100L,
                clientName = "천사푸드",
                clientDeadlineTime = "13:40",
                orderDate = "2026-02-01",
                deliveryDate = "2026-02-04",
                totalAmount = 612000000L,
                approvalStatus = "APPROVED",
                isClosed = false
            )
            val page = PageImpl(listOf(order), PageRequest.of(0, 20), 1)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].client_deadline_time").value("13:40"))
        }

        @Test
        @DisplayName("마감시간이 없는 경우 null")
        fun getMyOrders_withoutDeadlineTime_returnsNull() {
            // Given
            val order = OrderSummaryResponse(
                id = 1L,
                orderRequestNumber = "OP00000074",
                clientId = 100L,
                clientName = "천사푸드",
                clientDeadlineTime = null,
                orderDate = "2026-02-01",
                deliveryDate = "2026-02-04",
                totalAmount = 612000000L,
                approvalStatus = "APPROVED",
                isClosed = false
            )
            val page = PageImpl(listOf(order), PageRequest.of(0, 20), 1)
            whenever(orderService.getMyOrders(
                eq(1L), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull()
            )).thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].client_deadline_time").doesNotExist())
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createOrderResponse(
        id: Long,
        orderRequestNumber: String,
        clientName: String,
        approvalStatus: String
    ): OrderSummaryResponse {
        return OrderSummaryResponse(
            id = id,
            orderRequestNumber = orderRequestNumber,
            clientId = 100L,
            clientName = clientName,
            clientDeadlineTime = "13:40",
            orderDate = "2026-02-01",
            deliveryDate = "2026-02-04",
            totalAmount = 612000000L,
            approvalStatus = approvalStatus,
            isClosed = false
        )
    }
}
