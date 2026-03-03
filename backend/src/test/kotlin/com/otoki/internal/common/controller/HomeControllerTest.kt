package com.otoki.internal.common.controller

import com.otoki.internal.common.dto.response.HomeResponse
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.common.service.HomeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/home - 홈 데이터 조회")
    inner class GetHomeData {

        @Test
        @DisplayName("여사원 홈 조회 성공 - role=USER, 안전점검 필요, 출근현황 포함")
        fun getHomeData_user_success() {
            // Given
            val commuteTime = LocalDateTime.of(2026, 2, 25, 8, 30, 0)
            val mockResponse = HomeResponse(
                todaySchedules = listOf(
                    HomeResponse.ScheduleInfo(
                        scheduleId = "SCH-20260225-001",
                        employeeName = "최금주",
                        employeeSfid = "20030117",
                        storeName = "이마트 부산점",
                        storeSfid = "ST-001",
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
                    employeeId = "20030117",
                    expiryCount = 1
                ),
                notices = listOf(
                    HomeResponse.NoticeInfo(
                        id = 1L,
                        title = "2월 영업 목표 달성 현황",
                        type = "BRANCH",
                        createdAt = LocalDateTime.of(2026, 2, 5, 10, 0, 0)
                    ),
                    HomeResponse.NoticeInfo(
                        id = 2L,
                        title = "신제품 출시 안내",
                        type = "ALL",
                        createdAt = LocalDateTime.of(2026, 2, 4, 9, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(homeService.getHomeData(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules
                .andExpect(jsonPath("$.data.today_schedules").isArray)
                .andExpect(jsonPath("$.data.today_schedules.length()").value(1))
                .andExpect(jsonPath("$.data.today_schedules[0].schedule_id").value("SCH-20260225-001"))
                .andExpect(jsonPath("$.data.today_schedules[0].employee_name").value("최금주"))
                .andExpect(jsonPath("$.data.today_schedules[0].employee_sfid").value("20030117"))
                .andExpect(jsonPath("$.data.today_schedules[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.today_schedules[0].store_sfid").value("ST-001"))
                .andExpect(jsonPath("$.data.today_schedules[0].work_category").value("순회"))
                .andExpect(jsonPath("$.data.today_schedules[0].work_type").value("진열"))
                .andExpect(jsonPath("$.data.today_schedules[0].is_commute_registered").value(true))
                .andExpect(jsonPath("$.data.today_schedules[0].commute_registered_at").exists())
                // attendance_summary
                .andExpect(jsonPath("$.data.attendance_summary.total_count").value(3))
                .andExpect(jsonPath("$.data.attendance_summary.registered_count").value(1))
                // safety_check_required
                .andExpect(jsonPath("$.data.safety_check_required").value(true))
                // expiry_alert
                .andExpect(jsonPath("$.data.expiry_alert.branch_name").value("부산1지점"))
                .andExpect(jsonPath("$.data.expiry_alert.employee_name").value("최금주"))
                .andExpect(jsonPath("$.data.expiry_alert.employee_id").value("20030117"))
                .andExpect(jsonPath("$.data.expiry_alert.expiry_count").value(1))
                // notices
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices.length()").value(2))
                .andExpect(jsonPath("$.data.notices[0].id").value(1))
                .andExpect(jsonPath("$.data.notices[0].title").value("2월 영업 목표 달성 현황"))
                .andExpect(jsonPath("$.data.notices[0].type").value("BRANCH"))
                .andExpect(jsonPath("$.data.notices[1].id").value(2))
                .andExpect(jsonPath("$.data.notices[1].title").value("신제품 출시 안내"))
                .andExpect(jsonPath("$.data.notices[1].type").value("ALL"))
                // current_date
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
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
                    HomeResponse.ScheduleInfo(
                        scheduleId = "SCH-20260225-010",
                        employeeName = "최금주",
                        employeeSfid = "20030117",
                        storeName = "이마트 부산점",
                        storeSfid = "ST-001",
                        workCategory = "순회",
                        workType = "진열",
                        isCommuteRegistered = true,
                        commuteRegisteredAt = commuteTime1
                    ),
                    HomeResponse.ScheduleInfo(
                        scheduleId = "SCH-20260225-011",
                        employeeName = "김영희",
                        employeeSfid = "20190523",
                        storeName = "홈플러스 서면점",
                        storeSfid = "ST-002",
                        workCategory = "전담",
                        workType = null,
                        isCommuteRegistered = false,
                        commuteRegisteredAt = null
                    ),
                    HomeResponse.ScheduleInfo(
                        scheduleId = "SCH-20260225-012",
                        employeeName = "박소현",
                        employeeSfid = "20210812",
                        storeName = null,
                        storeSfid = null,
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
                        type = "ALL",
                        createdAt = LocalDateTime.of(2026, 2, 24, 14, 0, 0)
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(homeService.getHomeData(2L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules - 팀원 3명 일정
                .andExpect(jsonPath("$.data.today_schedules").isArray)
                .andExpect(jsonPath("$.data.today_schedules.length()").value(3))
                // 첫 번째 팀원 (출근 등록됨)
                .andExpect(jsonPath("$.data.today_schedules[0].schedule_id").value("SCH-20260225-010"))
                .andExpect(jsonPath("$.data.today_schedules[0].employee_name").value("최금주"))
                .andExpect(jsonPath("$.data.today_schedules[0].employee_sfid").value("20030117"))
                .andExpect(jsonPath("$.data.today_schedules[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.today_schedules[0].store_sfid").value("ST-001"))
                .andExpect(jsonPath("$.data.today_schedules[0].work_category").value("순회"))
                .andExpect(jsonPath("$.data.today_schedules[0].work_type").value("진열"))
                .andExpect(jsonPath("$.data.today_schedules[0].is_commute_registered").value(true))
                .andExpect(jsonPath("$.data.today_schedules[0].commute_registered_at").exists())
                // 두 번째 팀원 (출근 미등록, work_type null)
                .andExpect(jsonPath("$.data.today_schedules[1].schedule_id").value("SCH-20260225-011"))
                .andExpect(jsonPath("$.data.today_schedules[1].employee_name").value("김영희"))
                .andExpect(jsonPath("$.data.today_schedules[1].employee_sfid").value("20190523"))
                .andExpect(jsonPath("$.data.today_schedules[1].store_name").value("홈플러스 서면점"))
                .andExpect(jsonPath("$.data.today_schedules[1].work_category").value("전담"))
                .andExpect(jsonPath("$.data.today_schedules[1].work_type").doesNotExist())
                .andExpect(jsonPath("$.data.today_schedules[1].is_commute_registered").value(false))
                .andExpect(jsonPath("$.data.today_schedules[1].commute_registered_at").doesNotExist())
                // 세 번째 팀원 (내근, store null)
                .andExpect(jsonPath("$.data.today_schedules[2].schedule_id").value("SCH-20260225-012"))
                .andExpect(jsonPath("$.data.today_schedules[2].employee_name").value("박소현"))
                .andExpect(jsonPath("$.data.today_schedules[2].employee_sfid").value("20210812"))
                .andExpect(jsonPath("$.data.today_schedules[2].store_name").doesNotExist())
                .andExpect(jsonPath("$.data.today_schedules[2].store_sfid").doesNotExist())
                .andExpect(jsonPath("$.data.today_schedules[2].work_category").value("내근"))
                .andExpect(jsonPath("$.data.today_schedules[2].is_commute_registered").value(false))
                // attendance_summary
                .andExpect(jsonPath("$.data.attendance_summary.total_count").value(5))
                .andExpect(jsonPath("$.data.attendance_summary.registered_count").value(2))
                // safety_check_required
                .andExpect(jsonPath("$.data.safety_check_required").value(false))
                // expiry_alert null
                .andExpect(jsonPath("$.data.expiry_alert").doesNotExist())
                // notices
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices.length()").value(1))
                .andExpect(jsonPath("$.data.notices[0].id").value(3))
                .andExpect(jsonPath("$.data.notices[0].title").value("3월 근태 관리 안내"))
                // current_date
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
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
                get("/api/v1/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                // today_schedules 빈 배열
                .andExpect(jsonPath("$.data.today_schedules").isArray)
                .andExpect(jsonPath("$.data.today_schedules").isEmpty)
                // attendance_summary 0/0
                .andExpect(jsonPath("$.data.attendance_summary.total_count").value(0))
                .andExpect(jsonPath("$.data.attendance_summary.registered_count").value(0))
                // safety_check_required false
                .andExpect(jsonPath("$.data.safety_check_required").value(false))
                // expiry_alert null
                .andExpect(jsonPath("$.data.expiry_alert").doesNotExist())
                // notices 빈 배열
                .andExpect(jsonPath("$.data.notices").isArray)
                .andExpect(jsonPath("$.data.notices").isEmpty)
                // current_date
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
        }

        @Test
        @DisplayName("사용자 없음 - 404 USER_NOT_FOUND")
        fun getHomeData_userNotFound() {
            // Given
            whenever(homeService.getHomeData(1L)).thenThrow(UserNotFoundException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/home")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }
}
