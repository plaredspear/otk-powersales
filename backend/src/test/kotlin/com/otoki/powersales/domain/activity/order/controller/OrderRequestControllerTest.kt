package com.otoki.powersales.domain.activity.order.controller

import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.activity.order.dto.response.CancelledLineResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderCancelResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestListResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestSummaryResponse
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelInvalidStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderCancelSapFailedException
import com.otoki.powersales.domain.activity.order.exception.OrderDateRangeTooWideException
import com.otoki.powersales.domain.activity.order.service.OrderCancelService
import com.otoki.powersales.domain.activity.order.service.OrderRequestService
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
import com.otoki.powersales.domain.activity.order.dto.response.OrderProcessingStatusResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.OrderedItemResponse
import com.otoki.powersales.domain.activity.order.dto.response.ProcessingItemResponse
import com.otoki.powersales.domain.activity.order.exception.ForbiddenOrderAccessException
import com.otoki.powersales.domain.activity.order.exception.OrderNotFoundException
import com.otoki.powersales.domain.activity.order.service.OrderRequestCreateService
import com.otoki.powersales.domain.activity.order.service.OrderRequestResendService
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderStatusException
import com.otoki.powersales.domain.activity.order.exception.OrderAlreadyClosedException
import io.mockk.Runs
import io.mockk.just
import io.mockk.verify
import org.springframework.http.MediaType
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
class OrderRequestControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var orderRequestService: OrderRequestService
    @MockkBean private lateinit var orderRequestCreateService: OrderRequestCreateService
    @MockkBean private lateinit var orderCancelService: OrderCancelService
    @MockkBean private lateinit var orderRequestResendService: OrderRequestResendService
    @MockkBean private lateinit var featureToggleService: com.otoki.powersales.admin.tools.feature.service.FeatureToggleService

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
                orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
                deliveryDate = LocalDate.of(2026, 5, 6),
                totalAmount = BigDecimal("1234567.00"),
                orderRequestStatus = "APPROVED",
                orderRequestStatusName = "승인완료",
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
                .andExpect(jsonPath("$.data.items[0].orderRequestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.items[0].orderRequestStatusName").value("승인완료"))
                .andExpect(jsonPath("$.data.total").value(1))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.activity.order.controller.OrderRequestControllerTest#getMyExceptions")
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
            val response = OrderRequestDetailResponse(
                id = 12345L,
                orderRequestNumber = "OR-0001234",
                clientId = 5678L,
                clientName = "홍길동상회",
                clientDeadlineTime = "13:50",
                orderDate = LocalDateTime.of(2026, 5, 4, 10, 0),
                deliveryDate = LocalDate.of(2026, 5, 6),
                totalAmount = BigDecimal("1234567.00"),
                totalApprovedAmount = BigDecimal("1200000.00"),
                orderRequestStatus = OrderRequestStatus.APPROVED.name,
                orderRequestStatusName = OrderRequestStatus.APPROVED.displayName,
                isClosed = true,
                cancelable = false,
                registrationInFlight = false,
                orderedItemCount = 1,
                orderedItems = listOf(
                    OrderedItemResponse(
                        orderProductId = 101L,
                        productCode = "1000023",
                        productName = "진라면 매운맛",
                        totalQuantityBoxes = BigDecimal("10"),
                        totalQuantityPieces = BigDecimal.valueOf(300L),
                        isCancelled = false,
                        isCancelRequested = false,
                    ),
                ),
                orderProcessingStatusList = listOf(
                    OrderProcessingStatusResponse(
                        sapOrderNumber = "0300004993",
                        items = listOf(
                            ProcessingItemResponse(
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
                // 상태는 코드(영문) + 한글 표시명을 분리해 내려준다 (모바일은 표시명을 그대로 출력)
                .andExpect(jsonPath("$.data.orderRequestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.orderRequestStatusName").value("승인완료"))
                .andExpect(jsonPath("$.data.isClosed").value(true))
                .andExpect(jsonPath("$.data.orderProcessingStatusList[0].items[0].deliveryStatus").value("DELIVERED"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.activity.order.controller.OrderRequestControllerTest#getDetailExceptions")
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
            // 모바일 API 는 enum 을 영문 코드로 직렬화한다 (목록/상세와 정합, 한글 displayName 금지)
            every { orderCancelService.cancel(any(), any(), any()) } returns
                OrderCancelResponse(
                    orderRequestId = 12345L,
                    orderRequestNumber = "OR00012345",
                    orderRequestStatus = OrderRequestStatus.CANCEL_REQUESTED.name,
                    orderRequestStatusName = OrderRequestStatus.CANCEL_REQUESTED.displayName,
                    cancelledLines = listOf(
                        CancelledLineResponse(
                            orderProductId = 101L,
                            lineNumber = BigDecimal.valueOf(10L),
                            productCode = "P001",
                            cancelledAt = LocalDateTime.of(2026, 5, 4, 13, 25),
                        ),
                    ),
                )

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/cancel"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.orderRequestStatus").value("CANCEL_REQUESTED"))
                .andExpect(jsonPath("$.data.orderRequestStatusName").value("주문취소"))
                .andExpect(jsonPath("$.data.cancelledLines[0].productCode").value("P001"))
        }

        @Test
        @DisplayName("성공 - 부분 취소(orderProductIds) → 200, status=APPROVED 유지")
        fun successPartialCancel() {
            // 부분 취소 시 orderRequestStatus 는 APPROVED 유지 → 영문 코드 "APPROVED"
            every { orderCancelService.cancel(any(), any(), any()) } returns
                OrderCancelResponse(
                    orderRequestId = 12345L,
                    orderRequestNumber = "OR00012345",
                    orderRequestStatus = OrderRequestStatus.APPROVED.name,
                    orderRequestStatusName = OrderRequestStatus.APPROVED.displayName,
                    cancelledLines = listOf(
                        CancelledLineResponse(101L, BigDecimal.valueOf(10L), "P001", LocalDateTime.of(2026, 5, 4, 13, 25)),
                    ),
                )

            mockMvc.perform(
                post("/api/v1/mobile/me/order-requests/12345/cancel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"orderProductIds":[101]}"""),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.orderRequestStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.orderRequestStatusName").value("승인완료"))
                .andExpect(jsonPath("$.data.cancelledLines.length()").value(1))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.activity.order.controller.OrderRequestControllerTest#cancelExceptions")
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

    @Nested
    @DisplayName("POST /api/v1/mobile/me/order-requests/{id}/resend - 주문 재전송 (F18)")
    inner class ResendOrderRequestTests {

        @Test
        @DisplayName("성공 - 재전송 접수 → 200, 서비스 호출")
        fun success() {
            every { orderRequestResendService.resend(12345L, any()) } just Runs

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/resend"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("주문이 재전송되었습니다"))

            verify(exactly = 1) { orderRequestResendService.resend(12345L, any()) }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.activity.order.controller.OrderRequestControllerTest#resendExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun resend_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String,
        ) {
            every { orderRequestResendService.resend(any(), any()) } throws exception

            mockMvc.perform(post("/api/v1/mobile/me/order-requests/12345/resend"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/me/order-requests/product-history - 거래처 주문이력")
    inner class GetAccountOrderHistoryTests {

        @Test
        @DisplayName("성공 - 주문일별 제품 그룹 200 OK")
        fun success() {
            val groups = listOf(
                OrderHistoryGroupResponse(
                    orderDate = "2026-05-06",
                    products = listOf(
                        historyDto("P001", "참깨라면"),
                        historyDto("P002", "진라면순한맛"),
                    ),
                ),
                OrderHistoryGroupResponse(
                    orderDate = "2026-05-04",
                    products = listOf(historyDto("P003", "열라면")),
                ),
            )
            every {
                orderRequestService.getAccountOrderHistory(
                    eq(1L), eq("0001234567"),
                    eq(LocalDate.of(2026, 5, 4)), eq(LocalDate.of(2026, 5, 6)),
                )
            } returns groups

            mockMvc.perform(
                get("/api/v1/mobile/me/order-requests/product-history")
                    .param("accountCode", "0001234567")
                    .param("startDate", "2026-05-04")
                    .param("endDate", "2026-05-06"),
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
                get("/api/v1/mobile/me/order-requests/product-history")
                    .param("startDate", "2026-05-04")
                    .param("endDate", "2026-05-06"),
            )
                .andExpect(status().isBadRequest)
        }

        private fun historyDto(code: String, name: String): OrderProductDto =
            OrderProductDto(
                productCode = code,
                productName = name,
                barcode = "8800000000000",
                storageType = "실온",
                shelfLife = "9개월",
                unitPrice = 1000,
                boxSize = 8,
                isFavorite = false,
                categoryMid = null,
                categorySub = null,
                productType = null,
                tasteGiftType = null,
            )
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
                ForbiddenOrderAccessException(),
                403,
                "FORBIDDEN",
            ),
            Arguments.of(
                "notFound -> 404 ORDER_NOT_FOUND",
                OrderNotFoundException(),
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

        @JvmStatic
        fun resendExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "invalidStatus -> 400 INVALID_ORDER_STATUS",
                InvalidOrderStatusException(),
                400,
                "INVALID_ORDER_STATUS",
            ),
            Arguments.of(
                "alreadyClosed -> 400 ORDER_ALREADY_CLOSED",
                OrderAlreadyClosedException("마감된 주문은 재전송할 수 없습니다"),
                400,
                "ORDER_ALREADY_CLOSED",
            ),
            Arguments.of(
                "forbidden -> 403 FORBIDDEN",
                ForbiddenOrderAccessException(),
                403,
                "FORBIDDEN",
            ),
            Arguments.of(
                "notFound -> 404 ORDER_NOT_FOUND",
                OrderNotFoundException(),
                404,
                "ORDER_NOT_FOUND",
            ),
        )
    }
}
