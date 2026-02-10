package com.otoki.internal.controller

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.*
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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

    // ========== 주문 상세 조회 테스트 ==========

    @Nested
    @DisplayName("주문 상세 조회 - GET /api/v1/me/orders/{orderId}")
    inner class GetOrderDetail {

        @Test
        @DisplayName("마감전 주문 상세 조회 성공 - 200 OK")
        fun getOrderDetail_beforeClosed_returnsOk() {
            // Given
            val detailResponse = OrderDetailResponse(
                id = 1L,
                orderRequestNumber = "OP00000074",
                clientId = 100L,
                clientName = "천사푸드",
                clientDeadlineTime = "13:40",
                orderDate = "2026-02-01",
                deliveryDate = "2026-02-04",
                totalAmount = 612000000L,
                totalApprovedAmount = 612000000L,
                approvalStatus = "APPROVED",
                isClosed = false,
                orderedItemCount = 2,
                orderedItems = listOf(
                    OrderedItemResponse(
                        productCode = "P001",
                        productName = "진라면 순한맛 120g*40입",
                        totalQuantityBoxes = 50.0,
                        totalQuantityPieces = 0,
                        isCancelled = false
                    ),
                    OrderedItemResponse(
                        productCode = "P002",
                        productName = "진라면 매운맛 120g*40입",
                        totalQuantityBoxes = 30.0,
                        totalQuantityPieces = 10,
                        isCancelled = false
                    )
                ),
                orderProcessingStatus = null,
                rejectedItems = null
            )
            whenever(orderService.getOrderDetail(eq(1L), eq(1L))).thenReturn(detailResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.order_request_number").value("OP00000074"))
                .andExpect(jsonPath("$.data.client_id").value(100))
                .andExpect(jsonPath("$.data.client_name").value("천사푸드"))
                .andExpect(jsonPath("$.data.client_deadline_time").value("13:40"))
                .andExpect(jsonPath("$.data.order_date").value("2026-02-01"))
                .andExpect(jsonPath("$.data.delivery_date").value("2026-02-04"))
                .andExpect(jsonPath("$.data.total_amount").value(612000000))
                .andExpect(jsonPath("$.data.total_approved_amount").value(612000000))
                .andExpect(jsonPath("$.data.approval_status").value("APPROVED"))
                .andExpect(jsonPath("$.data.is_closed").value(false))
                .andExpect(jsonPath("$.data.ordered_item_count").value(2))
                .andExpect(jsonPath("$.data.ordered_items").isArray)
                .andExpect(jsonPath("$.data.ordered_items.length()").value(2))
                .andExpect(jsonPath("$.data.ordered_items[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.ordered_items[0].product_name").value("진라면 순한맛 120g*40입"))
                .andExpect(jsonPath("$.data.ordered_items[0].total_quantity_boxes").value(50.0))
                .andExpect(jsonPath("$.data.ordered_items[0].total_quantity_pieces").value(0))
                .andExpect(jsonPath("$.data.ordered_items[0].is_cancelled").value(false))
                .andExpect(jsonPath("$.data.order_processing_status").doesNotExist())
                .andExpect(jsonPath("$.data.rejected_items").doesNotExist())
                .andExpect(jsonPath("$.message").value("조회 성공"))
        }

        @Test
        @DisplayName("마감후 주문 상세 조회 성공 (반려 없음) - 200 OK")
        fun getOrderDetail_afterClosed_withoutRejection_returnsOk() {
            // Given
            val detailResponse = OrderDetailResponse(
                id = 1L,
                orderRequestNumber = "OP00000074",
                clientId = 100L,
                clientName = "천사푸드",
                clientDeadlineTime = "13:40",
                orderDate = "2026-02-01",
                deliveryDate = "2026-02-04",
                totalAmount = 612000000L,
                totalApprovedAmount = 612000000L,
                approvalStatus = "APPROVED",
                isClosed = true,
                orderedItemCount = 2,
                orderedItems = listOf(
                    OrderedItemResponse(
                        productCode = "P001",
                        productName = "진라면 순한맛 120g*40입",
                        totalQuantityBoxes = 50.0,
                        totalQuantityPieces = 0,
                        isCancelled = false
                    )
                ),
                orderProcessingStatus = OrderProcessingStatusResponse(
                    sapOrderNumber = "SAP1234567",
                    items = listOf(
                        ProcessingItemResponse(
                            productCode = "P001",
                            productName = "진라면 순한맛 120g*40입",
                            deliveredQuantity = "50박스",
                            deliveryStatus = "출하완료"
                        )
                    )
                ),
                rejectedItems = null
            )
            whenever(orderService.getOrderDetail(eq(1L), eq(1L))).thenReturn(detailResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_closed").value(true))
                .andExpect(jsonPath("$.data.order_processing_status").exists())
                .andExpect(jsonPath("$.data.order_processing_status.sap_order_number").value("SAP1234567"))
                .andExpect(jsonPath("$.data.order_processing_status.items").isArray)
                .andExpect(jsonPath("$.data.order_processing_status.items.length()").value(1))
                .andExpect(jsonPath("$.data.order_processing_status.items[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.order_processing_status.items[0].product_name").value("진라면 순한맛 120g*40입"))
                .andExpect(jsonPath("$.data.order_processing_status.items[0].delivered_quantity").value("50박스"))
                .andExpect(jsonPath("$.data.order_processing_status.items[0].delivery_status").value("출하완료"))
                .andExpect(jsonPath("$.data.rejected_items").doesNotExist())
        }

        @Test
        @DisplayName("마감후 주문 상세 조회 (반려 있음) - 200 OK")
        fun getOrderDetail_afterClosed_withRejection_returnsOk() {
            // Given
            val detailResponse = OrderDetailResponse(
                id = 1L,
                orderRequestNumber = "OP00000074",
                clientId = 100L,
                clientName = "천사푸드",
                clientDeadlineTime = "13:40",
                orderDate = "2026-02-01",
                deliveryDate = "2026-02-04",
                totalAmount = 612000000L,
                totalApprovedAmount = 400000000L,
                approvalStatus = "APPROVED",
                isClosed = true,
                orderedItemCount = 2,
                orderedItems = listOf(
                    OrderedItemResponse(
                        productCode = "P001",
                        productName = "진라면 순한맛 120g*40입",
                        totalQuantityBoxes = 50.0,
                        totalQuantityPieces = 0,
                        isCancelled = false
                    ),
                    OrderedItemResponse(
                        productCode = "P002",
                        productName = "진라면 매운맛 120g*40입",
                        totalQuantityBoxes = 30.0,
                        totalQuantityPieces = 10,
                        isCancelled = true
                    )
                ),
                orderProcessingStatus = OrderProcessingStatusResponse(
                    sapOrderNumber = "SAP1234567",
                    items = listOf(
                        ProcessingItemResponse(
                            productCode = "P001",
                            productName = "진라면 순한맛 120g*40입",
                            deliveredQuantity = "50박스",
                            deliveryStatus = "출하완료"
                        )
                    )
                ),
                rejectedItems = listOf(
                    RejectedItemResponse(
                        productCode = "P002",
                        productName = "진라면 매운맛 120g*40입",
                        orderQuantityBoxes = 30,
                        rejectionReason = "재고부족"
                    )
                )
            )
            whenever(orderService.getOrderDetail(eq(1L), eq(1L))).thenReturn(detailResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_closed").value(true))
                .andExpect(jsonPath("$.data.total_amount").value(612000000))
                .andExpect(jsonPath("$.data.total_approved_amount").value(400000000))
                .andExpect(jsonPath("$.data.rejected_items").isArray)
                .andExpect(jsonPath("$.data.rejected_items.length()").value(1))
                .andExpect(jsonPath("$.data.rejected_items[0].product_code").value("P002"))
                .andExpect(jsonPath("$.data.rejected_items[0].product_name").value("진라면 매운맛 120g*40입"))
                .andExpect(jsonPath("$.data.rejected_items[0].order_quantity_boxes").value(30))
                .andExpect(jsonPath("$.data.rejected_items[0].rejection_reason").value("재고부족"))
        }

        @Test
        @DisplayName("존재하지 않는 주문 - 404 ORDER_NOT_FOUND")
        fun getOrderDetail_notFound_returns404() {
            // Given
            whenever(orderService.getOrderDetail(eq(1L), eq(999L)))
                .thenThrow(OrderNotFoundException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders/999")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("주문을 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("다른 사용자의 주문 - 403 FORBIDDEN")
        fun getOrderDetail_forbidden_returns403() {
            // Given
            whenever(orderService.getOrderDetail(eq(1L), eq(1L)))
                .thenThrow(ForbiddenOrderAccessException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/me/orders/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다"))
        }
    }

    // ========== 주문 재전송 테스트 ==========

    @Nested
    @DisplayName("주문 재전송 - POST /api/v1/me/orders/{orderId}/resend")
    inner class ResendOrder {

        @Test
        @DisplayName("재전송 성공 - 200 OK")
        fun resendOrder_success_returnsOk() {
            // Given
            doNothing().whenever(orderService).resendOrder(eq(1L), eq(1L))

            // When & Then
            mockMvc.perform(
                post("/api/v1/me/orders/1/resend")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.message").value("주문이 재전송되었습니다"))
        }

        @Test
        @DisplayName("잘못된 상태 - 400 INVALID_ORDER_STATUS")
        fun resendOrder_invalidStatus_returns400() {
            // Given
            whenever(orderService.resendOrder(eq(1L), eq(1L)))
                .thenThrow(InvalidOrderStatusException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/me/orders/1/resend")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ORDER_STATUS"))
                .andExpect(jsonPath("$.error.message").value("전송실패 상태의 주문만 재전송할 수 있습니다"))
        }

        @Test
        @DisplayName("마감후 - 400 ORDER_ALREADY_CLOSED")
        fun resendOrder_alreadyClosed_returns400() {
            // Given
            whenever(orderService.resendOrder(eq(1L), eq(1L)))
                .thenThrow(OrderAlreadyClosedException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/me/orders/1/resend")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORDER_ALREADY_CLOSED"))
                .andExpect(jsonPath("$.error.message").value("마감된 주문은 재전송할 수 없습니다"))
        }

        @Test
        @DisplayName("존재하지 않는 주문 - 404 ORDER_NOT_FOUND")
        fun resendOrder_notFound_returns404() {
            // Given
            whenever(orderService.resendOrder(eq(1L), eq(999L)))
                .thenThrow(OrderNotFoundException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/me/orders/999/resend")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("주문을 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("다른 사용자 주문 - 403 FORBIDDEN")
        fun resendOrder_forbidden_returns403() {
            // Given
            whenever(orderService.resendOrder(eq(1L), eq(1L)))
                .thenThrow(ForbiddenOrderAccessException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/me/orders/1/resend")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다"))
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
