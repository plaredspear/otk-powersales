package com.otoki.powersales._migration.sf.dto

import java.time.Instant

/**
 * Stage 2-A FK Resolve 진행 상태 응답 — UI polling 용.
 *
 * status: IDLE (아직 한 번도 안 돌림) / RUNNING (진행 중) / COMPLETED (정상 종료) / FAILED (예외 종료)
 */
data class SfFkResolveProgressResponse(
    val status: String,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val totalTables: Int,
    val completedTables: Int,
    val currentTable: String?,
    val currentTableChunk: Int,
    val currentTableTotalChunks: Int,
    val totalRowsAffected: Long,
    val tableResults: List<SubstepResult>,
    val errors: List<String>,
)
