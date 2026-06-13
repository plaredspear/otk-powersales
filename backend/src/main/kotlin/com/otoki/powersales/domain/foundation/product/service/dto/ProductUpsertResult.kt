package com.otoki.powersales.domain.foundation.product.service.dto

/**
 * 제품 UPSERT 도메인 결과.
 *
 * 부분 실패 시멘틱 — 행 단위 검증 실패는 트랜잭션 롤백 없이 [failures] 누적 후 성공 행만 saveAll.
 */
data class ProductUpsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ProductUpsertFailedRow>
)
