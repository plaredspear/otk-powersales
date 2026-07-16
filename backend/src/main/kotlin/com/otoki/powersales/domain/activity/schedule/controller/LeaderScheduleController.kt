package com.otoki.powersales.domain.activity.schedule.controller

import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderEventScheduleChangeRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderEventScheduleChangeResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderMonthlyCalendarResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.domain.activity.schedule.service.LeaderScheduleService
import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * 조장 대리 일정 등록 API Controller (Spec #554 P1-B).
 */
@RestController
@RequestMapping("/api/v1/mobile/leader")
class LeaderScheduleController(
    private val leaderScheduleService: LeaderScheduleService
) {

    /**
     * 조장 대리 일정 등록
     * POST /api/v1/mobile/leader/team-member-schedule
     */
    @PostMapping("/team-member-schedule")
    fun createTeamMemberSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: LeaderScheduleCreateRequest
    ): ResponseEntity<ApiResponse<LeaderScheduleCreateResponse>> {
        val response = leaderScheduleService.createTeamMemberSchedule(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "팀원 일정이 등록되었습니다"))
    }

    /**
     * 조장 행사 일정 변경 — 담당 여사원/투입일 재배정 (레거시 `scheduleChangePromo` M)
     * PUT /api/v1/mobile/leader/event-schedule/{scheduleId}
     *
     * scheduleId = 일별 현황 eventWorkers[].scheduleId (team_member_schedule ID).
     */
    @PutMapping("/event-schedule/{scheduleId}")
    fun changeEventAssignment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: LeaderEventScheduleChangeRequest
    ): ResponseEntity<ApiResponse<LeaderEventScheduleChangeResponse>> {
        val response = leaderScheduleService.changeEventAssignment(principal.userId, scheduleId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "행사 일정이 변경되었습니다"))
    }

    /**
     * 조장 행사 일정 삭제 — 행사 배정 해제 (레거시 `scheduleChangePromo` D)
     * DELETE /api/v1/mobile/leader/event-schedule/{scheduleId}
     */
    @DeleteMapping("/event-schedule/{scheduleId}")
    fun deleteEventAssignment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable scheduleId: Long
    ): ResponseEntity<ApiResponse<Unit>> {
        leaderScheduleService.deleteEventAssignment(principal.userId, scheduleId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "행사 일정이 삭제되었습니다"))
    }

    /**
     * 본인 팀원 목록 조회
     * GET /api/v1/mobile/leader/team-members
     */
    @GetMapping("/team-members")
    fun getTeamMembers(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<LeaderTeamMemberListResponse>>> {
        val response = leaderScheduleService.getTeamMembers(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "팀원 목록 조회 성공"))
    }

    /**
     * 조장 — 본인 팀원(여사원) 단말 초기화 (레거시 SF UUIDReset Quick Action)
     * POST /api/v1/mobile/leader/team-members/{employeeId}/reset-device
     *
     * 조장 권한 + 본인 지점 소속 검증 후 deviceUuid 회수 + 기존 기기 즉시 로그아웃.
     */
    @PostMapping("/team-members/{employeeId}/reset-device")
    fun resetTeamMemberDevice(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetDeviceResponse>> {
        val response = leaderScheduleService.resetTeamMemberDevice(principal.userId, employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "단말이 초기화되었습니다"))
    }

    /**
     * 조장 — 본인 팀원(여사원) 비밀번호 초기화 (레거시 SF PasswordReset Quick Action)
     * POST /api/v1/mobile/leader/team-members/{employeeId}/reset-password
     *
     * 임시 비밀번호 발급 + 다음 로그인 강제 변경. 조장 권한 + 본인 지점 소속 검증.
     */
    @PostMapping("/team-members/{employeeId}/reset-password")
    fun resetTeamMemberPassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetPasswordResponse>> {
        val response = leaderScheduleService.resetTeamMemberPassword(principal.userId, employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }

    /**
     * 조장 — 여사원 월간 일정 캘린더 (레거시 `employee/mgnSchedule.jsp` + `calSchedule`)
     * GET /api/v1/mobile/leader/schedule/monthly?year=&month=&employeeId=(선택)
     *
     * employeeId 미지정 시 "여사원 전체" 모드, 지정 시 해당 조원 단독 집계.
     */
    @GetMapping("/schedule/monthly")
    fun getMonthlyCalendar(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) employeeId: Long?
    ): ResponseEntity<ApiResponse<LeaderMonthlyCalendarResponse>> {
        require(year > 0) { "연도와 월을 확인해주세요" }
        require(month in 1..12) { "연도와 월을 확인해주세요" }
        val response = leaderScheduleService.getMonthlyCalendar(
            principal.userId, employeeId, year, month
        )
        return ResponseEntity.ok(ApiResponse.success(response, "월간 일정 캘린더 조회 성공"))
    }

    /**
     * 본인 거래처 목록 조회
     * GET /api/v1/mobile/leader/accounts
     */
    @GetMapping("/accounts")
    fun getAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<List<LeaderAccountListResponse>>> {
        val response = leaderScheduleService.getAccounts(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "거래처 목록 조회 성공"))
    }

    /**
     * 여사원 일별 현황 조회 (레거시 `employee/mngDaily.jsp` — 조회 전용)
     * GET /api/v1/mobile/leader/daily-status?date=YYYY-MM-DD
     *
     * 본인 팀 여사원의 해당 날짜 진열/행사/연차 근무 현황 + 거래처별 출근 등록 현황.
     */
    @GetMapping("/daily-status")
    fun getDailyStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam date: String
    ): ResponseEntity<ApiResponse<LeaderDailyStatusResponse>> {
        val localDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("날짜 형식을 확인해주세요")
        }
        val response = leaderScheduleService.getDailyStatus(principal.userId, localDate)
        return ResponseEntity.ok(ApiResponse.success(response, "여사원 일별 현황 조회 성공"))
    }
}
