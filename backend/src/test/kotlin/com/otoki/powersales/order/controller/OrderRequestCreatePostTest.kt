package com.otoki.powersales.order.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.request.OrderRequestCreateLine
import com.otoki.powersales.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.order.exception.OrderAccountForbiddenException
import com.otoki.powersales.order.exception.OrderLoanExceededException
import com.otoki.powersales.order.service.OrderRequestCreateService
import com.otoki.powersales.order.service.OrderRequestService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(OrderRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("POST /api/v1/mobile/order-requests (#592)")
class OrderRequestCreatePostTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockitoBean private lateinit var orderRequestService: OrderRequestService
    @MockitoBean private lateinit var orderRequestCreateService: OrderRequestCreateService
    @MockitoBean private lateinit var orderCancelService: com.otoki.powersales.order.service.OrderCancelService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService

    private val principal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Test
    @DisplayName("성공 — 201 Created + 응답 검증")
    fun success() {
        val response = OrderRequestCreateResponse(
            orderRequestId = 12345L,
            orderRequestNumber = "ORD-20260505-42",
            status = OrderRequestStatus.SENT,
            totalAmount = BigDecimal.valueOf(1234567),
        )
        whenever(orderRequestCreateService.create(eq(1L), any())).thenReturn(response)

        val body = sampleRequest()

        mockMvc.perform(
            post("/api/v1/mobile/order-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderRequestId").value(12345L))
            .andExpect(jsonPath("$.data.orderRequestNumber").value("ORD-20260505-42"))
            .andExpect(jsonPath("$.data.status").value("전송"))
    }

    @Test
    @DisplayName("ORD_ACCOUNT_FORBIDDEN — 403")
    fun forbidden() {
        whenever(orderRequestCreateService.create(eq(1L), any()))
            .thenThrow(OrderAccountForbiddenException())

        mockMvc.perform(
            post("/api/v1/mobile/order-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest()))
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.error.code").value("ORD_ACCOUNT_FORBIDDEN"))
    }

    @Test
    @DisplayName("ORD_LOAN_EXCEEDED — 400")
    fun loanExceeded() {
        whenever(orderRequestCreateService.create(eq(1L), any()))
            .thenThrow(OrderLoanExceededException(BigDecimal.valueOf(500_000), BigDecimal.valueOf(1_234_567)))

        mockMvc.perform(
            post("/api/v1/mobile/order-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRequest()))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("ORD_LOAN_EXCEEDED"))
    }

    @Test
    @DisplayName("입력 검증 실패 — 라인 누락 시 400")
    fun emptyLines() {
        val invalidJson = """
            {"accountId": 5678, "deliveryDate": "${LocalDate.now().plus(2, java.time.temporal.ChronoUnit.DAYS)}", "totalAmount": 1, "lines": []}
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/mobile/order-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
    }

    private fun sampleRequest() = OrderRequestCreateRequest(
        accountId = 5678L,
        deliveryDate = LocalDate.now().plus(2, java.time.temporal.ChronoUnit.DAYS),
        totalAmount = 100_000L,
        lines = listOf(
            OrderRequestCreateLine(
                lineNumber = 10,
                productCode = "P001",
                quantity = BigDecimal.TEN,
                unit = "BOX",
                quantityPieces = 300,
                quantityBoxes = BigDecimal.TEN,
            )
        ),
    )
}
