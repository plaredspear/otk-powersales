package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.promotion.service.AdminPPTMasterService
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.exception.PPTMasterDuplicateException
import com.otoki.powersales.promotion.exception.PPTMasterNotFoundException
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminPPTMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPPTMasterController 테스트")
class AdminPPTMasterControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockitoBean private lateinit var adminPPTMasterService: AdminPPTMasterService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeNumber = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    private fun createResponse(): PPTMasterResponse = PPTMasterResponse(
        id = 1L,
        employeeId = 1L,
        employeeCode = "12345678",
        employeeName = "홍길동",
        accountId = 1,
        accountCode = "SAP001",
        accountName = "이마트 강남점",
        teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
        startDate = LocalDate.of(2026, 4, 1),
        endDate = null,
        isConfirmed = true,
        branchCode = "1100",
        createdAt = LocalDateTime.of(2026, 3, 22, 9, 0),
        updatedAt = LocalDateTime.of(2026, 3, 22, 9, 0)
    )

    @Nested
    @DisplayName("GET /api/v1/admin/ppt-masters - 목록 조회")
    inner class GetMasters {

        @Test
        @DisplayName("성공 - 목록 조회")
        fun getMasters_success() {
            val listResponse = PPTMasterListResponse(
                content = listOf(createResponse()),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            whenever(adminPPTMasterService.getMasters(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
                .thenReturn(listResponse)

            mockMvc.perform(get("/api/v1/admin/ppt-masters"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].teamType").value("라면세일조"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/ppt-masters/{id} - 상세 조회")
    inner class GetMaster {

        @Test
        @DisplayName("성공 - 마스터 상세 조회")
        fun getMaster_success() {
            whenever(adminPPTMasterService.getMaster(1L)).thenReturn(createResponse())

            mockMvc.perform(get("/api/v1/admin/ppt-masters/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeName").value("홍길동"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> 404")
        fun getMaster_notFound() {
            whenever(adminPPTMasterService.getMaster(999L)).thenThrow(PPTMasterNotFoundException())

            mockMvc.perform(get("/api/v1/admin/ppt-masters/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/ppt-masters - 생성")
    inner class CreateMaster {

        @Test
        @DisplayName("성공 - 마스터 생성 -> 201")
        fun createMaster_success() {
            whenever(adminPPTMasterService.createMaster(any())).thenReturn(createResponse())

            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = true
            )

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.teamType").value("라면세일조"))
        }

        @Test
        @DisplayName("실패 - 중복 -> 409")
        fun createMaster_duplicate() {
            whenever(adminPPTMasterService.createMaster(any())).thenThrow(PPTMasterDuplicateException())

            val request = PPTMasterCreateRequest(
                employeeId = 1L, accountId = 1, teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = LocalDate.of(2026, 4, 1), isConfirmed = true
            )

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/ppt-masters/{id} - 수정")
    inner class UpdateMaster {

        @Test
        @DisplayName("성공 - 마스터 수정")
        fun updateMaster_success() {
            val response = createResponse().copy(teamType = ProfessionalPromotionTeamType.FRESH_SALE_REFRIGERATED)
            whenever(adminPPTMasterService.updateMaster(eq(1L), any())).thenReturn(response)

            val requestJson = """{"employeeId":1,"accountId":1,"teamType":"프레시세일조_냉장","startDate":"2026-04-01","isConfirmed":false}"""

            mockMvc.perform(
                put("/api/v1/admin/ppt-masters/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.teamType").value("프레시세일조_냉장"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/ppt-masters/{id} - 삭제")
    inner class DeleteMaster {

        @Test
        @DisplayName("성공 - 마스터 삭제 -> 204")
        fun deleteMaster_success() {
            mockMvc.perform(delete("/api/v1/admin/ppt-masters/1"))
                .andExpect(status().isNoContent)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/ppt-masters/bulk - 일괄 검증")
    inner class ValidateBulk {

        @Test
        @DisplayName("성공 - 일괄 검증 결과 반환")
        fun validateBulk_success() {
            val response = BulkValidationResponse(
                totalCount = 2, successCount = 2, errorCount = 0, isAllValid = true,
                results = listOf(
                    BulkValidationResultItem(1, true, null),
                    BulkValidationResultItem(2, true, null)
                )
            )
            whenever(adminPPTMasterService.validateBulk(any())).thenReturn(response)

            val requestJson = """{"items":[{"employeeCode":"12345678","accountCode":"SAP001","teamType":"라면세일조","startDate":"2026-04-01"}]}"""

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isAllValid").value(true))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/ppt-masters/bulk/confirm - 일괄 확정")
    inner class ConfirmBulk {

        @Test
        @DisplayName("성공 - 일괄 확정 -> 201")
        fun confirmBulk_success() {
            whenever(adminPPTMasterService.confirmBulk(any())).thenReturn(BulkConfirmResponse(createdCount = 3))

            val requestJson = """{"items":[{"employeeCode":"12345678","accountCode":"SAP001","teamType":"라면세일조","startDate":"2026-04-01"}]}"""

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters/bulk/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.createdCount").value(3))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/ppt-masters/{masterId}/history - 변경 이력 조회")
    inner class GetHistory {

        @Test
        @DisplayName("성공 - 변경 이력 조회")
        fun getHistory_success() {
            val historyResponse = PPTMasterHistoryListResponse(
                content = listOf(
                    PPTMasterHistoryResponse(
                        id = 1L, employeeId = 1L, employeeName = "홍길동",
                        oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                        changedAt = LocalDateTime.of(2026, 3, 22, 9, 0)
                    )
                ),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            whenever(adminPPTMasterService.getHistory(eq(1L), any())).thenReturn(historyResponse)

            mockMvc.perform(get("/api/v1/admin/ppt-masters/1/history"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].oldValue").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].newValue").value("라면세일조"))
        }
    }
}
