package com.otoki.powersales.sfmigration.stage1

import java.time.Instant

/**
 * Stage 1 S3 → COPY 적재 진행 상태 응답 — UI polling 용.
 *
 * status: IDLE (아직 한 번도 안 돌림) / RUNNING (진행 중) / COMPLETED (정상 종료) / FAILED (예외 종료)
 * mode: SINGLE (target 1개 단건) / BATCH (전체 일괄)
 *
 * SINGLE 모드: targetName / s3Key 가 채워지고 entityResults 는 비어있다.
 * BATCH 모드: s3Bucket 만 채워지고 (s3Key 는 현재 처리 중 entity 의 key) entityResults 가 entity 별
 *             상태 (PENDING/RUNNING/COMPLETED/FAILED/SKIPPED) 와 실패 사유를 포함.
 *
 * 진행 단위: processedRows 누적 (CSV streaming 이라 totalRows 사전 미상). BATCH 시 누적은 모든
 * entity 합산. UI 는 entityResults 의 각 entity 별 row 카운트를 별도 표시.
 */
data class Stage1CopyProgressResponse(
    val status: String,
    val mode: String,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val targetName: String?,
    val s3Bucket: String?,
    val s3Key: String?,
    val processedRows: Long,
    val filteredOut: Long,
    val insertedRows: Long,
    val errors: List<String>,
    val entityResults: List<Stage1EntityResultResponse>,
)

data class Stage1EntityResultResponse(
    val targetName: String,
    val status: String,
    val s3Key: String?,
    val processedRows: Long,
    val filteredOut: Long,
    val insertedRows: Long,
    val errorMessage: String?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)
