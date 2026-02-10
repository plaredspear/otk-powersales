package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.AttendanceRequest
import com.otoki.internal.dto.response.AttendanceResponse
import com.otoki.internal.dto.response.AttendanceStatusResponse
import com.otoki.internal.dto.response.StoreListResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.AttendanceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 출근등록 API Controller
 */
@RestController
@RequestMapping("/api/v1/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService
) {

    /**
     * 오늘 출근 거래처 목록 조회
     * GET /api/v1/attendance/stores
     */
    @GetMapping("/stores")
    fun getStoreList(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<StoreListResponse>> {
        val response = attendanceService.getStoreList(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 출근등록
     * POST /api/v1/attendance
     */
    @PostMapping
    fun registerAttendance(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AttendanceRequest
    ): ResponseEntity<ApiResponse<AttendanceResponse>> {
        val response = attendanceService.registerAttendance(
            userId = principal.userId,
            storeId = request.storeId!!,
            workTypeStr = request.workType!!
        )
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "출근등록 완료"))
    }

    /**
     * 출근등록 현황 조회
     * GET /api/v1/attendance/status
     */
    @GetMapping("/status")
    fun getAttendanceStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<AttendanceStatusResponse>> {
        val response = attendanceService.getAttendanceStatus(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
