package com.otoki.powersales.admin.controller

import com.otoki.powersales.apppackage.dto.AppPackageDetailDto
import com.otoki.powersales.apppackage.dto.AppPackageDistributionUrlsDto
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

    /**
     * 대규모 배포용 고정 링크 (iOS 설치 페이지 + Android APK 다운로드). 버전과 무관한 고정값이라
     * web 이 각 플랫폼 탭 상단에 상시 표시한다. url 이 null 이면 API 도메인 미설정 환경(local).
     */
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @GetMapping("/distribution-urls")
    fun distributionUrls(): ResponseEntity<ApiResponse<AppPackageDistributionUrlsDto>> {
        val dto = AppPackageDistributionUrlsDto(
            iosInstallUrl = adminAppPackageService.iosInstallUrl(),
            androidDownloadUrl = adminAppPackageService.androidDownloadUrl(),
        )
        return ResponseEntity.ok(ApiResponse.success(dto))
    }

    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    @PostMapping(consumes = ["multipart/form-data"])
    fun upload(
        @RequestParam platform: AppPlatform,
        @RequestParam(required = false) versionName: String?,
        @RequestParam(required = false) versionCode: Long?,
        @RequestParam(required = false, defaultValue = "false") forceUpdate: Boolean,
        @RequestParam(required = false) releaseNote: String?,
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AppPackageDetailDto>> {
        // iOS 는 bundleIdentifier / versionName / versionCode 를 업로드된 .ipa 의
        // Info.plist 에서 자동 추출한다(미입력 허용). Android 는 버전 필드가 필수.
        val result = adminAppPackageService.upload(
            platform, versionName, versionCode, forceUpdate, releaseNote, file, principal.employeeId
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
