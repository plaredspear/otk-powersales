package com.otoki.powersales.schedule.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.dto.response.AccountListResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.schedule.dto.request.AttendanceRegisterRequest
import com.otoki.powersales.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.schedule.dto.response.AttendanceStatusResponse
import com.otoki.powersales.schedule.service.AttendanceService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 출근 등록 API Controller
 */
@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService
) {

    /**
     * 출근 거래처 목록 조회
     * GET /api/v1/attendance/accounts
     */
    @GetMapping("/accounts")
    fun getAccountList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<AccountListResponse>> {
        val response = attendanceService.getAccountList(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 출근 등록
     * POST /api/v1/attendance
     */
    @PostMapping
    fun register(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AttendanceRegisterRequest
    ): ResponseEntity<ApiResponse<AttendanceRegisterResponse>> {
        val response = attendanceService.register(
            userId = principal.userId,
            scheduleId = request.scheduleId,
            displayWorkScheduleId = request.displayWorkScheduleId,
            latitude = request.latitude!!,
            longitude = request.longitude!!,
            workType = request.workType
        )
        return ResponseEntity.ok(ApiResponse.success(response, "출근등록 완료"))
    }

    /**
     * 출근 현황 조회
     * GET /api/v1/attendance/status
     */
    @GetMapping("/status")
    fun getStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<AttendanceStatusResponse>> {
        val response = attendanceService.getStatus(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
