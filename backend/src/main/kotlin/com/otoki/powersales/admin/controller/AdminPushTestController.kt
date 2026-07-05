package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminPushTestRequest
import com.otoki.powersales.admin.dto.response.AdminPushTestResponse
import com.otoki.powersales.admin.service.AdminPushTestService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * FCM push 발송 테스트 컨트롤러 (개발자 도구 > 외부 API 테스트 > push 발송 테스트 탭).
 *
 * 입력 사번에 등록된 FCM 토큰으로 임의 제목/본문의 테스트 알림을 1건 발송한다. 개발자 도구성
 * 엔드포인트로 SYSTEM(MODIFY_ALL_DATA = SYSTEM_ADMIN) 권한을 요구한다.
 */
@RestController
class AdminPushTestController(
    private val adminPushTestService: AdminPushTestService,
) {

    @PostMapping("/api/v1/admin/push/test")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun test(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminPushTestRequest,
    ): ResponseEntity<ApiResponse<AdminPushTestResponse>> {
        val response = adminPushTestService.test(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
