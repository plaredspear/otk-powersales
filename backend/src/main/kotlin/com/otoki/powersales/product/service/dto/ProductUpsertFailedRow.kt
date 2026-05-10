package com.otoki.powersales.product.service.dto

/**
 * 제품 UPSERT 실패 행. 도메인 결과 [ProductUpsertResult.failures] 의 원소.
 *
 * - [identifier] : 식별자 (현재 채택은 productCode 값)
 * - [reason] : 실패 사유 (예: `"StandardPrice 변환 실패: abc"`, `"LaunchDate 형식 오류: 2020-01-01"`)
 */
data class ProductUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
