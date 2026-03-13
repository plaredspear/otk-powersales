package com.otoki.internal.teammemberschedule.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.dto.response.StoreListResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.teammemberschedule.dto.request.CommuteRequest
import com.otoki.internal.teammemberschedule.dto.response.CommuteResponse
import com.otoki.internal.teammemberschedule.dto.response.CommuteStatusResponse
import com.otoki.internal.teammemberschedule.service.AttendanceService
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
     * 출근 등록
     * POST /api/v1/attendance
     */
    @PostMapping
    fun registerCommute(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CommuteRequest
    ): ResponseEntity<ApiResponse<CommuteResponse>> {
        val response = attendanceService.registerCommute(
            userId = principal.userId,
            teamMemberScheduleSfid = request.teamMemberScheduleSfid,
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
    fun getCommuteStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<CommuteStatusResponse>> {
        val response = attendanceService.getCommuteStatus(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
