package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeDetailResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.AdminThemeListResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.domain.activity.inspection.dto.admin.ThemeMutationResponse
import com.otoki.powersales.domain.activity.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.domain.activity.inspection.service.AdminInspectionThemeService
import com.otoki.powersales.domain.activity.inspection.service.AdminThemeExcelExporter
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
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
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
) {

    /**
     * 현장점검 테마 관리 화면 지점 셀렉터 옵션 — 여사원 현황/여사원 일정과 동일하게
     * [WomenScheduleBranchResolver] 로 권한별 지점 화이트리스트를 산출한다(단일 출처).
     *
     * 반환 목록은 곧 해당 사용자가 조회 허용된 지점이며, 목록 조회([getThemes]) 는 DataScope 로
     * 동일 화이트리스트를 재적용해 임의 branchCode 조회(IDOR) 를 차단한다. 여사원 현황용
     * `ppt-masters/branches`(professional_promotion_team_master 가드) 를 빌려쓰지 않고
     * 화면 게이팅과 동일한 `inspection_theme` READ 로 가드해, 전문행사조 권한 없는 조장도 지점 목록을 받는다.
     */
    @GetMapping("/branches")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(womenScheduleBranchResolver.resolveBranches(principal)))
    }

    @GetMapping
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemes(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) department: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<AdminThemeListResponse>> {
        return ResponseEntity.ok(
            ApiResponse.success(themeService.search(scope, keyword, department, branchCode, page, size))
        )
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemeDetail(
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<AdminThemeDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.getDetail(scope, id)))
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
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ByteArray> {
        // 목록과 동일 지점 스코프 가드 — 스코프 밖 테마 export 차단(403)
        themeService.validateThemeScope(scope, id)
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
