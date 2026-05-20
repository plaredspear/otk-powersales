package com.otoki.powersales.sfmigration.stage1

import java.time.Instant

/**
 * Stage 1 S3 → COPY 적재 진행 상태 응답 — UI polling 용.
 *
 * status: IDLE (아직 한 번도 안 돌림) / RUNNING (진행 중) / COMPLETED (정상 종료) / FAILED (예외 종료)
 *
 * 진행 단위: processedRows 누적 (CSV streaming 이라 totalRows 사전 미상). UI 는
 * processedRows 만 표시하거나 표시 후 종료 시 inserted/filteredOut 으로 갱신.
 */
data class Stage1CopyProgressResponse(
    val status: String,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val targetName: String?,
    val s3Bucket: String?,
    val s3Key: String?,
    val processedRows: Long,
    val filteredOut: Long,
    val insertedRows: Long,
    val errors: List<String>,
)
