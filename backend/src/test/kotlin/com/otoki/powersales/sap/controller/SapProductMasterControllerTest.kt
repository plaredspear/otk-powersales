package com.otoki.powersales.sap.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.sap.config.SapAuthProperties
import com.otoki.powersales.sap.dto.SapProductMasterRequest
import com.otoki.powersales.sap.dto.SapSyncResult
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.filter.SapApiKeyFilter
import com.otoki.powersales.sap.service.SapProductMasterService
import com.otoki.powersales.sap.service.SapSyncLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapProductMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapProductMasterController 테스트")
class SapProductMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapProductMasterService: SapProductMasterService

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
    @DisplayName("POST /api/v1/sap/product-master")
    inner class SyncProductMaster {

        @Test
        @DisplayName("성공 - 정상 동기화 요청")
        fun sync_success() {
            whenever(sapProductMasterService.sync(any())).thenReturn(
                SapSyncResult(successCount = 1, failCount = 0)
            )

            val request = SapProductMasterRequest(
                reqItemList = listOf(
                    SapProductMasterRequest.ReqItem(
                        productCode = "12345678",
                        productName = "오뚜기 진라면"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/product-master")
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
            val request = SapProductMasterRequest(reqItemList = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/product-master")
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
            whenever(sapProductMasterService.sync(any())).thenThrow(
                RuntimeException("DB connection lost")
            )

            val request = SapProductMasterRequest(
                reqItemList = listOf(
                    SapProductMasterRequest.ReqItem(productCode = "12345678", productName = "테스트")
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/product-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
                .andExpect(jsonPath("$.result_msg").value("Transaction failed: DB connection lost"))
        }
    }
}
