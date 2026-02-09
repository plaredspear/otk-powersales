package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.dto.response.SafetyCheckItemsResponse
import com.otoki.internal.dto.response.SafetyCheckSubmitResponse
import com.otoki.internal.dto.response.SafetyCheckTodayResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.SafetyCheckService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 안전점검 API Controller
 */
@RestController
@RequestMapping("/api/v1/safety-check")
class SafetyCheckController(
    private val safetyCheckService: SafetyCheckService
) {

    /**
     * 안전점검 항목 조회
     * GET /api/v1/safety-check/items
     */
    @GetMapping("/items")
    fun getChecklistItems(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<SafetyCheckItemsResponse>> {
        val response = safetyCheckService.getChecklistItems()
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }

    /**
     * 안전점검 제출
     * POST /api/v1/safety-check/submit
     */
    @PostMapping("/submit")
    fun submitSafetyCheck(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: SafetyCheckSubmitRequest
    ): ResponseEntity<ApiResponse<SafetyCheckSubmitResponse>> {
        val response = safetyCheckService.submitSafetyCheck(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "안전점검이 완료되었습니다."))
    }

    /**
     * 오늘 안전점검 여부 조회
     * GET /api/v1/safety-check/today
     */
    @GetMapping("/today")
    fun getTodayStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<SafetyCheckTodayResponse>> {
        val response = safetyCheckService.getTodayStatus(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "조회 성공"))
    }
}
