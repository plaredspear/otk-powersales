package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingType
import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleMassDeleteRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.service.AdminTeamScheduleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminTeamScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminTeamScheduleController 테스트")
class AdminTeamScheduleControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminTeamScheduleService: AdminTeamScheduleService

    @BeforeEach
    fun setUpLeaderPrincipal() {
        authenticateAsAdmin(role = AppAuthority.LEADER, costCenterCode = "1234")
    }

    @Nested
    @DisplayName("GET /api/v1/admin/team-schedule/members - 여사원 목록 조회")
    inner class GetMembers {

        @Test
        @DisplayName("성공 - 여사원 목록 반환")
        fun getMembers_success() {
            val members = listOf(
                TeamMemberDto(employeeId = 1L, employeeCode = "20030001", name = "홍길동"),
                TeamMemberDto(employeeId = 2L, employeeCode = "20030002", name = "김영희")
            )
            every { adminTeamScheduleService.getMembers(any()) } returns members

            mockMvc.perform(get("/api/v1/admin/team-schedule/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].employeeCode").value("20030001"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/team-schedule/accounts - 거래처 목록 조회")
    inner class GetAccounts {

        @Test
        @DisplayName("성공 - 거래처 목록 반환")
        fun getAccounts_success() {
            val accounts = listOf(
                TeamScheduleAccountDto(accountId = 1001, externalKey = "EXT001", name = "이마트 강남점"),
                TeamScheduleAccountDto(accountId = 1002, externalKey = "EXT002", name = "홈플러스 잠실점")
            )
            every { adminTeamScheduleService.getAccounts(any(), null) } returns accounts

            mockMvc.perform(get("/api/v1/admin/team-schedule/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].externalKey").value("EXT001"))
        }

        @Test
        @DisplayName("성공 - branch_code 파라미터 지정")
        fun getAccounts_withBranchCode() {
            val accounts = listOf(
                TeamScheduleAccountDto(accountId = 1003, externalKey = "EXT003", name = "롯데마트 서울역점")
            )
            every { adminTeamScheduleService.getAccounts(any(), eq("BR001")) } returns accounts

            mockMvc.perform(
                get("/api/v1/admin/team-schedule/accounts")
                    .param("branchCode", "BR001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountId").value(1003))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/team-schedule/branches - 지점 목록 조회")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 지점 목록 반환")
        fun getBranches_success() {
            val branches = listOf(
                BranchResponse(branchCode = "1234", branchName = "서울지점"),
                BranchResponse(branchCode = "5678", branchName = "부산지점")
            )
            every { adminTeamScheduleService.getBranches(any()) } returns branches

            mockMvc.perform(get("/api/v1/admin/team-schedule/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].branchCode").value("1234"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/team-schedule - 월간 일정 + 일별 요약 통합 조회")
    inner class GetMonthlySchedules {

        @Test
        @DisplayName("성공 - schedules와 daily_summary 모두 포함된 응답 반환")
        fun getMonthlySchedules_success() {
            val response = MonthlyScheduleWithSummaryDto(
                schedules = listOf(
                    TeamScheduleDto(
                        id = 1L,
                        employeeCode = "20030001",
                        employeeName = "홍길동",
                        workingDate = "2026-03-15",
                        workingType = "진열",
                        workingCategory1 = "고정",
                        workingCategory2 = "상시",
                        workingCategory3 = null,
                        accountId = 1001,
                        accountName = "이마트 강남점",
                        accountExternalKey = "EXT001",
                        isClockIn = true,
                        promotionId = null
                    )
                ),
                dailySummary = listOf(
                    DailySummaryDto(
                        date = "2026-03-15",
                        displayExpected = 5,
                        displayActual = 3,
                        promotionExpected = 2,
                        promotionActual = 1,
                        annualLeave = 1,
                        compensatoryLeave = 0
                    )
                )
            )
            every {
                adminTeamScheduleService.getSchedulesWithSummary(
                    eq(java.time.LocalDate.of(2026, 3, 1)),
                    eq(java.time.LocalDate.of(2026, 3, 31)),
                    null,
                    null,
                    null
                )
            } returns response

            mockMvc.perform(
                get("/api/v1/admin/team-schedule")
                    .param("from", "2026-03-01")
                    .param("to", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schedules.length()").value(1))
                .andExpect(jsonPath("$.data.schedules[0].id").value(1))
                .andExpect(jsonPath("$.data.dailySummary.length()").value(1))
                .andExpect(jsonPath("$.data.dailySummary[0].date").value("2026-03-15"))
        }

        @Test
        @DisplayName("성공 - 필터 없이 조회 시 빈 배열 반환")
        fun getMonthlySchedules_emptyResult() {
            val response = MonthlyScheduleWithSummaryDto(schedules = emptyList(), dailySummary = emptyList())
            every {
                adminTeamScheduleService.getSchedulesWithSummary(
                    eq(java.time.LocalDate.of(2026, 3, 1)),
                    eq(java.time.LocalDate.of(2026, 3, 31)),
                    null,
                    null,
                    null
                )
            } returns response

            mockMvc.perform(
                get("/api/v1/admin/team-schedule")
                    .param("from", "2026-03-01")
                    .param("to", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.schedules.length()").value(0))
                .andExpect(jsonPath("$.data.dailySummary.length()").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/team-schedule - 일정 등록")
    inner class CreateSchedule {

        @Test
        @DisplayName("성공 - 일정 등록")
        fun createSchedule_success() {
            val request = TeamScheduleCreateRequest(
                employeeCode = "SF001",
                workingDate = "2026-03-20",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                workingCategory2 = WorkingCategory2.DEDICATED,
                accountId = 1001
            )
            every { adminTeamScheduleService.createSchedule(any(), any()) } returns TeamScheduleCreateResultDto(id = 10L)

            mockMvc.perform(
                post("/api/v1/admin/team-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.message").value("일정이 등록되었습니다"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminTeamScheduleControllerTest#createExceptionCases")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun createSchedule_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedCode: String
        ) {
            val request = TeamScheduleCreateRequest(
                employeeCode = "SF001",
                workingDate = "2026-03-20",
                workingType = WorkingType.WORK
            )
            every { adminTeamScheduleService.createSchedule(any(), any()) } throws exception

            mockMvc.perform(
                post("/api/v1/admin/team-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/team-schedule/{id} - 일정 수정")
    inner class UpdateSchedule {

        @Test
        @DisplayName("성공 - 일정 수정")
        fun updateSchedule_success() {
            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-03-21",
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                accountId = 1002
            )
            every { adminTeamScheduleService.updateSchedule(any(), any(), any()) } just Runs

            mockMvc.perform(
                put("/api/v1/admin/team-schedule/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정이 수정되었습니다"))
        }

        @Test
        @DisplayName("실패 - 미존재 일정 수정")
        fun updateSchedule_notFound() {
            val request = TeamScheduleUpdateRequest(workingDate = "2026-03-21", workingType = WorkingType.WORK)
            every { adminTeamScheduleService.updateSchedule(any(), eq(999L), any()) } throws TeamScheduleNotFoundException()

            mockMvc.perform(
                put("/api/v1/admin/team-schedule/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/team-schedule/{id} - 일정 삭제")
    inner class DeleteSchedule {

        @Test
        @DisplayName("성공 - 일정 삭제")
        fun deleteSchedule_success() {
            every { adminTeamScheduleService.deleteSchedule(any(), any()) } just Runs

            mockMvc.perform(delete("/api/v1/admin/team-schedule/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminTeamScheduleControllerTest#deleteExceptionCases")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun deleteSchedule_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            scheduleId: Long,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { adminTeamScheduleService.deleteSchedule(any(), eq(scheduleId)) } throws exception

            mockMvc.perform(delete("/api/v1/admin/team-schedule/$scheduleId"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/team-schedule/mass-delete - 일정 다건 삭제 (Spec #691 P1-B)")
    inner class MassDelete {

        @Test
        @DisplayName("성공 - 다건 삭제 + deletedCount 응답")
        fun massDelete_success() {
            val request = TeamScheduleMassDeleteRequest(ids = listOf(1L, 2L, 3L))
            every { adminTeamScheduleService.massDelete(any(), eq(listOf(1L, 2L, 3L))) } returns 3

            mockMvc.perform(
                post("/api/v1/admin/team-schedule/mass-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다"))
                .andExpect(jsonPath("$.data.deletedCount").value(3))
        }

        @Test
        @DisplayName("validation - 빈 ids 배열 → 400 BAD_REQUEST")
        fun massDelete_emptyIds_validationFails() {
            val request = TeamScheduleMassDeleteRequest(ids = emptyList())

            mockMvc.perform(
                post("/api/v1/admin/team-schedule/mass-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminTeamScheduleControllerTest#massDeleteExceptionCases")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun massDelete_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            val request = TeamScheduleMassDeleteRequest(ids = listOf(1L, 2L))
            every { adminTeamScheduleService.massDelete(any(), any()) } throws exception

            mockMvc.perform(
                post("/api/v1/admin/team-schedule/mass-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun createExceptionCases(): List<Arguments> = listOf(
            Arguments.of("employeeOnLeave -> EMPLOYEE_ON_LEAVE", TeamScheduleEmployeeOnLeaveException(), "EMPLOYEE_ON_LEAVE"),
            Arguments.of(
                "scheduleConflict -> SCHEDULE_CONFLICT",
                TeamScheduleConflictException("해당 날짜에 이미 일정이 등록되어 있습니다"),
                "SCHEDULE_CONFLICT",
            ),
        )

        @JvmStatic
        fun deleteExceptionCases(): List<Arguments> = listOf(
            Arguments.of("forbidden -> 403 FORBIDDEN", 1L, TeamScheduleDeleteForbiddenException(), 403, "FORBIDDEN"),
            Arguments.of("notFound -> 404 NOT_FOUND", 999L, TeamScheduleNotFoundException(), 404, "NOT_FOUND"),
        )

        @JvmStatic
        fun massDeleteExceptionCases(): List<Arguments> = listOf(
            Arguments.of("rowLimit -> 400 ROW_LIMIT_EXCEEDED", TeamScheduleMassDeleteRowLimitExceededException(), 400, "ROW_LIMIT_EXCEEDED"),
            Arguments.of("notFoundPartial -> 404 TEAM_SCHEDULE_NOT_FOUND_PARTIAL", TeamScheduleNotFoundPartialException(listOf(99L)), 404, "TEAM_SCHEDULE_NOT_FOUND_PARTIAL"),
            Arguments.of("workReportDelete -> 409 WORK_REPORT_DELETE_CONSTRAINT (Q5 옵션 1 전체 rollback)", TeamScheduleWorkReportDeleteException(), 409, "WORK_REPORT_DELETE_CONSTRAINT"),
            Arguments.of("displayMasterLink -> 409 DISPLAY_MASTER_LINK_CONSTRAINT", TeamScheduleDisplayMasterLinkException(), 409, "DISPLAY_MASTER_LINK_CONSTRAINT"),
            Arguments.of("branchManager -> 403 FORBIDDEN", TeamScheduleDeleteForbiddenException(), 403, "FORBIDDEN"),
        )
    }
}
