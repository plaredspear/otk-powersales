package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.AdminLoginResponse
import com.otoki.powersales.auth.dto.request.LoginRequest
import com.otoki.powersales.auth.dto.request.RefreshTokenRequest
import com.otoki.powersales.auth.dto.response.TokenResponse
import com.otoki.powersales.auth.service.AuthService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/auth")
class AdminAuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AdminLoginResponse>> {
        val response = authService.adminLogin(request)
        return ResponseEntity.ok(ApiResponse.success(response, "관리자 로그인 성공"))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<ApiResponse<TokenResponse>> {
        val response = authService.refreshAccessToken(request)
        return ResponseEntity.ok(ApiResponse.success(response, "토큰 갱신 성공"))
    }
}
