package com.otoki.powersales.account.service.dto

/**
 * 거래처 UPSERT 도메인 결과.
 *
 * 부분 실패 시멘틱 — 행 단위 검증 실패는 트랜잭션 롤백 없이 [failures] 누적 후 성공 행만 saveAll. 도메인 자체에서
 * throw 하는 경우는 [com.otoki.powersales.account.service.AccountUpsertService] 의 `@Transactional` 로 전체 롤백.
 */
data class AccountUpsertResult(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<AccountUpsertFailedRow>
)
