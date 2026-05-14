package com.otoki.powersales.order.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.ClientOrderItemResponse
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.service.ClientOrderQueryService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(ClientOrderController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClientOrderController 테스트 (#593)")
class ClientOrderControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var clientOrderQueryService: ClientOrderQueryService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.authorities)
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
                totalApprovedAmount = 1_250_000L,
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
            whenever(clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)))
                .thenReturn(response)

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
            whenever(clientOrderQueryService.getClientOrderDetail(eq(1L), eq("abc")))
                .thenThrow(InvalidSapOrderNumberException())

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", "abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORD_INVALID_SAP_NUMBER"))
        }

        @Test
        @DisplayName("실패 - 권한 없음 시 403 ORD_FORBIDDEN")
        fun forbidden() {
            whenever(clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)))
                .thenThrow(ClientOrderForbiddenException())

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", sapOrderNumber))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("ORD_FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - SAP 주문번호 미존재 시 404 ORD_SAP_NOT_FOUND")
        fun notFound() {
            whenever(clientOrderQueryService.getClientOrderDetail(eq(1L), eq(sapOrderNumber)))
                .thenThrow(SapOrderNotFoundException())

            mockMvc.perform(get("/api/v1/mobile/client-orders/{sapOrderNumber}", sapOrderNumber))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ORD_SAP_NOT_FOUND"))
        }
    }
}
