package com.otoki.powersales.common.controller

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.dto.response.HomeResponse
import com.otoki.powersales.common.service.HomeService
import com.otoki.powersales.common.test.MobileControllerTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(HomeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HomeController 테스트")
class HomeControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var homeService: HomeService

    @Nested
    @DisplayName("GET /api/v1/mobile/home - 홈 데이터 조회")
    inner class GetHomeData {

        // 분기 명세 (가드레일 5.3):
        //  - 여사원 케이스: safetyCheckRequired=true / expiryAlert 보유
        //  - 조장 케이스: safetyCheckRequired=false / expiryAlert null / 팀원 다수 일정 / workType null + accountName null serialization
        @Test
        @DisplayName("여사원 홈 조회 성공 - role=USER, 안전점검 필요, 출근현황 포함")
        fun getHomeData_user_success() {
            val commuteTime = java.time.LocalDateTime.of(2026, 2, 25, 8, 30, 0)
            val mockResponse = HomeResponse(
                todaySchedules = listOf(
                    HomeResponse.TeamMemberScheduleInfo(
                        scheduleId = 1L,
                        employeeName = "최금주",
                        employeeCode = "20030117",
                        accountName = "이마트 부산점",
                        accountId = 1,
                        workCategory = "순회",
                        workType = "진열",
                        isCommuteRegistered = true,
                        commuteRegisteredAt = commuteTime
                    )
                ),
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(totalCount = 3, registeredCount = 1),
                attendanceApplicable = true,
                safetyCheckRequired = true,
                expiryAlert = HomeResponse.ExpiryAlertInfo(
                    branchName = "부산1지점",
                    employeeName = "최금주",
                    employeeCode = "20030117",
                    expiryCount = 1
                ),
                notices = listOf(
                    HomeResponse.NoticeInfo(
                        id = 1L, title = "2월 영업 목표 달성 현황",
                        category = "BRANCH", categoryName = "지점공지",
                        createdAt = java.time.LocalDateTime.of(2026, 2, 5, 10, 0, 0)
                    ),
                    HomeResponse.NoticeInfo(
                        id = 2L, title = "신제품 출시 안내",
                        category = "COMPANY", categoryName = "회사공지",
                        createdAt = java.time.LocalDateTime.of(2026, 2, 4, 9, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            every { homeService.getHomeData(1L) } returns mockResponse

            mockMvc.perform(
                get("/api/v1/mobile/home").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todaySchedules.length()").value(1))
                .andExpect(jsonPath("$.data.attendanceSummary.totalCount").value(3))
                .andExpect(jsonPath("$.data.safetyCheckRequired").value(true))
                .andExpect(jsonPath("$.data.expiryAlert.expiryCount").value(1))
                .andExpect(jsonPath("$.data.notices.length()").value(2))
        }

        @Test
        @DisplayName("조장 홈 조회 성공 - role=LEADER, 안전점검 불필요, 팀원 다수 일정 + null serialization")
        fun getHomeData_leader_success() {
            authenticateAs(userId = 2L, role = AppAuthority.LEADER)

            val commuteTime1 = java.time.LocalDateTime.of(2026, 2, 25, 8, 15, 0)
            val mockResponse = HomeResponse(
                todaySchedules = listOf(
                    HomeResponse.TeamMemberScheduleInfo(
                        scheduleId = 10L,
                        employeeName = "최금주",
                        employeeCode = "20030117",
                        accountName = "이마트 부산점",
                        accountId = 1,
                        workCategory = "순회",
                        workType = "진열",
                        isCommuteRegistered = true,
                        commuteRegisteredAt = commuteTime1
                    ),
                    HomeResponse.TeamMemberScheduleInfo(
                        scheduleId = 11L,
                        employeeName = "김영희",
                        employeeCode = "20190523",
                        accountName = "홈플러스 서면점",
                        accountId = 2,
                        workCategory = "전담",
                        workType = null,
                        isCommuteRegistered = false,
                        commuteRegisteredAt = null
                    ),
                    HomeResponse.TeamMemberScheduleInfo(
                        scheduleId = 12L,
                        employeeName = "박소현",
                        employeeCode = "20210812",
                        accountName = null,
                        accountId = null,
                        workCategory = "내근",
                        workType = null,
                        isCommuteRegistered = false,
                        commuteRegisteredAt = null
                    )
                ),
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(totalCount = 5, registeredCount = 2),
                attendanceApplicable = true,
                safetyCheckRequired = false,
                expiryAlert = null,
                notices = listOf(
                    HomeResponse.NoticeInfo(
                        id = 3L, title = "3월 근태 관리 안내",
                        category = "COMPANY", categoryName = "회사공지",
                        createdAt = java.time.LocalDateTime.of(2026, 2, 24, 14, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            every { homeService.getHomeData(2L) } returns mockResponse

            mockMvc.perform(
                get("/api/v1/mobile/home").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.todaySchedules.length()").value(3))
                // null serialization 분기 명세 (DTO 의 nullable 필드가 doesNotExist 로 직렬화)
                .andExpect(jsonPath("$.data.todaySchedules[1].workType").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[1].commuteRegisteredAt").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[2].accountName").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[2].accountId").doesNotExist())
                .andExpect(jsonPath("$.data.safetyCheckRequired").value(false))
                .andExpect(jsonPath("$.data.expiryAlert").doesNotExist())
                .andExpect(jsonPath("$.data.notices.length()").value(1))
        }

        @Test
        @DisplayName("데이터 없는 경우 - 빈 일정/공지, 안전점검 불필요")
        fun getHomeData_emptyData() {
            val mockResponse = HomeResponse(
                todaySchedules = emptyList(),
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(totalCount = 0, registeredCount = 0),
                attendanceApplicable = true,
                safetyCheckRequired = false,
                expiryAlert = null,
                notices = emptyList(),
                currentDate = "2026-02-25"
            )

            every { homeService.getHomeData(1L) } returns mockResponse

            mockMvc.perform(
                get("/api/v1/mobile/home").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.todaySchedules").isEmpty)
                .andExpect(jsonPath("$.data.notices").isEmpty)
                .andExpect(jsonPath("$.data.expiryAlert").doesNotExist())
        }

        @Test
        @DisplayName("사용자 없음 - 404 USER_NOT_FOUND")
        fun getHomeData_userNotFound() {
            every { homeService.getHomeData(1L) } throws EmployeeNotFoundException()

            mockMvc.perform(
                get("/api/v1/mobile/home").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }
}
