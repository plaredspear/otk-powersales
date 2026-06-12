package com.otoki.powersales.admin.dto.response

import java.time.LocalDateTime

data class ScheduledJobRunDto(
    val id: Long,
    val jobName: String,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val durationMs: Long?,
    val status: String,
    val errorMessage: String?,
    val metadata: String?,
)

data class ScheduledJobRunListResponse(
    val items: List<ScheduledJobRunDto>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)

data class ScheduledJobSummaryResponse(
    val windowFrom: LocalDateTime,
    val windowTo: LocalDateTime,
    val totalCount: Long,
    val runningCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val distinctJobNames: List<String>,
)

data class RegisteredScheduledJobDto(
    val jobName: String,
    val cron: String,
    val description: String,
)

/**
 * ORORA 월매출 수동 적재 트리거 결과.
 *
 * @property salesMonth 적재한 대상 매출월 (`YYYYMM`)
 * @property fetchedCount ORORA view 에서 조회된 row 수
 * @property upsertedCount RDS 에 적재(신규+갱신)된 row 수
 * @property skippedAccountUnmatchedCount account 미매칭으로 account_id=null 적재된 row 수
 */
data class OroraMonthlyMaterializeTriggerResponse(
    val salesMonth: String,
    val fetchedCount: Int,
    val upsertedCount: Int,
    val skippedAccountUnmatchedCount: Int,
)
