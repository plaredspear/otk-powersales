package com.otoki.powersales.admin.userrole

import com.otoki.powersales.admin.userrole.dto.UserRoleNode
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * SF UserRole (조직 계층) admin 조회 endpoint.
 *
 * SF "설정 > 사용자 > 역할" 페이지 동등. parent-child 트리로 응답하여 web 측에서
 * Ant Design `Tree` 로 렌더링.
 */
@RestController
@RequestMapping("/api/v1/admin/user-roles")
class AdminUserRoleController(
    private val service: AdminUserRoleService,
) {

    @GetMapping("/tree")
    @RequiresSfPermission(entity = "user_role", operation = SfPermissionOperation.READ)
    fun getTree(): ResponseEntity<ApiResponse<List<UserRoleNode>>> {
        return ResponseEntity.ok(ApiResponse.success(service.getTree()))
    }
}
