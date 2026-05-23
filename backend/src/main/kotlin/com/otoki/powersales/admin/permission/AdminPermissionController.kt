package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.PermissionMatrix
import com.otoki.powersales.admin.permission.dto.PermissionSetDetail
import com.otoki.powersales.admin.permission.dto.PermissionSetSummary
import com.otoki.powersales.admin.permission.dto.ProfileDetail
import com.otoki.powersales.admin.permission.dto.ProfileSummary
import com.otoki.powersales.admin.permission.exception.PermissionSetNotFoundException
import com.otoki.powersales.admin.permission.exception.ProfileNotFoundException
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Spec #803 — 권한 관리 admin 조회 endpoint.
 */
@RestController
@RequestMapping("/api/v1/admin/permissions")
class AdminPermissionController(
    private val inspectionService: AdminPermissionInspectionService,
) {

    @GetMapping("/profiles")
    @RequiresSfPermission(entity = "profile", operation = SfPermissionOperation.READ)
    fun listProfiles(): ResponseEntity<ApiResponse<List<ProfileSummary>>> {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.listProfiles()))
    }

    @GetMapping("/profiles/{profileId}")
    @RequiresSfPermission(entity = "profile", operation = SfPermissionOperation.READ)
    fun getProfile(
        @PathVariable profileId: Long,
        @RequestParam(defaultValue = "0") userPage: Int,
        @RequestParam(defaultValue = "20") userSize: Int,
        @RequestParam(required = false) userKeyword: String?,
    ): ResponseEntity<ApiResponse<ProfileDetail>> {
        val detail = inspectionService.getProfile(profileId, userPage, userSize, userKeyword)
            ?: throw ProfileNotFoundException(profileId)
        return ResponseEntity.ok(ApiResponse.success(detail))
    }

    @GetMapping("/permission-sets")
    @RequiresSfPermission(entity = "permission_set", operation = SfPermissionOperation.READ)
    fun listPermissionSets(): ResponseEntity<ApiResponse<List<PermissionSetSummary>>> {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.listPermissionSets()))
    }

    @GetMapping("/permission-sets/{permissionSetId}")
    @RequiresSfPermission(entity = "permission_set", operation = SfPermissionOperation.READ)
    fun getPermissionSet(
        @PathVariable permissionSetId: Long,
        @RequestParam(defaultValue = "0") userPage: Int,
        @RequestParam(defaultValue = "20") userSize: Int,
        @RequestParam(required = false) userKeyword: String?,
    ): ResponseEntity<ApiResponse<PermissionSetDetail>> {
        val detail = inspectionService.getPermissionSet(permissionSetId, userPage, userSize, userKeyword)
            ?: throw PermissionSetNotFoundException(permissionSetId)
        return ResponseEntity.ok(ApiResponse.success(detail))
    }

    @GetMapping("/matrix")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getMatrix(): ResponseEntity<ApiResponse<PermissionMatrix>> {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.getMatrix()))
    }
}
