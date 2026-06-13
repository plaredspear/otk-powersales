package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.AccountCategoryMaster
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처 카테고리 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapAccountCategoryService]
 * - origin spec: #558 (SAP 거래처 마스터 인바운드) — 어댑터/도메인 분리: #635 P1-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AccountCategoryUpsertCommand>` (UPSERT 키 [AccountCategoryUpsertCommand.accountCode]).
 * 2. 행 단위 검증: 필수값 (`accountCode`, `name`) 누락 시 [AccountCategoryUpsertResult.failures] 에 누적.
 * 3. 외부 호출: [AccountCategoryMasterRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class AccountCategoryUpsertService(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository
) {

    @Transactional
    fun upsert(commands: List<AccountCategoryUpsertCommand>): AccountCategoryUpsertResult {
        val failures = mutableListOf<AccountCategoryUpsertFailedRow>()
        val toSave = mutableListOf<AccountCategoryMaster>()

        commands.forEach { command ->
            val accountCode = command.accountCode?.takeIf { it.isNotBlank() }
            val name = command.name?.takeIf { it.isNotBlank() }
            if (accountCode == null) {
                failures += AccountCategoryUpsertFailedRow(null, "AccountCode 필수")
                return@forEach
            }
            if (name == null) {
                failures += AccountCategoryUpsertFailedRow(accountCode, "Name 필수")
                return@forEach
            }
            val existing = accountCategoryMasterRepository.findByAccountCode(accountCode)
            val entity = if (existing == null) {
                AccountCategoryMaster(accountCode = accountCode, name = name)
            } else {
                existing.name = name
                existing
            }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            accountCategoryMasterRepository.saveAll(toSave)
        }

        return AccountCategoryUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }
}
