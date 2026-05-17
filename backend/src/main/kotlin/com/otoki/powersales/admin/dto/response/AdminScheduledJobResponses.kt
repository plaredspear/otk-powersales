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
