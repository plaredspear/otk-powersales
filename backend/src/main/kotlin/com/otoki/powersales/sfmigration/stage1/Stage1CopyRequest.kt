package com.otoki.powersales.sfmigration.stage1

import jakarta.validation.constraints.NotBlank

/**
 * S3 의 SF export CSV 1개를 backend 가 직접 COPY 적재 (SINGLE 모드).
 *
 * @param targetName Stage1Targets.list() 중 하나 (예: "ErpOrderProduct")
 * @param s3Bucket   CSV 보관 bucket
 * @param s3Key      CSV 의 S3 key (예: "sf-migration/input/erp_order_products.csv")
 */
data class Stage1CopyRequest(
    @field:NotBlank val targetName: String,
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3Key: String,
)

/**
 * S3 prefix 아래 등록된 모든 entity 의 CSV 를 의존성 순서대로 일괄 적재 (BATCH 모드).
 *
 * 각 entity 의 S3 key 는 `<s3KeyPrefix>/<EntityMetadata.csvFileName>` 으로 자동 조립.
 *
 * @param s3Bucket    CSV 보관 bucket
 * @param s3KeyPrefix CSV 들의 공통 prefix (예: "sf-migration/input")
 */
data class Stage1CopyAllRequest(
    @field:NotBlank val s3Bucket: String,
    @field:NotBlank val s3KeyPrefix: String,
)
