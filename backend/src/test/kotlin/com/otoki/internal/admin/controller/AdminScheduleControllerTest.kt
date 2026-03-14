package com.otoki.internal.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.request.ScheduleConfirmRequest
import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.exception.*
import com.otoki.internal.admin.service.AdminScheduleService
import com.otoki.internal.admin.service.MissingCostCenterCodeException
import com.otoki.internal.admin.service.OrganizationNotFoundException
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminScheduleController 테스트")
class AdminScheduleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminScheduleService: AdminScheduleService

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
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedule/branches - 지점 목록 조회")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 지점 목록 반환")
        fun getBranches_success() {
            val branches = listOf(
                BranchDto("1234", "서울지점"),
                BranchDto("5678", "부산지점")
            )
            whenever(adminScheduleService.getBranches()).thenReturn(branches)

            mockMvc.perform(get("/api/v1/admin/schedule/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].cost_center_code").value("1234"))
                .andExpect(jsonPath("$.data[0].branch_name").value("서울지점"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedule/template - 양식 다운로드")
    inner class DownloadTemplate {

        @Test
        @DisplayName("성공 - Excel 파일 다운로드")
        fun downloadTemplate_success() {
            val result = AdminScheduleService.TemplateResult(
                bytes = ByteArray(100),
                filename = "진열스케줄_양식_1234_20260314120000.xlsx"
            )
            whenever(adminScheduleService.generateTemplate("1234")).thenReturn(result)

            mockMvc.perform(
                get("/api/v1/admin/schedule/template")
                    .param("costCenterCode", "1234")
            )
                .andExpect(status().isOk)
                .andExpect(
                    header().string(
                        "Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                )
                .andExpect(header().exists("Content-Disposition"))
        }

        @Test
        @DisplayName("실패 - cost_center_code 파라미터 누락")
        fun downloadTemplate_missingParam() {
            mockMvc.perform(get("/api/v1/admin/schedule/template"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 지점 코드")
        fun downloadTemplate_orgNotFound() {
            whenever(adminScheduleService.generateTemplate("0000"))
                .thenThrow(OrganizationNotFoundException())

            mockMvc.perform(
                get("/api/v1/admin/schedule/template")
                    .param("costCenterCode", "0000")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/schedule/upload - Excel 업로드")
    inner class UploadExcel {

        @Test
        @DisplayName("성공 - 검증 결과 반환")
        fun upload_success() {
            val uploadResult = ScheduleUploadResultDto(
                uploadId = "test-uuid",
                totalRows = 2,
                successRows = 1,
                errorRows = 1,
                errors = listOf(
                    RowError(5, "A", "사원번호", "999999", "사원번호 999999: 존재하지 않는 사원")
                ),
                previews = listOf(
                    RowPreview(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상시", "2026-04-01", null)
                )
            )
            whenever(adminScheduleService.uploadAndValidate(any())).thenReturn(uploadResult)

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/schedule/upload").file(file))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.upload_id").value("test-uuid"))
                .andExpect(jsonPath("$.data.total_rows").value(2))
                .andExpect(jsonPath("$.data.success_rows").value(1))
                .andExpect(jsonPath("$.data.error_rows").value(1))
                .andExpect(jsonPath("$.data.errors[0].row").value(5))
                .andExpect(jsonPath("$.data.errors[0].column").value("A"))
                .andExpect(jsonPath("$.data.errors[0].message").value("사원번호 999999: 존재하지 않는 사원"))
                .andExpect(jsonPath("$.data.previews[0].employee_code").value("20030001"))
                .andExpect(jsonPath("$.data.previews[0].account_name").value("이마트 강남점"))
        }

        @Test
        @DisplayName("실패 - 파일 미첨부")
        fun upload_noFile() {
            mockMvc.perform(post("/api/v1/admin/schedule/upload"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("FILE_REQUIRED"))
        }

        @Test
        @DisplayName("실패 - 잘못된 확장자")
        fun upload_invalidFileType() {
            whenever(adminScheduleService.uploadAndValidate(any()))
                .thenThrow(ScheduleInvalidFileTypeException())

            val file = MockMultipartFile(
                "file", "test.csv", "text/csv", ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_TYPE"))
        }

        @Test
        @DisplayName("실패 - 빈 파일")
        fun upload_emptyFile() {
            whenever(adminScheduleService.uploadAndValidate(any()))
                .thenThrow(ScheduleEmptyFileException())

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMPTY_FILE"))
        }

        @Test
        @DisplayName("실패 - 행 초과")
        fun upload_rowLimitExceeded() {
            whenever(adminScheduleService.uploadAndValidate(any()))
                .thenThrow(ScheduleRowLimitExceededException())

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ROW_LIMIT_EXCEEDED"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/schedule/upload/confirm - 업로드 확정")
    inner class ConfirmUpload {

        @Test
        @DisplayName("성공 - 등록 완료")
        fun confirm_success() {
            val confirmResult = ScheduleConfirmResultDto(insertedCount = 10)
            whenever(adminScheduleService.confirmUpload("test-uuid")).thenReturn(confirmResult)

            val request = ScheduleConfirmRequest(uploadId = "test-uuid")

            mockMvc.perform(
                post("/api/v1/admin/schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inserted_count").value(10))
                .andExpect(jsonPath("$.message").value("등록이 완료되었습니다"))
        }

        @Test
        @DisplayName("실패 - 만료된 upload_id")
        fun confirm_notFound() {
            whenever(adminScheduleService.confirmUpload("expired-id"))
                .thenThrow(ScheduleUploadNotFoundException())

            val request = ScheduleConfirmRequest(uploadId = "expired-id")

            mockMvc.perform(
                post("/api/v1/admin/schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("UPLOAD_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 에러 있는 상태 확정")
        fun confirm_hasErrors() {
            whenever(adminScheduleService.confirmUpload("error-id"))
                .thenThrow(ScheduleHasValidationErrorsException())

            val request = ScheduleConfirmRequest(uploadId = "error-id")

            mockMvc.perform(
                post("/api/v1/admin/schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("HAS_VALIDATION_ERRORS"))
        }
    }
}
