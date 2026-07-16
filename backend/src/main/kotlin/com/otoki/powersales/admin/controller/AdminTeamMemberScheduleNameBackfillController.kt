package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleNameBackfillService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 — 여사원일정(team_member_schedule) name 백필 endpoint.
 *
 * 자동 채번([TeamMemberScheduleNameGenerator])은 신규 INSERT 부터 적용되므로, 그 이전에 name 이
 * 비어 저장된 기존 일정에 SF AutoNumber 재현값(`TS{00000000}`)을 소급 부여한다.
 *
 * 권한: 운영 데이터 변경이므로 [SfSystemPermission.MODIFY_ALL_DATA] (SYSTEM_ADMIN). preview 는
 * 조회만이라 [SfSystemPermission.VIEW_ALL_DATA].
 */
@RestController
@RequestMapping("/api/v1/admin/team-member-schedule/name-backfill")
class AdminTeamMemberScheduleNameBackfillController(
    private val service: TeamMemberScheduleNameBackfillService,
) {

    /** 채번이 필요한(name 이 비어있는) 일정 건수 조회. */
    @GetMapping("/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun preview(): ResponseEntity<ApiResponse<PreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(PreviewResponse(missing = service.countMissing())))

    /**
     * name 이 비어있는 일정에 채번값을 소급 부여. id 오래된 순 최대 limit 건.
     * @param limit 이번 실행 처리 상한 (기본 1000, 최대 5000 로 서비스에서 clamp).
     */
    @PostMapping("/execute")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun execute(
        @RequestParam(required = false) limit: Int?,
    ): ResponseEntity<ApiResponse<TeamMemberScheduleNameBackfillService.BackfillResult>> {
        val result = service.backfill(limit ?: TeamMemberScheduleNameBackfillService.DEFAULT_LIMIT)
        return ResponseEntity.ok(ApiResponse.success(result, "백필을 실행했습니다"))
    }

    data class PreviewResponse(val missing: Long)
}
