package com.otoki.internal.controller

import com.otoki.internal.dto.response.HomeResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.HomeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
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
class HomeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var homeService: HomeService

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== 정상 조회 Tests ==========

    @Test
    @DisplayName("홈 데이터 정상 조회 - 200 OK, 모든 데이터 포함")
    fun getHomeData_success_allData() {
        // Given
        val mockResponse = HomeResponse(
            todaySchedules = listOf(
                HomeResponse.ScheduleInfo(
                    id = 1L,
                    storeName = "이마트 부산점",
                    startTime = "09:00",
                    endTime = "12:00",
                    type = "순회"
                )
            ),
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
            currentDate = "2026-02-07"
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
            .andExpect(jsonPath("$.data.today_schedules").isArray)
            .andExpect(jsonPath("$.data.today_schedules[0].id").value(1))
            .andExpect(jsonPath("$.data.today_schedules[0].store_name").value("이마트 부산점"))
            .andExpect(jsonPath("$.data.today_schedules[0].start_time").value("09:00"))
            .andExpect(jsonPath("$.data.today_schedules[0].end_time").value("12:00"))
            .andExpect(jsonPath("$.data.today_schedules[0].type").value("순회"))
            .andExpect(jsonPath("$.data.expiry_alert.branch_name").value("부산1지점"))
            .andExpect(jsonPath("$.data.expiry_alert.employee_name").value("최금주"))
            .andExpect(jsonPath("$.data.expiry_alert.employee_id").value("20030117"))
            .andExpect(jsonPath("$.data.expiry_alert.expiry_count").value(1))
            .andExpect(jsonPath("$.data.notices").isArray)
            .andExpect(jsonPath("$.data.notices[0].id").value(1))
            .andExpect(jsonPath("$.data.notices[0].title").value("2월 영업 목표 달성 현황"))
            .andExpect(jsonPath("$.data.notices[0].type").value("BRANCH"))
            .andExpect(jsonPath("$.data.notices[1].id").value(2))
            .andExpect(jsonPath("$.data.notices[1].title").value("신제품 출시 안내"))
            .andExpect(jsonPath("$.data.notices[1].type").value("ALL"))
            .andExpect(jsonPath("$.data.current_date").value("2026-02-07"))
    }

    @Test
    @DisplayName("홈 데이터 정상 조회 - 데이터 없는 경우 빈 배열과 null 반환")
    fun getHomeData_success_emptyData() {
        // Given
        val mockResponse = HomeResponse(
            todaySchedules = emptyList(),
            expiryAlert = null,
            notices = emptyList(),
            currentDate = "2026-02-07"
        )

        whenever(homeService.getHomeData(1L)).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/home")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.today_schedules").isArray)
            .andExpect(jsonPath("$.data.today_schedules").isEmpty)
            .andExpect(jsonPath("$.data.expiry_alert").doesNotExist())
            .andExpect(jsonPath("$.data.notices").isArray)
            .andExpect(jsonPath("$.data.notices").isEmpty)
            .andExpect(jsonPath("$.data.current_date").value("2026-02-07"))
    }

    // ========== 에러 Tests ==========

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
