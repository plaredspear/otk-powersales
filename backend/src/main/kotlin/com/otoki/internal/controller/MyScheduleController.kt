package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.DailyScheduleResponse
import com.otoki.internal.dto.response.MonthlyScheduleResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.MyScheduleService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 마이페이지 일정 API Controller
 */
@RestController
@RequestMapping("/api/v1/mypage/schedule")
class MyScheduleController(
    private val myScheduleService: MyScheduleService
) {

    /**
     * 월간 일정 조회
     * GET /api/v1/mypage/schedule/monthly
     *
     * @param year 조회 연도 (필수)
     * @param month 조회 월 (1~12, 필수)
     * @return 월간 근무일 목록
     */
    @GetMapping("/monthly")
    fun getMonthlySchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<ApiResponse<MonthlyScheduleResponse>> {
        // 파라미터 검증
        require(year > 0) { "연도와 월을 확인해주세요" }
        require(month in 1..12) { "연도와 월을 확인해주세요" }

        val response = myScheduleService.getMonthlySchedule(principal.userId, year, month)
        return ResponseEntity.ok(ApiResponse.success(response, "월간 일정 조회 성공"))
    }

    /**
     * 일간 일정 상세 조회
     * GET /api/v1/mypage/schedule/daily
     *
     * @param date 조회 날짜 (YYYY-MM-DD 형식, 필수)
     * @return 일간 거래처별 일정 + 등록 현황
     */
    @GetMapping("/daily")
    fun getDailySchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam date: String
    ): ResponseEntity<ApiResponse<DailyScheduleResponse>> {
        // 날짜 파싱 (형식 오류 시 IllegalArgumentException 발생)
        val localDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("날짜 형식을 확인해주세요")
        }

        val response = myScheduleService.getDailySchedule(principal.userId, localDate)
        return ResponseEntity.ok(ApiResponse.success(response, "일간 일정 조회 성공"))
    }
}
