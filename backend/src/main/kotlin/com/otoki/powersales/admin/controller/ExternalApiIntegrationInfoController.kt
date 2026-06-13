package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.ExternalApiIntegrationInfoResponse
import com.otoki.powersales.admin.service.ExternalApiIntegrationInfoService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 외부 API 연동 정보 조회 API (개발자 도구 — 외부 API 테스트).
 *
 * 각 외부 API 테스트 탭이 외부 시스템 endpoint / HTTP method / 인증 방식을 노출하도록 현재 환경의
 * 실제 설정값을 조회해 반환한다. naver-geocode 테스트와 동일하게 SYSTEM(MODIFY_ALL_DATA) 권한 필요.
 */
@RestController
class ExternalApiIntegrationInfoController(
    private val externalApiIntegrationInfoService: ExternalApiIntegrationInfoService,
) {

    @GetMapping("/api/v1/admin/external-api/integration-info")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun getIntegrationInfo(): ResponseEntity<ApiResponse<ExternalApiIntegrationInfoResponse>> {
        return ResponseEntity.ok(ApiResponse.success(externalApiIntegrationInfoService.getIntegrationInfo()))
    }
}
