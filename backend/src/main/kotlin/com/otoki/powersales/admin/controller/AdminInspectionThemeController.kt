package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeDetailResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeListResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.domain.activity.inspection.dto.admin.ThemeMutationResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.admin.dto.AdminUserListResponse
import com.otoki.powersales.admin.service.AdminUserService
import com.otoki.powersales.domain.activity.inspection.service.AdminInspectionThemeService
import com.otoki.powersales.domain.activity.inspection.service.AdminThemeExcelExporter
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
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
 * admin 현장점검(등록) 테마 관리 API.
 *
 * 레거시 SF `현장점검(등록)` = `Theme__c` 표준 레코드 페이지 동등 — 목록/상세/등록/수정/삭제 +
 * 테마 단위 하위 현장점검 결과 엑셀 다운로드(`SiteActivityToExcel`). SF Permission `inspection_theme` 가드.
 */
@RestController
@RequestMapping("/api/v1/admin/inspection-themes")
class AdminInspectionThemeController(
    private val themeService: AdminInspectionThemeService,
    private val excelExporter: AdminThemeExcelExporter,
    private val adminUserService: AdminUserService,
) {

    /**
     * 현장점검 테마 관리 화면 지점 셀렉터 옵션.
     *
     * 테마의 `branch_code` 는 생성자 사원의 `costCenterCode`(단일 지점) 로 적재되므로, 지점 스코프도
     * 조직 트리 확장 없이 본인 `costCenterCode` 단일값 기준으로 판정한다([AdminInspectionThemeService.getBranches]).
     * 여사원 일정용 조직 트리 확장(`findTeamScheduleBranches`) 은 상위 조직에 매칭되는 형제 지점까지 딸려와
     * 테마 조회에는 과도하므로 쓰지 않는다. 전사 권한자(시스템개발자/영업지원/본부장) 는 전 지점을 받는다.
     * 화면 게이팅과 동일한 `inspection_theme` READ 로 가드.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.getBranches(principal)))
    }

    /**
     * 테마 소유자 변경 Select 후보 lookup — 활성 User 검색.
     *
     * 레거시 SF `Theme__c` 는 표준 Edit 폼에서 OwnerId 를 전사 활성 User 전체에서 골라 변경했고(별도 권한 게이트 없음),
     * 소유자 변경은 테마 Edit 권한자면 가능한 기능이었다. 신규가 이 후보 조회를 `user` READ 가드 `/admin/users` 로
     * 채우면 테마 EDIT 만 있고 user READ 없는 사용자(예: 여사원 대행)가 403 → 레거시 대비 과다 제약.
     * 화면 게이팅과 동일한 `inspection_theme` READ 로 가드한 전용 lookup 으로 제공해 레거시 정합을 복원한다.
     */
    @GetMapping("/owner-candidates")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getOwnerCandidates(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<AdminUserListResponse>> {
        // 활성 User 만, 소유자 후보 Select 용 소량(size). 레거시 정합 — 전사 범위(지점 제한 없음).
        return ResponseEntity.ok(
            ApiResponse.success(adminUserService.findUsers(keyword, isActive = true, profileId = null, page = 0, size = size))
        )
    }

    @GetMapping
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemes(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<AdminThemeListResponse>> {
        return ResponseEntity.ok(
            ApiResponse.success(themeService.search(principal, keyword, department, branchCode, page, size))
        )
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemeDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<AdminThemeDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.getDetail(principal, id)))
    }

    @PostMapping
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.CREATE)
    fun createTheme(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestBody request: CreateThemeRequest,
    ): ResponseEntity<ApiResponse<ThemeMutationResponse>> {
        val response = themeService.create(principal, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.EDIT)
    fun updateTheme(
        @PathVariable id: Long,
        @RequestBody request: UpdateThemeRequest,
    ): ResponseEntity<ApiResponse<ThemeMutationResponse>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.update(id, request)))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.DELETE)
    fun deleteTheme(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Any?>> {
        themeService.delete(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "테마가 삭제되었습니다"))
    }

    /** 테마 단위 하위 현장점검 결과 엑셀 다운로드. */
    @GetMapping("/{id}/export")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun exportTheme(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
    ): ResponseEntity<ByteArray> {
        // 목록과 동일 지점 스코프 가드 — 스코프 밖 테마 export 차단(403)
        themeService.validateThemeScope(principal, id)
        val result = excelExporter.export(id)
        val headers = HttpHeaders()
        headers.contentType = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
        val encodedFilename = URLEncoder.encode(result.filename, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedFilename")
        return ResponseEntity.ok().headers(headers).body(result.bytes)
    }
}
