package com.otoki.powersales._migration.heroku.stage1

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * Heroku Stage 1 — S3 의 export CSV 1개를 backend 가 직접 COPY 적재 (SINGLE 모드).
 *
 * 파일명은 입력하지 않는다 — target 의 `HerokuEntityMeta.csvFileName` 으로 backend 가
 * `<s3KeyPrefix>/<csvFileName>` 을 자동 조립 (BATCH 모드와 대칭). 매핑 SoT 는 [HerokuStage1Targets].
 *
 * @param targetName  HerokuStage1Targets.list() 중 하나 (예: "TmpOrder")
 * @param s3Bucket    CSV 보관 bucket
 * @param s3KeyPrefix CSV 가 위치한 공통 prefix (예: "heroku-migration/input")
 * @param reset       true 면 적재 전 대상 테이블 TRUNCATE (스펙 Q1 옵션 1, 기본 true)
 * @param maxRows     sample 적재 상한 (CSV row 기준). null=전체 적재.
 */
data class HerokuStage1CopyRequest(
    @field:NotBlank val targetName: String,
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3KeyPrefix: String,
    val reset: Boolean = true,
    @field:Positive val maxRows: Int? = null,
)

/**
 * Heroku Stage 1 — S3 prefix 아래 등록된 모든 entity 의 CSV 를 의존성 순서대로 일괄 적재 (BATCH 모드).
 *
 * 각 entity 의 S3 key 는 `<s3KeyPrefix>/<HerokuEntityMeta.csvFileName>` 으로 자동 조립.
 *
 * @param s3Bucket    CSV 보관 bucket
 * @param s3KeyPrefix CSV 들의 공통 prefix (예: "heroku-migration/input")
 * @param reset       true 면 각 entity 적재 전 TRUNCATE (기본 true)
 * @param maxRows     entity 별 sample 적재 상한 (CSV row 기준). null=전체 적재.
 */
data class HerokuStage1CopyAllRequest(
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3KeyPrefix: String,
    val reset: Boolean = true,
    @field:Positive val maxRows: Int? = null,
)
