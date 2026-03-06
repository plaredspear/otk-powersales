package com.otoki.internal.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import com.otoki.internal.sap.dto.SapBarcodeMasterRequest
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.sap.filter.SapApiKeyFilter
import com.otoki.internal.sap.service.SapBarcodeMasterService
import com.otoki.internal.sap.service.SapSyncLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapBarcodeMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapBarcodeMasterController 테스트")
class SapBarcodeMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapBarcodeMasterService: SapBarcodeMasterService

    @MockitoBean
    private lateinit var sapSyncLogService: SapSyncLogService

    @MockitoBean
    private lateinit var sapAuthProperties: SapAuthProperties

    @MockitoBean
    private lateinit var sapApiKeyFilter: SapApiKeyFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Nested
    @DisplayName("POST /api/v1/sap/barcode-master")
    inner class SyncBarcodeMaster {

        @Test
        @DisplayName("성공 - 정상 동기화 요청")
        fun sync_success() {
            whenever(sapBarcodeMasterService.sync(any())).thenReturn(
                SapSyncResult(successCount = 1, failCount = 0)
            )

            val request = SapBarcodeMasterRequest(
                reqItemList = listOf(
                    SapBarcodeMasterRequest.ReqItem(
                        productCode = "12345678",
                        productUnit = "EA",
                        productSequence = "001",
                        productBarcode = "8801045520001"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/barcode-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("SUCCESS"))
        }

        @Test
        @DisplayName("빈 요청 - EMPTY_REQUEST 반환")
        fun sync_emptyRequest() {
            val request = SapBarcodeMasterRequest(reqItemList = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/barcode-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
                .andExpect(jsonPath("$.result_msg").value("EMPTY_REQUEST"))
        }

        @Test
        @DisplayName("트랜잭션 실패 - result_code=0 반환")
        fun sync_transactionFailure() {
            whenever(sapBarcodeMasterService.sync(any())).thenThrow(
                RuntimeException("DB connection lost")
            )

            val request = SapBarcodeMasterRequest(
                reqItemList = listOf(
                    SapBarcodeMasterRequest.ReqItem(
                        productCode = "12345678",
                        productUnit = "EA",
                        productSequence = "001"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/barcode-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
                .andExpect(jsonPath("$.result_msg").value("Transaction failed: DB connection lost"))
        }
    }
}
