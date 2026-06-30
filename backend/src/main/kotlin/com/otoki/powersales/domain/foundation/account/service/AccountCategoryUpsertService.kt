package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.domain.foundation.account.service.dto.AccountCategoryUpsertFailedRow
import com.otoki.powersales.domain.foundation.account.service.dto.AccountCategoryUpsertResult
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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
 * 2. 행 단위 적재: 레거시 `Database.upsert(list, AccountCode__c, false)` 정합 — `AccountCode__c`/`Name`
 *    모두 SF `nillable=true` (AccountCode__c `required=false`) 라 **명시 필수 검증을 두지 않고 raw 적재**한다.
 *    blank 는 null 로 정규화. 단 `accountCode` 는 SF external key 이자 신규 `account_code` UNIQUE 제약이므로,
 *    행마다 별도 트랜잭션([Propagation.REQUIRES_NEW])에서 flush 해 UNIQUE 충돌이 나면 그 행만 failure 로
 *    격리한다 (레거시 `allOrNone=false` 동등 — 한 행 실패가 다른 행 적재를 막지 않음).
 * 3. 외부 호출: [AccountCategoryRowUpsertService.persistRow] (행 단위 saveAndFlush).
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class AccountCategoryUpsertService(
    private val rowUpsertService: AccountCategoryRowUpsertService
) {

    fun upsert(commands: List<AccountCategoryUpsertCommand>): AccountCategoryUpsertResult {
        val failures = mutableListOf<AccountCategoryUpsertFailedRow>()
        var successCount = 0

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_AccountMaster 정합 — AccountCode/Name 명시 검증 없이 raw 적재.
            val accountCode = command.accountCode?.takeIf { it.isNotBlank() }
            val name = command.name?.takeIf { it.isNotBlank() }
            try {
                rowUpsertService.persistRow(accountCode, name)
                successCount++
            } catch (ex: DataIntegrityViolationException) {
                failures += AccountCategoryUpsertFailedRow(accountCode, "적재 실패: ${ex.mostSpecificCauseText()}")
            }
        }

        return AccountCategoryUpsertResult(
            successCount = successCount,
            failureCount = failures.size,
            failures = failures
        )
    }
}

/**
 * [AccountCategoryUpsertService] 의 행 단위 트랜잭션 경계 빈.
 *
 * 한 요청 안의 여러 행 중 `account_code` UNIQUE 충돌이 난 행만 격리하기 위해 행마다 [Propagation.REQUIRES_NEW]
 * 로 별도 트랜잭션을 연다 (같은 빈 내 self-invocation 은 Spring AOP 프록시를 우회해 트랜잭션 경계가 적용되지
 * 않으므로 별도 빈으로 분리). 충돌 시 본 트랜잭션만 롤백되고 호출 루프는 다음 행을 계속 처리한다.
 */
@Service
class AccountCategoryRowUpsertService(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun persistRow(accountCode: String?, name: String?) {
        val existing = accountCode?.let { accountCategoryMasterRepository.findByAccountCode(it) }
        val entity = if (existing == null) {
            AccountCategoryMaster(accountCode = accountCode, name = name)
        } else {
            existing.name = name
            existing
        }
        accountCategoryMasterRepository.saveAndFlush(entity)
    }
}

private fun Throwable.mostSpecificCauseText(): String {
    var cause: Throwable = this
    while (cause.cause != null && cause.cause !== cause) cause = cause.cause!!
    return cause.message ?: cause::class.simpleName ?: "unknown"
}
