package com.otoki.powersales.admin.controller

import com.otoki.powersales.apppackage.dto.AppPackageDetailDto
import com.otoki.powersales.apppackage.dto.AppPackageForceUpdateRequest
import com.otoki.powersales.apppackage.dto.AppPackageListItemDto
import com.otoki.powersales.apppackage.dto.AppPackageReleaseNoteUpdateRequest
import com.otoki.powersales.apppackage.entity.AppPlatform
import com.otoki.powersales.apppackage.service.AdminAppPackageService
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 웹 관리자 모바일 앱 패키지(APK/IPA) 버전 관리.
 *
 * SF 비대응 자체 엔티티 → 전 메서드 SYSTEM(MODIFY_ALL_DATA) 가드 (RequiresSfPermission.kt 정책).
 */
@RestController
@RequestMapping("/api/v1/admin/app-package")
class AdminAppPackageController(
    private val adminAppPackageService: AdminAppPackageService,
) {

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @GetMapping
    fun list(
        @RequestParam(required = false) platform: AppPlatform?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<Page<AppPackageListItemDto>>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "versionCode"))
        return ResponseEntity.ok(ApiResponse.success(adminAppPackageService.list(platform, pageable)))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @GetMapping("/{id}")
    fun detail(@PathVariable id: Long): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        return ResponseEntity.ok(ApiResponse.success(adminAppPackageService.getDetail(id)))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @PostMapping(consumes = ["multipart/form-data"])
    fun upload(
        @RequestParam platform: AppPlatform,
        @RequestParam versionName: String,
        @RequestParam versionCode: Long,
        @RequestParam(required = false, defaultValue = "false") forceUpdate: Boolean,
        @RequestParam(required = false) releaseNote: String?,
        @RequestParam(required = false) bundleIdentifier: String?,
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        val result = adminAppPackageService.upload(
            platform, versionName, versionCode, forceUpdate, releaseNote, bundleIdentifier, file, principal.employeeId
        )
        return ResponseEntity.ok(ApiResponse.success(result, "패키지가 업로드되었습니다"))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @PatchMapping("/{id}/latest")
    fun setLatest(@PathVariable id: Long): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        return ResponseEntity.ok(ApiResponse.success(adminAppPackageService.setLatest(id), "최신 버전으로 지정되었습니다"))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @PatchMapping("/{id}/force-update")
    fun setForceUpdate(
        @PathVariable id: Long,
        @RequestBody request: AppPackageForceUpdateRequest,
    ): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        return ResponseEntity.ok(ApiResponse.success(adminAppPackageService.setForceUpdate(id, request.forceUpdate)))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @PatchMapping("/{id}")
    fun updateReleaseNote(
        @PathVariable id: Long,
        @RequestBody request: AppPackageReleaseNoteUpdateRequest,
    ): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        return ResponseEntity.ok(ApiResponse.success(adminAppPackageService.updateReleaseNote(id, request.releaseNote)))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        adminAppPackageService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "삭제되었습니다"))
    }
}
