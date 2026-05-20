package com.otoki.powersales.sfmigration.stage1

import jakarta.validation.constraints.NotBlank

/**
 * S3 의 SF export CSV 1개를 backend 가 직접 COPY 적재.
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
