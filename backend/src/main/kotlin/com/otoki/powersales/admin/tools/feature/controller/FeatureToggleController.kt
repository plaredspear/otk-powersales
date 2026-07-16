package com.otoki.powersales.admin.tools.feature.controller

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import com.otoki.powersales.admin.tools.feature.dto.FeatureToggleItem
import com.otoki.powersales.admin.tools.feature.dto.FeatureToggleListResponse
import com.otoki.powersales.admin.tools.feature.dto.UpdateFeatureToggleRequest
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleService
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 — 등록 기능(제품 클레임/물류 클레임/주문) on/off 컨트롤러.
 *
 * 등록 API 를 런타임에 차단하는 강력한 기능이라 **시스템 관리자 전용**으로 가드한다. 로그 레벨
 * 관리와 동일하게, entity CRUD 성격이 아니므로 `@RequiresSfPermission` 대신 컨트롤러 내부에서
 * [SystemAdminProfilePolicy.isSystemAdmin] 으로 직접 판정한다.
 */
@RestController
@RequestMapping("/api/v1/admin/tools/feature-toggles")
class FeatureToggleController(
    private val featureToggleService: FeatureToggleService,
) {

    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<FeatureToggleListResponse>> {
        requireSystemAdmin(principal)
        return ResponseEntity.ok(
            ApiResponse.success(FeatureToggleListResponse(featureToggleService.list())),
        )
    }

    @PostMapping
    fun update(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: UpdateFeatureToggleRequest,
    ): ResponseEntity<ApiResponse<FeatureToggleItem>> {
        requireSystemAdmin(principal)
        val flag = FeatureFlag.fromCode(request.code)
            ?: throw BusinessException(
                errorCode = "INVALID_FEATURE_CODE",
                message = "알 수 없는 기능 code: ${request.code}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        val updated = featureToggleService.setEnabled(flag, request.enabled, request.reason)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "PERMISSION_DENIED",
                message = "기능 활성화 관리는 시스템 관리자만 사용할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}
