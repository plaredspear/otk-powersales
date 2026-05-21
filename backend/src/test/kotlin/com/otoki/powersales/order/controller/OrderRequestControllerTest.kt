package com.otoki.powersales.order.controller

import com.otoki.powersales.common.test.MobileControllerTestSupport
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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import io.mockk.every
import com.ninjasquad.springmockk.MockkBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@WebMvcTest(OrderRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderRequestController 테스트")
class OrderRequestControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var orderRequestService: OrderRequestService
    @MockkBean private lateinit var orderRequestCreateService: com.otoki.powersales.order.service.OrderRequestCreateService
    @MockkBean private lateinit var orderCancelService: OrderCancelService

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
                orderRequestService.getMyOrderRequests(any(), any(), any(), any(), any(), any(), any())
            } returns response

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests")
                    .param("deliveryDateFrom", "2026-05-01")
                    .param("deliveryDateTo", "2026-05-07"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.items[0].orderRequestNumber").value("OR-0001234"))
                .andExpect(jsonPath("$.data.total").value(1))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.order.controller.OrderRequestControllerTest#getMyExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun getMy_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedCode: String,
        ) {
            every {
                orderRequestService.getMyOrderRequests(any(), any(), any(), any(), any(), any(), any())
            } throws exception

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests")
                    .param("deliveryDateFrom", "2026-05-01")
                    .param("deliveryDateTo", "2026-05-07"),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
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
                .andExpect(jsonPath("$.data.orderRequestNumber").value("OR-0001234"))
                .andExpect(jsonPath("$.data.isClosed").value(true))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].items[0].deliveryStatus").value("DELIVERED"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.order.controller.OrderRequestControllerTest#getDetailExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun getDetail_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String,
        ) {
            every { orderRequestService.getOrderRequestDetail(any(), any()) } throws exception

            mockMvc.perform(get("/api/v1/mobile/me/order-requests/12345"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/me/order-requests/{id}/cancel - 주문 취소 (#597)")
    inner class CancelOrderRequestTests {

        @Test
        @DisplayName("성공 - 빈 본문(전체 취소) → 200")
        fun successFullCancel() {
            // controller 후처리: orderRequestStatus enum → 한글 표시명 변환 ("주문취소") — 가드레일 5.3
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
                .andExpect(jsonPath("$.data.orderRequestStatus").value("주문취소"))
                .andExpect(jsonPath("$.data.cancelledLines[0].productCode").value("P001"))
        }

        @Test
        @DisplayName("성공 - 부분 취소(orderProductIds) → 200, status=승인완료 유지")
        fun successPartialCancel() {
            // controller 후처리: 부분 취소 시 orderRequestStatus 가 APPROVED 유지 → 한글 "승인완료"
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

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.order.controller.OrderRequestControllerTest#cancelExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun cancel_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String,
        ) {
            every { orderCancelService.cancel(any(), any(), any()) } throws exception

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun getMyExceptions(): List<Arguments> = listOf(
            Arguments.of("tooWideRange -> ORD_DATE_RANGE_TOO_WIDE", OrderDateRangeTooWideException(), "ORD_DATE_RANGE_TOO_WIDE"),
            Arguments.of(
                "invalidSortBy -> ORD_INVALID_PARAM",
                InvalidOrderParameterException("정렬 기준 오류"),
                "ORD_INVALID_PARAM",
            ),
        )

        @JvmStatic
        fun getDetailExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "forbidden -> 403 FORBIDDEN",
                com.otoki.powersales.order.exception.ForbiddenOrderAccessException(),
                403,
                "FORBIDDEN",
            ),
            Arguments.of(
                "notFound -> 404 ORDER_NOT_FOUND",
                com.otoki.powersales.order.exception.OrderNotFoundException(),
                404,
                "ORDER_NOT_FOUND",
            ),
        )

        @JvmStatic
        fun cancelExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "deadlinePassed -> 400 ORD_CANCEL_DEADLINE_PASSED",
                OrderCancelDeadlinePassedException(),
                400,
                "ORD_CANCEL_DEADLINE_PASSED",
            ),
            Arguments.of(
                "invalidStatus -> 400 ORD_CANCEL_INVALID_STATUS",
                OrderCancelInvalidStatusException("DRAFT"),
                400,
                "ORD_CANCEL_INVALID_STATUS",
            ),
            Arguments.of(
                "sapFailed -> 502 ORD_CANCEL_SAP_FAILED",
                OrderCancelSapFailedException("SAP error"),
                502,
                "ORD_CANCEL_SAP_FAILED",
            ),
        )
    }
}
