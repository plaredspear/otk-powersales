package com.otoki.powersales.schedule.service

import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.entity.QMonthlySalesHistory.Companion.monthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.schedule.dto.response.TeamMemberScheduleResultItem
import com.otoki.powersales.schedule.dto.response.TeamMemberScheduleSearchResult
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * 팀멤버 일정 검색 서비스 (SF `ScheduleSearchByTeamMemberController.cls:28-186`).
 *
 * MFEIS 검색 결과 라인 + 거래처별 6개월 평균 ABC 마감실적 (ClosingAmountSum) 산출.
 *
 * D3=(a): SF `MonthlySalesHistory.ClosingAmountSum__c` (formula) = `abcClosingSumAmount + shipClosingSumAmount` 합산
 * D4=(a): SF `MFEIS.BranchName__c` (formula) = `mfeis.employee.orgName` lazy join
 */
@Service
@Transactional(readOnly = true)
class TeamMemberScheduleSearchService(
    private val expander: BranchCodeExpander,
    private val queryFactory: JPAQueryFactory,
) {

    /**
     * SF `getSchedule(year, month, orgValues)` 동등.
     * @param year 년 ("2026")
     * @param month 월 ("5" / "05" 모두 허용)
     * @param orgValues 사용자 선택 지점 코드 리스트
     */
    fun search(year: String, month: String, orgValues: List<String>): TeamMemberScheduleSearchResult {
        val normMonth = month.toInt().toString()
        val expandedCodes = expander.expand(orgValues)
        if (expandedCodes.isEmpty()) {
            return TeamMemberScheduleSearchResult(resultCode = "S", resultMsg = "검색결과가 없습니다.", result = emptyList())
        }

        val schedules = fetchMfeisOrdered(year, normMonth, expandedCodes)
        val avgSalesByAccountId = computeSixMonthAverageSales(schedules, year, normMonth)

        val items = schedules.map { toResultItem(it, avgSalesByAccountId) }

        return TeamMemberScheduleSearchResult(
            resultCode = "S",
            resultMsg = if (items.isEmpty()) "검색결과가 없습니다." else null,
            result = items,
        )
    }

    /**
     * SF L41-50 SOQL — MFEIS 조회 + ORDER BY BranchName, AccountCode, EmployeeNumber.
     * BranchName / AccountCode / EmployeeNumber 가 모두 SF formula 이므로 backend 는 lazy join 컬럼으로 정렬.
     */
    internal fun fetchMfeisOrdered(
        year: String,
        month: String,
        costCenterCodes: Collection<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val q = monthlyFemaleEmployeeIntegrationSchedule
        return queryFactory
            .selectFrom(q)
            .where(
                q.year.eq(year)
                    .and(q.month.eq(month))
                    .and(q.costCenterCode.`in`(costCenterCodes))
            )
            // SF ORDER BY BranchName__c, AccountCode__c, EmployeeNumber__c (모두 formula)
            // backend lazy join 컬럼으로 동등 정렬
            .orderBy(
                q.employee.orgName.asc(),
                q.account.externalKey.asc(),
                q.employee.employeeCode.asc(),
            )
            .fetch()
    }

    /**
     * SF `getSalesAmount` (cls:128-184) 의 6개월 평균 ABC 마감실적 산출.
     *
     * 산출 규칙 (SF L143-150):
     * - 선택 년월 == 당월: [선택월 -6, 선택월 -1]
     * - 그 외: [선택월 -5, 선택월]
     *
     * ABC 마감실적 합계 = SF `ClosingAmountSum__c` formula = `abcClosingSumAmount + shipClosingSumAmount` (D3=a).
     * 평균 = 합산 / 데이터 존재 월 수 (divider, SF L164-181).
     */
    internal fun computeSixMonthAverageSales(
        schedules: List<MonthlyFemaleEmployeeIntegrationSchedule>,
        year: String,
        month: String,
    ): Map<Long, BigDecimal> {
        val accountIds = schedules.mapNotNull { it.account?.id?.toLong() }.toSet()
        if (accountIds.isEmpty()) return emptyMap()

        val (startYm, endYm) = sixMonthRange(year, month, today = LocalDate.now())
        val monthList = enumerateMonths(startYm, endYm)
        val salesYears = monthList.map { SalesYear.valueOf("Y${it.year}") }.toSet().toList()
        val salesMonths = monthList.map { SalesMonth.valueOf("M${"%02d".format(it.monthValue)}") }.toSet().toList()

        // YearMonth set 으로 in-memory 재필터 (SalesYear x SalesMonth 카르테시안 곱이 6개월 범위를 초과할 수 있음)
        val monthSet = monthList.toSet()

        val q = monthlySalesHistory
        val rows: List<MonthlySalesHistory> = queryFactory
            .selectFrom(q)
            .where(
                q.account.id.`in`(accountIds.map { it.toInt() })
                    .and(q.salesYear.`in`(salesYears))
                    .and(q.salesMonth.`in`(salesMonths))
            )
            .fetch()

        // SF divider — 데이터 존재 월 수 (SF L164-181: avgAmount += closing; div++)
        val sumByAccount = mutableMapOf<Long, BigDecimal>()
        val countByAccount = mutableMapOf<Long, Int>()
        for (row in rows) {
            val ym = rowYearMonth(row) ?: continue
            if (ym !in monthSet) continue
            val accId = row.account?.id?.toLong() ?: continue
            val closingSum = closingAmountSum(row)
            sumByAccount.merge(accId, closingSum) { a, b -> a + b }
            countByAccount.merge(accId, 1) { a, b -> a + b }
        }

        return accountIds.associateWith { accId ->
            val sum = sumByAccount[accId] ?: BigDecimal.ZERO
            val div = countByAccount[accId] ?: 0
            if (div == 0) BigDecimal.ZERO else (sum.divide(BigDecimal(div), 0, RoundingMode.HALF_UP))
        }
    }

    /**
     * D3=(a): SF `ClosingAmountSum__c = ABCClosingSumAmount__c + ShipClosingSumAmount__c`.
     */
    internal fun closingAmountSum(row: MonthlySalesHistory): BigDecimal {
        val abc = row.abcClosingSumAmount?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
        val ship = row.shipClosingSumAmount?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
        return abc + ship
    }

    private fun rowYearMonth(row: MonthlySalesHistory): YearMonth? {
        val y = row.salesYear?.value?.toIntOrNull() ?: return null
        val m = row.salesMonth?.value?.toIntOrNull() ?: return null
        return YearMonth.of(y, m)
    }

    /**
     * SF L143-150 — 선택월 == 당월이면 [-6, -1], 그 외엔 [-5, 0].
     */
    internal fun sixMonthRange(year: String, month: String, today: LocalDate): Pair<YearMonth, YearMonth> {
        val selected = YearMonth.of(year.toInt(), month.toInt())
        val current = YearMonth.from(today)
        return if (selected == current) {
            selected.minusMonths(6) to selected.minusMonths(1)
        } else {
            selected.minusMonths(5) to selected
        }
    }

    internal fun enumerateMonths(start: YearMonth, end: YearMonth): List<YearMonth> {
        val result = mutableListOf<YearMonth>()
        var cur = start
        while (!cur.isAfter(end)) {
            result.add(cur)
            cur = cur.plusMonths(1)
        }
        return result
    }

    /**
     * MFEIS 1건 → ResultItem 변환 (SF `getItemList` cls:251-326).
     */
    private fun toResultItem(
        sm: MonthlyFemaleEmployeeIntegrationSchedule,
        avgSalesByAccountId: Map<Long, BigDecimal>,
    ): TeamMemberScheduleResultItem {
        val emp = sm.employee
        val acc = sm.account
        val accId = acc?.id?.toLong()
        val actualAmount = accId?.let { avgSalesByAccountId[it] } ?: BigDecimal.ZERO

        return TeamMemberScheduleResultItem(
            year = sm.year,
            month = sm.month,
            name = sm.name,
            // D4=(a): SF formula AccountBranchName__c = Account__r.BranchName__c
            accountBranchName = acc?.branchName,
            // SF Account__r.Name
            accountName = acc?.name,
            // D4=(a): SF formula AccountCode__c = Account__r.ExternalKey__c
            accountCode = acc?.externalKey,
            // D4=(a): SF formula BranchName__c = FullName__r.DKRetail__OrgName__c
            orgName = emp?.orgName,
            // D4=(a): SF formula EmployeeNumber__c = FullName__r.DKRetail__EmpCode__c
            employeeNumber = emp?.employeeCode,
            // D4=(a): SF formula Title__c = FullName__r.DKRetail__Jikwee__c
            title = emp?.jikwee,
            // SF FullName__r.Name
            employeeName = emp?.name,
            workingCategory1 = sm.workingCategory1,
            workingCategory3 = sm.workingCategory3,
            workingCategory4 = sm.workingCategory4,
            workingCategory5 = sm.workingCategory5,
            numberOfInputs = sm.numberOfInputs,
            equivalentNumberOfWorkingDays = sm.equivalentNumberOfWorkingDays ?: BigDecimal.ZERO,
            convertedHeadcount = sm.convertedHeadcount ?: BigDecimal.ZERO,
            actualAmount = actualAmount,
        )
    }
}
