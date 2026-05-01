package com.otoki.powersales.schedule.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.schedule.dto.request.LeaderScheduleCreateRequest
import com.otoki.powersales.schedule.dto.response.LeaderAccountListResponse
import com.otoki.powersales.schedule.dto.response.LeaderScheduleCreateResponse
import com.otoki.powersales.schedule.dto.response.LeaderTeamMemberListResponse
import com.otoki.powersales.schedule.service.LeaderScheduleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 조장 대리 일정 등록 API Controller (Spec #554 P1-B).
 */
@RestController
@RequestMapping("/api/v1/leader")
class LeaderScheduleController(
    private val leaderScheduleService: LeaderScheduleService
) {

    /**
     * 조장 대리 일정 등록
     * POST /api/v1/leader/team-member-schedule
     */
    @PostMapping("/team-member-schedule")
    fun createTeamMemberSchedule(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: LeaderScheduleCreateRequest
    ): ResponseEntity<ApiResponse<LeaderScheduleCreateResponse>> {
        val response = leaderScheduleService.createTeamMemberSchedule(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "팀원 일정이 등록되었습니다"))
    }

    /**
     * 본인 팀원 목록 조회
     * GET /api/v1/leader/team-members
     */
    @GetMapping("/team-members")
    fun getTeamMembers(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<LeaderTeamMemberListResponse>>> {
        val response = leaderScheduleService.getTeamMembers(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response, "팀원 목록 조회 성공"))
    }

    /**
     * 본인 거래처 목록 조회
     * GET /api/v1/leader/accounts
     */
    @GetMapping("/accounts")
    fun getAccounts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<List<LeaderAccountListResponse>>> {
        val response = leaderScheduleService.getAccounts(principal.userId, keyword)
        return ResponseEntity.ok(ApiResponse.success(response, "거래처 목록 조회 성공"))
    }
}
