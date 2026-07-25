package com.otoki.powersales.domain.activity.schedule.controller

import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.activity.schedule.service.MyScheduleService
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.activity.schedule.controller.MyScheduleController
import com.otoki.powersales.domain.activity.schedule.dto.response.DailyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.DisplayWorkScheduleItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.ReportProgressDto
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkDayDto
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
    @DisplayName("일간 일정 상세 조회 - 거래처 항목 boolean 필드가 is 접두사 그대로 직렬화")
    fun getDailySchedule_serializesBooleanFieldsWithIsPrefix() {
        // Given — Kotlin `is` prefix 프로퍼티는 Java bean getter 규칙상 접두사가 떨어져
        // `registered` / `legacyVisible` 로 나갈 수 있다. 모바일은 `isRegistered` /
        // `isLegacyVisible` 키로 파싱하므로 계약이 어긋나면 참고 일정 구분이 통째로 무력화된다
        // (isLegacyVisible 누락 시 모바일 fallback 이 전건 주 일정으로 처리).
        val mockResponse = DailyScheduleResponse(
            date = "2026-08-01",
            dayOfWeek = "토",
            memberName = "홍유미",
            employeeCode = "20210283",
            reportProgress = ReportProgressDto(completed = 0, total = 1, workType = "행사"),
            accounts = listOf(
                DisplayWorkScheduleItemDto(
                    accountId = 1L,
                    accountName = "(주)오동",
                    workType1 = "행사",
                    workType2 = "",
                    workType3 = "순회",
                    isRegistered = false,
                    isLegacyVisible = true
                ),
                DisplayWorkScheduleItemDto(
                    accountId = 2L,
                    accountName = "상시진열거래처",
                    workType1 = "진열",
                    workType2 = "상시",
                    workType3 = "고정",
                    isRegistered = false,
                    isLegacyVisible = false
                )
            )
        )
        every { myScheduleService.getDailySchedule(eq(1L), any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/mypage/schedule/daily")
                .param("date", "2026-08-01")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accounts[0].isRegistered").value(false))
            .andExpect(jsonPath("$.data.accounts[0].isLegacyVisible").value(true))
            .andExpect(jsonPath("$.data.accounts[1].isLegacyVisible").value(false))
            // 접두사가 떨어진 키가 함께 나가면 계약 혼선 → 존재하지 않아야 한다
            .andExpect(jsonPath("$.data.accounts[0].registered").doesNotExist())
            .andExpect(jsonPath("$.data.accounts[0].legacyVisible").doesNotExist())
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
