package com.otoki.powersales.herokumigration.controller

import java.time.Instant

/**
 * Heroku Stage 2 FK Resolve 진행 상태 응답 — UI polling 용.
 *
 * SF `SfFkResolveProgressResponse` 와 동형 + Heroku 고유 `unmatched` (자연 키 매칭 실패 집계).
 */
data class HerokuFkResolveProgressResponse(
    val status: String,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val totalTables: Int,
    val completedTables: Int,
    val currentTable: String?,
    val totalRowsAffected: Long,
    val tableResults: List<HerokuFkTableResult>,
    val unmatched: List<HerokuFkUnmatched>,
    val errors: List<String>,
)

data class HerokuFkTableResult(
    val table: String,
    val column: String,
    val rowsAffected: Long,
)

data class HerokuFkUnmatched(
    val table: String,
    val column: String,
    val naturalKey: String,
    val unmatchedCount: Long,
)
