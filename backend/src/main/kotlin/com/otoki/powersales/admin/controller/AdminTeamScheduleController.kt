package com.otoki.powersales.admin.controller

import com.otoki.powersales.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.powersales.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
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

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/members")
    fun getMembers(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<List<TeamMemberDto>>> {
        val result = adminTeamScheduleService.getMembers(principal, branchCode)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/accounts")
    fun getAccounts(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<List<TeamScheduleAccountDto>>> {
        val result = adminTeamScheduleService.getAccounts(principal, branchCode)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = adminTeamScheduleService.getBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/professional-promotion-teams")
    fun getProfessionalPromotionTeams(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<String>>> {
        val result = adminTeamScheduleService.getProfessionalPromotionTeams()
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping
    fun getSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
        @RequestParam(required = false) employeeIds: String?,
        @RequestParam(required = false) accountIds: String?,
        @RequestParam(required = false) promotionTeams: String?
    ): ResponseEntity<ApiResponse<MonthlyScheduleWithSummaryDto>> {
        val employeeIdList = employeeIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toLong() }
        val accountIdList = accountIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toInt() }
        val promotionTeamList = promotionTeams?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        val result = adminTeamScheduleService.getSchedulesWithSummary(
            from, to, employeeIdList, accountIdList, promotionTeamList
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: TeamScheduleCreateRequest
    ): ResponseEntity<ApiResponse<TeamScheduleCreateResultDto>> {
        val result = adminTeamScheduleService.createSchedule(principal, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "일정이 등록되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: TeamScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.updateSchedule(principal, id, request)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 수정되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.deleteSchedule(principal, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 삭제되었습니다"))
    }
}
