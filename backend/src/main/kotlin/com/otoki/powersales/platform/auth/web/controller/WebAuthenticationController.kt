package com.otoki.powersales.platform.auth.web.controller

import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.auth.web.dto.WebChangePasswordRequest
import com.otoki.powersales.platform.auth.web.dto.WebChangePasswordResponse
import com.otoki.powersales.platform.auth.web.dto.WebLoginRequest
import com.otoki.powersales.platform.auth.web.dto.WebLoginResponse
import com.otoki.powersales.platform.auth.web.dto.WebRefreshTokenRequest
import com.otoki.powersales.platform.auth.web.dto.WebTokenResponse
import com.otoki.powersales.platform.auth.web.service.WebAuthenticationService
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Web 인증 API (Spec #760).
 *
 * Spec §5.1~5.3 — 로그인 / 토큰 갱신 / 비밀번호 변경.
 * URL prefix `/api/v1/admin/auth/` (Q1: 기존 backend admin API 컨벤션 정합).
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
class WebAuthenticationController(
    private val webAuthenticationService: WebAuthenticationService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: WebLoginRequest): ResponseEntity<ApiResponse<WebLoginResponse>> {
        val response = webAuthenticationService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: WebRefreshTokenRequest): ResponseEntity<ApiResponse<WebTokenResponse>> {
        val response = webAuthenticationService.refresh(request)
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신 성공"))
    }

    @PostMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: WebChangePasswordRequest
    ): ResponseEntity<ApiResponse<WebChangePasswordResponse>> {
        val response = webAuthenticationService.changePassword(principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 변경되었습니다"))
    }
}
