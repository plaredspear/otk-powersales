package com.otoki.powersales.domain.activity.schedule.controller

import com.otoki.powersales.domain.activity.schedule.dto.request.ProxyAttendanceRegisterRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.domain.activity.schedule.service.ProxyAttendanceService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * AccountViewAll 대리출근 API Controller (지점 선택형).
 *
 * 조장 대리출근(POST /api/v1/mobile/leader/attendance)을 대체 — AccountViewAll 사용자가
 * 지점을 선택해 그 지점 여사원의 대리출근을 등록한다. 권한/지점 IDOR 검증은 서비스 책임.
 */
@RestController
@RequestMapping("/api/v1/mobile/proxy-attendance")
class ProxyAttendanceController(
    private val proxyAttendanceService: ProxyAttendanceService
) {

    /**
     * 대리출근 지점 선택 옵션 조회
     * GET /api/v1/mobile/proxy-attendance/branches
     */
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val response = proxyAttendanceService.getBranches(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "지점 목록 조회 성공"))
    }

    /**
     * 선택 지점 여사원 목록 조회
     * GET /api/v1/mobile/proxy-attendance/team-members?branchCode=
     */
    @GetMapping("/team-members")
    fun getTeamMembers(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam branchCode: String
    ): ResponseEntity<ApiResponse<List<LeaderTeamMemberListResponse>>> {
        val response = proxyAttendanceService.getTeamMembers(principal.userId, branchCode)
        return ResponseEntity.ok(ApiResponse.success(response, "여사원 목록 조회 성공"))
    }

    /**
     * 선택 지점 여사원 일별 현황 조회
     * GET /api/v1/mobile/proxy-attendance/daily-status?branchCode=&date=YYYY-MM-DD
     */
    @GetMapping("/daily-status")
    fun getDailyStatus(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam branchCode: String,
        @RequestParam date: String
    ): ResponseEntity<ApiResponse<LeaderDailyStatusResponse>> {
        val localDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            throw IllegalArgumentException("날짜 형식을 확인해주세요")
        }
        val response = proxyAttendanceService.getDailyStatus(principal.userId, branchCode, localDate)
        return ResponseEntity.ok(ApiResponse.success(response, "여사원 일별 현황 조회 성공"))
    }

    /**
     * 대리출근 등록 (레거시 mngDaily `addScheduleProc`). GPS 미적용.
     * POST /api/v1/mobile/proxy-attendance
     *
     * 진열=display_work_schedule_id, 행사·기배정=schedule_id 중 하나 전달.
     */
    @PostMapping
    fun registerProxyAttendance(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ProxyAttendanceRegisterRequest
    ): ResponseEntity<ApiResponse<AttendanceRegisterResponse>> {
        val response = proxyAttendanceService.registerProxyAttendance(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "대리출근 등록 완료"))
    }
}
