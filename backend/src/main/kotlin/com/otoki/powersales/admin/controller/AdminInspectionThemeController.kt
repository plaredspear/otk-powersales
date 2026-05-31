package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.inspection.dto.admin.AdminThemeDetailResponse
import com.otoki.powersales.inspection.dto.admin.AdminThemeListResponse
import com.otoki.powersales.inspection.dto.admin.CreateThemeRequest
import com.otoki.powersales.inspection.dto.admin.ThemeMutationResponse
import com.otoki.powersales.inspection.dto.admin.UpdateThemeRequest
import com.otoki.powersales.inspection.service.AdminInspectionThemeService
import com.otoki.powersales.inspection.service.AdminThemeExcelExporter
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
) {

    @GetMapping
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemes(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<AdminThemeListResponse>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.search(keyword, page, size)))
    }

    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "inspection_theme", operation = SfPermissionOperation.READ)
    fun getThemeDetail(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<AdminThemeDetailResponse>> {
        return ResponseEntity.ok(ApiResponse.success(themeService.getDetail(id)))
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
        @PathVariable id: Long,
    ): ResponseEntity<ByteArray> {
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
