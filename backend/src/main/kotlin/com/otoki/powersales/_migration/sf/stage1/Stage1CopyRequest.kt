package com.otoki.powersales._migration.sf.stage1

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * S3 의 SF export CSV 1개를 backend 가 직접 COPY 적재 (SINGLE 모드).
 *
 * 파일명은 입력하지 않는다 — target 의 `EntityMetadata.csvFileName` 으로 backend 가
 * `<s3KeyPrefix>/<csvFileName>` 을 자동 조립 (BATCH 모드와 대칭). 매핑 SoT 는 [Stage1Targets].
 *
 * @param targetName  Stage1Targets.list() 중 하나 (예: "ErpOrderProduct")
 * @param s3Bucket    CSV 보관 bucket
 * @param s3KeyPrefix CSV 가 위치한 공통 prefix (예: "sf-migration/input")
 * @param maxRows     sample 적재 상한 (CSV row 기준 — filterOut 포함). null=전체 적재.
 */
data class Stage1CopyRequest(
    @field:NotBlank val targetName: String,
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3KeyPrefix: String,
    @field:Positive val maxRows: Int? = null,
)

/**
 * S3 prefix 아래 등록된 모든 entity 의 CSV 를 의존성 순서대로 일괄 적재 (BATCH 모드).
 *
 * 각 entity 의 S3 key 는 `<s3KeyPrefix>/<EntityMetadata.csvFileName>` 으로 자동 조립.
 *
 * @param s3Bucket    CSV 보관 bucket
 * @param s3KeyPrefix CSV 들의 공통 prefix (예: "sf-migration/input")
 * @param maxRows     entity 별 sample 적재 상한 (CSV row 기준). null=전체 적재.
 */
data class Stage1CopyAllRequest(
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3KeyPrefix: String,
    @field:Positive val maxRows: Int? = null,
)
