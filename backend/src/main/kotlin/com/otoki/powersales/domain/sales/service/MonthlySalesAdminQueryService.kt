package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardSummaryResponse
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.repository.SalesProgressRateMasterRepository
import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.collections.get

/**
 * 월매출 대시보드 admin 조회 service.
 *
 * ## 데이터 source
 * RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) 100% 의존
 * ([MonthlySalesHistoryQueryGateway] 경유 — 메인 RDS DataSource).
 * 외부 ORORA view (`OroraMonthlySalesHistoryQueryGateway`) 는 본 화면 조회 경로에서 호출하지 않는다.
 * ORORA view 는 RDS 로의 복제 적재 배치 (`OroraMonthlySalesChunkProcessor`) 에서만 읽으며, 화면/집계는
 * 항상 RDS 적재본을 본다. SF 레거시도 화면 조회가 ORORA 직접이 아닌 `MonthlySalesHistory__c`
 * SObject 를 읽은 것과 동등.
 * 실적(마감 합계)은 모바일 「월 매출」과 동일하게 `ClosingAmountSum`(ABC합 + Ship합)을 쓰며, RDS row 조인
 * 키는 `account_id` FK (sap_account_code 텍스트 컬럼은 적재품질 의존이라 미사용 — [MonthlySalesHistoryQueryGateway.findBySalesDatesByAccountId]).
 * 목표/달성률은 단건 상세([getDetail])·상단 요약([getSummary])·거래처별 명세([getList]) 전부
 * `SalesProgressRateMaster`(연·월 1행) 기반으로 모바일 「월 매출」 정합. 명세는 합계 목표 + 카테고리 4종
 * (상온/라면/냉동냉장/유지) 목표를 함께 반환한다 (모바일 categorySales 정합).
 * 단, 확정 상태(`isConfirmed`)는 신규 시스템 미재현이라 폐기 유지 (`false`).
 *
 * ## 동작 요약
 * 권한 범위 거래처 N건의 마감실적을 합산 + 거래처별 명세 + 단건 상세 형태로 반환.
 * 레거시 매핑: 모바일 「월매출조회-물류배부」 페이지 (docs/plan/legacy-pages-heroku/월매출조회-물류배부) 의 9개 UC 를 web admin 으로 확장.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — admin 측 대시보드 view 는 레거시 미존재 (모바일 페이지의 web admin 확장). origin spec: #776.
 */
@Service
@Transactional(readOnly = true)
class MonthlySalesAdminQueryService(
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    private val salesProgressRateMasterRepository: SalesProgressRateMasterRepository,
) {

    /**
     * 상단 KPI + 최근 6개월 월별 추이 조회.
     *
     * 권한 범위 거래처의 당월 마감실적 합산 + 목표 합산 + 달성률 + 전년 동월 비교 + 기준 진도율 (영업일 기반) 산출.
     * 목표는 거래처별 `SalesProgressRateMaster`(연·월 1행) 합계의 총합, 달성률 = `round(실적/목표×100)` (목표 0 이면 0.0).
     * 월별 추이 6개월은 (조회 기준 월) 부터 5개월 이전까지 + 같은 기간의 전년 동월 매출.
     *
     * @throws AdminForbiddenException 권한 범위와 입력 costCenterCodes 의 교집합이 비어있을 때
     * @throws InvalidParameterException year/month/costCenterCodes 가 비정상일 때
     */
    fun getSummary(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        customerKeyword: String?,
        accountGroup: String?,
    ): MonthlySalesDashboardSummaryResponse {
        validateParams(year, month, costCenterCodes)
        val effectiveCodes = applyScope(scope, costCenterCodes)
        val accounts = findAccounts(effectiveCodes, accountGroup, customerKeyword)

        if (accounts.isEmpty()) {
            return MonthlySalesDashboardSummaryResponse(
                salesYear = year,
                salesMonth = month,
                totalTargetAmount = 0L,
                totalAchievedAmount = 0L,
                overallAchievementRate = 0.0,
                referenceAchievementRate = referenceAchievementRate(year, month, LocalDate.now()),
                totalLastYearAchievedAmount = null,
                lastYearComparisonRatio = null,
                monthlyTrend = buildMonthlyTrend(year, month, emptyList()),
            )
        }

        // RDS fetch — 당월 + 전년 동월 일괄 1 trip (account_id FK 조인 — sap_account_code 적재품질 무관, 모바일 정합)
        val accountIds = accounts.map { it.id }
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDatesByAccountId(listOf(currentSalesDate, lastYearSalesDate), accountIds)
            .associateBy { it.accountId to it.salesDate }

        val totalAchieved = accounts.sumOf { acc ->
            closingSum(oroByKey[acc.id to currentSalesDate])
        }
        val totalLastYearAchieved = accounts
            .sumOf { acc -> closingSum(oroByKey[acc.id to lastYearSalesDate]) }
            .takeIf { it > 0L }
        val lastYearRatio = if (totalLastYearAchieved == null || totalLastYearAchieved == 0L) null
        else (totalAchieved.toDouble() / totalLastYearAchieved.toDouble()) * 100.0

        // 목표 합계 — 거래처별 (연, 월) SalesProgressRateMaster 1행의 합계를 총합 (미등록 거래처는 0)
        val targetByAccountId = findTargetMap(accountIds, year, month)
        val totalTarget = targetByAccountId.values.sumOf { targetSumOf(it) }

        return MonthlySalesDashboardSummaryResponse(
            salesYear = year,
            salesMonth = month,
            totalTargetAmount = totalTarget,
            totalAchievedAmount = totalAchieved,
            overallAchievementRate = rate(totalAchieved, totalTarget),
            referenceAchievementRate = referenceAchievementRate(year, month, LocalDate.now()),
            totalLastYearAchievedAmount = totalLastYearAchieved,
            lastYearComparisonRatio = lastYearRatio,
            monthlyTrend = buildMonthlyTrend(year, month, accounts),
        )
    }

    /**
     * 하단 거래처별 명세 조회 — 페이징 + 정렬 + 필터.
     *
     * 권한 범위 거래처 N건의 당월 마감실적 row 별 카테고리 4종 (상온/라면/냉동냉장/유지) + 전년 동월 비교 응답.
     * 페이징은 조회된 거래처 list 를 메모리에서 sort + slice (거래처 수가 통상 수백 이내라 충분).
     *
     * @throws AdminForbiddenException 권한 범위 위반
     */
    fun getList(scope: DataScope, request: MonthlySalesDashboardListRequest): MonthlySalesDashboardListResponse {
        validateParams(request.year, request.month, request.costCenterCodes)
        val effectiveCodes = applyScope(scope, request.costCenterCodes)

        val items = buildListItems(effectiveCodes, request)
        val sorted = sortItems(items, request.sort)
        val totalElements = sorted.size.toLong()
        val pageSize = request.size.coerceIn(1, 100)
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + pageSize - 1) / pageSize).toInt()
        val fromIndex = (request.page * pageSize).coerceAtLeast(0)
        val toIndex = (fromIndex + pageSize).coerceAtMost(sorted.size)
        val pageItems = if (fromIndex >= sorted.size) emptyList() else sorted.subList(fromIndex, toIndex)

        return MonthlySalesDashboardListResponse(
            items = pageItems,
            pageInfo = MonthlySalesDashboardListResponse.PageInfo(
                page = request.page,
                size = pageSize,
                totalElements = totalElements,
                totalPages = totalPages,
            ),
        )
    }

    /**
     * 엑셀 export 용 전체 명세 산출 (페이징 미적용).
     */
    fun getListForExport(scope: DataScope, request: MonthlySalesDashboardListRequest): List<MonthlySalesDashboardListItem> {
        validateParams(request.year, request.month, request.costCenterCodes)
        val effectiveCodes = applyScope(scope, request.costCenterCodes)
        return sortItems(buildListItems(effectiveCodes, request), request.sort)
    }

    /**
     * 단건 거래처 상세 조회 — 모바일 동등 6 영역 (진도율 바 / 목표·실적 / 카테고리 4종 / 전년 동월 / 전년 평균).
     * 목표 / 실적 / 달성률은 모바일 「월 매출」([MonthlySalesService.getMonthlySales]) 정합:
     * 실적 = `ClosingAmountSum`(ABC합 + Ship합), 목표 = `SalesProgressRateMaster`(연·월 1행) 합계,
     * 달성률 = `round(실적/목표×100)`. 조인 키는 `account_id` FK (sap_account_code 텍스트 컬럼 미사용).
     *
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getDetail(scope: DataScope, customerId: Long, year: Int, month: Int): MonthlySalesDashboardDetailResponse {
        validateYearMonth(year, month)
        val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
            ?: throw BusinessException(
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "거래처를 찾을 수 없습니다: $customerId",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        if (!scope.validateAccess(account.branchCode)) throw AdminForbiddenException()

        // RDS fetch — 당월 + 전년 + 1~조회월 누적 일괄 1 trip (account_id FK 조인 — 모바일 정합)
        val months = (1..month).toList()
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val currentRangeSalesDates = months.map { toSalesDate(year, it) }
        val previousRangeSalesDates = months.map { toSalesDate(year - 1, it) }
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDatesByAccountId(
                (currentRangeSalesDates + previousRangeSalesDates).distinct(),
                listOf(account.id),
            )
            .associateBy { it.accountId to it.salesDate }
        val currentOro = oroByKey[account.id to currentSalesDate]
        val lastYearOro = oroByKey[account.id to lastYearSalesDate]

        val achieved = closingSum(currentOro)

        // 목표 — 조회 거래처의 (연, 월) 1행 (SalesProgressRateMaster). 미등록 시 0.
        val target = findTarget(account.id, year, month)
        val targetSum = target?.let { targetSumOf(it) } ?: 0L

        val today = LocalDate.now()
        val isPastMonth = year < today.year || (year == today.year && month < today.monthValue)
        val categorySales = if (isPastMonth) buildCategorySales(currentOro, target) else emptyList()

        val lastYearAchieved = closingSum(lastYearOro)
        val yearComparison = MonthlySalesDashboardDetailResponse.YearComparisonInfo(
            currentYear = achieved / MILLION,
            previousYear = lastYearAchieved / MILLION,
        )

        // 1월~조회월 누적 평균 (백만원 단위 절사) — RDS row 기반
        val currentAvg = if (months.isEmpty()) 0L
        else currentRangeSalesDates.sumOf { sd -> closingSum(oroByKey[account.id to sd]) } / months.size
        val previousAvg = if (months.isEmpty()) 0L
        else previousRangeSalesDates.sumOf { sd -> closingSum(oroByKey[account.id to sd]) } / months.size
        val monthlyAverage = MonthlySalesDashboardDetailResponse.MonthlyAverageInfo(
            currentYearAverage = currentAvg / MILLION,
            previousYearAverage = previousAvg / MILLION,
            startMonth = 1,
            endMonth = month,
        )

        return MonthlySalesDashboardDetailResponse(
            customerId = account.id,
            customerName = account.name,
            salesYear = year,
            salesMonth = month,
            targetAmount = targetSum,
            achievedAmount = achieved,
            achievementRate = rate(achieved, targetSum),
            referenceAchievementRate = referenceAchievementRate(year, month, LocalDate.now()),
            categorySales = categorySales,
            yearComparison = yearComparison,
            monthlyAverage = monthlyAverage,
        )
    }

    // ------------------- helpers -------------------

    private fun buildListItems(effectiveCodes: List<String>, request: MonthlySalesDashboardListRequest): List<MonthlySalesDashboardListItem> {
        val accounts = findAccounts(effectiveCodes, request.accountGroup, request.customerKeyword)
            .let { all ->
                if (request.accountIds.isEmpty()) all else all.filter { it.id in request.accountIds }
            }
        if (accounts.isEmpty()) return emptyList()

        // RDS fetch — 당월 + 전년 동월 일괄 1 trip (account_id FK 조인, 모바일 정합)
        val currentSalesDate = toSalesDate(request.year, request.month)
        val lastYearSalesDate = toSalesDate(request.year - 1, request.month)
        val accountIds = accounts.map { it.id }
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDatesByAccountId(listOf(currentSalesDate, lastYearSalesDate), accountIds)
            .associateBy { it.accountId to it.salesDate }

        // 목표 — 거래처 N건의 (연, 월) SalesProgressRateMaster 1행 일괄 조회 (account_id 별 batch, N+1 회피)
        val targetByAccountId = findTargetMap(accountIds, request.year, request.month)

        return accounts.map { account ->
            val currentOro = oroByKey[account.id to currentSalesDate]
            val lastYearOro = oroByKey[account.id to lastYearSalesDate]

            val achieved = closingSum(currentOro)
            val lastYearAchieved = closingSum(lastYearOro)
            val lastYearRatio = if (lastYearAchieved > 0)
                (achieved.toDouble() / lastYearAchieved.toDouble()) * 100.0 else null

            val target = targetByAccountId[account.id]
            val targetSum = target?.let { targetSumOf(it) } ?: 0L

            MonthlySalesDashboardListItem(
                accountId = account.id,
                accountName = account.name,
                sapAccountCode = account.externalKey,
                branchCode = account.branchCode,
                branchName = account.branchName,
                salesYear = request.year,
                salesMonth = request.month,
                targetAmount = targetSum,
                totalAchievedAmount = achieved,
                achievementRate = rate(achieved, targetSum),
                ambientTargetAmount = target?.let { categoryTarget(it, SalesCategory.AMBIENT) } ?: 0L,
                ambientAchievedAmount = categoryAchieved(currentOro, SalesCategory.AMBIENT),
                noodleTargetAmount = target?.let { categoryTarget(it, SalesCategory.NOODLE) } ?: 0L,
                noodleAchievedAmount = categoryAchieved(currentOro, SalesCategory.NOODLE),
                frozenRefrigeratedTargetAmount = target?.let { categoryTarget(it, SalesCategory.FROZEN_REFRIGERATED) } ?: 0L,
                frozenRefrigeratedAchievedAmount = categoryAchieved(currentOro, SalesCategory.FROZEN_REFRIGERATED),
                oilFatTargetAmount = target?.let { categoryTarget(it, SalesCategory.OIL_FAT) } ?: 0L,
                oilFatAchievedAmount = categoryAchieved(currentOro, SalesCategory.OIL_FAT),
                lastYearAchievedAmount = lastYearAchieved,
                lastYearComparisonRatio = lastYearRatio,
                isConfirmed = false,
            )
        }
    }

    private fun sortItems(items: List<MonthlySalesDashboardListItem>, sort: String?): List<MonthlySalesDashboardListItem> {
        if (sort.isNullOrBlank()) return items.sortedBy { it.accountName ?: "" }
        val parts = sort.split(",")
        val field = parts[0].trim()
        val desc = parts.getOrNull(1)?.trim()?.equals("desc", ignoreCase = true) == true
        val comparator: Comparator<MonthlySalesDashboardListItem> = when (field) {
            "accountName" -> compareBy(nullsLast()) { it.accountName }
            "achievementRate" -> compareBy(nullsLast()) { it.achievementRate }
            "totalAchievedAmount" -> compareBy(nullsLast()) { it.totalAchievedAmount }
            "targetAmount" -> compareBy(nullsLast()) { it.targetAmount }
            else -> compareBy(nullsLast()) { it.accountName }
        }
        return if (desc) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
    }

    private fun buildMonthlyTrend(
        baseYear: Int,
        baseMonth: Int,
        accounts: List<Account>,
    ): List<MonthlySalesDashboardSummaryResponse.MonthlyTrendPoint> {
        // 최근 6개월 — (baseYear, baseMonth) 부터 5개월 이전까지 역순 → 시간순으로 재정렬
        val keys = (0..5).map { offset ->
            val totalMonths = baseYear * 12 + (baseMonth - 1) - offset
            val y = totalMonths / 12
            val m = (totalMonths % 12) + 1
            y to m
        }.reversed()

        if (accounts.isEmpty()) {
            return keys.map { (y, m) ->
                MonthlySalesDashboardSummaryResponse.MonthlyTrendPoint(
                    salesYear = y, salesMonth = m,
                    targetAmount = 0L, achievedAmount = 0L, lastYearAchievedAmount = null,
                )
            }
        }

        val lastYearKeys = keys.map { (y, m) -> (y - 1) to m }

        // RDS fetch — 12개월 (현재 6 + 전년 6) 일괄 1 trip
        val accountIds = accounts.map { it.id }
        val allSalesDates = (keys + lastYearKeys).map { (y, m) -> toSalesDate(y, m) }.distinct()
        val oroByKey = monthlySalesHistoryGateway.findBySalesDatesByAccountId(allSalesDates, accountIds)
            .groupBy { it.salesDate }

        // 목표 — 추이 6개월에 걸친 연도들의 목표 행을 연도별 1 trip 으로 일괄 조회 후 (연·월) 합산
        val targetYears = keys.map { (y, _) -> y }.distinct()
        val targetsByYear = targetYears.associateWith { y -> findTargets(accountIds, y) }

        return keys.map { (y, m) ->
            val currentSalesDate = toSalesDate(y, m)
            val lastYearSalesDate = toSalesDate(y - 1, m)
            val currentOroRows = oroByKey[currentSalesDate].orEmpty()
            val lastYearOroRows = oroByKey[lastYearSalesDate].orEmpty()
            val targetSum = targetsByYear[y].orEmpty()
                .filter { it.targetMonth?.trim()?.toIntOrNull() == m }
                .sumOf { targetSumOf(it) }
            MonthlySalesDashboardSummaryResponse.MonthlyTrendPoint(
                salesYear = y,
                salesMonth = m,
                targetAmount = targetSum,
                achievedAmount = currentOroRows.sumOf { closingSum(it) },
                lastYearAchievedAmount = if (lastYearOroRows.isEmpty()) null
                else lastYearOroRows.sumOf { closingSum(it) },
            )
        }
    }

    private fun findAccounts(
        effectiveCodes: List<String>,
        accountGroup: String?,
        customerKeyword: String?,
    ): List<Account> {
        val candidates = if (accountGroup != null) {
            effectiveCodes.flatMap { code ->
                accountRepository.findByBranchCodeAndAccountGroupInAndIsDeletedNot(
                    branchCode = code,
                    accountGroups = listOf(accountGroup),
                    isDeleted = true,
                )
            }
        } else {
            accountRepository.findByBranchCodeIn(effectiveCodes)
        }
        return if (customerKeyword.isNullOrBlank()) candidates
        else candidates.filter { it.name?.contains(customerKeyword, ignoreCase = true) == true }
    }

    private fun buildCategorySales(
        oroRow: MonthlySalesRow?,
        target: SalesProgressRateMaster?,
    ): List<MonthlySalesDashboardDetailResponse.CategorySalesInfo> {
        if (oroRow == null) return emptyList()
        return SalesCategory.entries.map { category ->
            val achievedAmount = categoryAchieved(oroRow, category)
            val targetAmount = target?.let { categoryTarget(it, category) } ?: 0L
            MonthlySalesDashboardDetailResponse.CategorySalesInfo(
                category = category.name,
                targetAmount = targetAmount,
                achievedAmount = achievedAmount,
                achievementRate = rate(achievedAmount, targetAmount),
            )
        }
    }

    /**
     * 카테고리별 마감실적 — SF Apex `IF_REST_MOBILE_MonthlySalesHistory.cls` 원본 가공 로직 정합:
     * ABC 채널 (`ABCClosingAmount{N}`) + 물류 배부 채널 (`ShipClosingAmount{N}`) 명시적 합산.
     *
     * RDS `MonthlySalesHistory` 는 두 컬럼을 분리해 보관 — service 단에서 합산 책임 부담.
     * 본 helper 는 row 가 부재 (미적재) 일 때 0 반환.
     */
    private fun categoryAchieved(oro: MonthlySalesRow?, category: SalesCategory): Long {
        if (oro == null) return 0L
        val abc = when (category) {
            SalesCategory.AMBIENT -> oro.abcClosingAmount1
            SalesCategory.NOODLE -> oro.abcClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.abcClosingAmount3
            SalesCategory.OIL_FAT -> oro.abcClosingAmount4
        }
        val ship = when (category) {
            SalesCategory.AMBIENT -> oro.shipClosingAmount1
            SalesCategory.NOODLE -> oro.shipClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> oro.shipClosingAmount3
            SalesCategory.OIL_FAT -> oro.shipClosingAmount4
        }
        return (abc ?: BigDecimal.ZERO).toLong() + (ship ?: BigDecimal.ZERO).toLong()
    }

    /**
     * RDS row 의 마감 합계 실적 — SF `ClosingAmountSum__c` (ABC합 + Ship합) 동등.
     * 게이트웨이([MonthlySalesHistoryQueryGateway])가 원본 합계 컬럼을 더해 산출한 [MonthlySalesRow.closingAmountSum]
     * 을 그대로 사용 (개별 카테고리 재합산과 달리 물류매출 누락 없음). row 부재 시 0.
     * 모바일 「월 매출」([MonthlySalesService]) "마감 합계 실적" 정합.
     */
    private fun closingSum(oro: MonthlySalesRow?): Long =
        oro?.closingAmountSum?.toLong() ?: 0L

    /**
     * RDS row 의 카테고리 4종 `ShipClosingAmount` 합산 — 물류 배부 채널 누적 마감실적.
     *
     * SF Apex `IF_REST_MOBILE_MonthlySalesHistory.cls` 응답의 `ShipClosingSumAmount` 동등. row 부재 시 0.
     * [sumInvestedAccountSales] (여사원 투입현황 매출현황 탭, Spec 850 D7) 전용 — 월매출 대시보드는 ABC+Ship
     * 합계([closingSum]) 사용.
     */
    private fun shipClosingSum(oro: MonthlySalesRow?): Long {
        if (oro == null) return 0L
        return listOfNotNull(
            oro.shipClosingAmount1, oro.shipClosingAmount2,
            oro.shipClosingAmount3, oro.shipClosingAmount4,
        ).sumOf { it.toLong() }
    }

    /**
     * 조회 거래처의 (연, 월) 목표 1행 — RDS `SalesProgressRateMaster` (SF `SalesProgressRateMaster__c` 복제).
     * `targetYear` 는 `"YYYY"` 문자열, `targetMonth` 는 SF 적재 포맷(zero-pad 비보장) 이라 정수 파싱 후
     * 월 일치로 매칭. soft-delete row 제외. (모바일 [MonthlySalesService.getMonthlySales] 의 findTarget 정합)
     */
    private fun findTarget(accountId: Long, year: Int, month: Int): SalesProgressRateMaster? =
        salesProgressRateMasterRepository
            .findByAccountIdAndTargetYear(accountId, year.toString())
            .asSequence()
            .filter { it.isDeleted != true }
            .firstOrNull { it.targetMonth?.trim()?.toIntOrNull() == month }

    /**
     * 거래처 N건 + 해당 연도의 목표 행 일괄 조회 (soft-delete 제외). [findTarget] 의 batch 판 —
     * 월 매칭/그룹핑은 호출 측이 수행. account FK 미연결 행은 제외.
     */
    private fun findTargets(accountIds: List<Long>, year: Int): List<SalesProgressRateMaster> {
        if (accountIds.isEmpty()) return emptyList()
        return salesProgressRateMasterRepository
            .findByAccountIdInAndTargetYear(accountIds, year.toString())
            .filter { it.isDeleted != true && it.account?.id != null }
    }

    /**
     * 거래처 N건의 (연, 월) 목표 행 일괄 조회 → accountId 키 map.
     * 동일 거래처에 (연, 월) 행이 복수면 첫 행 채택 (단건 [findTarget] 의 firstOrNull 정합).
     */
    private fun findTargetMap(accountIds: List<Long>, year: Int, month: Int): Map<Long, SalesProgressRateMaster> =
        findTargets(accountIds, year)
            .filter { it.targetMonth?.trim()?.toIntOrNull() == month }
            .groupBy { it.account!!.id }
            .mapValues { (_, rows) -> rows.first() }

    /** 카테고리별 목표 — SF `RT/RM/FR/FO TargetAmount__c` 정합. */
    private fun categoryTarget(target: SalesProgressRateMaster, category: SalesCategory): Long {
        val amount = when (category) {
            SalesCategory.AMBIENT -> target.rtTargetAmount
            SalesCategory.NOODLE -> target.rmTargetAmount
            SalesCategory.FROZEN_REFRIGERATED -> target.frTargetAmount
            SalesCategory.OIL_FAT -> target.foTargetAmount
        }
        return amount?.toLong() ?: 0L
    }

    /** 목표 합계 — SF `TargetSum__c` (= RT + FR + RM + FO) 동등. */
    private fun targetSumOf(target: SalesProgressRateMaster): Long =
        ((target.rtTargetAmount ?: 0.0) +
            (target.frTargetAmount ?: 0.0) +
            (target.rmTargetAmount ?: 0.0) +
            (target.foTargetAmount ?: 0.0)).toLong()

    /** 달성률 — `round(실적 / 목표 × 100)`. 목표 0 이면 0 (NaN/Infinity 방지, 레거시 정합). */
    private fun rate(achieved: Long, target: Long): Double =
        if (target <= 0L) 0.0 else Math.round(achieved.toDouble() / target * 100).toDouble()

    /**
     * 영업일 기반 기준 진도율 — 평일 (월~금) 기준 1일~오늘 / 1일~월말.
     *
     * 과거 월 조회 시 100.0 / 미래 월 조회 시 0.0 / 당월 조회 시 영업일 비율.
     * 본 helper 는 휴일 처리 정밀화 없이 평일 기반 단순 산출 — 후속 보강 검토 (#770 의 `BusinessDayCalculator` 도입 시 위임 대체).
     */
    internal fun referenceAchievementRate(year: Int, month: Int, today: LocalDate): Double {
        val firstDay = LocalDate.of(year, month, 1)
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        if (today.isAfter(lastDay)) return 100.0
        if (today.isBefore(firstDay)) return 0.0
        val totalBusinessDays = countBusinessDays(firstDay, lastDay)
        val elapsedBusinessDays = countBusinessDays(firstDay, today)
        if (totalBusinessDays == 0) return 0.0
        return (elapsedBusinessDays.toDouble() / totalBusinessDays.toDouble()) * 100.0
    }

    private fun countBusinessDays(from: LocalDate, to: LocalDate): Int {
        var count = 0
        var cursor = from
        while (!cursor.isAfter(to)) {
            val dow = cursor.dayOfWeek.value
            if (dow in 1..5) count++
            cursor = cursor.plusDays(1)
        }
        return count
    }

    internal fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

    private fun validateParams(year: Int, month: Int, costCenterCodes: List<String>) {
        validateYearMonth(year, month)
        if (costCenterCodes.isEmpty()) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "cost_center_codes는 필수입니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    private fun validateYearMonth(year: Int, month: Int) {
        if (year !in 2019..2099) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "year는 2019~2099 범위여야 합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        if (month !in 1..12) {
            throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "month는 1~12 범위여야 합니다",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    /**
     * 여사원 투입 거래처의 마감실적 합산 — 투입현황 대시보드 매출현황 탭 (Spec 850, 결정 D7).
     *
     * 입력 account 집합의 당월/전년 동월 `ShipClosing` 마감실적을 RDS `MonthlySalesHistory`
     * ([MonthlySalesHistoryQueryGateway]) 1 trip 으로 일괄 합산 — 외부 ORORA view 직접 호출 아님.
     * RDS 조회 키는 (`sap_account_code`, `sales_year`, `sales_month`) 복합 인덱스로 가속된다.
     * 목표(target)는 투입 거래처별 `SalesProgressRateMaster`(연·월 1행) 합계의 총합 — 상단 요약([getSummary]) 정합.
     * 미등록 거래처는 0. row 부재 (미적재) 시 실적/전년 0 반환.
     * 부수 효과: 없음 (조회 전용).
     */
    fun sumInvestedAccountSales(accounts: List<Account>, year: Int, month: Int): InvestedAccountSales {
        val accountSapCodes = accounts.mapNotNull { it.externalKey }
        if (accountSapCodes.isEmpty()) {
            return InvestedAccountSales(
                actualAmount = 0L, targetAmount = 0L, lastYearAmount = 0L,
                hasActualData = false, hasLastYearData = false,
            )
        }
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDates(listOf(currentSalesDate, lastYearSalesDate), accountSapCodes)
            .associateBy { it.sapAccountCode to it.salesDate }

        val actual = accounts.sumOf { shipClosingSum(oroByKey[it.externalKey to currentSalesDate]) }
        val lastYear = accounts.sumOf { shipClosingSum(oroByKey[it.externalKey to lastYearSalesDate]) }

        // 적재 데이터 존재 여부 — row 부재(미적재)와 실제 0/음수를 구분하기 위함.
        // 투입 거래처 중 해당 월 RDS row 가 하나라도 있으면 데이터 존재로 본다.
        val hasActualData = accounts.any { oroByKey.containsKey(it.externalKey to currentSalesDate) }
        val hasLastYearData = accounts.any { oroByKey.containsKey(it.externalKey to lastYearSalesDate) }

        // 목표 합계 — 투입 거래처별 (연, 월) SalesProgressRateMaster 1행 합계의 총합 (미등록 거래처는 0).
        val accountIds = accounts.mapNotNull { it.id }
        val targetByAccountId = findTargetMap(accountIds, year, month)
        val target = targetByAccountId.values.sumOf { targetSumOf(it) }

        return InvestedAccountSales(
            actualAmount = actual, targetAmount = target, lastYearAmount = lastYear,
            hasActualData = hasActualData, hasLastYearData = hasLastYearData,
        )
    }

    /**
     * 투입 거래처 매출 실적 합산 결과 (당월 실적/목표/전년 동월).
     * has*Data 는 RDS row 적재 여부 — 0원이 "미적재"인지 "실제 0"인지 구분하는 데 쓴다.
     */
    data class InvestedAccountSales(
        val actualAmount: Long,
        val targetAmount: Long,
        val lastYearAmount: Long,
        val hasActualData: Boolean,
        val hasLastYearData: Boolean,
    )

    /**
     * 매출월 매칭 키 (`YYYYMM` 6자) 생성. [MonthlySalesHistoryQueryGateway] 가 이를
     * `SalesYear` / `SalesMonth` picklist 조합으로 변환해 RDS 조회 키로 사용.
     */
    private fun toSalesDate(year: Int, month: Int): String = "%04d%02d".format(year, month)

    companion object {
        private const val MILLION = 1_000_000L
    }
}
