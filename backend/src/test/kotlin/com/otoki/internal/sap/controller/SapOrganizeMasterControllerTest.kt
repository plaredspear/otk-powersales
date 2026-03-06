package com.otoki.internal.sap.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import com.otoki.internal.sap.dto.SapOrganizeMasterRequest
import com.otoki.internal.sap.dto.SapSyncResult
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.sap.filter.SapApiKeyFilter
import com.otoki.internal.sap.service.SapOrganizeMasterService
import com.otoki.internal.sap.service.SapSyncLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapOrganizeMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapOrganizeMasterController 테스트")
class SapOrganizeMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var sapOrganizeMasterService: SapOrganizeMasterService

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
    @DisplayName("POST /api/v1/sap/organize-master")
    inner class SyncOrganizeMaster {

        @Test
        @DisplayName("성공 - 정상 동기화 요청")
        fun sync_success() {
            whenever(sapOrganizeMasterService.sync(any())).thenReturn(
                SapSyncResult(successCount = 1, failCount = 0)
            )

            val request = SapOrganizeMasterRequest(
                reqItemList = listOf(
                    SapOrganizeMasterRequest.ReqItem(
                        ccCd2 = "1000", orgCd2 = "O100", orgNm2 = "오뚜기"
                    )
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/organize-master")
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
            val request = SapOrganizeMasterRequest(reqItemList = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/organize-master")
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
            whenever(sapOrganizeMasterService.sync(any())).thenThrow(
                RuntimeException("DB connection lost")
            )

            val request = SapOrganizeMasterRequest(
                reqItemList = listOf(
                    SapOrganizeMasterRequest.ReqItem(ccCd2 = "1000")
                )
            )

            mockMvc.perform(
                post("/api/v1/sap/organize-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("0"))
                .andExpect(jsonPath("$.result_msg").value("Transaction failed: DB connection lost"))
        }

        @Test
        @DisplayName("다건 동기화 - 5건 요청 성공")
        fun sync_multipleItems() {
            whenever(sapOrganizeMasterService.sync(any())).thenReturn(
                SapSyncResult(successCount = 5, failCount = 0)
            )

            val items = (1..5).map {
                SapOrganizeMasterRequest.ReqItem(ccCd2 = "100$it", orgCd2 = "O10$it", orgNm2 = "조직$it")
            }
            val request = SapOrganizeMasterRequest(reqItemList = items)

            mockMvc.perform(
                post("/api/v1/sap/organize-master")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.result_code").value("200"))
        }
    }
}
