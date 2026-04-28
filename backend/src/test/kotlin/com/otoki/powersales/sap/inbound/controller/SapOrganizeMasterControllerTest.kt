package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.sap.inbound.service.SapOrganizeMasterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

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
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "otoki-sap-client",
                null,
                listOf(SimpleGrantedAuthority("SCOPE_sap.org.write"))
            )
    }

    private fun samplePayload(): String = """
        {
          "req_item_list": [
            {
              "CC_CD2": "1000",
              "ORG_CD2": "10000",
              "ORG_NM2": "본사",
              "CC_CD3": "1100",
              "ORG_CD3": "11000",
              "ORG_NM3": "Retail사업부",
              "CC_CD4": "1110",
              "ORG_CD4": "11100",
              "ORG_NM4": "영업1팀",
              "CC_CD5": "1111",
              "ORG_CD5": "11110",
              "ORG_NM5": "서울지점"
            }
          ]
        }
    """.trimIndent()

    @Nested
    @DisplayName("POST /api/v1/sap/organization")
    inner class ReplaceOrganizations {

        @Test
        @DisplayName("성공 - 200 + SUCCESS")
        fun replace_success() {
            doNothing().whenever(sapOrganizeMasterService).replaceAll(any())

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("SUCCESS"))

            val captor = argumentCaptor<List<com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequestItem>>()
            verify(sapOrganizeMasterService).replaceAll(captor.capture())
            assert(captor.firstValue.size == 1)
        }

        @Test
        @DisplayName("성공 - PascalCase 키가 정확히 매핑됨")
        fun replace_pascalCaseKeysMapped() {
            doNothing().whenever(sapOrganizeMasterService).replaceAll(any())

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().isOk)

            val captor = argumentCaptor<List<com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequestItem>>()
            verify(sapOrganizeMasterService).replaceAll(captor.capture())
            val item = captor.firstValue.first()
            assert(item.ccCd2 == "1000")
            assert(item.orgCd5 == "11110")
            assert(item.orgNm5 == "서울지점")
        }

        @Test
        @DisplayName("실패 - 서비스가 INVALID_PAYLOAD 예외를 던지면 422")
        fun replace_invalidPayload() {
            whenever(sapOrganizeMasterService.replaceAll(any()))
                .thenThrow(SapInvalidPayloadException("필수 필드 누락 (line 2)"))

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().`is`(422))
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYLOAD"))
                .andExpect(jsonPath("$.error.message").value("필수 필드 누락 (line 2)"))
        }

        @Test
        @DisplayName("실패 - req_item_list 누락 -> 422 INVALID_PAYLOAD")
        fun replace_missingReqItemList() {
            val payload = """{"other_field": []}"""

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isBadRequest)

            verify(sapOrganizeMasterService, never()).replaceAll(any())
        }
    }
}
