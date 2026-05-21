package com.otoki.powersales.schedule.controller

import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.schedule.service.MyScheduleService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MyScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MyScheduleController 테스트")
class MyScheduleControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var myScheduleService: MyScheduleService

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
            ),
            annualLeaveCount = 0,
            substituteHolidayCount = 0
        )
        every { myScheduleService.getMonthlySchedule(eq(1L), eq(2020), eq(8)) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/monthly")
                .param("year", "2020")
                .param("month", "8")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("월간 일정 조회 성공"))
            .andExpect(jsonPath("$.data.year").value(2020))
            .andExpect(jsonPath("$.data.month").value(8))
            .andExpect(jsonPath("$.data.workDays").isArray)
            .andExpect(jsonPath("$.data.workDays[0].date").value("2020-08-01"))
            .andExpect(jsonPath("$.data.workDays[0].hasWork").value(true))
    }

    @Test
    @DisplayName("월간 일정 조회 - year 파라미터 누락 시 400 에러")
    fun getMonthlySchedule_missingYear() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/monthly")
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
            get("/api/v1/mobile/mypage/schedule/monthly")
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
            employeeCode = "20030117",
            reportProgress = ReportProgressDto(
                completed = 0,
                total = 3,
                workType = "진열"
            ),
            accounts = listOf(
                DisplayWorkScheduleItemDto(
                    accountId = 1L,
                    accountName = "(주)이마트트레이더스명지점",
                    workType1 = "진열",
                    workType2 = "전담",
                    workType3 = "순회",
                    isRegistered = false
                )
            )
        )
        every { myScheduleService.getDailySchedule(eq(1L), any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/daily")
                .param("date", "2020-08-04")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("일간 일정 조회 성공"))
            .andExpect(jsonPath("$.data.date").value("2020-08-04"))
            .andExpect(jsonPath("$.data.dayOfWeek").value("화"))
            .andExpect(jsonPath("$.data.memberName").value("최금주"))
            .andExpect(jsonPath("$.data.employeeCode").value("20030117"))
            .andExpect(jsonPath("$.data.reportProgress.completed").value(0))
            .andExpect(jsonPath("$.data.reportProgress.total").value(3))
            .andExpect(jsonPath("$.data.accounts").isArray)
    }

    @Test
    @DisplayName("일간 일정 상세 조회 - date 파라미터 누락 시 400 에러")
    fun getDailySchedule_missingDate() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/daily")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("일간 일정 상세 조회 - date 형식 오류 시 400 에러")
    fun getDailySchedule_invalidDateFormat() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/daily")
                .param("date", "20200804")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }
}
