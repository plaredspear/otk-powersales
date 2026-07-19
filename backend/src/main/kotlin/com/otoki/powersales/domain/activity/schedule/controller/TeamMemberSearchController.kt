package com.otoki.powersales.domain.activity.schedule.controller

import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberCategorySearchResult
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberScheduleSearchResult
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberCategorySearchService
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleSearchService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 팀멤버 검색 controller (web admin).
 *
 * SF `CategorySearchByTeamMemberController` + `ScheduleSearchByTeamMemberController` 의 backend 이식.
 * BranchCodeExpander 적용 + SF formula 필드는 lazy join + 합산으로 환원 (Spec 813 D2~D4).
 */
@RestController
@RequestMapping("/api/v1/admin/team-member-search")
class TeamMemberSearchController(
    private val categoryService: TeamMemberCategorySearchService,
    private val scheduleService: TeamMemberScheduleSearchService,
) {

    /**
     * 카테고리별 인원 현황 검색 — SF `CategorySearchByTeamMemberController.getCategory(year, month, orgValues)` 동등.
     * principal 기반 지점 교집합 가드 (SF `CurrentUserBranchNameList` ∩ orgValues) 는 service 내부에서 적용.
     */
    @RequiresSfPermission(entity = "monthly_female_employee_integration_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/category")
    fun searchCategory(
        @RequestParam year: String,
        @RequestParam month: String,
        @RequestParam(required = false) orgValues: List<String>?,
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<TeamMemberCategorySearchResult>> {
        val result = categoryService.search(year, month, orgValues.orEmpty(), principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 일정 라인 검색 (+ 거래처 6개월 평균 ABC 마감실적) — SF `ScheduleSearchByTeamMemberController.getSchedule(year, month, orgValues)` 동등.
     */
    @RequiresSfPermission(entity = "monthly_female_employee_integration_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/schedule")
    fun searchSchedule(
        @RequestParam year: String,
        @RequestParam month: String,
        @RequestParam(required = false) orgValues: List<String>?,
    ): ResponseEntity<ApiResponse<TeamMemberScheduleSearchResult>> {
        val result = scheduleService.search(year, month, orgValues.orEmpty())
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
