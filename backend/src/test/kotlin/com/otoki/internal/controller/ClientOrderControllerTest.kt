package com.otoki.internal.controller

import com.otoki.internal.dto.response.ClientOrderDetailResponse
import com.otoki.internal.dto.response.ClientOrderItemResponse
import com.otoki.internal.dto.response.ClientOrderSummaryResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.ForbiddenClientAccessException
import com.otoki.internal.exception.InvalidOrderParameterException
import com.otoki.internal.exception.OrderNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ClientOrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(ClientOrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientOrderController 테스트")
class ClientOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var clientOrderService: ClientOrderService

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

    @Nested
    @DisplayName("GET /api/v1/client-orders - 목록 조회")
    inner class GetClientOrders {

        @Test
        @DisplayName("기본 조회 성공 - clientId만 지정")
        fun getClientOrders_withClientIdOnly_success() {
            // given
            val clientId = 100L
            val summaryList = listOf(
                ClientOrderSummaryResponse(
                    sapOrderNumber = "SO001",
                    clientId = clientId,
                    clientName = "거래처A",
                    totalAmount = 500000L
                ),
                ClientOrderSummaryResponse(
                    sapOrderNumber = "SO002",
                    clientId = clientId,
                    clientName = "거래처A",
                    totalAmount = 300000L
                )
            )
            val page = PageImpl(summaryList, PageRequest.of(0, 20), summaryList.size.toLong())

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(null),
                    page = eq(null),
                    size = eq(null)
                )
            ).thenReturn(page)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].sap_order_number").value("SO001"))
                .andExpect(jsonPath("$.data.content[0].client_id").value(clientId))
                .andExpect(jsonPath("$.data.content[0].client_name").value("거래처A"))
                .andExpect(jsonPath("$.data.content[0].total_amount").value(500000))
                .andExpect(jsonPath("$.data.content[1].sap_order_number").value("SO002"))
                .andExpect(jsonPath("$.data.content[1].total_amount").value(300000))
                .andExpect(jsonPath("$.data.total_elements").value(2))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
        }

        @Test
        @DisplayName("납기일 지정 조회")
        fun getClientOrders_withDeliveryDate_success() {
            // given
            val clientId = 100L
            val deliveryDate = LocalDate.of(2024, 3, 15)
            val summaryList = listOf(
                ClientOrderSummaryResponse(
                    sapOrderNumber = "SO003",
                    clientId = clientId,
                    clientName = "거래처B",
                    totalAmount = 450000L
                )
            )
            val page = PageImpl(summaryList, PageRequest.of(0, 20), summaryList.size.toLong())

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(deliveryDate),
                    page = eq(null),
                    size = eq(null)
                )
            ).thenReturn(page)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
                    .param("deliveryDate", "2024-03-15")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].sap_order_number").value("SO003"))
        }

        @Test
        @DisplayName("페이지네이션 적용 조회")
        fun getClientOrders_withPagination_success() {
            // given
            val clientId = 100L
            val page = 1
            val size = 10
            val summaryList = listOf(
                ClientOrderSummaryResponse(
                    sapOrderNumber = "SO011",
                    clientId = clientId,
                    clientName = "거래처C",
                    totalAmount = 200000L
                )
            )
            val pagedResult = PageImpl(summaryList, PageRequest.of(page, size), 25L)

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(null),
                    page = eq(page),
                    size = eq(size)
                )
            ).thenReturn(pagedResult)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
                    .param("page", page.toString())
                    .param("size", size.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.number").value(page))
                .andExpect(jsonPath("$.data.size").value(size))
                .andExpect(jsonPath("$.data.total_elements").value(25))
        }

        @Test
        @DisplayName("빈 결과 조회")
        fun getClientOrders_emptyResult_success() {
            // given
            val clientId = 100L
            val emptyPage = PageImpl<ClientOrderSummaryResponse>(emptyList(), PageRequest.of(0, 20), 0L)

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(null),
                    page = eq(null),
                    size = eq(null)
                )
            ).thenReturn(emptyPage)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }

        @Test
        @DisplayName("clientId 미입력 - 400 Bad Request")
        fun getClientOrders_missingClientId_badRequest() {
            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("존재하지 않는 거래처 - 404 NOT_FOUND")
        fun getClientOrders_clientNotFound_notFound() {
            // given
            val clientId = 999L

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(null),
                    page = eq(null),
                    size = eq(null)
                )
            ).thenThrow(ClientNotFoundException())

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("거래처를 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("접근 권한 없음 - 403 FORBIDDEN")
        fun getClientOrders_forbiddenAccess_forbidden() {
            // given
            val clientId = 100L

            whenever(
                clientOrderService.getClientOrders(
                    userId = eq(testPrincipal.userId),
                    clientId = eq(clientId),
                    deliveryDate = eq(null),
                    page = eq(null),
                    size = eq(null)
                )
            ).thenThrow(ForbiddenClientAccessException())

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders")
                    .param("clientId", clientId.toString())
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/client-orders/{sapOrderNumber} - 상세 조회")
    inner class GetClientOrderDetail {

        @Test
        @DisplayName("정상 조회 성공 - 모든 필드 포함")
        fun getClientOrderDetail_success() {
            // given
            val sapOrderNumber = "SO001"
            val detailResponse = ClientOrderDetailResponse(
                sapOrderNumber = sapOrderNumber,
                clientId = 100L,
                clientName = "거래처A",
                clientDeadlineTime = "12:00",
                orderDate = "2024-03-10",
                deliveryDate = "2024-03-15",
                totalApprovedAmount = 500000L,
                orderedItemCount = 3,
                orderedItems = listOf(
                    ClientOrderItemResponse(
                        productCode = "P001",
                        productName = "제품1",
                        deliveredQuantity = "10",
                        deliveryStatus = "배송완료"
                    ),
                    ClientOrderItemResponse(
                        productCode = "P002",
                        productName = "제품2",
                        deliveredQuantity = "5",
                        deliveryStatus = "배송중"
                    ),
                    ClientOrderItemResponse(
                        productCode = "P003",
                        productName = "제품3",
                        deliveredQuantity = "20",
                        deliveryStatus = "배송대기"
                    )
                )
            )

            whenever(
                clientOrderService.getClientOrderDetail(
                    userId = eq(testPrincipal.userId),
                    sapOrderNumber = eq(sapOrderNumber)
                )
            ).thenReturn(detailResponse)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders/{sapOrderNumber}", sapOrderNumber)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.sap_order_number").value(sapOrderNumber))
                .andExpect(jsonPath("$.data.client_id").value(100))
                .andExpect(jsonPath("$.data.client_name").value("거래처A"))
                .andExpect(jsonPath("$.data.client_deadline_time").value("12:00"))
                .andExpect(jsonPath("$.data.order_date").value("2024-03-10"))
                .andExpect(jsonPath("$.data.delivery_date").value("2024-03-15"))
                .andExpect(jsonPath("$.data.total_approved_amount").value(500000))
                .andExpect(jsonPath("$.data.ordered_item_count").value(3))
                .andExpect(jsonPath("$.data.ordered_items").isArray)
                .andExpect(jsonPath("$.data.ordered_items.length()").value(3))
                .andExpect(jsonPath("$.data.ordered_items[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.ordered_items[0].product_name").value("제품1"))
                .andExpect(jsonPath("$.data.ordered_items[0].delivered_quantity").value("10"))
                .andExpect(jsonPath("$.data.ordered_items[0].delivery_status").value("배송완료"))
                .andExpect(jsonPath("$.data.ordered_items[1].product_code").value("P002"))
                .andExpect(jsonPath("$.data.ordered_items[1].delivery_status").value("배송중"))
                .andExpect(jsonPath("$.data.ordered_items[2].product_code").value("P003"))
                .andExpect(jsonPath("$.data.ordered_items[2].delivery_status").value("배송대기"))
        }

        @Test
        @DisplayName("clientDeadlineTime이 null인 경우")
        fun getClientOrderDetail_nullDeadlineTime_success() {
            // given
            val sapOrderNumber = "SO002"
            val detailResponse = ClientOrderDetailResponse(
                sapOrderNumber = sapOrderNumber,
                clientId = 200L,
                clientName = "거래처B",
                clientDeadlineTime = null,
                orderDate = "2024-03-11",
                deliveryDate = "2024-03-16",
                totalApprovedAmount = 300000L,
                orderedItemCount = 1,
                orderedItems = listOf(
                    ClientOrderItemResponse(
                        productCode = "P100",
                        productName = "제품100",
                        deliveredQuantity = "15",
                        deliveryStatus = "배송완료"
                    )
                )
            )

            whenever(
                clientOrderService.getClientOrderDetail(
                    userId = eq(testPrincipal.userId),
                    sapOrderNumber = eq(sapOrderNumber)
                )
            ).thenReturn(detailResponse)

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders/{sapOrderNumber}", sapOrderNumber)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sap_order_number").value(sapOrderNumber))
                .andExpect(jsonPath("$.data.client_deadline_time").isEmpty)
        }

        @Test
        @DisplayName("존재하지 않는 SAP 주문번호 - 404 NOT_FOUND")
        fun getClientOrderDetail_orderNotFound_notFound() {
            // given
            val sapOrderNumber = "INVALID_ORDER"

            whenever(
                clientOrderService.getClientOrderDetail(
                    userId = eq(testPrincipal.userId),
                    sapOrderNumber = eq(sapOrderNumber)
                )
            ).thenThrow(OrderNotFoundException())

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders/{sapOrderNumber}", sapOrderNumber)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("주문을 찾을 수 없습니다"))
        }

        @Test
        @DisplayName("접근 권한 없음 - 403 FORBIDDEN")
        fun getClientOrderDetail_forbiddenAccess_forbidden() {
            // given
            val sapOrderNumber = "SO001"

            whenever(
                clientOrderService.getClientOrderDetail(
                    userId = eq(testPrincipal.userId),
                    sapOrderNumber = eq(sapOrderNumber)
                )
            ).thenThrow(ForbiddenClientAccessException())

            // when & then
            mockMvc.perform(
                get("/api/v1/client-orders/{sapOrderNumber}", sapOrderNumber)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.error.message").value("접근 권한이 없습니다"))
        }
    }
}
