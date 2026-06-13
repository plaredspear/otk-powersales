package com.otoki.powersales.platform.auth.web.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStartRequest
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStartResponse
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStopResponse
import com.otoki.powersales.platform.auth.web.service.WebImpersonationService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Web 관리자 대행 로그인 (Impersonation) API (Spec #851).
 *
 * - `start`: `SYSTEM:MANAGE_USERS` 보유 관리자만. 권한 미보유는 `@RequiresSfPermission` 표준 가드가
 *   PERMISSION_DENIED(403) 로 차단.
 * - `stop`: 대행 중인 토큰 자체가 자격이므로 권한 가드 미부착 (대상 사용자 권한 검사 회피).
 *
 * 이력 조회 API 는 본 spec 비범위 (Q5 옵션 1 — `impersonation_log` 적재만).
 */
@RestController
@RequestMapping("/api/v1/admin/impersonation")
class WebImpersonationController(
    private val webImpersonationService: WebImpersonationService
) {

    @PostMapping("/start")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun start(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: WebImpersonationStartRequest
    ): ResponseEntity<ApiResponse<WebImpersonationStartResponse>> {
        val response = webImpersonationService.start(principal, request)
        return ResponseEntity.ok(ApiResponse.success(response, "대행 로그인을 시작했습니다"))
    }

    @PostMapping("/stop")
    fun stop(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<WebImpersonationStopResponse>> {
        val response = webImpersonationService.stop(principal)
        return ResponseEntity.ok(ApiResponse.success(response, "대행 로그인을 종료했습니다"))
    }
}
