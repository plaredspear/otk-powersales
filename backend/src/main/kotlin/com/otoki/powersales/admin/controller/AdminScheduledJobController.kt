package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.request.OroraMonthlyMaterializeTriggerRequest
import com.otoki.powersales.admin.dto.response.OroraMonthlyMaterializeTriggerResponse
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.admin.service.AdminScheduledJobService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Admin 스케줄 잡 실행 이력 조회 endpoint.
 *
 * - `/runs`: 페이지네이션된 실행 이력 (필터: jobName, status, startedAt 범위)
 * - `/catalog`: 등록된 `@Scheduled` 잡의 정적 목록 (jobName + cron + 설명, [com.otoki.powersales.platform.batch.ScheduledJobCatalog] 단일 출처)
 * - `/summary`: 최근 N시간 윈도우의 status 별 카운트 + distinct jobName 목록
 *
 * 모든 endpoint 는 [AdminPermission.SCHEDULED_JOB_READ] 권한 필수
 * — [com.otoki.powersales.admin.security.RolePermissionMatrix] 의 `SYSTEM_ADMIN` 에만 부여.
 */
@RestController
class AdminScheduledJobController(
    private val adminScheduledJobService: AdminScheduledJobService,
) {

    @GetMapping("/api/v1/admin/scheduled-jobs/runs")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun searchRuns(
        @RequestParam(required = false) jobName: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<ScheduledJobRunListResponse>> {
        val response = adminScheduledJobService.search(
            AdminScheduledJobQuery(
                jobName = jobName,
                status = status,
                from = from,
                to = to,
                page = page,
                size = size,
            )
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/scheduled-jobs/catalog")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getCatalog(): ResponseEntity<ApiResponse<List<RegisteredScheduledJobDto>>> {
        return ResponseEntity.ok(ApiResponse.success(adminScheduledJobService.catalog()))
    }

    @GetMapping("/api/v1/admin/scheduled-jobs/summary")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getSummary(
        @RequestParam(defaultValue = "24") windowHours: Long,
    ): ResponseEntity<ApiResponse<ScheduledJobSummaryResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminScheduledJobService.summary(windowHours)))
    }

    /**
     * ORORA 월매출 적재를 특정 월로 수동 실행한다 (`salesMonth` 미지정 시 전월).
     * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     */
    @PostMapping("/api/v1/admin/scheduled-jobs/orora-monthly/trigger")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun triggerOroraMonthly(
        @RequestBody(required = false) request: OroraMonthlyMaterializeTriggerRequest?,
    ): ResponseEntity<ApiResponse<OroraMonthlyMaterializeTriggerResponse>> {
        val response = adminScheduledJobService.triggerOroraMonthly(request?.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
