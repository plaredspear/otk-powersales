package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertFailedRow
import com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertResult
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.common.config.CacheConfig
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapClientMasterService]
 * - origin spec: #558 (SAP 거래처 마스터 인바운드) — 본 도메인 분리 PoC: #634
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AccountUpsertCommand>` (외부 키 [AccountUpsertCommand.externalKey] = SAP 거래처 코드).
 * 2. 캐시 빌드: [AccountRepository.findByExternalKeyIn] / [EmployeeRepository.findByEmployeeCodeIn] / [OrganizationLookup.build].
 * 3. 행 단위 검증 (try/catch): 필수값(`externalKey`/`name`) →
 *    [OrganizationLookup.match] 폴백 lookup → [AccountUpsertMapper] 로 신규 생성 또는 기존 갱신.
 *    `consignmentAcc` 는 레거시 정합으로 화이트리스트 검증 없이 raw 적재한다.
 *    `employeeCode` 는 미매칭이어도 owner=null 로 silent 저장 (레거시 정합 — row failure 아님).
 * 4. 외부 호출: [AccountRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 [AccountUpsertResult.failures] 누적, 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class AccountUpsertService(
    private val accountRepository: AccountRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val mapper: AccountUpsertMapper
) {

    /**
     * SAP inbound 거래처 마스터 upsert.
     *
     * 적재 직후 [CacheConfig.CACHE_MONTHLY_INTEGRATION_FILTER_OPTIONS] 를 무효화한다 —
     * 월별여사원 통합일정 조회조건 드롭다운(유통형태/거래처유형)의 원천이 Account 코드 분류이므로,
     * 거래처 마스터가 적재되면 옵션 목록이 stale 가능하다. `allEntries = true` (고정 key 단건이라 무해).
     *
     * 본 서비스는 행 단위 부분 성공 모델(검증 실패 행은 누적, 트랜잭션 롤백 안 함)이라
     * OrganizationReplaceService 의 all-or-nothing 교체와 실패 시맨틱이 다르다.
     * `beforeInvocation = false` (기본) — 커밋 후 evict:
     *  - 성공 행이 1건이라도 커밋되면 옵션 집합이 바뀔 수 있어 그 시점에 전체 evict.
     *  - saveAll 예외 등으로 트랜잭션 전체가 롤백될 때만 evict 스킵되어 기존 캐시 보존.
     * evict 누락 케이스도 24h TTL fallback + 다음 daily sync 로 자연 수렴.
     * NoOp profile (test / local) 에서는 무동작.
     */
    @CacheEvict(value = [CacheConfig.CACHE_MONTHLY_INTEGRATION_FILTER_OPTIONS], allEntries = true)
    @Transactional
    fun upsert(commands: List<AccountUpsertCommand>): AccountUpsertResult {
        val externalKeys = commands.mapNotNull { it.externalKey?.takeIf { key -> key.isNotBlank() } }
        val accountCache = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            accountRepository.findByExternalKeyIn(externalKeys.distinct())
                .associateBy { it.externalKey!! }
                .toMutableMap()
        }

        val employeeCodes = commands.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }

        // Spec #758: owner FK 가 User 로 전환됨. owner 적재는 User.employee_code 매칭 결과를 사용한다.
        // 미매칭 시 owner=null (레거시 silent 동작) — 거래처 적재 자체는 막지 않는다.
        val userByEmployeeCode: Map<String, User> = if (employeeCodes.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                // findByEmployeeCodeIn 의 결과는 사번 보유 user 만 — employeeCode non-null 보장.
                .associateBy { it.employeeCode!! }
        }

        val orgLookup = OrganizationLookup.build(organizationRepository)

        val failures = mutableListOf<AccountUpsertFailedRow>()
        val toSave = mutableListOf<Account>()

        commands.forEach { command ->
            try {
                val externalKey = command.externalKey?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException(Reasons.EXTERNAL_KEY_REQUIRED)
                val name = command.name?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException(Reasons.NAME_REQUIRED)

                // 레거시 IF_REST_SAP_ClientMasterReceive 정합 — EmployeeCode 미매칭 시 OwnerId 만 미설정하고
                // 거래처는 그대로 저장한다 (silent). SF userMap.get(EmployeeCode) 가 null 이면 OwnerId set 을
                // skip 하고 진행하는 동작 동등. 담당 사원 마스터가 거래처보다 늦게 도착해도 거래처 유실 없음.
                // (employeeByCode 존재 검증은 row failure 로 막지 않는다 — owner=null 로 수렴.)

                // 레거시 정합 — ConsignmentAcc 는 화이트리스트 검증 없이 raw 그대로 적재한다.
                // (IF_REST_SAP_ClientMasterReceive.cls:127 `acc.ConsignmentAcc__c = obj.ConsignmentAcc` —
                //  Y/N/"" 외 값도 무검증 통과. SAP 가 거래처 마스터 권위 소스이므로 inbound 에서 막지 않는다.)

                val matchedOrg = orgLookup.match(command)
                val matchedUser = command.employeeCode?.let { userByEmployeeCode[it] }
                val account = accountCache[externalKey]?.also { mapper.update(it, name, command, matchedOrg, matchedUser) }
                    ?: mapper.newAccount(externalKey, name, command, matchedOrg, matchedUser)
                        .also { accountCache[externalKey] = it }
                toSave += account
            } catch (ex: IllegalArgumentException) {
                failures += AccountUpsertFailedRow(command.externalKey, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            accountRepository.saveAll(toSave)
        }

        return AccountUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    companion object {
        private object Reasons {
            const val EXTERNAL_KEY_REQUIRED = "SAPAccountCode 필수"
            const val NAME_REQUIRED = "Name 필수"
        }
    }
}
