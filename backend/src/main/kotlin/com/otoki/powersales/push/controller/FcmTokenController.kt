package com.otoki.powersales.push.controller

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import com.otoki.powersales.push.dto.request.FcmTokenRegisterRequest
import com.otoki.powersales.push.service.FcmTokenService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * FCM 디바이스 토큰 API Controller (mobile).
 *
 * 토큰 등록 시점은 모바일 앱의 인증 흐름에 연동된다:
 * - 로그인/자동로그인 성공 → POST (등록·갱신)
 * - FCM onTokenRefresh → POST (재등록)
 * - 로그아웃 → DELETE (해제)
 */
@RestController
@RequestMapping("/api/v1/mobile/fcm-token")
class FcmTokenController(
    private val fcmTokenService: FcmTokenService
) {

    /**
     * FCM 토큰 등록/갱신
     * POST /api/v1/mobile/fcm-token
     */
    @PostMapping
    fun register(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: FcmTokenRegisterRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        fcmTokenService.register(principal.userId, request.token!!)
        return ResponseEntity.ok(ApiResponse.success(Unit, "FCM 토큰이 등록되었습니다"))
    }

    /**
     * FCM 토큰 해제 (로그아웃)
     * DELETE /api/v1/mobile/fcm-token
     */
    @DeleteMapping
    fun unregister(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<Unit>> {
        fcmTokenService.unregister(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(Unit, "FCM 토큰이 해제되었습니다"))
    }
}
