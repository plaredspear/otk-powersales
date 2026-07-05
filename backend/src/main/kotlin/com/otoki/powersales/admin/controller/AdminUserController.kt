package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.AdminUserDetailResponse
import com.otoki.powersales.admin.dto.AdminUserListResponse
import com.otoki.powersales.admin.dto.AdminUserPasswordResetResponse
import com.otoki.powersales.admin.dto.AdminUserProfileOption
import com.otoki.powersales.admin.dto.UpdateUserActiveStatusRequest
import com.otoki.powersales.admin.service.AdminUserService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * web admin User 엔티티 관리 화면 컨트롤러.
 *
 * - GET /api/v1/admin/users — 검색/필터/페이지네이션 목록 (USER_READ)
 * - GET /api/v1/admin/users/{userId} — 상세 (USER_READ)
 * - POST /api/v1/admin/users/{userId}/reset-password — 임시 비밀번호 리셋 (USER_WRITE)
 * - PUT  /api/v1/admin/users/{userId}/active — 활성/비활성 토글 (USER_WRITE)
 */
@RestController
@RequestMapping("/api/v1/admin/users")
class AdminUserController(
    private val adminUserService: AdminUserService
) {

    @GetMapping
    @RequiresSfPermission(entity = "user", operation = SfPermissionOperation.READ)
    fun getUsers(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(required = false) profileId: Long?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminUserListResponse>> {
        val response = adminUserService.findUsers(keyword, isActive, profileId, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 사용자 관리 화면 필터용 프로파일 옵션 목록 — `user` READ 로 가드.
     *
     * 프로파일 관리 상세 목록(`/permissions/profiles`, `profile` READ) 을 빌려쓰면 `user` 권한만
     * 가진 관리자는 필터 로딩에서 403 이 나므로, 화면 게이팅 권한(`user`)과 동일하게 가드한 경량
     * lookup 으로 분리한다 (id/name 만 반환).
     */
    @GetMapping("/profile-options")
    @RequiresSfPermission(entity = "user", operation = SfPermissionOperation.READ)
    fun getProfileOptions(): ResponseEntity<ApiResponse<List<AdminUserProfileOption>>> {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getProfileOptions()))
    }

    @GetMapping("/{userId}")
    @RequiresSfPermission(entity = "user", operation = SfPermissionOperation.READ)
    fun getUser(
        @PathVariable userId: Long
    ): ResponseEntity<ApiResponse<AdminUserDetailResponse>> {
        val response = adminUserService.findUserDetail(userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{userId}/reset-password")
    @RequiresSfPermission(entity = "user", operation = SfPermissionOperation.EDIT)
    fun resetPassword(
        @PathVariable userId: Long
    ): ResponseEntity<ApiResponse<AdminUserPasswordResetResponse>> {
        val response = adminUserService.resetPassword(userId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }

    @PutMapping("/{userId}/active")
    @RequiresSfPermission(entity = "user", operation = SfPermissionOperation.EDIT)
    fun updateActiveStatus(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable userId: Long,
        @RequestBody request: UpdateUserActiveStatusRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        adminUserService.updateActiveStatus(
            targetUserId = userId,
            requesterUserId = principal.userId,
            isActive = request.isActive
        )
        val message = if (request.isActive) "사용자가 활성화되었습니다" else "사용자가 비활성화되었습니다"
        return ResponseEntity.ok(ApiResponse.success(Unit, message))
    }
}
