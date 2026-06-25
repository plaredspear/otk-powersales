package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.request.OroraDailyMaterializeChunkTriggerRequest
import com.otoki.powersales.admin.dto.request.OroraDailyMaterializeTriggerRequest
import com.otoki.powersales.admin.dto.request.OroraMonthlyMaterializeChunkTriggerRequest
import com.otoki.powersales.admin.dto.request.OroraMonthlyMaterializeTriggerRequest
import com.otoki.powersales.admin.dto.response.OroraDailyChunkCatalogResponse
import com.otoki.powersales.admin.dto.response.OroraDailyMaterializeTriggerResponse
import com.otoki.powersales.admin.dto.response.OroraMonthlyChunkCatalogResponse
import com.otoki.powersales.admin.dto.response.OroraMonthlyMaterializeTriggerResponse
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobManualTriggerResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.admin.service.AdminScheduledJobService
import com.otoki.powersales.admin.service.PPTMasterManualTriggerService
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
    private val pptMasterManualTriggerService: PPTMasterManualTriggerService,
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

    /**
     * ORORA 월매출 거래처 청크 메타(전체 청크 수 + 각 청크 거래처 경계) 조회.
     *
     * 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공한다. 정적 거래처 범위에서
     * 산출되므로 ORORA 호출 없이 즉시 반환. 조회 권한(`VIEW_ALL_DATA`) 이면 충분하다.
     */
    @GetMapping("/api/v1/admin/scheduled-jobs/orora-monthly/chunks")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getOroraMonthlyChunks(): ResponseEntity<ApiResponse<OroraMonthlyChunkCatalogResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminScheduledJobService.ororaMonthlyChunkCatalog()))
    }

    /**
     * ORORA 월매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다.
     *
     * 전체 범위를 도는 [triggerOroraMonthly] 와 달리 선택 청크의 거래처 구간만 적재한다. 스케줄 배치와
     * 동일 잡명으로 이력에 남는다. 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     */
    @PostMapping("/api/v1/admin/scheduled-jobs/orora-monthly/chunk/trigger")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun triggerOroraMonthlyChunk(
        @RequestBody request: OroraMonthlyMaterializeChunkTriggerRequest,
    ): ResponseEntity<ApiResponse<OroraMonthlyMaterializeTriggerResponse>> {
        val response = adminScheduledJobService.triggerOroraMonthlyChunk(request.chunkIndex, request.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * ORORA 일매출 적재를 특정 월로 수동 실행한다 (`salesMonth` 미지정 시 당월).
     * 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     */
    @PostMapping("/api/v1/admin/scheduled-jobs/orora-daily/trigger")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun triggerOroraDaily(
        @RequestBody(required = false) request: OroraDailyMaterializeTriggerRequest?,
    ): ResponseEntity<ApiResponse<OroraDailyMaterializeTriggerResponse>> {
        val response = adminScheduledJobService.triggerOroraDaily(request?.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * ORORA 일매출 거래처 청크 메타(전체 청크 수 + 각 청크 거래처 경계) 조회.
     *
     * 수동 트리거 UI 가 "전체 N개 중 몇 번째 청크" 를 선택하도록 제공한다. 정적 거래처 범위에서
     * 산출되므로 ORORA 호출 없이 즉시 반환. 조회 권한(`VIEW_ALL_DATA`) 이면 충분하다.
     */
    @GetMapping("/api/v1/admin/scheduled-jobs/orora-daily/chunks")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun getOroraDailyChunks(): ResponseEntity<ApiResponse<OroraDailyChunkCatalogResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminScheduledJobService.ororaDailyChunkCatalog()))
    }

    /**
     * ORORA 일매출 적재를 거래처 청크 1개(`chunkIndex`, 0-based) 만 대상으로 수동 실행한다.
     *
     * 전체 범위를 도는 [triggerOroraDaily] 와 달리 선택 청크의 거래처 구간만 적재한다. 스케줄 배치와
     * 동일 잡명으로 이력에 남는다. 외부 ORORA 호출 + RDS upsert 라 `MODIFY_ALL_DATA` 권한 필요.
     */
    @PostMapping("/api/v1/admin/scheduled-jobs/orora-daily/chunk/trigger")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun triggerOroraDailyChunk(
        @RequestBody request: OroraDailyMaterializeChunkTriggerRequest,
    ): ResponseEntity<ApiResponse<OroraDailyMaterializeTriggerResponse>> {
        val response = adminScheduledJobService.triggerOroraDailyChunk(request.chunkIndex, request.salesMonth)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 전문행사조(PPT) 마스터 배치를 수동 실행한다.
     *
     * [action] `expire` = "금일 전문행사조 마감"(`pptMaster.expire`), `sync` = "금일 전문행사조 반영"
     * (`pptMaster.syncValid`). jobName 의 `.` 이 path 확장자로 오인되지 않도록 짧은 action 세그먼트로 받는다.
     *
     * 자동 스케줄과 동일하게 이력이 남으므로 실행 후 화면 이력 탭에서 결과를 조회할 수 있다.
     * 사원 행사조 소속을 변경하는 쓰기 작업이라 `MODIFY_ALL_DATA` 권한 필요.
     */
    @PostMapping("/api/v1/admin/scheduled-jobs/ppt-master/{action}/trigger")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun triggerPptMaster(
        @PathVariable action: String,
    ): ResponseEntity<ApiResponse<ScheduledJobManualTriggerResponse>> {
        val jobName = when (action) {
            "expire" -> com.otoki.powersales.platform.batch.PPTMasterExpireBatch.JOB_NAME
            "sync" -> com.otoki.powersales.platform.batch.PPTMasterSyncBatch.JOB_NAME
            else -> throw IllegalArgumentException("지원하지 않는 PPT 배치 action 입니다: $action (expire | sync)")
        }
        val response = pptMasterManualTriggerService.trigger(jobName)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
