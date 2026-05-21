package com.otoki.powersales.schedule.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.schedule.exception.LeaderScheduleCategory3LimitExceededException
import com.otoki.powersales.schedule.exception.LeaderScheduleDuplicateLeaveException
import com.otoki.powersales.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.schedule.service.LeaderScheduleService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(LeaderScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LeaderScheduleController 테스트")
class LeaderScheduleControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var leaderScheduleService: LeaderScheduleService

    @MockkBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockkBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockkBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockkBean private lateinit var gpsConsentFilter: GpsConsentFilter

    private val leaderId = 4001L
    private val testPrincipal = UserPrincipal(userId = leaderId, role = UserRoleEnum.LEADER)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.authorities)
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/leader/team-member-schedule")
    inner class CreateTeamMemberSchedule {

        @Test
        @DisplayName("성공 - 201 Created + proxy_registered_by 응답")
        fun create_success() {
            val request = createValidRequest()
            val response = LeaderScheduleCreateResponse(
                scheduleId = 78901L,
                targetEmployeeId = 5012L,
                workingDate = "2026-05-15",
                workingType = "근무",
                workingCategory3 = "고정",
                proxyRegisteredBy = leaderId
            )
            every { leaderScheduleService.createTeamMemberSchedule(eq(leaderId), any()) } returns response

            mockMvc.perform(
                post("/api/v1/mobile/leader/team-member-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(78901))
                .andExpect(jsonPath("$.data.targetEmployeeId").value(5012))
                .andExpect(jsonPath("$.data.workingDate").value("2026-05-15"))
                .andExpect(jsonPath("$.data.workingType").value("근무"))
                .andExpect(jsonPath("$.data.workingCategory3").value("고정"))
                .andExpect(jsonPath("$.data.proxyRegisteredBy").value(leaderId))
        }

        @Test
        @DisplayName("실패 - 비조장 -> 403 NOT_LEADER")
        fun create_notLeader() {
            stubServiceThrows(LeaderScheduleNotLeaderException())
            mockMvc.perform(postValid())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("NOT_LEADER"))
        }

        @Test
        @DisplayName("실패 - 다른 팀의 직원 -> 403 NOT_TEAM_MEMBER")
        fun create_notTeamMember() {
            stubServiceThrows(LeaderScheduleNotTeamMemberException())
            mockMvc.perform(postValid())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("NOT_TEAM_MEMBER"))
        }

        @Test
        @DisplayName("실패 - 다른 거래처 -> 403 NOT_LEADER_ACCOUNT")
        fun create_notLeaderAccount() {
            stubServiceThrows(LeaderScheduleNotLeaderAccountException())
            mockMvc.perform(postValid())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("NOT_LEADER_ACCOUNT"))
        }

        @Test
        @DisplayName("실패 - 휴직 직원 -> 403 TARGET_EMPLOYEE_INACTIVE")
        fun create_targetInactive() {
            stubServiceThrows(LeaderScheduleTargetEmployeeInactiveException())
            mockMvc.perform(postValid())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("TARGET_EMPLOYEE_INACTIVE"))
        }

        @Test
        @DisplayName("실패 - 미존재 직원 -> 404 TARGET_EMPLOYEE_NOT_FOUND")
        fun create_targetNotFound() {
            stubServiceThrows(LeaderScheduleTargetEmployeeNotFoundException())
            mockMvc.perform(postValid())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("TARGET_EMPLOYEE_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 근무 외 working_type -> 400 INVALID_WORKING_TYPE")
        fun create_invalidWorkingType() {
            stubServiceThrows(LeaderScheduleInvalidWorkingTypeException())
            mockMvc.perform(postValid())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_WORKING_TYPE"))
        }

        @Test
        @DisplayName("실패 - 거래처 누락 -> 400 ACCOUNT_REQUIRED")
        fun create_accountRequired() {
            stubServiceThrows(LeaderScheduleAccountRequiredException())
            mockMvc.perform(postValid())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_REQUIRED"))
        }

        @Test
        @DisplayName("실패 - 연차 충돌 -> 409 DUPLICATE_LEAVE_SCHEDULE")
        fun create_duplicateLeave() {
            stubServiceThrows(LeaderScheduleDuplicateLeaveException())
            mockMvc.perform(postValid())
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_LEAVE_SCHEDULE"))
        }

        @Test
        @DisplayName("실패 - 카테고리3 갯수 초과 -> 409 CATEGORY3_LIMIT_EXCEEDED")
        fun create_category3Limit() {
            stubServiceThrows(LeaderScheduleCategory3LimitExceededException())
            mockMvc.perform(postValid())
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("CATEGORY3_LIMIT_EXCEEDED"))
        }

        @Test
        @DisplayName("실패 - working_date 누락 -> 400 (Validation)")
        fun create_missingWorkingDate() {
            val invalidJson = """
                {
                  "targetEmployeeId": 5012,
                  "workingType": "근무",
                  "workingCategory2": "전담",
                  "workingCategory3": "고정",
                  "accountId": 90234
                }
            """.trimIndent()
            mockMvc.perform(
                post("/api/v1/mobile/leader/team-member-schedule")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        private fun stubServiceThrows(ex: RuntimeException) {
            every { leaderScheduleService.createTeamMemberSchedule(eq(leaderId), any()) } throws ex
        }

        private fun postValid() = post("/api/v1/mobile/leader/team-member-schedule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createValidRequest()))
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/leader/team-members")
    inner class GetTeamMembers {
        @Test
        @DisplayName("성공 - 팀원 목록 반환")
        fun success() {
            every { leaderScheduleService.getTeamMembers(eq(leaderId)) } returns listOf(
                LeaderTeamMemberListResponse(
                    id = 5012, employeeCode = "20300001", name = "팀원1",
                    status = "활동", costCenterCode = "C001"
                ),
                LeaderTeamMemberListResponse(
                    id = 5013, employeeCode = "20300002", name = "팀원2",
                    status = "휴직", costCenterCode = "C001"
                )
            )

            mockMvc.perform(get("/api/v1/mobile/leader/team-members"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].employeeCode").value("20300001"))
                .andExpect(jsonPath("$.data[0].costCenterCode").value("C001"))
                .andExpect(jsonPath("$.data[1].status").value("휴직"))
        }

        @Test
        @DisplayName("실패 - 비조장 -> 403 NOT_LEADER")
        fun notLeader() {
            every { leaderScheduleService.getTeamMembers(eq(leaderId)) } throws LeaderScheduleNotLeaderException()

            mockMvc.perform(get("/api/v1/mobile/leader/team-members"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("NOT_LEADER"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/leader/accounts")
    inner class GetAccounts {
        @Test
        @DisplayName("성공 - 거래처 목록 반환")
        fun success() {
            every { leaderScheduleService.getAccounts(eq(leaderId), null) } returns listOf(
                LeaderAccountListResponse(
                    id = 90234, name = "AlphaMart", address1 = "Seoul",
                    branchCode = "C001", accountGroup = "1000", accountType = "TYPE_A"
                )
            )

            mockMvc.perform(get("/api/v1/mobile/leader/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(90234))
                .andExpect(jsonPath("$.data[0].branchCode").value("C001"))
                .andExpect(jsonPath("$.data[0].accountGroup").value("1000"))
                .andExpect(jsonPath("$.data[0].accountType").value("TYPE_A"))
        }

        @Test
        @DisplayName("성공 - keyword 파라미터 전달")
        fun withKeyword() {
            every { leaderScheduleService.getAccounts(eq(leaderId), eq("alpha")) } returns emptyList()

            mockMvc.perform(get("/api/v1/mobile/leader/accounts").param("keyword", "alpha"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isArray)
        }

        @Test
        @DisplayName("실패 - 비조장 -> 403 NOT_LEADER")
        fun notLeader() {
            every { leaderScheduleService.getAccounts(eq(leaderId), any()) } throws LeaderScheduleNotLeaderException()

            mockMvc.perform(get("/api/v1/mobile/leader/accounts"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("NOT_LEADER"))
        }
    }

    private fun createValidRequest(): LeaderScheduleCreateRequest = LeaderScheduleCreateRequest(
        targetEmployeeId = 5012L,
        workingDate = "2026-05-15",
        workingType = "근무",
        workingCategory2 = "전담",
        workingCategory3 = "고정",
        accountId = 90234,
        workingCategory1 = "진열"
    )
}
