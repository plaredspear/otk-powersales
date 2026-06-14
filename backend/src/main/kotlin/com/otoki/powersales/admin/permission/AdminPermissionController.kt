package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.AvailablePermissionResources
import com.otoki.powersales.admin.permission.dto.PaginatedPermissionSetChangeLogList
import com.otoki.powersales.admin.permission.dto.PermissionMatrix
import com.otoki.powersales.admin.permission.dto.PermissionSetCreateRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetDetail
import com.otoki.powersales.admin.permission.dto.PermissionSetMatrix
import com.otoki.powersales.admin.permission.dto.PermissionSetMutationResponse
import com.otoki.powersales.admin.permission.dto.PermissionSetSummary
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateFlagsRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateMetaRequest
import com.otoki.powersales.admin.permission.dto.ProfileDetail
import com.otoki.powersales.admin.permission.dto.ProfileFlagsMutationResponse
import com.otoki.powersales.admin.permission.dto.ProfileUpdateFlagsRequest
import com.otoki.powersales.admin.permission.dto.ProfileSummary
import com.otoki.powersales.admin.permission.dto.SfObjectResource
import com.otoki.powersales.admin.permission.exception.PermissionSetNotFoundException
import com.otoki.powersales.admin.permission.exception.ProfileNotFoundException
import com.otoki.powersales.platform.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
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
    private val mutationService: AdminPermissionSetMutationService,
    private val profileFlagsMutationService: AdminProfileFlagsMutationService,
    private val entitySfNameRegistry: EntitySfNameRegistry,
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

    /**
     * Profile 권한 비트 편집 — system 5종 + object/custom 권한 전체 교체.
     *
     * SF 정합 — 직책별 Profile 에 객체권한 (예: monthly_sales_history Read) 을 박으면 발령으로 해당
     * 직책이 된 사원에게 화면 권한이 자동 전파된다. 편집 시 is_locally_modified set (SF 재적재 보호).
     */
    @PutMapping("/profiles/{profileId}/flags")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun updateProfileFlags(
        @PathVariable profileId: Long,
        @Valid @RequestBody request: ProfileUpdateFlagsRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<ProfileFlagsMutationResponse>> {
        val response = profileFlagsMutationService.updateFlags(profileId, request, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
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

    /**
     * "페이지별 필요 권한" 가이드 페이지가 사용. 모든 PermissionSet 의 시스템권한 flag + entity
     * 객체권한 매트릭스를 한 번에 반환한다.
     */
    @GetMapping("/permission-sets/matrix")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getPermissionSetMatrix(): ResponseEntity<ApiResponse<PermissionSetMatrix>> {
        return ResponseEntity.ok(ApiResponse.success(inspectionService.getPermissionSetMatrix()))
    }

    // ── Spec #837 — PermissionSet 자체 관리 endpoint ─────────────────────────

    /**
     * 권한 비트 매트릭스 편집 UI 의 자원 카탈로그.
     * SF 매핑 entity 와 가상 자원 (`@PermissionResource`) 을 분리하여 반환.
     */
    @GetMapping("/permission-sets/available-resources")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun listAvailableResources(): ResponseEntity<ApiResponse<AvailablePermissionResources>> {
        val sfMapping = entitySfNameRegistry.snapshot()
        val sfObjects = sfMapping.entries
            .map { (entity, sfApiName) -> SfObjectResource(sfApiName = sfApiName, entity = entity) }
            .sortedBy { it.sfApiName }
        val customResources = (entitySfNameRegistry.allResources() - sfMapping.keys).toList().sorted()
        return ResponseEntity.ok(
            ApiResponse.success(AvailablePermissionResources(sfObjects = sfObjects, customResources = customResources)),
        )
    }

    @PostMapping("/permission-sets")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun createPermissionSet(
        @Valid @RequestBody request: PermissionSetCreateRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<PermissionSetMutationResponse>> {
        val response = mutationService.create(request, principal.userId)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "PermissionSet 이 생성되었습니다"))
    }

    @PutMapping("/permission-sets/{permissionSetId}")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun updatePermissionSetMeta(
        @PathVariable permissionSetId: Long,
        @Valid @RequestBody request: PermissionSetUpdateMetaRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<PermissionSetMutationResponse>> {
        val response = mutationService.updateMeta(permissionSetId, request, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/permission-sets/{permissionSetId}/flags")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun updatePermissionSetFlags(
        @PathVariable permissionSetId: Long,
        @Valid @RequestBody request: PermissionSetUpdateFlagsRequest,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<PermissionSetMutationResponse>> {
        val response = mutationService.updateFlags(permissionSetId, request, principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/permission-sets/{permissionSetId}")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun deletePermissionSet(
        @PathVariable permissionSetId: Long,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<Void> {
        mutationService.delete(permissionSetId, principal.userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/permission-sets/{permissionSetId}/change-log")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MANAGE_USERS)
    fun listPermissionSetChangeLog(
        @PathVariable permissionSetId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<PaginatedPermissionSetChangeLogList>> {
        val pageResult = mutationService.listChangeLog(permissionSetId, page, size)
        return ResponseEntity.ok(
            ApiResponse.success(
                PaginatedPermissionSetChangeLogList(
                    totalElements = pageResult.totalElements,
                    totalPages = pageResult.totalPages,
                    number = pageResult.number,
                    size = pageResult.size,
                    content = pageResult.content,
                ),
            ),
        )
    }
}
