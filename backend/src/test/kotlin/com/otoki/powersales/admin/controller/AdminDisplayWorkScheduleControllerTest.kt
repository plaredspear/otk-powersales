package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleConfirmRequest
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.service.AdminDisplayWorkScheduleService
import com.otoki.powersales.domain.activity.schedule.service.MissingCostCenterException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.activity.schedule.dto.response.RowError
import com.otoki.powersales.domain.activity.schedule.dto.response.RowPreview
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteFailure
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchUnconfirmFailure
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchUnconfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleDetailDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleDeleteForbiddenException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleEditBlockedAfterConfirmException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleEmptyFileException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleHasValidationErrorsException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleInvalidFileTypeException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleRowLimitExceededException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleUploadNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleValidationException
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(AdminDisplayWorkScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminDisplayWorkScheduleController 테스트")
class AdminDisplayWorkScheduleControllerTest : AdminControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var adminDisplayWorkScheduleService: AdminDisplayWorkScheduleService

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    // @AutoConfigureMockMvc(addFilters = false) 환경에서 WebAdminContextFilter 가 동작하지 않으므로
    // ArgumentResolver 자체를 stub 하여 ALL scope 기본값 주입.
    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @MockkBean
    private lateinit var whitelistBranchScopeResolver: com.otoki.powersales.admin.service.WhitelistBranchScopeResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
        // 목록/엑셀 지점 스코프 산출 — 기본은 전사(All, 지점 필터 미적용). branchCode 미지정 케이스 정합.
        every { whitelistBranchScopeResolver.effectiveBranchCodes(any(), any()) } returns com.otoki.powersales.admin.dto.EffectiveBranchResult.All
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/meta - 목록 조회 조건 로드")
    inner class GetScheduleListMeta {

        private fun staticMeta() = com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListMetaResponse(
            filters = listOf(
                com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterMeta(
                    key = "typeOfWork3",
                    type = com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterType.SELECT,
                    options = listOf(
                        com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterOption("고정", "고정"),
                    ),
                ),
            ),
            defaults = com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListDefaults(
                pageSize = 50,
                sort = "startDate,DESC",
            ),
        )

        @Test
        @DisplayName("성공 - 전사 권한자: 지점 옵션 다건이 filters 에 조립되어 반환")
        fun getListMeta_allBranches() {
            every { adminDisplayWorkScheduleService.getScheduleListMetaStatic() } returns staticMeta()
            every { whitelistBranchScopeResolver.getBranches(any()) } returns listOf(
                com.otoki.powersales.platform.common.dto.response.BranchResponse("1101", "서울지점"),
                com.otoki.powersales.platform.common.dto.response.BranchResponse("1102", "부산지점"),
            )

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/meta"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.defaults.pageSize").value(50))
                .andExpect(jsonPath("$.data.filters[0].key").value("typeOfWork3"))
                // branchCode 필터가 정적 메타 뒤에 조립됨
                .andExpect(jsonPath("$.data.filters[1].key").value("branchCode"))
                .andExpect(jsonPath("$.data.filters[1].type").value("SELECT"))
                .andExpect(jsonPath("$.data.filters[1].options[0].value").value("1101"))
                .andExpect(jsonPath("$.data.filters[1].options[0].label").value("서울지점"))
                .andExpect(jsonPath("$.data.filters[1].options[1].value").value("1102"))
        }

        @Test
        @DisplayName("성공 - 지점 사용자: 본인 지점 1건만 조립")
        fun getListMeta_singleBranch() {
            every { adminDisplayWorkScheduleService.getScheduleListMetaStatic() } returns staticMeta()
            every { whitelistBranchScopeResolver.getBranches(any()) } returns listOf(
                com.otoki.powersales.platform.common.dto.response.BranchResponse("1101", "서울지점"),
            )

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/meta"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.filters[1].key").value("branchCode"))
                .andExpect(jsonPath("$.data.filters[1].options.length()").value(1))
                .andExpect(jsonPath("$.data.filters[1].options[0].value").value("1101"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/list - 스케줄 목록 조회")
    inner class ListSchedules {

        @Test
        @DisplayName("성공 - 스케줄 목록 반환")
        fun list_success() {
            val items = listOf(
                ScheduleListItemDto(
                    id = 1L,
                    employeeId = 10L,
                    employeeCode = "20030001",
                    employeeName = "홍길동",
                    branchName = "성수지점",
                    employmentStatus = "재직",
                    validData = "유효",
                    valid = "GREEN",
                    accountId = 100,
                    accountCode = "SAP001",
                    accountName = "이마트 성수점",
                    accountType = "대형마트(3대)",
                    accountStatus = "거래",
                    typeOfWork3 = "고정",
                    typeOfWork4 = "상온",
                    typeOfWork5 = "상시",
                    startDate = LocalDate.of(2026, 1, 1),
                    endDate = LocalDate.of(2026, 12, 31),
                    confirmed = false,
                    costCenterCode = "A100",
                    lastMonthRevenue = 15000000L
                )
            )
            val page = PageImpl(items, PageRequest.of(0, 20), 1)
            every { adminDisplayWorkScheduleService.listSchedules(
                any(), eq(0), eq(20), null, null, null, null, null, null, null, null, null, any(), any()
            ) } returns page

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/list"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 적용")
        fun list_withFilters() {
            val emptyPage = PageImpl<ScheduleListItemDto>(emptyList(), PageRequest.of(0, 20), 0)
            every { adminDisplayWorkScheduleService.listSchedules(
                any(), eq(0), eq(20), eq("123"), null, null, eq(true), null, null, null, null, null, any(), any()
            ) } returns emptyPage

            mockMvc.perform(
                get("/api/v1/admin/display-work-schedule/list")
                    .param("employeeCode", "123")
                    .param("confirmed", "true")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty())
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun list_empty() {
            val emptyPage = PageImpl<ScheduleListItemDto>(emptyList(), PageRequest.of(0, 20), 0)
            every { adminDisplayWorkScheduleService.listSchedules(
                any(), eq(0), eq(20), null, null, null, null, null, null, null, null, null, any(), any()
            ) } returns emptyPage

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/list"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - preset 파라미터 (END) 가 service 에 전달")
        fun list_withPreset() {
            val emptyPage = PageImpl<ScheduleListItemDto>(emptyList(), PageRequest.of(0, 20), 0)
            every { adminDisplayWorkScheduleService.listSchedules(
                any(), eq(0), eq(20), null, null, null, null, null, null, null,
                eq(SchedulePreset.END), any(), any(), any()
            ) } returns emptyPage

            mockMvc.perform(
                get("/api/v1/admin/display-work-schedule/list")
                    .param("preset", "END")
            )
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("성공 - sortBy/sortDir 파라미터가 Sort 로 변환")
        fun list_withSort() {
            val emptyPage = PageImpl<ScheduleListItemDto>(emptyList(), PageRequest.of(0, 20), 0)
            every { adminDisplayWorkScheduleService.listSchedules(
                any(), eq(0), eq(20), null, null, null, null, null, null, null, null,
                any(), any(),
                match { it.getOrderFor("endDate")?.direction == Sort.Direction.ASC }
            ) } returns emptyPage

            mockMvc.perform(
                get("/api/v1/admin/display-work-schedule/list")
                    .param("sortBy", "endDate")
                    .param("sortDir", "asc")
            )
                .andExpect(status().isOk)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/display-work-schedule - 단건 신규 등록")
    inner class CreateSchedule {

        @Test
        @DisplayName("성공 - 등록 결과 반환")
        fun create_success() {
            val request = AdminScheduleCreateRequest(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork1 = "진열",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null
            )
            val result = ScheduleCreateResultDto(
                id = 1L,
                employeeCode = "20030001",
                employeeName = "홍길동",
                accountCode = "ACC001",
                accountName = "이마트 강남점",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null,
                costCenterCode = "A10010",
                lastMonthRevenue = 5000000L
            )
            every { adminDisplayWorkScheduleService.createSchedule(any(), eq(1L), any()) } returns result

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("스케줄이 등록되었습니다"))
        }

        @Test
        @DisplayName("실패 - 검증 에러 (ScheduleValidationException 400)")
        fun create_validationFailure() {
            val request = AdminScheduleCreateRequest(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork1 = "진열",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = null
            )
            every { adminDisplayWorkScheduleService.createSchedule(any(), eq(1L), any()) } throws ScheduleValidationException("기간내에 동일한 거래처가 등록되어 있습니다")

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("기간내에 동일한 거래처가 등록되어 있습니다"))
        }

        @Test
        @DisplayName("실패 - 필수 파라미터 누락 (employeeCode blank)")
        fun create_blankEmployeeCode() {
            val invalidBody = """
                {"employeeCode":"","accountCode":"ACC001","typeOfWork3":"고정","typeOfWork4":"상온","typeOfWork5":"상시","startDate":"2026-05-01"}
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/{id} - 단건 상세 조회")
    inner class GetScheduleDetail {

        @Test
        @DisplayName("성공 - 편집 필드 + readonly 계산 정보 반환")
        fun getDetail_success() {
            val detail = ScheduleDetailDto(
                id = 10L,
                name = "SM-00002829",
                confirmed = false,
                employeeCode = "20030001",
                employeeName = "홍길동",
                accountCode = "ACC001",
                accountName = "이마트 강남점",
                typeOfWork1 = "진열",
                typeOfWork3 = "순회",
                typeOfWork4 = null,
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 3, 19),
                endDate = null,
                branchName = "강남53지점",
                title = "사원",
                employmentStatus = "재직",
                accountStatus = "거래",
                accountType = "대형마트(3대)",
                valid = "유효",
                validData = "유효",
                costCenterCode = "5452",
                lastMonthRevenue = 121861916L,
            )
            every { adminDisplayWorkScheduleService.getScheduleDetail(any(), eq(10L)) } returns detail

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.name").value("SM-00002829"))
                .andExpect(jsonPath("$.data.accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.employmentStatus").value("재직"))
                .andExpect(jsonPath("$.data.valid").value("유효"))
                .andExpect(jsonPath("$.data.lastMonthRevenue").value(121861916))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/display-work-schedule/{id} - 단건 편집")
    inner class UpdateSchedule {

        @Test
        @DisplayName("성공 - 편집 결과 반환")
        fun update_success() {
            val request = AdminScheduleUpdateRequest(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork1 = "진열",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 12, 31)
            )
            val result = ScheduleCreateResultDto(
                id = 10L,
                employeeCode = "20030001",
                employeeName = "홍길동",
                accountCode = "ACC001",
                accountName = "이마트 강남점",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 12, 31),
                costCenterCode = "A10010",
                lastMonthRevenue = 3000000L
            )
            every { adminDisplayWorkScheduleService.updateSchedule(any(), eq(1L), eq(10L), any()) } returns result

            mockMvc.perform(
                put("/api/v1/admin/display-work-schedule/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.message").value("스케줄이 수정되었습니다"))
        }

        @Test
        @DisplayName("실패 - 확정 후 차단 (UC-05) → 409")
        fun update_blockedAfterConfirm() {
            val request = AdminScheduleUpdateRequest(
                employeeCode = "20030001",
                accountCode = "ACC_NEW",
                typeOfWork1 = "진열",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 12, 31)
            )
            every { adminDisplayWorkScheduleService.updateSchedule(any(), eq(1L), eq(10L), any()) } throws ScheduleEditBlockedAfterConfirmException()

            mockMvc.perform(
                put("/api/v1/admin/display-work-schedule/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_EDIT_BLOCKED_AFTER_CONFIRM"))
        }

        @Test
        @DisplayName("실패 - 미존재 스케줄 → 404")
        fun update_notFound() {
            val request = AdminScheduleUpdateRequest(
                employeeCode = "20030001",
                accountCode = "ACC001",
                typeOfWork1 = "진열",
                typeOfWork3 = "고정",
                typeOfWork4 = "상온",
                typeOfWork5 = "상시",
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 12, 31)
            )
            every { adminDisplayWorkScheduleService.updateSchedule(any(), eq(1L), eq(999L), any()) } throws ScheduleNotFoundException(
                "존재하지 않거나 삭제된 스케줄입니다"
            )

            mockMvc.perform(
                put("/api/v1/admin/display-work-schedule/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/display-work-schedule/confirm - 일괄 확정")
    inner class BatchConfirm {

        @Test
        @DisplayName("성공 - 3건 확정")
        fun confirm_success() {
            val result = ScheduleBatchConfirmResultDto(updatedCount = 3)
            every { adminDisplayWorkScheduleService.batchConfirm(listOf(1L, 2L, 3L)) } returns result

            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 2, 3]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(3))
                .andExpect(jsonPath("$.message").value("3건이 확정되었습니다"))
        }

        @Test
        @DisplayName("실패 - 빈 ids 목록")
        fun confirm_emptyIds() {
            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": []}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 미존재 ID 포함")
        fun confirm_notFound() {
            every { adminDisplayWorkScheduleService.batchConfirm(listOf(1L, 999L)) } throws ScheduleNotFoundException()

            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 999]}""")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/display-work-schedule/unconfirm - 확정 해제")
    inner class BatchUnconfirm {

        @Test
        @DisplayName("성공 - 2건 확정 해제")
        fun unconfirm_success() {
            val result = ScheduleBatchUnconfirmResultDto(updatedCount = 2, failedCount = 0, failures = emptyList())
            every { adminDisplayWorkScheduleService.batchUnconfirm(any(), any(), listOf(1L, 2L)) } returns result

            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/unconfirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 2]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(2))
                .andExpect(jsonPath("$.message").value("2건이 확정 해제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 빈 ids 목록")
        fun unconfirm_emptyIds() {
            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/unconfirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": []}""")
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("partial success - 차단 건은 failures 로 응답")
        fun unconfirm_partialFail() {
            val result = ScheduleBatchUnconfirmResultDto(
                updatedCount = 1,
                failedCount = 1,
                failures = listOf(
                    ScheduleBatchUnconfirmFailure(999L, "SCHEDULE_NOT_FOUND", "존재하지 않거나 삭제된 스케줄입니다")
                )
            )
            every { adminDisplayWorkScheduleService.batchUnconfirm(any(), any(), listOf(1L, 999L)) } returns result

            mockMvc.perform(
                patch("/api/v1/admin/display-work-schedule/unconfirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 999]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.updatedCount").value(1))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.failures[0].errorCode").value("SCHEDULE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/display-work-schedule/export - 선택 다운로드 (UC-08)")
    inner class ExportSchedules {

        @Test
        @DisplayName("성공 - Excel byte 응답 + Content-Disposition")
        fun export_success() {
            val result = AdminDisplayWorkScheduleService.TemplateResult(
                bytes = ByteArray(800),
                filename = "진열스케줄_20260516_120000.xlsx"
            )
            every { adminDisplayWorkScheduleService.exportSchedules(any(), eq(listOf(1L, 2L, 3L))) } returns result

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/export")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 2, 3]}""")
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
        @DisplayName("실패 - 빈 ids 목록 → 400")
        fun export_emptyIds() {
            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/export")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": []}""")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/export-all - 검색결과 전체 다운로드 (UC-08)")
    inner class ExportAllSchedules {

        @Test
        @DisplayName("성공 - Excel byte 응답 + Content-Disposition")
        fun exportAll_success() {
            val result = ExcelResult(
                bytes = ByteArray(800),
                filename = "진열스케줄_20260516_120000.xlsx"
            )
            every {
                adminDisplayWorkScheduleService.exportAllSchedules(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns result

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/export-all"))
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
        @DisplayName("성공 - 검색/프리셋 필터 파라미터가 service 에 전달")
        fun exportAll_filterParams() {
            val result = ExcelResult(bytes = ByteArray(10), filename = "진열스케줄.xlsx")
            every {
                adminDisplayWorkScheduleService.exportAllSchedules(
                    any(), eq("2003"), eq("이마트"), any(), eq(true), eq("고정"), any(), any(), eq(SchedulePreset.END), any(), any(), any()
                )
            } returns result

            mockMvc.perform(
                get("/api/v1/admin/display-work-schedule/export-all")
                    .param("employeeCode", "2003")
                    .param("accountName", "이마트")
                    .param("confirmed", "true")
                    .param("typeOfWork3", "고정")
                    .param("preset", "END")
            )
                .andExpect(status().isOk)

            verify {
                adminDisplayWorkScheduleService.exportAllSchedules(
                    any(), eq("2003"), eq("이마트"), any(), eq(true), eq("고정"), any(), any(), eq(SchedulePreset.END), any(), any(), any()
                )
            }
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/display-work-schedule/batch-delete - 일괄 삭제 (UC-07)")
    inner class BatchDelete {

        @Test
        @DisplayName("성공 - partial success 결과 반환")
        fun batchDelete_partialSuccess() {
            val result = ScheduleBatchDeleteResultDto(
                deletedCount = 2,
                failedCount = 1,
                failures = listOf(
                    ScheduleBatchDeleteFailure(
                        id = 21L,
                        errorCode = "SCHEDULE_DELETE_CONSTRAINT",
                        message = "확정된 스케줄에 연결된 여사원 일정이 존재하여 삭제할 수 없습니다"
                    )
                )
            )
            every { adminDisplayWorkScheduleService.batchDelete(any(), eq(1L), eq(listOf(21L, 22L, 23L))) } returns result

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [21, 22, 23]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.deletedCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(1))
                .andExpect(jsonPath("$.data.failures[0].errorCode").value("SCHEDULE_DELETE_CONSTRAINT"))
                .andExpect(jsonPath("$.message").value("2건 삭제 / 1건 실패"))
        }

        @Test
        @DisplayName("성공 - 전체 삭제")
        fun batchDelete_allSucceed() {
            val result = ScheduleBatchDeleteResultDto(
                deletedCount = 3, failedCount = 0, failures = emptyList()
            )
            every { adminDisplayWorkScheduleService.batchDelete(any(), eq(1L), any()) } returns result

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 2, 3]}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.deletedCount").value(3))
                .andExpect(jsonPath("$.message").value("3건이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - BRANCH_MANAGER → 403")
        fun batchDelete_branchManagerForbidden() {
            every { adminDisplayWorkScheduleService.batchDelete(any(), eq(1L), any()) } throws ScheduleDeleteForbiddenException()

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": [1, 2]}""")
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 빈 ids 목록 → 400")
        fun batchDelete_emptyIds() {
            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"ids": []}""")
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/template - 양식 다운로드")
    inner class DownloadTemplate {

        @Test
        @DisplayName("성공 - Excel 파일 다운로드 (파라미터 없이)")
        fun downloadTemplate_success() {
            val result = AdminDisplayWorkScheduleService.TemplateResult(
                bytes = ByteArray(100),
                filename = "진열스케줄_양식_20260314120000.xlsx"
            )
            every { adminDisplayWorkScheduleService.generateTemplate(any(), eq(1L)) } returns result

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/template"))
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
        @DisplayName("실패 - 사용자 미존재")
        fun downloadTemplate_userNotFound() {
            every { adminDisplayWorkScheduleService.generateTemplate(any(), eq(1L)) } throws EmployeeNotFoundException()

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/template"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 소속 지점 미설정")
        fun downloadTemplate_missingCostCenter() {
            every { adminDisplayWorkScheduleService.generateTemplate(any(), eq(1L)) } throws MissingCostCenterException()

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/template"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("MISSING_COST_CENTER"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/display-work-schedule/branches - 지점 셀렉터 옵션")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 권한별 지점 화이트리스트 반환 (정적 경로가 /{id} 보다 우선 매칭)")
        fun getBranches_success() {
            every { whitelistBranchScopeResolver.getBranches(any()) } returns listOf(
                com.otoki.powersales.platform.common.dto.response.BranchResponse(branchCode = "1101", branchName = "성수지점"),
                com.otoki.powersales.platform.common.dto.response.BranchResponse(branchCode = "1102", branchName = "강남지점"),
            )

            mockMvc.perform(get("/api/v1/admin/display-work-schedule/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("1101"))
                .andExpect(jsonPath("$.data[0].branchName").value("성수지점"))
                .andExpect(jsonPath("$.data[1].branchCode").value("1102"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/display-work-schedule/upload - Excel 업로드")
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
                    RowPreview(4, "20030001", "홍길동", "ACC001", "이마트 강남점", "고정", "상온", "상시", "2026-04-01", null)
                )
            )
            every { adminDisplayWorkScheduleService.uploadAndValidate(any(), any()) } returns uploadResult

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/display-work-schedule/upload").file(file))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.uploadId").value("test-uuid"))
                .andExpect(jsonPath("$.data.errorRows").value(1))
                .andExpect(jsonPath("$.data.errors[0].row").value(5))
        }

        @Test
        @DisplayName("실패 - 파일 미첨부")
        fun upload_noFile() {
            mockMvc.perform(post("/api/v1/admin/display-work-schedule/upload"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("FILE_REQUIRED"))
        }

        @Test
        @DisplayName("실패 - 잘못된 확장자")
        fun upload_invalidFileType() {
            every { adminDisplayWorkScheduleService.uploadAndValidate(any(), any()) } throws ScheduleInvalidFileTypeException()

            val file = MockMultipartFile(
                "file", "test.csv", "text/csv", ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/display-work-schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE_TYPE"))
        }

        @Test
        @DisplayName("실패 - 빈 파일")
        fun upload_emptyFile() {
            every { adminDisplayWorkScheduleService.uploadAndValidate(any(), any()) } throws ScheduleEmptyFileException()

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/display-work-schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMPTY_FILE"))
        }

        @Test
        @DisplayName("실패 - 행 초과")
        fun upload_rowLimitExceeded() {
            every { adminDisplayWorkScheduleService.uploadAndValidate(any(), any()) } throws ScheduleRowLimitExceededException()

            val file = MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                ByteArray(100)
            )

            mockMvc.perform(multipart("/api/v1/admin/display-work-schedule/upload").file(file))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ROW_LIMIT_EXCEEDED"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/display-work-schedule/upload/confirm - 업로드 확정")
    inner class ConfirmUpload {

        @Test
        @DisplayName("성공 - 등록 완료")
        fun confirm_success() {
            val confirmResult = ScheduleConfirmResultDto(insertedCount = 10)
            every { adminDisplayWorkScheduleService.confirmUpload("test-uuid", any()) } returns confirmResult

            val request = ScheduleConfirmRequest(uploadId = "test-uuid")

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.insertedCount").value(10))
                .andExpect(jsonPath("$.message").value("등록이 완료되었습니다"))
        }

        @Test
        @DisplayName("실패 - 만료된 upload_id")
        fun confirm_notFound() {
            every { adminDisplayWorkScheduleService.confirmUpload("expired-id", any()) } throws ScheduleUploadNotFoundException()

            val request = ScheduleConfirmRequest(uploadId = "expired-id")

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("UPLOAD_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 에러 있는 상태 확정")
        fun confirm_hasErrors() {
            every { adminDisplayWorkScheduleService.confirmUpload("error-id", any()) } throws ScheduleHasValidationErrorsException()

            val request = ScheduleConfirmRequest(uploadId = "error-id")

            mockMvc.perform(
                post("/api/v1/admin/display-work-schedule/upload/confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("HAS_VALIDATION_ERRORS"))
        }
    }
}
