package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * 거래처목표등록마스터의 **당월/전월 매출 실적**을 조회 시점에 월매출이력에서 산출한다.
 *
 * ## 왜 저장값이 아니라 조회 시점 산출인가
 * 실적 2컬럼은 SF 에서도 입력값이 아니라 파생값이었다 — 레거시 `Batch_SalesProgressRate_M` 이
 * `MonthlySalesHistory__c.ClosingAmountSum__c` (전산마감합 + 물류마감합) 를 `거래처 + 연 + 월` 로
 * 매칭해 주기적으로 대입했다. 신규 시스템은 그 원천(`monthly_sales_history`)을 이미 RDS 에 갖고 있고,
 * 모바일 「월 매출」/월매출 대시보드는 진작부터 같은 값을 **조회 시점에 직접 산출**한다. 이 화면만
 * 복제본을 들고 있을 이유가 없어 동일 방식으로 통일했다 (진도율도 원래부터 저장값이 아닌 산출값).
 *
 * 복제본 + 주기 갱신 구조는 실제로 장애를 냈다 — SF sync 가 응답에 없는 실적 필드의 null 을 그대로
 * 대입해 2026-06 이후 당월·전월 실적이 매 사이클 지워졌다. 쓰는 주체를 없애 그 종류의 드리프트 자체를
 * 제거한다.
 *
 * ## 산출 규칙 (레거시 `Batch_SalesProgressRate_M` 정합)
 * - 당월 실적 = 목표행 (`targetYear`, `targetMonth`) 의 `ClosingAmountSum`
 * - 전월 실적 = 그 **직전 월**의 동일 값. 1월 목표행의 전월은 **전년 12월** — 레거시는 연도를 그대로 둬
 *   (`lastm = (lastm == 1) ? 12 : (lastm - 1)`, TargetYear 유지) 1월 행의 전월 실적이 영원히 비었는데,
 *   신규는 [YearMonth.minusMonths] 로 정상 rollover 한다 (레거시 결함 이탈).
 * - 매칭 축은 `account_id` FK — 모바일 「월 매출」/월매출 대시보드와 동일한 조인 축.
 *
 * ## 저장 컬럼 폴백
 * 월매출이력 row 가 없는 (연, 월) 은 엔티티의 저장 컬럼값을 그대로 쓴다. 이 값은 앱이 더 이상 쓰지 않는
 * **SF→RDS 이관 시점 스냅샷**이며, ORORA 적재 범위 밖의 과거 월 화면값을 보존하는 용도로만 남는다.
 */
@Component
class SalesProgressRateMasterActualsResolver(
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
) {

    /**
     * 목표행 1건의 산출 결과.
     *
     * @property currentMonthSalesAmount 당월 실적 (월매출이력 없으면 저장 컬럼 폴백)
     * @property previousMonthSalesAmount 전월 실적 (동일)
     * @property currentMonthSourceUpdatedAt 당월 실적의 원천 월매출이력 최종 적재 시각. 폴백 시 null
     * @property previousMonthSourceUpdatedAt 전월 실적의 원천 월매출이력 최종 적재 시각. 폴백 시 null
     */
    data class Actuals(
        val currentMonthSalesAmount: Double?,
        val previousMonthSalesAmount: Double?,
        val currentMonthSourceUpdatedAt: LocalDateTime?,
        val previousMonthSourceUpdatedAt: LocalDateTime?,
    )

    /**
     * 목표행 N건의 실적을 월매출이력 1 trip (청크당) 으로 일괄 산출한다.
     *
     * @return 목표행 id → [Actuals]. 거래처(account FK) 미연결이나 연월 파싱 불가 행은 저장 컬럼만 담긴다.
     */
    fun resolve(targets: Collection<SalesProgressRateMaster>): Map<Long, Actuals> {
        if (targets.isEmpty()) return emptyMap()

        // 거래처 IN 절 비대화 방지 — 청크 단위로 월매출이력을 조회한다 (목록 1페이지는 단일 청크).
        return targets
            .chunked(ACCOUNT_CHUNK_SIZE)
            .fold(mutableMapOf<Long, Actuals>()) { acc, chunk -> acc.apply { putAll(resolveChunk(chunk)) } }
    }

    /** 목표행 1건 산출 (상세 화면). */
    fun resolveOne(target: SalesProgressRateMaster): Actuals =
        resolve(listOf(target))[target.id] ?: fallbackOf(target)

    private fun resolveChunk(chunk: List<SalesProgressRateMaster>): Map<Long, Actuals> {
        data class Keyed(val target: SalesProgressRateMaster, val accountId: Long, val yearMonth: YearMonth)

        val keyed = chunk.mapNotNull { target ->
            val accountId = target.account?.id ?: return@mapNotNull null
            val yearMonth = yearMonthOf(target) ?: return@mapNotNull null
            Keyed(target, accountId, yearMonth)
        }
        // 산출 불가 행(거래처 미연결/연월 파싱 실패)은 저장 컬럼 폴백으로 채운다.
        val fallbacks = chunk.filter { t -> keyed.none { it.target === t } }.associate { it.id to fallbackOf(it) }
        if (keyed.isEmpty()) return fallbacks

        val salesDates = keyed
            .flatMap { listOf(salesDateOf(it.yearMonth), salesDateOf(it.yearMonth.minusMonths(1))) }
            .distinct()
        val accountIds = keyed.map { it.accountId }.distinct()

        val rowByKey: Map<Pair<Long, String>, MonthlySalesRow> = monthlySalesHistoryGateway
            .findBySalesDatesByAccountId(salesDates, accountIds)
            .mapNotNull { row -> row.accountId?.let { (it to row.salesDate) to row } }
            .toMap()

        return fallbacks + keyed.associate { (target, accountId, yearMonth) ->
            val current = rowByKey[accountId to salesDateOf(yearMonth)]
            val previous = rowByKey[accountId to salesDateOf(yearMonth.minusMonths(1))]
            target.id to Actuals(
                // 월매출이력 row 가 있으면 그 값이 정본, 없으면 이관 스냅샷(저장 컬럼) 유지.
                currentMonthSalesAmount = current?.closingAmountSum?.toDouble() ?: target.currentMonthSalesAmount,
                previousMonthSalesAmount = previous?.closingAmountSum?.toDouble() ?: target.previousMonthSalesAmount,
                currentMonthSourceUpdatedAt = current?.updatedAt,
                previousMonthSourceUpdatedAt = previous?.updatedAt,
            )
        }
    }

    /** 월매출이력을 매칭할 수 없는 행 — 저장 컬럼(SF 이관 스냅샷) 그대로. */
    private fun fallbackOf(target: SalesProgressRateMaster) = Actuals(
        currentMonthSalesAmount = target.currentMonthSalesAmount,
        previousMonthSalesAmount = target.previousMonthSalesAmount,
        currentMonthSourceUpdatedAt = null,
        previousMonthSourceUpdatedAt = null,
    )

    /** 목표행의 (`targetYear`, `targetMonth`) → [YearMonth]. 파싱 불가 시 null. */
    private fun yearMonthOf(target: SalesProgressRateMaster): YearMonth? {
        val year = target.targetYear?.trim()?.toIntOrNull() ?: return null
        val month = target.targetMonth?.trim()?.toIntOrNull() ?: return null
        if (month !in 1..12) return null
        return runCatching { YearMonth.of(year, month) }.getOrNull()
    }

    /** [YearMonth] → 월매출이력 조회 키 `YYYYMM`. */
    private fun salesDateOf(yearMonth: YearMonth): String =
        "%04d%02d".format(yearMonth.year, yearMonth.monthValue)

    companion object {
        /** 월매출이력 조회 1 trip 당 목표행 수 (거래처 IN 절 크기 상한). */
        const val ACCOUNT_CHUNK_SIZE = 500
    }
}
