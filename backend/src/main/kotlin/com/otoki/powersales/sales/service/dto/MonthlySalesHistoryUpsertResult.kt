package com.otoki.powersales.sales.service.dto

/**
 * 월 매출 이력 UPSERT 도메인 결과 (단일 청크 단위).
 *
 * 부분 실패 시멘틱 — 행 단위 검증 실패는 트랜잭션 롤백 없이 [failures] 누적 후 성공 행만 saveAll.
 * 청크 분할 / 청크 commit 실패 분기는 어댑터 책임.
 */
data class MonthlySalesHistoryUpsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<MonthlySalesHistoryUpsertFailedRow>
)
