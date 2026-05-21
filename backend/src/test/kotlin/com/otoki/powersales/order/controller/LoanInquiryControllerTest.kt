package com.otoki.powersales.order.controller

import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.order.dto.response.LoanInquiryResponse
import com.otoki.powersales.order.exception.LoanSapErrorException
import com.otoki.powersales.order.exception.LoanSapHtmlResponseException
import com.otoki.powersales.order.exception.LoanSapUnavailableException
import com.otoki.powersales.order.service.LoanInquiryService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import io.mockk.every
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.OffsetDateTime

@WebMvcTest(LoanInquiryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LoanInquiryController 테스트 (#594)")
class LoanInquiryControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var loanInquiryService: LoanInquiryService

    @Test
    @DisplayName("성공 — 200 OK + 응답 매핑")
    fun success() {
        val response = LoanInquiryResponse(
            externalKey = "EK001",
            totalCredit = BigDecimal.valueOf(10_000_000),
            creditBalance = BigDecimal.valueOf(2_500_000),
            currency = "KRW",
            dataAsOf = OffsetDateTime.now(),
        )
        every { loanInquiryService.inquireByExternalKey(eq("EK001")) } returns response

        mockMvc.perform(get("/api/v1/mobile/clients/{externalKey}/loan-inquiry", "EK001"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.externalKey").value("EK001"))
            .andExpect(jsonPath("$.data.totalCredit").value(10_000_000))
            .andExpect(jsonPath("$.data.creditBalance").value(2_500_000))
            .andExpect(jsonPath("$.data.currency").value("KRW"))
    }

    @Test
    @DisplayName("LOAN_SAP_ERROR — 500")
    fun sapError() {
        every { loanInquiryService.inquireByExternalKey(eq("EK_BAD")) } throws LoanSapErrorException("거래처 미존재")

        mockMvc.perform(get("/api/v1/mobile/clients/{externalKey}/loan-inquiry", "EK_BAD"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("LOAN_SAP_ERROR"))
            .andExpect(jsonPath("$.error.message").value("거래처 미존재"))
    }

    @Test
    @DisplayName("LOAN_SAP_HTML_RESPONSE — 502")
    fun htmlResponse() {
        every { loanInquiryService.inquireByExternalKey(eq("EK001")) } throws LoanSapHtmlResponseException()

        mockMvc.perform(get("/api/v1/mobile/clients/{externalKey}/loan-inquiry", "EK001"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.error.code").value("LOAN_SAP_HTML_RESPONSE"))
    }

    @Test
    @DisplayName("LOAN_SAP_UNAVAILABLE — 503")
    fun unavailable() {
        every { loanInquiryService.inquireByExternalKey(eq("EK001")) } throws LoanSapUnavailableException()

        mockMvc.perform(get("/api/v1/mobile/clients/{externalKey}/loan-inquiry", "EK001"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error.code").value("LOAN_SAP_UNAVAILABLE"))
    }
}
