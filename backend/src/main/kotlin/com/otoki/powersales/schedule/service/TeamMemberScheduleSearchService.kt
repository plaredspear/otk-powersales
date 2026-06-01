package com.otoki.powersales.schedule.service

import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.sales.service.MonthlySalesRow
import com.otoki.powersales.schedule.dto.response.TeamMemberScheduleResultItem
import com.otoki.powersales.schedule.dto.response.TeamMemberScheduleSearchResult
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.core.types.Projections
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
 * D3=(a): SF `MonthlySalesHistory.ClosingAmountSum__c` (formula) = `(abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)` 합산
 * D4=(a): SF `MFEIS.BranchName__c` (formula) = `mfeis.employee.orgName` lazy join
 *
 * 마감실적 source 는 RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) — SF formula
 * `ClosingAmountSum__c` 동등 산출은 [MonthlySalesHistoryQueryGateway] 가 책임 (개별 카테고리 컬럼 재합산).
 */
@Service
@Transactional(readOnly = true)
class TeamMemberScheduleSearchService(
    private val expander: BranchCodeExpander,
    private val queryFactory: JPAQueryFactory,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
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

        val rows = fetchMfeisOrdered(year, normMonth, expandedCodes)
        val avgSalesByAccountId = computeSixMonthAverageSales(rows, year, normMonth)

        val items = rows.map { toResultItem(it, avgSalesByAccountId) }

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
    ): List<TeamMemberScheduleRow> {
        val q = monthlyFemaleEmployeeIntegrationSchedule
        // DTO projection — MFEIS + employee/account 의 필요 컬럼만 select.
        // 엔티티(특히 Employee) 를 hydrate 하지 않으므로 Employee.employeeInfo @OneToOne 강제 로딩
        // (N+1) 을 회피한다. 화면에 쓰는 컬럼은 orgName/employeeCode/jikwee/name(employee),
        // id/externalKey/branchName/name(account) 뿐.
        return queryFactory
            .select(
                Projections.constructor(
                    TeamMemberScheduleRow::class.java,
                    q.year,
                    q.month,
                    q.name,
                    q.account.id,
                    q.account.externalKey,
                    q.account.branchName,
                    q.account.name,
                    q.employee.orgName,
                    q.employee.employeeCode,
                    q.employee.jikwee,
                    q.employee.name,
                    q.workingCategory1,
                    q.workingCategory3,
                    q.workingCategory4,
                    q.workingCategory5,
                    q.numberOfInputs,
                    q.equivalentNumberOfWorkingDays,
                    q.convertedHeadcount,
                )
            )
            .from(q)
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
     * ABC 마감실적 합계 = SF `ClosingAmountSum__c` formula (D3=a) = [MonthlySalesRow.closingAmountSum]
     *                  = (abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4)
     * 평균 = 합산 / 데이터 존재 월 수 (divider, SF L164-181).
     */
    internal fun computeSixMonthAverageSales(
        rows: List<TeamMemberScheduleRow>,
        year: String,
        month: String,
    ): Map<Long, BigDecimal> {
        val accountByExternalKey: Map<String, Long> = rows
            .mapNotNull { row ->
                val accId = row.accountId ?: return@mapNotNull null
                val externalKey = row.accountExternalKey ?: return@mapNotNull null
                externalKey to accId.toLong()
            }
            .toMap()
        if (accountByExternalKey.isEmpty()) return emptyMap()

        val (startYm, endYm) = sixMonthRange(year, month, today = LocalDate.now())
        val salesDates = enumerateMonths(startYm, endYm)
            .map { "%d%02d".format(it.year, it.monthValue) }

        val salesRows: List<MonthlySalesRow> = monthlySalesHistoryGateway.findBySalesDates(
            salesDates = salesDates,
            sapAccountCodes = accountByExternalKey.keys,
        )

        // SF divider — 데이터 존재 월 수 (SF L164-181: avgAmount += closing; div++)
        val sumByAccount = mutableMapOf<Long, BigDecimal>()
        val countByAccount = mutableMapOf<Long, Int>()
        for (row in salesRows) {
            val accId = accountByExternalKey[row.sapAccountCode] ?: continue
            val closingSum = row.closingAmountSum
            sumByAccount.merge(accId, closingSum) { a, b -> a + b }
            countByAccount.merge(accId, 1) { a, b -> a + b }
        }

        return accountByExternalKey.values.associateWith { accId ->
            val sum = sumByAccount[accId] ?: BigDecimal.ZERO
            val div = countByAccount[accId] ?: 0
            if (div == 0) BigDecimal.ZERO else (sum.divide(BigDecimal(div), 0, RoundingMode.HALF_UP))
        }
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
     * MFEIS projection row → ResultItem 변환 (SF `getItemList` cls:251-326).
     */
    private fun toResultItem(
        row: TeamMemberScheduleRow,
        avgSalesByAccountId: Map<Long, BigDecimal>,
    ): TeamMemberScheduleResultItem {
        val accId = row.accountId?.toLong()
        val actualAmount = accId?.let { avgSalesByAccountId[it] } ?: BigDecimal.ZERO

        return TeamMemberScheduleResultItem(
            year = row.year,
            month = row.month,
            name = row.name,
            // D4=(a): SF formula AccountBranchName__c = Account__r.BranchName__c
            accountBranchName = row.accountBranchName,
            // SF Account__r.Name
            accountName = row.accountName,
            // D4=(a): SF formula AccountCode__c = Account__r.ExternalKey__c
            accountCode = row.accountExternalKey,
            // D4=(a): SF formula BranchName__c = FullName__r.DKRetail__OrgName__c
            orgName = row.employeeOrgName,
            // D4=(a): SF formula EmployeeNumber__c = FullName__r.DKRetail__EmpCode__c
            employeeNumber = row.employeeCode,
            // D4=(a): SF formula Title__c = FullName__r.DKRetail__Jikwee__c
            title = row.employeeJikwee,
            // SF FullName__r.Name
            employeeName = row.employeeName,
            workingCategory1 = row.workingCategory1,
            workingCategory3 = row.workingCategory3,
            workingCategory4 = row.workingCategory4,
            workingCategory5 = row.workingCategory5,
            numberOfInputs = row.numberOfInputs,
            equivalentNumberOfWorkingDays = row.equivalentNumberOfWorkingDays ?: BigDecimal.ZERO,
            convertedHeadcount = row.convertedHeadcount ?: BigDecimal.ZERO,
            actualAmount = actualAmount,
        )
    }
}

/**
 * MFEIS 검색 projection row — 화면/계산에 필요한 컬럼만 보유.
 * Employee 엔티티 통째 로딩 (→ employeeInfo @OneToOne 강제 SELECT N+1) 을 피하기 위한 DTO projection.
 * 생성자 파라미터 순서 = QueryDSL `Projections.constructor` select 순서.
 */
data class TeamMemberScheduleRow(
    val year: String?,
    val month: String?,
    val name: String?,
    val accountId: Int?,
    val accountExternalKey: String?,
    val accountBranchName: String?,
    val accountName: String?,
    val employeeOrgName: String?,
    val employeeCode: String?,
    val employeeJikwee: String?,
    val employeeName: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val numberOfInputs: BigDecimal?,
    val equivalentNumberOfWorkingDays: BigDecimal?,
    val convertedHeadcount: BigDecimal?,
)
