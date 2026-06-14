package com.otoki.powersales.external.sap.inbound.controller

import com.otoki.powersales.platform.common.security.GpsConsentFilter
import com.otoki.powersales.platform.common.security.JwtAuthenticationFilter
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import com.otoki.powersales.platform.auth.sharing.service.FlsService
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.organization.OrganizationMasterDetail
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.external.sap.inbound.service.SapOrganizationMasterService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.external.sap.inbound.dto.organization.OrganizationMasterRequestItem
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper

@WebMvcTest(SapOrganizationMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SapOrganizationMasterController 테스트")
class SapOrganizationMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var sapOrganizationMasterService: SapOrganizationMasterService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var flsService: FlsService

    @MockkBean
    private lateinit var permissionSetEvaluator: PermissionSetEvaluator

    @MockkBean
    private lateinit var userRepository: UserRepository


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
          "reqItemList": [
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
        @DisplayName("성공 - 200 + RESULT_CODE=200 + RESULT_DETAIL.success_count")
        fun replace_success() {
            val captor = slot<List<OrganizationMasterRequestItem>>()
            every { sapOrganizationMasterService.replaceAll(capture(captor)) } returns
                OrganizationMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_MSG").value("OK"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.failure_count").value(0))
                .andExpect(jsonPath("$.RESULT_DETAIL.failures").isArray)
                .andExpect(jsonPath("$.RESULT_DETAIL.failures").isEmpty)

            assert(captor.captured.size == 1)
        }

        @Test
        @DisplayName("성공 - PascalCase 키가 정확히 매핑됨")
        fun replace_pascalCaseKeysMapped() {
            val captor = slot<List<OrganizationMasterRequestItem>>()
            every { sapOrganizationMasterService.replaceAll(capture(captor)) } returns
                OrganizationMasterDetail(successCount = 1, failureCount = 0, failures = emptyList())

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().isOk)

            val item = captor.captured.first()
            assert(item.ccCd2 == "1000")
            assert(item.orgCd5 == "11110")
            assert(item.orgNm5 == "서울지점")
        }

        @Test
        @DisplayName("실패 - 서비스가 INVALID_PAYLOAD 예외를 던지면 422 + RESULT_CODE=INVALID_PAYLOAD")
        fun replace_invalidPayload() {
            every { sapOrganizationMasterService.replaceAll(any()) } throws
                SapInvalidPayloadException("필수 필드 누락 (line 2)")

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(samplePayload())
            )
                .andExpect(status().`is`(422))
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))
                .andExpect(jsonPath("$.RESULT_MSG").value("필수 필드 누락 (line 2)"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> 400 + RESULT_CODE=INVALID_PAYLOAD")
        fun replace_missingReqItemList() {
            val payload = """{"otherField": []}"""

            mockMvc.perform(
                post("/api/v1/sap/organization")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(exactly = 0) { sapOrganizationMasterService.replaceAll(any()) }
        }
    }
}
