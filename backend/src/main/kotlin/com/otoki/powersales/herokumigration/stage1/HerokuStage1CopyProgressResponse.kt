package com.otoki.powersales.herokumigration.stage1

import java.time.Instant

/**
 * Heroku Stage 1 S3 → COPY 적재 진행 상태 응답 — UI polling 용.
 *
 * SF [com.otoki.powersales.sfmigration.stage1.Stage1CopyProgressResponse] 와 동형 + Heroku
 * 고유 `unmatchedRows` (EmployeeInfo 적재 시점 PK resolve 미매칭 수).
 */
data class HerokuStage1CopyProgressResponse(
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
    val unmatchedRows: Long,
    val errors: List<String>,
    val entityResults: List<HerokuStage1EntityResultResponse>,
)

data class HerokuStage1EntityResultResponse(
    val targetName: String,
    val status: String,
    val s3Key: String?,
    val processedRows: Long,
    val filteredOut: Long,
    val insertedRows: Long,
    val unmatchedRows: Long,
    val errorMessage: String?,
    val startedAt: Instant?,
    val finishedAt: Instant?,
)

/**
 * Stage1 적재 폼 기본값 — UI 프리필 + 사용자 확인용.
 *
 * @param s3Bucket    운영 S3 bucket (S3_BUCKET 환경 속성). 미설정 시 빈 문자열.
 * @param s3KeyPrefix CSV 공통 prefix (예: "heroku-migration/input").
 */
data class HerokuStage1Defaults(
    val s3Bucket: String,
    val s3KeyPrefix: String,
)
