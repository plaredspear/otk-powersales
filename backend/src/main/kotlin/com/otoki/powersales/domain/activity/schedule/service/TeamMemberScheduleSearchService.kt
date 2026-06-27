package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberScheduleResultItem
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberScheduleSearchResult
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.sales.service.MonthlySalesRow
import com.otoki.powersales.domain.activity.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
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
    fun search(
        year: String,
        month: String,
        orgValues: List<String>,
        keyword: String? = null,
        accountKeyword: String? = null,
    ): TeamMemberScheduleSearchResult {
        val normMonth = month.toInt().toString()
        val expandedCodes = expander.expand(orgValues)
        if (expandedCodes.isEmpty()) {
            return TeamMemberScheduleSearchResult(resultCode = "S", resultMsg = "검색결과가 없습니다.", result = emptyList())
        }

        val rows = fetchMfeisOrdered(year, normMonth, expandedCodes, keyword, accountKeyword)
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
        keyword: String? = null,
        accountKeyword: String? = null,
    ): List<TeamMemberScheduleRow> {
        val q = monthlyFemaleEmployeeIntegrationSchedule
        // 사번 또는 이름 통합 검색어 — 사번 정확일치 OR 이름 부분일치(대소문자/공백 무시).
        // employee 는 이미 lazy join 으로 select 중이라 추가 join 없이 WHERE 조건만 덧붙인다.
        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val keywordPredicate = trimmedKeyword?.let {
            q.employee.employeeCode.eq(it).or(q.employee.name.containsIgnoreCase(it))
        }
        // 거래처 통합 검색어 — 거래처코드(ExternalKey) 정확일치 OR 거래처명 부분일치(대소문자/공백 무시).
        // account 도 이미 lazy join 으로 select 중이라 추가 join 없이 WHERE 조건만 덧붙인다.
        // keyword(사번/이름) 와 AND 결합 — 둘 다 입력 시 교집합.
        val trimmedAccountKeyword = accountKeyword?.trim()?.takeIf { it.isNotEmpty() }
        val accountKeywordPredicate = trimmedAccountKeyword?.let {
            q.account.externalKey.eq(it).or(q.account.name.containsIgnoreCase(it))
        }
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
                    q.account.accountStatusCode,
                    q.account.accountType,
                    q.account.abcTypeCode,
                    q.account.abcType,
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
                    .and(keywordPredicate)
                    .and(accountKeywordPredicate)
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
        // SF `SalesComparisonSearchController` 6개월 매출 join 키 정합 — MFEIS.Account__c(Account Id) =
        // MonthlySalesHistory__c.AccountId__c (cls:289-294). sapAccountCode(=ExternalKey) 가 아니라
        // account_id 로 조회해야, SF 가 `WHERE AccountId__c IN` 으로 배제하는 account_id=null 매출 행
        // (Account 삭제 시 SetNull 로 잔존) 을 신규도 동일하게 제외한다.
        val accountIds: Set<Long> = rows
            .mapNotNull { it.accountId?.toLong() }
            .toSet()
        if (accountIds.isEmpty()) return emptyMap()

        // SF `Date.today()` 는 사용자 세션(한국 운영자 = KST) timezone 기준 — 신규도 KST 로 맞춰야 SF 정합.
        // JVM 기본 timezone(배포 환경 UTC) 의 LocalDate.now() 는 KST 자정~09시 구간에 하루 어긋나
        // "검색월 == 당월" 분기를 뒤집어 6개월 윈도우를 통째로 밀 수 있다.
        val (startYm, endYm) = sixMonthRange(year, month, today = LocalDate.now(TimeZones.SEOUL_ZONE))
        val salesDates = enumerateMonths(startYm, endYm)
            .map { "%d%02d".format(it.year, it.monthValue) }

        val salesRows: List<MonthlySalesRow> = monthlySalesHistoryGateway.findBySalesDatesByAccountId(
            salesDates = salesDates,
            accountIds = accountIds,
        )

        // SF divider — 데이터 존재 월 수 (SF L164-181: avgAmount += closing; div++)
        val sumByAccount = mutableMapOf<Long, BigDecimal>()
        val countByAccount = mutableMapOf<Long, Int>()
        for (row in salesRows) {
            val accId = row.accountId ?: continue
            val closingSum = row.closingAmountSum
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
            // 유통형태/거래처유형 라벨 — Account 조합 규칙 정본(companion) 재사용 (projection 이라 인스턴스 없음)
            distributionChannelLabel = Account.distributionChannelLabel(
                row.accountStatusCode,
                row.accountType?.displayName,
            ),
            abcTypeLabel = Account.abcTypeLabel(row.abcTypeCode, row.abcType),
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
    val accountId: Long?,
    val accountExternalKey: String?,
    val accountBranchName: String?,
    val accountName: String?,
    val accountStatusCode: String?,
    val accountType: AccountType?,
    val abcTypeCode: String?,
    val abcType: String?,
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
