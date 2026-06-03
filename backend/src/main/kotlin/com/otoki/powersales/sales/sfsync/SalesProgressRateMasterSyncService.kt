package com.otoki.powersales.sales.sfsync

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.sales.repository.SalesProgressRateMasterRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SF 거래처목표등록마스터(`SalesProgressRateMaster__c`) fetch 결과를 신규 DB 에 upsert 하는 서비스.
 *
 * - **매칭 키**: ExternalKey(`연+월+거래처코드`). [SalesProgressRateMasterFetchDto.externalKey] 가
 *   비어 있으면 `targetYear + targetMonth + accountCode` 로 재조합 (SF Trigger 동등, month leftPad 없음).
 * - 기존 row 존재 → 목표/실적/영업일진도율/지점 컬럼 UPDATE. 없으면 INSERT.
 * - 거래처(account FK)는 거래처코드(Account.externalKey) 로 resolve. INSERT 시 미매칭이면 account
 *   미연결로 적재(목표 데이터 자체는 보존), UPDATE 시 미매칭이면 기존 FK 를 보존한다.
 * - **삭제 미반영**: SF fetch 결과에 없는 기존 row 는 건드리지 않는다 (upsert only).
 *
 * ## sync 비대상 컬럼
 * `accountCdUpl`(엑셀 업로드 임시), owner/createdBy/lastModifiedBy 계열 audit FK 는 SF fetch 입력
 * ([SalesProgressRateMasterFetchDto])에 포함하지 않는다 — 이들은 SF→RDS 마이그레이션(Stage1) 권위이며
 * 주기 sync 의 책임 밖이다.
 *
 * ## 충돌 처리 (FetchClient 구현 전 TODO)
 * `external_key` 는 unique 제약. 동일 사이클 내 중복은 [LinkedHashMap] dedupe 로 방어하나, 본 sync 와
 * 다른 적재 경로(Stage1 / admin)가 동시에 같은 ExternalKey 를 INSERT 하면 `saveAll` commit 시
 * `DataIntegrityViolationException` 으로 **해당 트랜잭션 전량 롤백**된다. FetchClient 가 실데이터를
 * 반환하기 전에 건별 격리(개별 트랜잭션) 또는 `ON CONFLICT` upsert 전략을 확정할 것.
 */
@Service
class SalesProgressRateMasterSyncService(
    private val fetchClient: SalesProgressRateMasterFetchClient,
    private val repository: SalesProgressRateMasterRepository,
    private val accountRepository: AccountRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** SF fetch → upsert 전체 경로 (배치 진입점). */
    @Transactional
    fun sync(context: ScheduledJobRunContext? = null): SyncResult {
        val fetched = fetchClient.fetch()
        return syncRecords(fetched, context)
    }

    /**
     * fetch 결과 리스트를 받아 upsert 수행 (핵심 경로 — 단위 테스트에서 직접 호출).
     *
     * @return upsert 통계
     */
    @Transactional
    fun syncRecords(
        fetched: List<SalesProgressRateMasterFetchDto>,
        context: ScheduledJobRunContext? = null,
    ): SyncResult {
        if (fetched.isEmpty()) {
            context?.metadata(mapOf("fetched" to 0, "inserted" to 0, "updated" to 0, "skipped" to 0))
            return SyncResult(fetched = 0, inserted = 0, updated = 0, skipped = 0)
        }

        // 1) 각 DTO 의 ExternalKey 확정 (없으면 재조합). 키 산출 불가 row 는 skip.
        val keyed = fetched.mapNotNull { dto ->
            val key = resolveExternalKey(dto)
            if (key == null) {
                log.warn("[sales-progress-sync] ExternalKey 산출 불가 — skip. sfid={}", dto.sfid)
                null
            } else {
                key to dto
            }
        }
        val skippedNoKey = fetched.size - keyed.size

        // 2) 거래처코드 → Account 일괄 resolve (N+1 회피).
        val accountCodes = keyed.mapNotNull { it.second.accountCode }.filter { it.isNotBlank() }.distinct()
        val accountByCode: Map<String, Account> =
            if (accountCodes.isEmpty()) emptyMap()
            else accountRepository.findByExternalKeyIn(accountCodes)
                .filter { it.externalKey != null }
                .associateBy { it.externalKey!! }

        // 3) 기존 row 일괄 조회 (ExternalKey IN).
        val externalKeys = keyed.map { it.first }.distinct()
        val existingByKey: Map<String, SalesProgressRateMaster> =
            repository.findByExternalKeyIn(externalKeys)
                .filter { it.externalKey != null }
                .associateBy { it.externalKey!! }

        var inserted = 0
        var updated = 0
        val toSave = mutableListOf<SalesProgressRateMaster>()

        // 동일 ExternalKey 가 fetch 응답에 중복으로 오는 경우 마지막 값이 이기도록 LinkedHashMap 으로 dedupe.
        val deduped = LinkedHashMap<String, SalesProgressRateMasterFetchDto>()
        keyed.forEach { (key, dto) -> deduped[key] = dto }

        for ((key, dto) in deduped) {
            val account = dto.accountCode?.let { accountByCode[it] }
            val existing = existingByKey[key]
            if (existing != null) {
                applyTo(existing, dto, key, account)
                toSave += existing
                updated++
            } else {
                toSave += newEntity(dto, key, account)
                inserted++
            }
        }

        repository.saveAll(toSave)

        val result = SyncResult(
            fetched = fetched.size,
            inserted = inserted,
            updated = updated,
            skipped = skippedNoKey,
        )
        context?.metadata(
            mapOf(
                "fetched" to result.fetched,
                "inserted" to result.inserted,
                "updated" to result.updated,
                "skipped" to result.skipped,
            )
        )
        log.info(
            "[sales-progress-sync] 완료 — fetched={}, inserted={}, updated={}, skipped={}",
            result.fetched, result.inserted, result.updated, result.skipped,
        )
        return result
    }

    /** ExternalKey 확정 — DTO 값 우선, 비어 있으면 `연+월+거래처코드` 재조합 (SF Trigger 동등). */
    private fun resolveExternalKey(dto: SalesProgressRateMasterFetchDto): String? {
        dto.externalKey?.takeIf { it.isNotBlank() }?.let { return it }
        val year = dto.targetYear?.takeIf { it.isNotBlank() } ?: return null
        val month = dto.targetMonth?.takeIf { it.isNotBlank() } ?: return null
        val accountCode = dto.accountCode?.takeIf { it.isNotBlank() } ?: return null
        return "$year$month$accountCode"
    }

    /** 신규 INSERT 엔티티 생성. */
    private fun newEntity(
        dto: SalesProgressRateMasterFetchDto,
        externalKey: String,
        account: Account?,
    ): SalesProgressRateMaster = SalesProgressRateMaster(
        sfid = dto.sfid,
        name = dto.name,
        externalKey = externalKey,
        targetYear = dto.targetYear,
        targetMonth = dto.targetMonth,
        rtTargetAmount = dto.rtTargetAmount,
        frTargetAmount = dto.frTargetAmount,
        rmTargetAmount = dto.rmTargetAmount,
        foTargetAmount = dto.foTargetAmount,
        targetSumAmount = dto.targetSumAmount,
        currentMonthSalesAmount = dto.currentMonthSalesAmount,
        previousMonthSalesAmount = dto.previousMonthSalesAmount,
        businessRate = dto.businessRate,
        accountBranchView = dto.accountBranchView,
        accountBranchCode = dto.accountBranchCode,
        accountSfid = account?.sfid,
        isDeleted = dto.isDeleted,
    ).also { it.account = account }

    /** 기존 row UPDATE — Name/sfid/ExternalKey 는 키 성격이라 갱신 대상에서 제외. */
    private fun applyTo(
        entity: SalesProgressRateMaster,
        dto: SalesProgressRateMasterFetchDto,
        externalKey: String,
        account: Account?,
    ) {
        entity.targetYear = dto.targetYear
        entity.targetMonth = dto.targetMonth
        entity.rtTargetAmount = dto.rtTargetAmount
        entity.frTargetAmount = dto.frTargetAmount
        entity.rmTargetAmount = dto.rmTargetAmount
        entity.foTargetAmount = dto.foTargetAmount
        entity.targetSumAmount = dto.targetSumAmount
        entity.currentMonthSalesAmount = dto.currentMonthSalesAmount
        entity.previousMonthSalesAmount = dto.previousMonthSalesAmount
        entity.businessRate = dto.businessRate
        entity.accountBranchView = dto.accountBranchView
        entity.accountBranchCode = dto.accountBranchCode
        // 거래처가 resolve 된 경우에만 연결 갱신 — 미매칭(account=null) 시 기존 FK 를 보존한다.
        // (Account 적재가 본 sync 보다 지연되는 경우 등에서 매 사이클 기존 연결을 끊지 않도록.)
        account?.let {
            entity.account = it
            it.sfid?.let { sfid -> entity.accountSfid = sfid }
        }
    }

    data class SyncResult(
        val fetched: Int,
        val inserted: Int,
        val updated: Int,
        val skipped: Int,
    )
}
