package com.otoki.powersales.sales.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.sales.dto.request.MonthlySalesDashboardListRequest
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardDetailResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardListItem
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardListResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardSummaryResponse
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 월매출 대시보드 admin 조회 service.
 *
 * 권한 범위 거래처 N건의 월매출 이력을 집계 + 거래처별 명세 + 단건 상세 형태로 반환한다.
 * 레거시 매핑: 모바일 「월매출조회-물류배부」 페이지 (docs/plan/legacy-pages-heroku/월매출조회-물류배부) 의 9개 UC 를 web admin 으로 확장 — SF chain 제거 + RDS 직접 조회 + 전체 거래처 집계 KPI / 월별 추이 차트 신규 추가.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — admin 측 대시보드 view 는 레거시 미존재 (모바일 페이지의 web admin 확장). origin spec: #776.
 */
@Service
@Transactional(readOnly = true)
class MonthlySalesAdminQueryService(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * 상단 KPI + 최근 6개월 월별 추이 조회.
     *
     * 권한 범위 거래처의 당월 목표 / 실적 / 진도율 합산 + 전년 동월 비교 + 기준 진도율 (영업일 기반) 산출.
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

        val salesYear = toSalesYear(year)
        val salesMonth = toSalesMonth(month)
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
                monthlyTrend = buildMonthlyTrend(year, month, emptyList(), emptyList()),
            )
        }

        val currentMonthRows = monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthAndAccountIn(salesYear, salesMonth, accounts)

        val totalTarget = currentMonthRows.sumOf { (it.thisMonthTarget ?: BigDecimal.ZERO).toLong() }
        val totalAchieved = currentMonthRows.sumOf { shipClosingSum(it) }
        val overallRate = computeAchievementRate(totalAchieved, totalTarget)
        val reference = referenceAchievementRate(year, month, LocalDate.now())

        val lastYearSalesYear = toSalesYearOrNull(year - 1)
        val lastYearRows = if (lastYearSalesYear != null) {
            monthlySalesHistoryRepository
                .findBySalesYearAndSalesMonthAndAccountIn(lastYearSalesYear, salesMonth, accounts)
        } else {
            emptyList()
        }
        val totalLastYearAchieved = if (lastYearRows.isEmpty()) null else lastYearRows.sumOf { shipClosingSum(it) }
        val lastYearRatio = if (totalLastYearAchieved == null || totalLastYearAchieved == 0L) null
        else (totalAchieved.toDouble() / totalLastYearAchieved.toDouble()) * 100.0

        val trend = buildMonthlyTrend(year, month, accounts, currentMonthRows + lastYearRows)

        return MonthlySalesDashboardSummaryResponse(
            salesYear = year,
            salesMonth = month,
            totalTargetAmount = totalTarget,
            totalAchievedAmount = totalAchieved,
            overallAchievementRate = overallRate,
            referenceAchievementRate = reference,
            totalLastYearAchievedAmount = totalLastYearAchieved,
            lastYearComparisonRatio = lastYearRatio,
            monthlyTrend = trend,
        )
    }

    /**
     * 하단 거래처별 명세 조회 — 페이징 + 정렬 + 필터.
     *
     * 권한 범위 거래처 N건의 당월 매출 이력 row 별 카테고리 4종 (상온/라면/냉동냉장/유지) 마감실적 + 전년 동월 비교 + 마감 상태 응답.
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
     *
     * @throws AdminForbiddenException 거래처가 권한 범위 밖일 때
     */
    fun getDetail(scope: DataScope, customerId: Int, year: Int, month: Int): MonthlySalesDashboardDetailResponse {
        validateYearMonth(year, month)
        val account = accountRepository.findByIdInAndIsDeletedNot(listOf(customerId), true).firstOrNull()
            ?: throw BusinessException(
                errorCode = "ACCOUNT_NOT_FOUND",
                message = "거래처를 찾을 수 없습니다: $customerId",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        if (!scope.validateAccess(account.branchCode)) throw AdminForbiddenException()

        val salesYear = toSalesYear(year)
        val salesMonth = toSalesMonth(month)
        val currentRow = monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthAndAccountIn(salesYear, salesMonth, listOf(account))
            .firstOrNull()

        val target = (currentRow?.thisMonthTarget ?: BigDecimal.ZERO).toLong()
        val achieved = currentRow?.let { shipClosingSum(it) } ?: 0L
        val rate = computeAchievementRate(achieved, target)
        val reference = referenceAchievementRate(year, month, LocalDate.now())

        val today = LocalDate.now()
        val isPastMonth = year < today.year || (year == today.year && month < today.monthValue)
        val categorySales = if (isPastMonth) buildCategorySales(currentRow) else emptyList()

        val lastYearSalesYear = toSalesYearOrNull(year - 1)
        val lastYearRow = if (lastYearSalesYear != null) {
            monthlySalesHistoryRepository
                .findBySalesYearAndSalesMonthAndAccountIn(lastYearSalesYear, salesMonth, listOf(account))
                .firstOrNull()
        } else null
        val lastYearAchieved = lastYearRow?.let { shipClosingSum(it) } ?: 0L
        val yearComparison = MonthlySalesDashboardDetailResponse.YearComparisonInfo(
            currentYear = achieved / MILLION,
            previousYear = lastYearAchieved / MILLION,
        )

        // 1월~조회월 누적 평균 (백만원 단위 절사)
        val months = (1..month).map { toSalesMonth(it) }
        val currentRange = monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthInAndAccountIn(salesYear, months, listOf(account))
        val currentAvg = if (months.isEmpty()) 0L else currentRange.sumOf { shipClosingSum(it) } / months.size
        val previousRange = if (lastYearSalesYear != null) {
            monthlySalesHistoryRepository
                .findBySalesYearAndSalesMonthInAndAccountIn(lastYearSalesYear, months, listOf(account))
        } else emptyList()
        val previousAvg = if (months.isEmpty()) 0L else previousRange.sumOf { shipClosingSum(it) } / months.size
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
            targetAmount = target,
            achievedAmount = achieved,
            achievementRate = rate,
            referenceAchievementRate = reference,
            categorySales = categorySales,
            yearComparison = yearComparison,
            monthlyAverage = monthlyAverage,
        )
    }

    // ------------------- helpers -------------------

    private fun buildListItems(effectiveCodes: List<String>, request: MonthlySalesDashboardListRequest): List<MonthlySalesDashboardListItem> {
        val salesYear = toSalesYear(request.year)
        val salesMonth = toSalesMonth(request.month)
        val accounts = findAccounts(effectiveCodes, request.accountGroup, request.customerKeyword)
            .let { all ->
                if (request.accountIds.isEmpty()) all else all.filter { it.id in request.accountIds }
            }
        if (accounts.isEmpty()) return emptyList()

        val currentRows = monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthAndAccountIn(salesYear, salesMonth, accounts)
            .associateBy { it.account?.id }
        val lastYearSalesYear = toSalesYearOrNull(request.year - 1)
        val lastYearRows = if (lastYearSalesYear != null) {
            monthlySalesHistoryRepository
                .findBySalesYearAndSalesMonthAndAccountIn(lastYearSalesYear, salesMonth, accounts)
                .associateBy { it.account?.id }
        } else emptyMap()

        return accounts.map { account ->
            val current = currentRows[account.id]
            val lastYear = lastYearRows[account.id]
            val target = current?.thisMonthTarget?.toLong()
            val achieved = current?.let { shipClosingSum(it) }
            val rate = if (target != null && achieved != null) computeAchievementRate(achieved, target) else null
            val lastYearAchieved = lastYear?.let { shipClosingSum(it) }
            val lastYearRatio = if (achieved != null && lastYearAchieved != null && lastYearAchieved > 0)
                (achieved.toDouble() / lastYearAchieved.toDouble()) * 100.0 else null

            MonthlySalesDashboardListItem(
                accountId = account.id,
                accountSfid = account.sfid,
                accountName = account.name,
                sapAccountCode = current?.sapAccountCode,
                branchCode = account.branchCode,
                branchName = account.branchName,
                salesYear = request.year,
                salesMonth = request.month,
                targetAmount = target,
                totalAchievedAmount = achieved,
                achievementRate = rate,
                ambientAchievedAmount = current?.let { categoryAchieved(it, SalesCategory.AMBIENT) },
                noodleAchievedAmount = current?.let { categoryAchieved(it, SalesCategory.NOODLE) },
                frozenRefrigeratedAchievedAmount = current?.let { categoryAchieved(it, SalesCategory.FROZEN_REFRIGERATED) },
                oilFatAchievedAmount = current?.let { categoryAchieved(it, SalesCategory.OIL_FAT) },
                lastYearAchievedAmount = lastYearAchieved,
                lastYearComparisonRatio = lastYearRatio,
                isConfirmed = current?.isConfirmed ?: false,
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
        precomputed: List<MonthlySalesHistory>,
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

        // 효율: 6개월 + 전년 동월 6개월 = 12개월. salesYear/salesMonth enum 변환 가능한 것만.
        val targetSalesYears = keys.mapNotNull { toSalesYearOrNull(it.first) }.distinct()
        val targetSalesMonths = keys.mapNotNull { toSalesMonthOrNull(it.second) }.distinct()
        val lastYearKeys = keys.map { (y, m) -> (y - 1) to m }
        val lastYearSalesYears = lastYearKeys.mapNotNull { toSalesYearOrNull(it.first) }.distinct()

        val currentRows = (targetSalesYears + lastYearSalesYears).distinct().flatMap { sy ->
            monthlySalesHistoryRepository
                .findBySalesYearAndSalesMonthInAndAccountIn(sy, targetSalesMonths, accounts)
        }.groupBy { (it.salesYear?.value?.toIntOrNull() ?: 0) to (it.salesMonth?.value?.toIntOrNull() ?: 0) }

        return keys.map { (y, m) ->
            val current = currentRows[y to m].orEmpty()
            val lastYear = currentRows[(y - 1) to m].orEmpty()
            MonthlySalesDashboardSummaryResponse.MonthlyTrendPoint(
                salesYear = y,
                salesMonth = m,
                targetAmount = current.sumOf { (it.thisMonthTarget ?: BigDecimal.ZERO).toLong() },
                achievedAmount = current.sumOf { shipClosingSum(it) },
                lastYearAchievedAmount = if (lastYear.isEmpty()) null else lastYear.sumOf { shipClosingSum(it) },
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

    private fun buildCategorySales(row: MonthlySalesHistory?): List<MonthlySalesDashboardDetailResponse.CategorySalesInfo> {
        if (row == null) return emptyList()
        return SalesCategory.entries.map { category ->
            val targetAmount = categoryTarget(row, category)
            val achievedAmount = categoryAchieved(row, category)
            MonthlySalesDashboardDetailResponse.CategorySalesInfo(
                category = category.name,
                targetAmount = targetAmount,
                achievedAmount = achievedAmount,
                achievementRate = computeAchievementRate(achievedAmount, targetAmount),
            )
        }
    }

    /**
     * 카테고리별 마감실적 — ABC 채널 + 물류 배부 채널 합산값 (`ABCClosingAmount{N}__c` 가 실제 ABC + Ship 합산임 — SF IF_REST_MOBILE_MonthlySalesHistory.cls 가공 로직).
     */
    private fun categoryAchieved(row: MonthlySalesHistory, category: SalesCategory): Long {
        val value = when (category) {
            SalesCategory.AMBIENT -> row.abcClosingAmount1
            SalesCategory.NOODLE -> row.abcClosingAmount2
            SalesCategory.FROZEN_REFRIGERATED -> row.abcClosingAmount3
            SalesCategory.OIL_FAT -> row.abcClosingAmount4
        }
        return (value ?: 0.0).toLong()
    }

    /**
     * 카테고리별 목표 — 현 entity 에 카테고리별 목표 필드가 없어 합계 목표를 카테고리 수로 균등 분배.
     *
     * 레거시는 FridgePurpose / AmbientPurpose 등 별도 필드가 있으나 신규 entity 에서는 일부만 매핑되어 있고 4종 모두 대응되지 않음. 후속 보강 검토 필요 (#776 분석 한계).
     */
    private fun categoryTarget(row: MonthlySalesHistory, category: SalesCategory): Long {
        val total = (row.thisMonthTarget ?: BigDecimal.ZERO).toLong()
        return total / SalesCategory.entries.size
    }

    private fun shipClosingSum(row: MonthlySalesHistory): Long {
        // 우선순위: 합계 필드 → 카테고리 합산 (합계가 0/null 일 때 대체)
        val sum = row.shipClosingSumAmount
        if (sum != null && sum != 0.0) return sum.toLong()
        return listOfNotNull(
            row.shipClosingAmount1, row.shipClosingAmount2,
            row.shipClosingAmount3, row.shipClosingAmount4
        ).sumOf { it.toLong() }
    }

    private fun computeAchievementRate(achieved: Long, target: Long): Double {
        if (target <= 0L) return 0.0
        return (achieved.toDouble() / target.toDouble()) * 100.0
    }

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

    private fun toSalesYear(year: Int): SalesYear =
        SalesYear.fromValueOrNull(year.toString())
            ?: throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "지원하지 않는 매출연도: $year",
                httpStatus = HttpStatus.BAD_REQUEST,
            )

    private fun toSalesYearOrNull(year: Int): SalesYear? = SalesYear.fromValueOrNull(year.toString())

    private fun toSalesMonth(month: Int): SalesMonth =
        SalesMonth.fromValueOrNull(month.toString().padStart(2, '0'))
            ?: throw BusinessException(
                errorCode = "INVALID_PARAMETER",
                message = "지원하지 않는 매출월: $month",
                httpStatus = HttpStatus.BAD_REQUEST,
            )

    private fun toSalesMonthOrNull(month: Int): SalesMonth? =
        SalesMonth.fromValueOrNull(month.toString().padStart(2, '0'))

    companion object {
        private const val MILLION = 1_000_000L
    }
}

/**
 * 매출 카테고리 — 레거시 SF `IF_REST_MOBILE_MonthlySalesHistory.cls` 가공 로직 정합.
 *
 * 카테고리 1~4 의 매출 합산은 entity 의 ABCClosingAmount1~4 필드에 매핑된다 (ABC 채널 + 물류 Ship 합산값).
 */
enum class SalesCategory {
    AMBIENT,
    NOODLE,
    FROZEN_REFRIGERATED,
    OIL_FAT,
}
