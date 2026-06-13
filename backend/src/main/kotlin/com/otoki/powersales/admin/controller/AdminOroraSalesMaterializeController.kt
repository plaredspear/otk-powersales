package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.batch.OroraDailySalesMaterializeBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.sales.dto.request.OroraSalesMaterializeRequest
import com.otoki.powersales.domain.sales.materialize.OroraDailyMaterializeResult
import com.otoki.powersales.domain.sales.materialize.OroraMonthlyMaterializeResult
import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * ORORA 매출이력 수동 적재 트리거 admin API (Spec #855).
 *
 * 평상시 적재는 스케줄 배치([OroraDailySalesMaterializeBatch] / [OroraMonthlySalesMaterializeBatch]) 가
 * 수행하며, 본 API 는 특정 월 소급 재적재 등 운영자 수동 트리거용 (레거시의 mdt 수동 지정 유연성 대체, Q2).
 *
 * 수동 트리거도 배치와 **동일한 JOB_NAME** 으로 [ScheduledJobRunner] 로 감싸 `scheduled_job_run` 실행
 * 이력에 기록한다 — 개발자 도구 > 스케줄 잡 페이지에서 배치/수동 실행이 한 잡명으로 함께 조회된다.
 * `@SchedulerLock` (분산 락) 은 부착하지 않는다 (운영자 단발 호출 — 동시 중복 실행은 운영자 책임).
 * metadata 에 `trigger=manual` 을 부가해 스케줄 fire 와 구분 가능하게 한다.
 *
 * 권한: 매출/실적 도메인(`monthly_sales_history`) 의 EDIT (upsert = 생성+갱신).
 */
@RestController
@RequestMapping("/api/v1/admin/sales/materialize")
@PermissionResource("monthly_sales_history")
class AdminOroraSalesMaterializeController(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Operation(summary = "ORORA 월별 마감 적재 (수동 트리거)")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.EDIT)
    @PostMapping("/monthly")
    fun materializeMonthly(
        @Valid @RequestBody(required = false) request: OroraSalesMaterializeRequest?,
    ): ResponseEntity<ApiResponse<OroraMonthlyMaterializeResult>> {
        val result = scheduledJobRunner.run(OroraMonthlySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeMonthly(request?.salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual",
                    "salesMonth" to r.salesMonth,
                    "fetchedCount" to r.fetchedCount,
                    "upsertedCount" to r.upsertedCount,
                    "skippedAccountUnmatchedCount" to r.skippedAccountUnmatchedCount,
                )
            )
            r
        }
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @Operation(summary = "ORORA 일별 매출 적재 + 월별 합계 갱신 (수동 트리거)")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.EDIT)
    @PostMapping("/daily")
    fun materializeDaily(
        @Valid @RequestBody(required = false) request: OroraSalesMaterializeRequest?,
    ): ResponseEntity<ApiResponse<OroraDailyMaterializeResult>> {
        val result = scheduledJobRunner.run(OroraDailySalesMaterializeBatch.JOB_NAME) { ctx ->
            val r = facade.materializeDaily(request?.salesMonth)
            ctx.metadata(
                mapOf(
                    "trigger" to "manual",
                    "salesMonth" to r.salesMonth,
                    "dailyUpsertedCount" to r.dailyUpsertedCount,
                    "monthlyAggregateUpdatedCount" to r.monthlyAggregateUpdatedCount,
                )
            )
            r
        }
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
