package com.otoki.internal.admin.controller

import com.otoki.internal.schedule.dto.request.TeamScheduleCreateRequest
import com.otoki.internal.schedule.dto.request.TeamScheduleUpdateRequest
import com.otoki.internal.schedule.dto.response.*
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.schedule.service.AdminTeamScheduleService
import com.otoki.internal.common.dto.response.BranchResponse
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/team-schedule")
class AdminTeamScheduleController(
    private val adminTeamScheduleService: AdminTeamScheduleService
) {

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/members")
    fun getMembers(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<TeamMemberDto>>> {
        val result = adminTeamScheduleService.getMembers(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/accounts")
    fun getAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<List<TeamScheduleAccountDto>>> {
        val result = adminTeamScheduleService.getAccounts(principal.userId, branchCode)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = adminTeamScheduleService.getBranches()
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping
    fun getMonthlySchedules(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) employeeIds: String?,
        @RequestParam(required = false) accountIds: String?
    ): ResponseEntity<ApiResponse<List<TeamScheduleDto>>> {
        val employeeIdList = employeeIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toLong() }
        val accountIdList = accountIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toInt() }
        val result = adminTeamScheduleService.getMonthlySchedules(
            principal.userId, year, month, employeeIdList, accountIdList
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_READ)
    @GetMapping("/summary")
    fun getDailySummary(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(required = false) employeeIds: String?,
        @RequestParam(required = false) accountIds: String?
    ): ResponseEntity<ApiResponse<List<DailySummaryDto>>> {
        val employeeIdList = employeeIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toLong() }
        val accountIdList = accountIds?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.map { it.toInt() }
        val result = adminTeamScheduleService.getDailySummary(
            principal.userId, year, month, employeeIdList, accountIdList
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: TeamScheduleCreateRequest
    ): ResponseEntity<ApiResponse<TeamScheduleCreateResultDto>> {
        val result = adminTeamScheduleService.createSchedule(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "일정이 등록되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: TeamScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.updateSchedule(principal.userId, id, request)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 수정되었습니다"))
    }

    @RequiresPermission(AdminPermission.SCHEDULE_WRITE)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminTeamScheduleService.deleteSchedule(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "일정이 삭제되었습니다"))
    }
}
