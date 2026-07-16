package com.otoki.powersales.domain.activity.schedule.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleAccountRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleCategory3LimitExceededException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleDuplicateLeaveException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleInvalidWorkingTypeException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderAccountException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotLeaderException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleNotTeamMemberException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeInactiveException
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.service.LeaderScheduleService
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.activity.schedule.controller.LeaderScheduleController
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(LeaderScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LeaderScheduleController 테스트")
class LeaderScheduleControllerTest : MobileControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var leaderScheduleService: LeaderScheduleService

    private val leaderId = 4001L

    @BeforeEach
    fun setUpLeaderPrincipal() {
        authenticateAs(leaderId, AppAuthority.LEADER)
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

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.activity.schedule.controller.LeaderScheduleControllerTest#createExceptionCases")
        @DisplayName("실패 - service 예외 → status + errorCode 매핑")
        fun create_exceptionMapping(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: RuntimeException,
            expectedStatus: Int,
            expectedCode: String,
        ) {
            stubServiceThrows(exception)
            mockMvc.perform(postValid())
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
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
                    status = "활동", costCenterCode = "C001", phone = "010-1111-2222",
                    deviceBound = true, loginActive = true
                ),
                LeaderTeamMemberListResponse(
                    id = 5013, employeeCode = "20300002", name = "팀원2",
                    status = "휴직", costCenterCode = "C001", phone = null,
                    deviceBound = false, loginActive = true
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

    companion object {
        @JvmStatic
        fun createExceptionCases(): List<Arguments> = listOf(
            Arguments.of("비조장 -> 403 NOT_LEADER", LeaderScheduleNotLeaderException(), 403, "NOT_LEADER"),
            Arguments.of("다른 팀의 직원 -> 403 NOT_TEAM_MEMBER", LeaderScheduleNotTeamMemberException(), 403, "NOT_TEAM_MEMBER"),
            Arguments.of("다른 거래처 -> 403 NOT_LEADER_ACCOUNT", LeaderScheduleNotLeaderAccountException(), 403, "NOT_LEADER_ACCOUNT"),
            Arguments.of("휴직 직원 -> 403 TARGET_EMPLOYEE_INACTIVE", LeaderScheduleTargetEmployeeInactiveException(), 403, "TARGET_EMPLOYEE_INACTIVE"),
            Arguments.of("미존재 직원 -> 404 TARGET_EMPLOYEE_NOT_FOUND", LeaderScheduleTargetEmployeeNotFoundException(), 404, "TARGET_EMPLOYEE_NOT_FOUND"),
            Arguments.of("근무 외 working_type -> 400 INVALID_WORKING_TYPE", LeaderScheduleInvalidWorkingTypeException(), 400, "INVALID_WORKING_TYPE"),
            Arguments.of("거래처 누락 -> 400 ACCOUNT_REQUIRED", LeaderScheduleAccountRequiredException(), 400, "ACCOUNT_REQUIRED"),
            Arguments.of("연차 충돌 -> 409 DUPLICATE_LEAVE_SCHEDULE", LeaderScheduleDuplicateLeaveException(), 409, "DUPLICATE_LEAVE_SCHEDULE"),
            Arguments.of("카테고리3 갯수 초과 -> 409 CATEGORY3_LIMIT_EXCEEDED", LeaderScheduleCategory3LimitExceededException(), 409, "CATEGORY3_LIMIT_EXCEEDED"),
        )
    }
}
