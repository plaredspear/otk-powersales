package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AssignmentBatchRequest
import com.otoki.powersales.admin.permission.dto.AssignmentBatchResult
import com.otoki.powersales.admin.permission.dto.AssignmentCreateRequest
import com.otoki.powersales.admin.permission.dto.AssignmentResponse
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Spec #804 — Assignment 부여/회수 endpoint.
 *
 * 모든 endpoint 가드: MANAGE_USERS 시스템 권한 (Q2 옵션 1).
 */
@RestController
@RequestMapping("/api/v1/admin/permissions/assignments")
class AdminPermissionAssignmentController(
    private val assignmentService: AdminPermissionAssignmentService,
) {

    @PostMapping
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun assign(
        @Valid @RequestBody request: AssignmentCreateRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AssignmentResponse>> {
        val response = assignmentService.assign(request.userId, request.permissionSetFlagsId, principal.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "PermissionSet 이 부여되었습니다"))
    }

    @DeleteMapping("/{assignmentId}")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun revoke(
        @PathVariable assignmentId: Long,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<Void> {
        assignmentService.revoke(assignmentId, principal.userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/batch")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun assignBatch(
        @RequestBody request: AssignmentBatchRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AssignmentBatchResult>> {
        val response = assignmentService.assignBatch(request, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
