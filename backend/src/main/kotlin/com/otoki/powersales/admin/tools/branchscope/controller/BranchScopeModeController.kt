package com.otoki.powersales.admin.tools.branchscope.controller

import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import com.otoki.powersales.admin.tools.branchscope.service.BranchScopeModeStore
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

/**
 * 개발자 도구 > 대시보드 > 지점 스코프 방식 — 통합 리졸버 on/off 컨트롤러.
 *
 * 투입현황 대시보드의 지점 판정/확장 방식을 런타임에 전환해 전/후 수치를 비교하기 위한 **한시적**
 * 스위치다 ([BranchScopeMode] KDoc 참조). 로그 레벨/기능 활성화와 동일하게 entity CRUD 성격이
 * 아니므로 `@RequiresSfPermission` 대신 [SystemAdminProfilePolicy.isSystemAdmin] 로 직접 가드한다.
 */
@RestController
@RequestMapping("/api/v1/admin/tools/branch-scope-mode")
class BranchScopeModeController(
    private val store: BranchScopeModeStore,
) {

    @GetMapping
    fun get(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<BranchScopeModeResponse>> {
        requireSystemAdmin(principal)
        return ResponseEntity.ok(ApiResponse.success(BranchScopeModeResponse(store.getMode().name)))
    }

    @PostMapping
    fun update(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: UpdateBranchScopeModeRequest,
    ): ResponseEntity<ApiResponse<BranchScopeModeResponse>> {
        requireSystemAdmin(principal)
        val mode = BranchScopeMode.fromNameOrNull(request.mode)
            ?: throw BusinessException(
                errorCode = "INVALID_BRANCH_SCOPE_MODE",
                message = "알 수 없는 지점 스코프 방식: ${request.mode}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        store.setMode(mode)
        return ResponseEntity.ok(
            ApiResponse.success(BranchScopeModeResponse(mode.name), "지점 스코프 방식이 변경되었습니다"),
        )
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "PERMISSION_DENIED",
                message = "지점 스코프 방식 변경은 시스템 관리자만 사용할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}

/** 현재 적용 중인 지점 스코프 방식 (`UNIFIED` | `LEGACY`). */
data class BranchScopeModeResponse(
    val mode: String,
)

data class UpdateBranchScopeModeRequest(
    @field:NotBlank(message = "mode 는 필수입니다")
    val mode: String,
)
