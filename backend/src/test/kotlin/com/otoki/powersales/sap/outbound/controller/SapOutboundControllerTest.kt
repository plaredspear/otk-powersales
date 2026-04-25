package com.otoki.powersales.sap.outbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.outbound.exception.SapOutboundException
import com.otoki.powersales.sap.outbound.sender.SapPPTMasterSendResult
import com.otoki.powersales.sap.outbound.sender.SapPPTMasterSender
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapOutboundController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapOutboundController 테스트")
class SapOutboundControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapPPTMasterSender: SapPPTMasterSender

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @Test
    @DisplayName("POST /api/v1/admin/sap/outbound/SD03300/send - 성공 시 200 응답에 송신 결과를 담아 반환")
    fun sendPPTMaster_success() {
        whenever(sapPPTMasterSender.send()).thenReturn(
            SapPPTMasterSendResult(
                requestCount = 15,
                batchCount = 1,
                resultCode = "200",
                resultMsg = "SUCCESS"
            )
        )

        mockMvc.perform(post("/api/v1/admin/sap/outbound/SD03300/send"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.interface_id").value("SD03300"))
            .andExpect(jsonPath("$.data.request_count").value(15))
            .andExpect(jsonPath("$.data.batch_count").value(1))
            .andExpect(jsonPath("$.data.result_code").value("200"))
            .andExpect(jsonPath("$.data.result_msg").value("SUCCESS"))
    }

    @Test
    @DisplayName("POST /api/v1/admin/sap/outbound/SD03300/send - SapOutboundException 발생 시 502 응답")
    fun sendPPTMaster_sapFailure() {
        whenever(sapPPTMasterSender.send()).thenThrow(SapOutboundException("HTTP 500"))

        mockMvc.perform(post("/api/v1/admin/sap/outbound/SD03300/send"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("SAP_OUTBOUND_FAILED"))
    }
}
