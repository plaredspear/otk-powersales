package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.request.ChangePasswordRequest
import com.otoki.internal.dto.request.LoginRequest
import com.otoki.internal.dto.request.RefreshTokenRequest
import com.otoki.internal.dto.request.VerifyPasswordRequest
import com.otoki.internal.dto.response.LoginResponse
import com.otoki.internal.dto.response.TokenResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * 로그인
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
    }

    /**
     * 토큰 갱신
     * POST /api/v1/auth/refresh
     */
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        val response = authService.refreshAccessToken(request)
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신 성공"))
    }

    /**
     * 비밀번호 검증
     * POST /api/v1/auth/verify-password
     */
    @PostMapping("/verify-password")
    fun verifyPassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: VerifyPasswordRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        authService.verifyPassword(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 확인되었습니다"))
    }

    /**
     * 비밀번호 변경
     * POST /api/v1/auth/change-password
     */
    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        authService.changePassword(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(null, "비밀번호가 변경되었습니다"))
    }

    /**
     * 로그아웃
     * POST /api/v1/auth/logout
     */
    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        val token = resolveToken(request)
        if (token != null) {
            authService.logout(token)
        }
        return ResponseEntity.noContent().build()
    }

    /**
     * GPS 동의 기록
     * POST /api/v1/auth/gps-consent
     */
    @PostMapping("/gps-consent")
    fun recordGpsConsent(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Any?>> {
        authService.recordGpsConsent(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(null, "GPS 사용 동의가 기록되었습니다"))
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
