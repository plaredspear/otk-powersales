package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.dto.response.StaffEvaluationResponse
import com.otoki.powersales.domain.org.employee.repository.StaffReviewRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 여사원 평가조회 service.
 *
 * ## 레거시 매핑 (`EmployeeController.evaluationList` / `evaluationListAjax`)
 * 1. 담당 거래처: `dkretail__teammemberschedule__c` 에서 본인(사번) 이 조회월에 근무한 거래처 (레거시 `selectEvalList` 서브쿼리 `mst`).
 * 2. 거래처별 목표/실적/달성률: 위 거래처 ⋈ `monthlysaleshistory__c`(연·월) **INNER JOIN** — 실적 row 가 없는 거래처는 미표시.
 * 3. 지점평가: `staffreview__c` 에서 본인의 해당월 평가 점수 (레거시 `selectBranchEval`).
 *
 * ## 데이터 source 정합
 * - 사번/세션 대신 인증 사용자(`employeeId`, 내부 FK) 기준 (sfid 비즈니스 로직 금지 정책 정합).
 * - 목표/실적/달성률 산출은 월매출 현황과 동일 ([MonthlySalesHistoryQueryGateway] + [SalesProgressRateMaster]).
 *   상세 deviation 은 [StaffEvaluationResponse] 참조.
 * - 조회월 미지정 시 기본값 = **전월** (레거시 `Calendar.add(MONTH, -1)` 정합).
 */
@Service
@Transactional(readOnly = true)
class StaffEvaluationService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository,
    private val accountRepository: AccountRepository,
    private val staffReviewRepository: StaffReviewRepository,
) {

    fun getEvaluation(employeeId: Long, yearMonth: String?): StaffEvaluationResponse {
        val ym = normalizeYearMonth(yearMonth)
        val year = ym.substring(0, 4).toInt()
        val month = ym.substring(4, 6).toInt()
        val firstDay = LocalDate.of(year, month, 1)
        val nextMonthFirst = firstDay.plusMonths(1)

        // 1. 담당 거래처 (조회월 근무). toDate 는 반열림 상한이라 다음달 1일 전달.
        val accountIds = teamMemberScheduleRepository
            .findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId, firstDay, nextMonthFirst)
        val accounts = buildAccountRows(accountIds, ym, year, month)

        // 3. 지점평가 — 평가대상월 1일 매칭 (soft-delete 제외).
        val branchScore = staffReviewRepository
            .findByEmployeeIdAndFirstDayOfMonth(employeeId, firstDay)
            .asSequence()
            .filter { it.isDeleted != true }
            .firstOrNull()
            ?.employeeTotalScore

        return StaffEvaluationResponse(
            yearMonth = ym,
            branchScore = branchScore,
            accounts = accounts,
        )
    }

    /**
     * 거래처별 목표/실적/달성률 행. 레거시 `selectEvalList` INNER JOIN 정합으로
     * **해당 (연,월) 월매출실적 row 가 존재하는 거래처만** 포함한다.
     */
    private fun buildAccountRows(
        accountIds: List<Long>,
        yearMonth: String,
        year: Int,
        month: Int,
    ): List<StaffEvaluationResponse.AccountEvaluationInfo> {
        if (accountIds.isEmpty()) return emptyList()

        // 실적: account_id FK 로 조회월 1개월분 — row 존재 거래처만 남는다 (INNER JOIN 정합).
        val rowsByAccountId = monthlySalesHistoryGateway
            .findBySalesDatesByAccountId(listOf(yearMonth), accountIds)
            .mapNotNull { row -> row.accountId?.let { it to row } }
            .toMap()
        if (rowsByAccountId.isEmpty()) return emptyList()

        val presentIds = rowsByAccountId.keys.toList()
        val accountsById = accountRepository.findByIdIn(presentIds).associateBy { it.id }
        val targetSumByAccountId = salesProgressRateMasterRepository
            .findByAccountIdInAndTargetYear(presentIds, year.toString())
            .asSequence()
            .filter { it.isDeleted != true && it.targetMonth?.trim()?.toIntOrNull() == month }
            .mapNotNull { master -> master.account?.id?.let { it to targetSumOf(master) } }
            .toMap()

        return presentIds
            .mapNotNull { accountId ->
                val account = accountsById[accountId] ?: return@mapNotNull null
                val performance = rowsByAccountId[accountId]?.closingAmountSum?.toLong() ?: 0L
                val target = targetSumByAccountId[accountId] ?: 0L
                StaffEvaluationResponse.AccountEvaluationInfo(
                    accountCode = account.externalKey ?: "",
                    accountName = account.name ?: "",
                    accountType = account.accountType?.displayName,
                    targetAmount = target,
                    performanceAmount = performance,
                    attainmentRate = rate(performance, target),
                )
            }
            .sortedBy { it.accountName }
    }

    /** 목표 합계 — SF `TargetSum__c` (= RT + FR + RM + FO) 동등. */
    private fun targetSumOf(target: SalesProgressRateMaster): Long =
        ((target.rtTargetAmount ?: 0.0) +
            (target.frTargetAmount ?: 0.0) +
            (target.rmTargetAmount ?: 0.0) +
            (target.foTargetAmount ?: 0.0)).toLong()

    /** 달성률 — `round(실적 / 목표 × 100)`. 목표 0 이면 0 (NaN/Infinity 방지, 월매출 화면 정합). */
    private fun rate(achieved: Long, target: Long): Double =
        if (target <= 0L) 0.0 else Math.round(achieved.toDouble() / target * 100).toDouble()

    /**
     * 조회월 정규화. `YYYYMM` 6자 숫자면 그대로, 그 외(미지정/형식오류) 는 전월 (레거시 기본 정합).
     */
    private fun normalizeYearMonth(yearMonth: String?): String {
        if (yearMonth != null && yearMonth.length == 6 && yearMonth.all { it.isDigit() }) {
            val month = yearMonth.substring(4, 6).toIntOrNull()
            if (month != null && month in 1..12) return yearMonth
        }
        val prev = LocalDate.now().minusMonths(1)
        return "%04d%02d".format(prev.year, prev.monthValue)
    }
}
