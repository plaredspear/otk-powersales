package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.schedule.dto.response.CategoryScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationFilterOptionsResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleResponse
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyIntegrationService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/schedules/monthly-integration")
class AdminMonthlyIntegrationController(
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService
) {

    @GetMapping
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun getMonthlyIntegration(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) accountKeyword: String?,
        @RequestParam(required = false) distributionKeyword: String?,
        @RequestParam(required = false) accountTypeKeyword: String?
    ): ResponseEntity<ApiResponse<MonthlyIntegrationScheduleResponse>> {
        val response = adminMonthlyIntegrationService.getMonthlyIntegration(
            year, month, costCenterCodes, keyword, accountKeyword, distributionKeyword, accountTypeKeyword,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 조회조건 드롭다운 옵션 — 유통형태 / 거래처유형 목록 + 유통형태별 종속 거래처유형 매핑. */
    @GetMapping("/filter-options")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun getFilterOptions(): ResponseEntity<ApiResponse<MonthlyIntegrationFilterOptionsResponse>> {
        val response = adminMonthlyIntegrationService.getFilterOptions()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** MFEIS row 상세 — 집계 근거가 된 여사원일정 목록. */
    @GetMapping("/{id}")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun getMonthlyIntegrationDetail(
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<MonthlyIntegrationDetailResponse>> {
        val response = adminMonthlyIntegrationService.getIntegrationDetail(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/export")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun exportMonthlyIntegration(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) accountKeyword: String?,
        @RequestParam(required = false) distributionKeyword: String?,
        @RequestParam(required = false) accountTypeKeyword: String?
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyIntegrationService.exportMonthlyIntegration(
            year, month, costCenterCodes, keyword, accountKeyword, distributionKeyword, accountTypeKeyword,
        )
        return ExcelResponseUtils.build(result)
    }

    // 근무형태별 인원현황 2종은 SF getCategory 정합으로 principal 기반 지점 교집합 가드 적용
    // (SF CurrentUserBranchNameList ∩ orgValues). 통합일정 계열은 SF getSchedule 에 교집합이 없어 미적용.
    @GetMapping("/category")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun getCategorySchedule(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<CategoryScheduleResponse>> {
        val response = adminMonthlyIntegrationService.getCategorySchedule(year, month, costCenterCodes, principal)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/category/export")
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    fun exportCategorySchedule(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam costCenterCodes: List<String>,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ByteArray> {
        val result = adminMonthlyIntegrationService.exportCategorySchedule(year, month, costCenterCodes, principal)
        return ExcelResponseUtils.build(result)
    }
}
