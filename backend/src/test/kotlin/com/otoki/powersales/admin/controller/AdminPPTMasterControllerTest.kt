package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.promotion.service.AdminPPTMasterService
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
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
        val principal = UserPrincipal(userId = 1L, role = UserRole.BRANCH_MANAGER)
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
        branchName = "서울지점",
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
                .andExpect(jsonPath("$.data.content[0].team_type").value("라면세일조"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
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
                .andExpect(jsonPath("$.data.employee_name").value("홍길동"))
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
                .andExpect(jsonPath("$.data.team_type").value("라면세일조"))
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

            val requestJson = """{"employee_id":1,"account_id":1,"team_type":"프레시세일조_냉장","start_date":"2026-04-01","is_confirmed":false}"""

            mockMvc.perform(
                put("/api/v1/admin/ppt-masters/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.team_type").value("프레시세일조_냉장"))
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

            val requestJson = """{"items":[{"employee_code":"12345678","account_code":"SAP001","team_type":"라면세일조","start_date":"2026-04-01"}]}"""

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.is_all_valid").value(true))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/ppt-masters/bulk/confirm - 일괄 확정")
    inner class ConfirmBulk {

        @Test
        @DisplayName("성공 - 일괄 확정 -> 201")
        fun confirmBulk_success() {
            whenever(adminPPTMasterService.confirmBulk(any())).thenReturn(BulkConfirmResponse(createdCount = 3))

            val requestJson = """{"items":[{"employee_code":"12345678","account_code":"SAP001","team_type":"라면세일조","start_date":"2026-04-01"}]}"""

            mockMvc.perform(
                post("/api/v1/admin/ppt-masters/bulk/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.created_count").value(3))
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
                        oldValue = ProfessionalPromotionTeamType.GENERAL, newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                        changedAt = LocalDateTime.of(2026, 3, 22, 9, 0)
                    )
                ),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            whenever(adminPPTMasterService.getHistory(eq(1L), any())).thenReturn(historyResponse)

            mockMvc.perform(get("/api/v1/admin/ppt-masters/1/history"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].old_value").value("일반"))
                .andExpect(jsonPath("$.data.content[0].new_value").value("라면세일조"))
        }
    }
}
