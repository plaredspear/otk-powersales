package com.otoki.powersales.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.powersales.sap.config.SapAuthProperties
import com.otoki.powersales.sap.dto.SapAccountCategoryMasterRequest
import com.otoki.powersales.sap.dto.SapSyncResult
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.filter.SapApiKeyFilter
import com.otoki.powersales.sap.service.SapAccountCategoryMasterService
import com.otoki.powersales.sap.service.SapSyncLogService
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

@WebMvcTest(SapAccountCategoryMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapAccountCategoryMasterController 테스트")
class SapAccountCategoryMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapAccountCategoryMasterService: SapAccountCategoryMasterService

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
    @DisplayName("POST /api/v1/sap/account-master")
    inner class SyncAccountCategoryMaster {

        @Test
        @DisplayName("성공 - 정상 동기화 요청")
        fun sync_success() {
            whenever(sapAccountCategoryMasterService.sync(any())).thenReturn(
                SapSyncResult(successCount = 1, failCount = 0)
            )

            val request = SapAccountCategoryMasterRequest(
                reqItemList = listOf(
                    SapAccountCategoryMasterRequest.ReqItem(
                        accountCode = "610000",
                        name = "매출"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/account-category-master")
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
            val request = SapAccountCategoryMasterRequest(reqItemList = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/account-category-master")
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
            whenever(sapAccountCategoryMasterService.sync(any())).thenThrow(
                RuntimeException("DB connection lost")
            )

            val request = SapAccountCategoryMasterRequest(
                reqItemList = listOf(
                    SapAccountCategoryMasterRequest.ReqItem(
                        accountCode = "610000",
                        name = "매출"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/account-category-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
                .andExpect(jsonPath("$.result_msg").value("Transaction failed: DB connection lost"))
        }
    }
}
