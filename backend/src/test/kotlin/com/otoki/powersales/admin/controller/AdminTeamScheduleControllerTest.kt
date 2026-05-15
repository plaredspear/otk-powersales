package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingType
import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.service.AdminTeamScheduleService
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

@WebMvcTest(AdminTeamScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminTeamScheduleController 테스트")
class AdminTeamScheduleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminTeamScheduleService: AdminTeamScheduleService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.LEADER,
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
            whenever(adminTeamScheduleService.getMembers(1L)).thenReturn(members)

            mockMvc.perform(get("/api/v1/admin/team-schedule/members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].employeeCode").value("20030001"))
                .andExpect(jsonPath("$.data[0].name").value("홍길동"))
                .andExpect(jsonPath("$.data[1].employeeCode").value("20030002"))
                .andExpect(jsonPath("$.data[1].name").value("김영희"))
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
            whenever(adminTeamScheduleService.getAccounts(eq(1L), eq(null))).thenReturn(accounts)

            mockMvc.perform(get("/api/v1/admin/team-schedule/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].accountId").value(1001))
                .andExpect(jsonPath("$.data[0].externalKey").value("EXT001"))
                .andExpect(jsonPath("$.data[0].name").value("이마트 강남점"))
        }

        @Test
        @DisplayName("성공 - branch_code 파라미터 지정")
        fun getAccounts_withBranchCode() {
            val accounts = listOf(
                TeamScheduleAccountDto(accountId = 1003, externalKey = "EXT003", name = "롯데마트 서울역점")
            )
            whenever(adminTeamScheduleService.getAccounts(eq(1L), eq("BR001"))).thenReturn(accounts)

            mockMvc.perform(
                get("/api/v1/admin/team-schedule/accounts")
                    .param("branchCode", "BR001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].accountId").value(1003))
                .andExpect(jsonPath("$.data[0].name").value("롯데마트 서울역점"))
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
            whenever(adminTeamScheduleService.getBranches(1L)).thenReturn(branches)

            mockMvc.perform(get("/api/v1/admin/team-schedule/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].branchCode").value("1234"))
                .andExpect(jsonPath("$.data[0].branchName").value("서울지점"))
                .andExpect(jsonPath("$.data[1].branchCode").value("5678"))
                .andExpect(jsonPath("$.data[1].branchName").value("부산지점"))
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
                        isClockIn = true
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
            whenever(
                adminTeamScheduleService.getSchedulesWithSummary(
                    eq(1L),
                    eq(java.time.LocalDate.of(2026, 3, 1)),
                    eq(java.time.LocalDate.of(2026, 3, 31)),
                    eq(null),
                    eq(null)
                )
            ).thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/team-schedule")
                    .param("from", "2026-03-01")
                    .param("to", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schedules").isArray)
                .andExpect(jsonPath("$.data.schedules.length()").value(1))
                .andExpect(jsonPath("$.data.schedules[0].id").value(1))
                .andExpect(jsonPath("$.data.schedules[0].employeeCode").value("20030001"))
                .andExpect(jsonPath("$.data.schedules[0].employeeName").value("홍길동"))
                .andExpect(jsonPath("$.data.schedules[0].workingDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.schedules[0].workingType").value("진열"))
                .andExpect(jsonPath("$.data.schedules[0].isClockIn").value(true))
                .andExpect(jsonPath("$.data.schedules[0].accountId").value(1001))
                .andExpect(jsonPath("$.data.schedules[0].accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.dailySummary").isArray)
                .andExpect(jsonPath("$.data.dailySummary.length()").value(1))
                .andExpect(jsonPath("$.data.dailySummary[0].date").value("2026-03-15"))
                .andExpect(jsonPath("$.data.dailySummary[0].displayExpected").value(5))
                .andExpect(jsonPath("$.data.dailySummary[0].displayActual").value(3))
                .andExpect(jsonPath("$.data.dailySummary[0].promotionExpected").value(2))
                .andExpect(jsonPath("$.data.dailySummary[0].promotionActual").value(1))
                .andExpect(jsonPath("$.data.dailySummary[0].annualLeave").value(1))
                .andExpect(jsonPath("$.data.dailySummary[0].compensatoryLeave").value(0))
        }

        @Test
        @DisplayName("성공 - 필터 없이 조회 시 빈 배열 반환")
        fun getMonthlySchedules_emptyResult() {
            val response = MonthlyScheduleWithSummaryDto(
                schedules = emptyList(),
                dailySummary = emptyList()
            )
            whenever(
                adminTeamScheduleService.getSchedulesWithSummary(
                    eq(1L),
                    eq(java.time.LocalDate.of(2026, 3, 1)),
                    eq(java.time.LocalDate.of(2026, 3, 31)),
                    eq(null),
                    eq(null)
                )
            ).thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/team-schedule")
                    .param("from", "2026-03-01")
                    .param("to", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schedules").isArray)
                .andExpect(jsonPath("$.data.schedules.length()").value(0))
                .andExpect(jsonPath("$.data.dailySummary").isArray)
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
            whenever(adminTeamScheduleService.createSchedule(eq(1L), any()))
                .thenReturn(TeamScheduleCreateResultDto(id = 10L))

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

        @Test
        @DisplayName("실패 - 휴직 사원 등록")
        fun createSchedule_employeeOnLeave() {
            val request = TeamScheduleCreateRequest(
                employeeCode = "SF001",
                workingDate = "2026-03-20",
                workingType = WorkingType.WORK
            )
            whenever(adminTeamScheduleService.createSchedule(eq(1L), any()))
                .thenThrow(TeamScheduleEmployeeOnLeaveException())

            mockMvc.perform(
                post("/api/v1/admin/team-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_ON_LEAVE"))
        }

        @Test
        @DisplayName("실패 - 중복 일정 등록")
        fun createSchedule_scheduleConflict() {
            val request = TeamScheduleCreateRequest(
                employeeCode = "SF001",
                workingDate = "2026-03-20",
                workingType = WorkingType.WORK
            )
            whenever(adminTeamScheduleService.createSchedule(eq(1L), any()))
                .thenThrow(TeamScheduleConflictException("해당 날짜에 이미 일정이 등록되어 있습니다"))

            mockMvc.perform(
                post("/api/v1/admin/team-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_CONFLICT"))
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
            val request = TeamScheduleUpdateRequest(
                workingDate = "2026-03-21",
                workingType = WorkingType.WORK
            )
            whenever(adminTeamScheduleService.updateSchedule(eq(1L), eq(999L), any()))
                .thenThrow(TeamScheduleNotFoundException())

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
            mockMvc.perform(delete("/api/v1/admin/team-schedule/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 지점장 삭제 권한 없음")
        fun deleteSchedule_forbidden() {
            whenever(adminTeamScheduleService.deleteSchedule(eq(1L), eq(1L)))
                .thenThrow(TeamScheduleDeleteForbiddenException())

            mockMvc.perform(delete("/api/v1/admin/team-schedule/1"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 미존재 일정 삭제")
        fun deleteSchedule_notFound() {
            whenever(adminTeamScheduleService.deleteSchedule(eq(1L), eq(999L)))
                .thenThrow(TeamScheduleNotFoundException())

            mockMvc.perform(delete("/api/v1/admin/team-schedule/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }
}
