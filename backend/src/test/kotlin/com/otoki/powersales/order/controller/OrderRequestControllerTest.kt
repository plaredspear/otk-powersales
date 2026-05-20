package com.otoki.powersales.order.controller

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.response.CancelledLineResponse
import com.otoki.powersales.order.dto.response.OrderCancelResponse
import com.otoki.powersales.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.order.dto.response.OrderRequestSummaryResponse
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.order.exception.InvalidOrderParameterException
import com.otoki.powersales.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.order.service.OrderCancelService
import com.otoki.powersales.order.service.OrderRequestService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import io.mockk.every
import io.mockk.verify
import com.ninjasquad.springmockk.MockkBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@WebMvcTest(OrderRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderRequestController 테스트")
class OrderRequestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var orderRequestService: OrderRequestService

    @MockkBean
    private lateinit var orderRequestCreateService: com.otoki.powersales.order.service.OrderRequestCreateService

    @MockkBean
    private lateinit var orderCancelService: OrderCancelService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/me/order-requests - 목록 조회")
    inner class GetMyOrderRequestsTests {

        @Test
        @DisplayName("성공 - 정상 응답")
        fun success() {
            val item = OrderRequestSummaryResponse(
                id = 12345L,
                orderRequestNumber = "OR-0001234",
                clientId = 5678L,
                clientName = "홍길동상회",
                orderDate = java.time.LocalDateTime.of(2026, 5, 4, 10, 0),
                deliveryDate = LocalDate.of(2026, 5, 6),
                totalAmount = BigDecimal("1234567.00"),
                orderRequestStatus = "APPROVED",
                isClosed = false,
            )
            val response = OrderRequestListResponse(
                items = listOf(item),
                total = 1,
                truncated = false,
                fetchedAt = OffsetDateTime.of(2026, 5, 5, 10, 30, 0, 0, ZoneOffset.ofHours(9)),
            )
            every {
                orderRequestService.getMyOrderRequests(
                    any(), any(), any(), any(), any(), any(), any(),
                )
            } returns response

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests")
                    .param("deliveryDateFrom", "2026-05-01")
                    .param("deliveryDateTo", "2026-05-07"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].id").value(12345))
                .andExpect(jsonPath("$.data.items[0].orderRequestNumber").value("OR-0001234"))
                .andExpect(jsonPath("$.data.items[0].clientName").value("홍길동상회"))
                .andExpect(jsonPath("$.data.items[0].orderRequestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.truncated").value(false))
        }

        @Test
        @DisplayName("실패 - 7일 초과 -> ORD_DATE_RANGE_TOO_WIDE")
        fun tooWideRange() {
            every {
                orderRequestService.getMyOrderRequests(
                    any(), any(), any(), any(), any(), any(), any(),
                )
            } throws OrderDateRangeTooWideException()

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests")
                    .param("deliveryDateFrom", "2026-05-01")
                    .param("deliveryDateTo", "2026-05-09"),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ORD_DATE_RANGE_TOO_WIDE"))
        }

        @Test
        @DisplayName("실패 - 잘못된 sortBy -> ORD_INVALID_PARAM")
        fun invalidSortBy() {
            every {
                orderRequestService.getMyOrderRequests(
                    any(), any(), any(), any(), any(), any(), any(),
                )
            } throws InvalidOrderParameterException("정렬 기준 오류")

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests")
                    .param("deliveryDateFrom", "2026-05-01")
                    .param("deliveryDateTo", "2026-05-07")
                    .param("sortBy", "createdAt"),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ORD_INVALID_PARAM"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/me/order-requests/{orderRequestId} - 상세 조회 (#595)")
    inner class GetOrderRequestDetailTests {

        @Test
        @DisplayName("성공 - 정상 응답")
        fun success() {
            val response = com.otoki.powersales.order.dto.response.OrderRequestDetailResponse(
                id = 12345L,
                orderRequestNumber = "OR-0001234",
                clientId = 5678L,
                clientName = "홍길동상회",
                clientDeadlineTime = "13:50",
                orderDate = java.time.LocalDateTime.of(2026, 5, 4, 10, 0),
                deliveryDate = LocalDate.of(2026, 5, 6),
                totalAmount = BigDecimal("1234567.00"),
                totalApprovedAmount = BigDecimal("1200000.00"),
                orderRequestStatus = OrderRequestStatus.APPROVED,
                isClosed = true,
                orderedItemCount = 1,
                orderedItems = listOf(
                    com.otoki.powersales.order.dto.response.OrderedItemResponse(
                        productCode = "1000023",
                        productName = "진라면 매운맛",
                        totalQuantityBoxes = BigDecimal("10"),
                        totalQuantityPieces = BigDecimal.valueOf(300L),
                        isCancelled = false,
                    ),
                ),
                orderProcessingStatusList = listOf(
                    com.otoki.powersales.order.dto.response.OrderProcessingStatusResponse(
                        sapOrderNumber = "0300004993",
                        items = listOf(
                            com.otoki.powersales.order.dto.response.ProcessingItemResponse(
                                productCode = "1000023",
                                productName = "진라면 매운맛",
                                deliveredQuantity = "10 BOX (300 EA)",
                                deliveryStatus = DeliveryStatus.DELIVERED,
                                driverName = "홍길동",
                                vehicle = "12가3456",
                                driverPhone = "010-1234-5678",
                                scheduleTime = "12:00",
                                completeTime = "14:30",
                            ),
                        ),
                    ),
                ),
                rejectedItems = null,
            )
            every { orderRequestService.getOrderRequestDetail(any(), any()) } returns response

            mockMvc.perform(get("/api/v1/mobile/me/order-requests/12345"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12345))
                .andExpect(jsonPath("$.data.orderRequestNumber").value("OR-0001234"))
                .andExpect(jsonPath("$.data.isClosed").value(true))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].sapOrderNumber").value("0300004993"))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].items[0].deliveryStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].items[0].driverName").value("홍길동"))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].items[0].scheduleTime").value("12:00"))
        }

        @Test
        @DisplayName("실패 - 본인 외 접근 -> 403")
        fun forbidden() {
            every { orderRequestService.getOrderRequestDetail(any(), any()) } throws com.otoki.powersales.order.exception.ForbiddenOrderAccessException()

            mockMvc.perform(get("/api/v1/mobile/me/order-requests/12345"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> 404")
        fun notFound() {
            every { orderRequestService.getOrderRequestDetail(any(), any()) } throws com.otoki.powersales.order.exception.OrderNotFoundException()

            mockMvc.perform(get("/api/v1/mobile/me/order-requests/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/me/order-requests/{id}/cancel - 주문 취소 (#597)")
    inner class CancelOrderRequestTests {

        @Test
        @DisplayName("성공 - 빈 본문(전체 취소) → 200")
        fun successFullCancel() {
            every { orderCancelService.cancel(any(), any(), any()) } returns
                OrderCancelResponse(
                    orderRequestId = 12345L,
                    orderRequestNumber = "ORD-20260504-12345",
                    orderRequestStatus = OrderRequestStatus.CANCELED,
                    cancelledLines = listOf(
                        CancelledLineResponse(
                            orderProductId = 101L,
                            lineNumber = BigDecimal.valueOf(10L),
                            productCode = "P001",
                            cancelledAt = java.time.LocalDateTime.of(2026, 5, 4, 13, 25),
                        ),
                    ),
                )

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderRequestId").value(12345))
                .andExpect(jsonPath("$.data.orderRequestStatus").value("주문취소"))
                .andExpect(jsonPath("$.data.cancelledLines[0].orderProductId").value(101))
                .andExpect(jsonPath("$.data.cancelledLines[0].productCode").value("P001"))
        }

        @Test
        @DisplayName("성공 - 부분 취소(orderProductIds) → 200")
        fun successPartialCancel() {
            every { orderCancelService.cancel(any(), any(), any()) } returns
                OrderCancelResponse(
                    orderRequestId = 12345L,
                    orderRequestNumber = "ORD-20260504-12345",
                    orderRequestStatus = OrderRequestStatus.APPROVED,
                    cancelledLines = listOf(
                        CancelledLineResponse(101L, BigDecimal.valueOf(10L), "P001", java.time.LocalDateTime.of(2026, 5, 4, 13, 25)),
                    ),
                )

            mockMvc.perform(
                post("/api/v1/mobile/me/order-requests/12345/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"orderProductIds":[101]}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.orderRequestStatus").value("승인완료"))
                .andExpect(jsonPath("$.data.cancelledLines.length()").value(1))
        }

        @Test
        @DisplayName("실패 - 마감 시각 초과 → 400 ORD_CANCEL_DEADLINE_PASSED")
        fun deadlinePassed() {
            every { orderCancelService.cancel(any(), any(), any()) } throws OrderCancelDeadlinePassedException()

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ORD_CANCEL_DEADLINE_PASSED"))
        }

        @Test
        @DisplayName("실패 - 잘못된 상태 → 400 ORD_CANCEL_INVALID_STATUS")
        fun invalidStatus() {
            every { orderCancelService.cancel(any(), any(), any()) } throws OrderCancelInvalidStatusException("DRAFT")

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ORD_CANCEL_INVALID_STATUS"))
        }

        @Test
        @DisplayName("실패 - SAP 송신 실패 → 502 ORD_CANCEL_SAP_FAILED")
        fun sapFailed() {
            every { orderCancelService.cancel(any(), any(), any()) } throws OrderCancelSapFailedException("SAP error")

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().isBadGateway)
                .andExpect(jsonPath("$.error.code").value("ORD_CANCEL_SAP_FAILED"))
        }
    }
}
