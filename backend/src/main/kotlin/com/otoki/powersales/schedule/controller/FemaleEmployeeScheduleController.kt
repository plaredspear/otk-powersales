package com.otoki.powersales.schedule.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeScheduleEventDto
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeScheduleSummaryDto
import com.otoki.powersales.schedule.service.FemaleEmployeeScheduleQueryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 여사원 일정 캘린더 조회 controller (web admin).
 *
 * SF `FullCalendarComponentController` (TeamMemberSchedule 도메인) 의 backend 이식.
 * 영업일지 (`FullCalendarComponentController2` + SalesDiary) 는 별도 controller 로 분리 예정 (817).
 */
@RestController
@RequestMapping("/api/v1/admin/female-employee-schedule")
class FemaleEmployeeScheduleController(
    private val service: FemaleEmployeeScheduleQueryService,
) {

    /**
     * 캘린더 이벤트 조회 — SF `fetchAllShcedule(accIds, teamMemberIds, year, month)` 동등.
     *
     * `accountIds` / `teamMemberIds` 둘 다 비어 있으면 빈 결과 반환 (SF 와 동일).
     * 둘 다 채워지면 `teamMemberIds` 우선 (SF if/else if 순서 정합).
     */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/events")
    fun fetchAll(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) accountIds: List<Long>?,
        @RequestParam(required = false) teamMemberIds: List<Long>?,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<List<FemaleEmployeeScheduleEventDto>>> {
        val result = service.fetchAll(
            currentUserSabun = principal.employeeCode,
            currentUserCostCenterCode = principal.costCenterCode,
            accountIds = accountIds.orEmpty(),
            teamMemberIds = teamMemberIds.orEmpty(),
            year = year,
            month = month,
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 월별 근무현황 요약 — SF `fetchScheduleSummary(year, month)` 동등.
     *
     * BranchMapping 미적용 (D1=b SF 비대칭 유지) — 본인 cost_center_code 단일 일치 + role='여사원'.
     */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/summary")
    fun summary(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): ResponseEntity<ApiResponse<List<FemaleEmployeeScheduleSummaryDto>>> {
        val result = service.summary(
            currentUserSabun = principal.employeeCode,
            currentUserCostCenterCode = principal.costCenterCode,
            year = year,
            month = month,
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
