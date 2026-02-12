package com.otoki.internal.controller

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.MyScheduleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

/**
 * MyScheduleController 테스트
 */
@WebMvcTest(MyScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MyScheduleController 테스트")
class MyScheduleControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var myScheduleService: MyScheduleService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        // 인증된 사용자 SecurityContext 설정
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== 월간 일정 조회 Tests ==========

    @Test
    @DisplayName("월간 일정 조회 성공 - 200 OK")
    fun getMonthlySchedule_success() {
        // Given
        val mockResponse = MonthlyScheduleResponse(
            year = 2020,
            month = 8,
            workDays = listOf(
                WorkDayDto(date = "2020-08-01", hasWork = true),
                WorkDayDto(date = "2020-08-04", hasWork = true),
                WorkDayDto(date = "2020-08-05", hasWork = false)
            )
        )
        whenever(myScheduleService.getMonthlySchedule(eq(1L), eq(2020), eq(8)))
            .thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/monthly")
                .param("year", "2020")
                .param("month", "8")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("월간 일정 조회 성공"))
            .andExpect(jsonPath("$.data.year").value(2020))
            .andExpect(jsonPath("$.data.month").value(8))
            .andExpect(jsonPath("$.data.work_days").isArray)
            .andExpect(jsonPath("$.data.work_days[0].date").value("2020-08-01"))
            .andExpect(jsonPath("$.data.work_days[0].has_work").value(true))
    }

    @Test
    @DisplayName("월간 일정 조회 - year 파라미터 누락 시 400 에러")
    fun getMonthlySchedule_missingYear() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/monthly")
                .param("month", "8")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("월간 일정 조회 - month 범위 초과 시 400 에러")
    fun getMonthlySchedule_invalidMonth() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/monthly")
                .param("year", "2020")
                .param("month", "13")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    // ========== 일간 일정 상세 조회 Tests ==========

    @Test
    @DisplayName("일간 일정 상세 조회 성공 - 200 OK")
    fun getDailySchedule_success() {
        // Given
        val mockResponse = DailyScheduleResponse(
            date = "2020-08-04",
            dayOfWeek = "화",
            memberName = "최금주",
            employeeNumber = "20030117",
            reportProgress = ReportProgressDto(
                completed = 0,
                total = 3,
                workType = "진열"
            ),
            stores = listOf(
                StoreScheduleItemDto(
                    storeId = 1L,
                    storeName = "(주)이마트트레이더스명지점",
                    workType1 = "진열",
                    workType2 = "전담",
                    workType3 = "순회",
                    isRegistered = false
                )
            )
        )
        whenever(myScheduleService.getDailySchedule(eq(1L), any()))
            .thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/daily")
                .param("date", "2020-08-04")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("일간 일정 조회 성공"))
            .andExpect(jsonPath("$.data.date").value("2020-08-04"))
            .andExpect(jsonPath("$.data.day_of_week").value("화"))
            .andExpect(jsonPath("$.data.member_name").value("최금주"))
            .andExpect(jsonPath("$.data.employee_number").value("20030117"))
            .andExpect(jsonPath("$.data.report_progress.completed").value(0))
            .andExpect(jsonPath("$.data.report_progress.total").value(3))
            .andExpect(jsonPath("$.data.stores").isArray)
    }

    @Test
    @DisplayName("일간 일정 상세 조회 - date 파라미터 누락 시 400 에러")
    fun getDailySchedule_missingDate() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/daily")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("일간 일정 상세 조회 - date 형식 오류 시 400 에러")
    fun getDailySchedule_invalidDateFormat() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mypage/schedule/daily")
                .param("date", "20200804")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
}
