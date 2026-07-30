package com.otoki.powersales.admin.tools.feature.controller

import com.otoki.powersales.admin.tools.feature.FeatureFlag
import com.otoki.powersales.admin.tools.feature.dto.AddFeatureToggleExemptEmployeeRequest
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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 — 등록 기능(제품 클레임/물류 클레임/주문) on/off 컨트롤러.
 *
 * on/off 와 함께 기능별 **예외 사번** 목록을 관리한다. 예외 사번으로 등록된 사원은 해당 기능이
 * 비활성이어도 등록할 수 있다.
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
        val flag = resolveFlag(request.code)
        val updated = featureToggleService.setEnabled(flag, request.enabled, request.reason)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    /**
     * 예외 사원 추가 — 해당 기능이 비활성이어도 이 사번의 사원은 등록할 수 있다.
     * 존재하지 않는 사번이면 400 으로 거부한다.
     */
    @PostMapping("/{code}/exempt-employees")
    fun addExemptEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable code: String,
        @Valid @RequestBody request: AddFeatureToggleExemptEmployeeRequest,
    ): ResponseEntity<ApiResponse<FeatureToggleItem>> {
        requireSystemAdmin(principal)
        val updated = featureToggleService.addExemptEmployee(resolveFlag(code), request.employeeCode)
        return ResponseEntity.ok(ApiResponse.success(updated, "예외 사번이 추가되었습니다"))
    }

    /** 예외 사원 제거. 목록에 없는 사번이어도 성공으로 처리한다(멱등). */
    @DeleteMapping("/{code}/exempt-employees/{employeeCode}")
    fun removeExemptEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable code: String,
        @PathVariable employeeCode: String,
    ): ResponseEntity<ApiResponse<FeatureToggleItem>> {
        requireSystemAdmin(principal)
        val updated = featureToggleService.removeExemptEmployee(resolveFlag(code), employeeCode)
        return ResponseEntity.ok(ApiResponse.success(updated, "예외 사번이 삭제되었습니다"))
    }

    private fun resolveFlag(code: String): FeatureFlag =
        FeatureFlag.fromCode(code)
            ?: throw BusinessException(
                errorCode = "INVALID_FEATURE_CODE",
                message = "알 수 없는 기능 code: $code",
                httpStatus = HttpStatus.BAD_REQUEST,
            )

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
