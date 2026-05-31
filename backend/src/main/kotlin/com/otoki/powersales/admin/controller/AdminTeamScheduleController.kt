package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleMassDeleteRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.service.AdminTeamScheduleService
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/team-schedule")
class AdminTeamScheduleController(
    private val adminTeamScheduleService: AdminTeamScheduleService,
) {

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = adminTeamScheduleService.getBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 여사원 일정관리 화면 초기 로드 통합 endpoint — branches/members/accounts/professional-promotion-teams/dailySummary
     * 5건 fetch 를 1 round-trip 으로 합친다.
     *
     * `branchCode` 지정 시 해당 지점 거래처를 채워 보낸다 (다중지점 사용자가 지점 드롭다운 변경 시 재호출).
     * 미지정 + 단일지점 사용자는 본인 지점 거래처 자동 사용.
     */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/form")
    fun getForm(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<TeamScheduleFormDto>> {
        val result = adminTeamScheduleService.getForm(principal, branchCode)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.READ)
    @GetMapping
    fun getSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) employeeIds: String?,
        @RequestParam(required = false) accountIds: String?,
        @RequestParam(required = false) promotionTeams: String?,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<MonthlyScheduleWithSummaryDto>> {
        val employeeIdList = employeeIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toLong() }
        val accountIdList = accountIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toInt() }
        val promotionTeamList = promotionTeams?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        val result = adminTeamScheduleService.getSchedulesWithSummary(
            from, to, employeeIdList, accountIdList, promotionTeamList, principal, branchCode
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: TeamScheduleCreateRequest
    ): ResponseEntity<ApiResponse<TeamScheduleCreateResultDto>> {
        val result = adminTeamScheduleService.createSchedule(principal, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "일정이 등록되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: TeamScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.updateSchedule(principal, id, request)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 수정되었습니다"))
    }

    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.deleteSchedule(principal, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 삭제되었습니다"))
    }

    /**
     * 여사원 일정 다건 삭제 (Spec #691 P1-B).
     *
     * legacy `MassDeleteTmScheduleController.doMassDelete` (VF `@RemoteAction` + 100건 + 진열 + CommuteLogId=null) 동등 endpoint.
     * Q5 옵션 1 — 전체 rollback (legacy `delete deleteList;` `allOrNone=true` 동등) — 1건이라도 가드 fail 시 첫 실패 row 의 도메인 예외 throw.
     */
    @RequiresSfPermission(entity = "team_member_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/mass-delete")
    fun massDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: TeamScheduleMassDeleteRequest
    ): ResponseEntity<ApiResponse<TeamScheduleMassDeleteResponse>> {
        val deletedCount = adminTeamScheduleService.massDelete(principal, request.ids)
        return ResponseEntity.ok(
            ApiResponse.success(
                TeamScheduleMassDeleteResponse(deletedCount = deletedCount),
                "일정이 삭제되었습니다"
            )
        )
    }
}
