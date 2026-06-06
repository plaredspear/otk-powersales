package com.otoki.powersales.auth.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.dto.request.ChangePasswordRequest
import com.otoki.powersales.common.dto.request.GpsConsentRequest
import com.otoki.powersales.auth.dto.request.LoginRequest
import com.otoki.powersales.auth.dto.request.RefreshTokenRequest
import com.otoki.powersales.auth.dto.request.VerifyPasswordRequest
import com.otoki.powersales.auth.dto.response.*
import com.otoki.powersales.common.dto.response.*
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.service.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 인증 관련 API Controller
 */
@RestController
@RequestMapping("/api/v1/mobile/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * 로그인
     * POST /api/v1/mobile/auth/login
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response, "로그인 성공"))
    }

    /**
     * 토큰 갱신
     * POST /api/v1/mobile/auth/refresh
     */
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        val response = authService.refreshAccessToken(request)
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신 성공"))
    }

    /**
     * 현재 사용자 정보 조회 (자동로그인 후 사용자 복원용)
     * GET /api/v1/mobile/auth/me
     */
    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<UserInfo>> {
        val response = authService.getMe(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "사용자 정보 조회 성공"))
    }

    /**
     * 비밀번호 검증
     * POST /api/v1/mobile/auth/verify-password
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
     * 비밀번호 변경 (강제/자발 통합 — Spec #584)
     * POST /api/v1/mobile/auth/change-password
     *
     * 응답에 새 토큰 페어를 포함한다 (클레임 `passwordChangeRequired=false` 반영).
     */
    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<ChangePasswordResponse>> {
        val response = authService.changePassword(principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 변경되었습니다"))
    }

    /**
     * 로그아웃
     * POST /api/v1/mobile/auth/logout
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
     * GPS 동의 약관 조회
     * GET /api/v1/mobile/auth/gps-consent/terms
     */
    @GetMapping("/gps-consent/terms")
    fun getGpsConsentTerms(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<GpsConsentTermsResponse>> {
        val response = authService.getGpsConsentTerms()
        return ResponseEntity.ok(ApiResponse.success(response, "약관 조회 성공"))
    }

    /**
     * GPS 동의 상태 조회
     * GET /api/v1/mobile/auth/gps-consent/status
     */
    @GetMapping("/gps-consent/status")
    fun getGpsConsentStatus(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<GpsConsentStatusResponse>> {
        val response = authService.getGpsConsentStatus(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "GPS 동의 상태 조회 성공"))
    }

    /**
     * GPS 동의 기록
     * POST /api/v1/mobile/auth/gps-consent
     */
    @PostMapping("/gps-consent")
    fun recordGpsConsent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestBody(required = false) request: GpsConsentRequest?
    ): ResponseEntity<ApiResponse<GpsConsentRecordResponse>> {
        val response = authService.recordGpsConsent(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response, "GPS 사용 동의가 기록되었습니다"))
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
