package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.AdminLoginResponse
import com.otoki.internal.auth.dto.request.LoginRequest
import com.otoki.internal.auth.dto.request.RefreshTokenRequest
import com.otoki.internal.auth.dto.response.TokenResponse
import com.otoki.internal.auth.service.AuthService
import com.otoki.internal.common.dto.ApiResponse
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
