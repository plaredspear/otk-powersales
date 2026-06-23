package com.otoki.powersales.domain.activity.order.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateLine
import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.domain.activity.order.dto.response.OrderRequestCreateResponse
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.OrderAccountForbiddenException
import com.otoki.powersales.domain.activity.order.exception.OrderLoanExceededException
import com.otoki.powersales.domain.activity.order.service.OrderCancelService
import com.otoki.powersales.domain.activity.order.service.OrderRequestCreateService
import com.otoki.powersales.domain.activity.order.service.OrderRequestResendService
import com.otoki.powersales.domain.activity.order.service.OrderRequestService
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@WebMvcTest(OrderRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("POST /api/v1/mobile/order-requests (#592)")
class OrderRequestCreatePostTest : MobileControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var orderRequestService: OrderRequestService
    @MockkBean private lateinit var orderRequestCreateService: OrderRequestCreateService
    @MockkBean private lateinit var orderCancelService: OrderCancelService
    @MockkBean private lateinit var orderRequestResendService: OrderRequestResendService

    @Test
    @DisplayName("성공 — 201 Created + 응답 검증")
    fun success() {
        val response = OrderRequestCreateResponse(
            orderRequestId = 12345L,
            orderRequestNumber = "ORD-20260505-42",
            status = OrderRequestStatus.SENT.name,
            statusName = OrderRequestStatus.SENT.displayName,
            totalAmount = BigDecimal.valueOf(1234567),
        )
        every { orderRequestCreateService.create(eq(1L), any()) } returns response

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
            .andExpect(jsonPath("$.data.status").value("SENT"))
            .andExpect(jsonPath("$.data.statusName").value("전송"))
    }

    @Test
    @DisplayName("ORD_ACCOUNT_FORBIDDEN — 403")
    fun forbidden() {
        every { orderRequestCreateService.create(eq(1L), any()) } throws OrderAccountForbiddenException()

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
        every { orderRequestCreateService.create(eq(1L), any()) } throws OrderLoanExceededException(BigDecimal.valueOf(500_000), BigDecimal.valueOf(1_234_567))

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
            {"accountId": 5678, "deliveryDate": "${LocalDate.now().plus(2, ChronoUnit.DAYS)}", "totalAmount": 1, "lines": []}
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
        deliveryDate = LocalDate.now().plus(2, ChronoUnit.DAYS),
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
