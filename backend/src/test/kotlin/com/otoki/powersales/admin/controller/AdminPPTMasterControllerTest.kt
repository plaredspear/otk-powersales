package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTConfirmedReportService
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTMasterService
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterDuplicateException
import com.otoki.powersales.domain.activity.promotion.exception.PPTMasterNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResultItem
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminPPTMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPPTMasterController 테스트")
class AdminPPTMasterControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockkBean private lateinit var adminPPTMasterService: AdminPPTMasterService
    @MockkBean private lateinit var pptConfirmedReportService: AdminPPTConfirmedReportService

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    private fun createResponse(): PPTMasterResponse = PPTMasterResponse(
        id = 1L,
        name = "PM0000001",
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
        branchName = "강남지점",
        employeeStatus = "재직",
        employeeAppLoginActive = true,
        employeeEndDate = null,
        accountType = null,
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
            every { adminPPTMasterService.getMasters(any(), any(), any(), any(), any(), any(), any()) } returns listResponse

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
            every { adminPPTMasterService.getMaster(1L) } returns createResponse()

            mockMvc.perform(get("/api/v1/admin/ppt-masters/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.employeeName").value("홍길동"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID -> 404")
        fun getMaster_notFound() {
            every { adminPPTMasterService.getMaster(999L) } throws PPTMasterNotFoundException()

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
            every { adminPPTMasterService.createMaster(any()) } returns createResponse()

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
            every { adminPPTMasterService.createMaster(any()) } throws PPTMasterDuplicateException()

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
            every { adminPPTMasterService.updateMaster(eq(1L), any()) } returns response

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
            every { adminPPTMasterService.deleteMaster(any()) } just Runs

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
            every { adminPPTMasterService.validateBulk(any()) } returns response

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
            every { adminPPTMasterService.confirmBulk(any()) } returns BulkConfirmResponse(createdCount = 3)

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
        @DisplayName("성공 - 변경 이력 조회 + 사원 컨텍스트 3 필드 포함")
        fun getHistory_success() {
            val historyResponse = PPTMasterHistoryListResponse(
                content = listOf(
                    PPTMasterHistoryResponse(
                        id = 1L, name = "PH0000001", employeeId = 1L, employeeName = "홍길동",
                        employeeCode = "12345678", orgName = "서울지점",
                        oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                        changedAt = LocalDateTime.of(2026, 3, 22, 9, 0)
                    )
                ),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            every { adminPPTMasterService.getHistory(eq(1L), any()) } returns historyResponse

            mockMvc.perform(get("/api/v1/admin/ppt-masters/1/history"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].oldValue").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].newValue").value("라면세일조"))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("12345678"))
                .andExpect(jsonPath("$.data.content[0].orgName").value("서울지점"))
                .andExpect(jsonPath("$.data.content[0].name").value("PH0000001"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/ppt-histories - 전 사원 시간순 이력 조회")
    inner class GetAllHistory {

        @Test
        @DisplayName("성공 - 필터 없이 전체 조회")
        fun getAllHistory_noFilter_success() {
            val historyResponse = PPTMasterHistoryListResponse(
                content = listOf(
                    PPTMasterHistoryResponse(
                        id = 10L, name = "PH0014972", employeeId = 5L, employeeName = "백은경",
                        employeeCode = "EMP005", orgName = "서울지점",
                        oldValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                        newValue = ProfessionalPromotionTeamType.CURRY_PROMOTION,
                        changedAt = LocalDateTime.of(2026, 5, 18, 14, 30)
                    )
                ),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            every { adminPPTMasterService.getAllHistory(any(), any(), any(), any(), any(), any()) } returns historyResponse

            mockMvc.perform(get("/api/v1/admin/ppt-histories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("백은경"))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("EMP005"))
                .andExpect(jsonPath("$.data.content[0].orgName").value("서울지점"))
                .andExpect(jsonPath("$.data.content[0].name").value("PH0014972"))
                .andExpect(jsonPath("$.data.content[0].oldValue").value("라면세일조"))
                .andExpect(jsonPath("$.data.content[0].newValue").value("카레행사조"))
        }

        @Test
        @DisplayName("성공 - 사원명 + 사번 + 팀유형 + 변경일 범위 필터 적용")
        fun getAllHistory_filterCombo_success() {
            val historyResponse = PPTMasterHistoryListResponse(
                content = emptyList(), totalElements = 0, totalPages = 0, number = 0, size = 20
            )
            every { adminPPTMasterService.getAllHistory(
                eq("홍"), eq("EMP001"), eq("라면세일조"),
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 31)), any()
            ) } returns historyResponse

            mockMvc.perform(get("/api/v1/admin/ppt-histories")
                .param("employeeName", "홍")
                .param("employeeCode", "EMP001")
                .param("teamType", "라면세일조")
                .param("changedAtFrom", "2026-05-01")
                .param("changedAtTo", "2026-05-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - 사원이 deleted 인 row 는 사원 컨텍스트 3 필드 모두 null")
        fun getAllHistory_deletedEmployee_nullContext() {
            val historyResponse = PPTMasterHistoryListResponse(
                content = listOf(
                    PPTMasterHistoryResponse(
                        id = 99L, name = "PH0000099", employeeId = 999L,
                        employeeName = null, employeeCode = null, orgName = null,
                        oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE,
                        changedAt = LocalDateTime.of(2026, 5, 18, 14, 30)
                    )
                ),
                totalElements = 1, totalPages = 1, number = 0, size = 20
            )
            every { adminPPTMasterService.getAllHistory(any(), any(), any(), any(), any(), any()) } returns historyResponse

            mockMvc.perform(get("/api/v1/admin/ppt-histories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].employeeName").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].employeeCode").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].orgName").doesNotExist())
        }
    }
}
