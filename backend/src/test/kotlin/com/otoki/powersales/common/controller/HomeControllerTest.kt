package com.otoki.powersales.common.controller

import com.otoki.powersales.common.dto.response.HomeResponse
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.common.service.HomeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

/**
 * HomeController 테스트
 * addFilters=false로 Security 필터 비활성화, 직접 SecurityContext 설정
 */
@WebMvcTest(HomeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("HomeController 테스트")
class HomeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var homeService: HomeService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/home - 홈 데이터 조회")
    inner class GetHomeData {

        @Test
        @DisplayName("여사원 홈 조회 성공 - role=USER, 안전점검 필요, 출근현황 포함")
        fun getHomeData_user_success() {
            // Given
            val commuteTime = LocalDateTime.of(2026, 2, 25, 8, 30, 0)
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
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(
                    totalCount = 3,
                    registeredCount = 1
                ),
                safetyCheckRequired = true,
                expiryAlert = HomeResponse.ExpiryAlertInfo(
                    branchName = "부산1지점",
                    employeeName = "최금주",
                    employeeCode = "20030117",
                    expiryCount = 1
                ),
                notices = listOf(
                    HomeResponse.NoticeInfo(
                        id = 1L,
                        title = "2월 영업 목표 달성 현황",
                        category = "BRANCH",
                        categoryName = "지점공지",
                        createdAt = LocalDateTime.of(2026, 2, 5, 10, 0, 0)
                    ),
                    HomeResponse.NoticeInfo(
                        id = 2L,
                        title = "신제품 출시 안내",
                        category = "COMPANY",
                        categoryName = "회사공지",
                        createdAt = LocalDateTime.of(2026, 2, 4, 9, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(homeService.getHomeData(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules
                .andExpect(jsonPath("$.data.todaySchedules").isArray)
                .andExpect(jsonPath("$.data.todaySchedules.length()").value(1))
                .andExpect(jsonPath("$.data.todaySchedules[0].scheduleId").value(1))
                .andExpect(jsonPath("$.data.todaySchedules[0].employeeName").value("최금주"))
                .andExpect(jsonPath("$.data.todaySchedules[0].employeeCode").value("20030117"))
                .andExpect(jsonPath("$.data.todaySchedules[0].accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.todaySchedules[0].accountId").value(1))
                .andExpect(jsonPath("$.data.todaySchedules[0].workCategory").value("순회"))
                .andExpect(jsonPath("$.data.todaySchedules[0].workType").value("진열"))
                .andExpect(jsonPath("$.data.todaySchedules[0].isCommuteRegistered").value(true))
                .andExpect(jsonPath("$.data.todaySchedules[0].commuteRegisteredAt").exists())
                // attendance_summary
                .andExpect(jsonPath("$.data.attendanceSummary.totalCount").value(3))
                .andExpect(jsonPath("$.data.attendanceSummary.registeredCount").value(1))
                // safety_check_required
                .andExpect(jsonPath("$.data.safetyCheckRequired").value(true))
                // expiry_alert
                .andExpect(jsonPath("$.data.expiryAlert.branchName").value("부산1지점"))
                .andExpect(jsonPath("$.data.expiryAlert.employeeName").value("최금주"))
                .andExpect(jsonPath("$.data.expiryAlert.employeeCode").value("20030117"))
                .andExpect(jsonPath("$.data.expiryAlert.expiryCount").value(1))
                // notices
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices.length()").value(2))
                .andExpect(jsonPath("$.data.notices[0].id").value(1))
                .andExpect(jsonPath("$.data.notices[0].title").value("2월 영업 목표 달성 현황"))
                .andExpect(jsonPath("$.data.notices[0].category").value("BRANCH"))
                .andExpect(jsonPath("$.data.notices[0].categoryName").value("지점공지"))
                .andExpect(jsonPath("$.data.notices[0].type").doesNotExist())
                .andExpect(jsonPath("$.data.notices[1].id").value(2))
                .andExpect(jsonPath("$.data.notices[1].title").value("신제품 출시 안내"))
                .andExpect(jsonPath("$.data.notices[1].category").value("COMPANY"))
                .andExpect(jsonPath("$.data.notices[1].categoryName").value("회사공지"))
                // current_date
                .andExpect(jsonPath("$.data.currentDate").value("2026-02-25"))
        }

        @Test
        @DisplayName("조장 홈 조회 성공 - role=LEADER, 안전점검 불필요, 팀원 다수 일정")
        fun getHomeData_leader_success() {
            // Given
            val leaderPrincipal = UserPrincipal(userId = 2L, role = UserRole.LEADER)
            val leaderAuth = UsernamePasswordAuthenticationToken(
                leaderPrincipal, null, leaderPrincipal.authorities
            )
            SecurityContextHolder.getContext().authentication = leaderAuth

            val commuteTime1 = LocalDateTime.of(2026, 2, 25, 8, 15, 0)
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
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(
                    totalCount = 5,
                    registeredCount = 2
                ),
                safetyCheckRequired = false,
                expiryAlert = null,
                notices = listOf(
                    HomeResponse.NoticeInfo(
                        id = 3L,
                        title = "3월 근태 관리 안내",
                        category = "COMPANY",
                        categoryName = "회사공지",
                        createdAt = LocalDateTime.of(2026, 2, 24, 14, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(homeService.getHomeData(2L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules - 팀원 3명 일정
                .andExpect(jsonPath("$.data.todaySchedules").isArray)
                .andExpect(jsonPath("$.data.todaySchedules.length()").value(3))
                // 첫 번째 팀원 (출근 등록됨)
                .andExpect(jsonPath("$.data.todaySchedules[0].scheduleId").value(10))
                .andExpect(jsonPath("$.data.todaySchedules[0].employeeName").value("최금주"))
                .andExpect(jsonPath("$.data.todaySchedules[0].employeeCode").value("20030117"))
                .andExpect(jsonPath("$.data.todaySchedules[0].accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.todaySchedules[0].accountId").value(1))
                .andExpect(jsonPath("$.data.todaySchedules[0].workCategory").value("순회"))
                .andExpect(jsonPath("$.data.todaySchedules[0].workType").value("진열"))
                .andExpect(jsonPath("$.data.todaySchedules[0].isCommuteRegistered").value(true))
                .andExpect(jsonPath("$.data.todaySchedules[0].commuteRegisteredAt").exists())
                // 두 번째 팀원 (출근 미등록, work_type null)
                .andExpect(jsonPath("$.data.todaySchedules[1].scheduleId").value(11))
                .andExpect(jsonPath("$.data.todaySchedules[1].employeeName").value("김영희"))
                .andExpect(jsonPath("$.data.todaySchedules[1].employeeCode").value("20190523"))
                .andExpect(jsonPath("$.data.todaySchedules[1].accountName").value("홈플러스 서면점"))
                .andExpect(jsonPath("$.data.todaySchedules[1].workCategory").value("전담"))
                .andExpect(jsonPath("$.data.todaySchedules[1].workType").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[1].isCommuteRegistered").value(false))
                .andExpect(jsonPath("$.data.todaySchedules[1].commuteRegisteredAt").doesNotExist())
                // 세 번째 팀원 (내근, store null)
                .andExpect(jsonPath("$.data.todaySchedules[2].scheduleId").value(12))
                .andExpect(jsonPath("$.data.todaySchedules[2].employeeName").value("박소현"))
                .andExpect(jsonPath("$.data.todaySchedules[2].employeeCode").value("20210812"))
                .andExpect(jsonPath("$.data.todaySchedules[2].accountName").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[2].accountId").doesNotExist())
                .andExpect(jsonPath("$.data.todaySchedules[2].workCategory").value("내근"))
                .andExpect(jsonPath("$.data.todaySchedules[2].isCommuteRegistered").value(false))
                // attendance_summary
                .andExpect(jsonPath("$.data.attendanceSummary.totalCount").value(5))
                .andExpect(jsonPath("$.data.attendanceSummary.registeredCount").value(2))
                // safety_check_required
                .andExpect(jsonPath("$.data.safetyCheckRequired").value(false))
                // expiry_alert null
                .andExpect(jsonPath("$.data.expiryAlert").doesNotExist())
                // notices
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices.length()").value(1))
                .andExpect(jsonPath("$.data.notices[0].id").value(3))
                .andExpect(jsonPath("$.data.notices[0].title").value("3월 근태 관리 안내"))
                // current_date
                .andExpect(jsonPath("$.data.currentDate").value("2026-02-25"))
        }

        @Test
        @DisplayName("데이터 없는 경우 - 빈 일정/공지, 안전점검 불필요")
        fun getHomeData_emptyData() {
            // Given
            val mockResponse = HomeResponse(
                todaySchedules = emptyList(),
                attendanceSummary = HomeResponse.AttendanceSummaryInfo(
                    totalCount = 0,
                    registeredCount = 0
                ),
                safetyCheckRequired = false,
                expiryAlert = null,
                notices = emptyList(),
                currentDate = "2026-02-25"
            )

            whenever(homeService.getHomeData(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules 빈 배열
                .andExpect(jsonPath("$.data.todaySchedules").isArray)
                .andExpect(jsonPath("$.data.todaySchedules").isEmpty)
                // attendance_summary 0/0
                .andExpect(jsonPath("$.data.attendanceSummary.totalCount").value(0))
                .andExpect(jsonPath("$.data.attendanceSummary.registeredCount").value(0))
                // safety_check_required false
                .andExpect(jsonPath("$.data.safetyCheckRequired").value(false))
                // expiry_alert null
                .andExpect(jsonPath("$.data.expiryAlert").doesNotExist())
                // notices 빈 배열
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices").isEmpty)
                // current_date
                .andExpect(jsonPath("$.data.currentDate").value("2026-02-25"))
        }

        @Test
        @DisplayName("사용자 없음 - 404 USER_NOT_FOUND")
        fun getHomeData_userNotFound() {
            // Given
            whenever(homeService.getHomeData(1L)).thenThrow(EmployeeNotFoundException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }
}
