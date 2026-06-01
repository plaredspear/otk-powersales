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
import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 월매출 대시보드 admin 조회 service.
 *
 * ## 데이터 source
 * RDS `MonthlySalesHistory` (SF `MonthlySalesHistory__c` 복제 적재) 100% 의존
 * ([MonthlySalesHistoryQueryGateway] 경유). SF 레거시도 화면 조회가 ORORA 직접이 아닌
 * `MonthlySalesHistory__c` SObject 를 읽은 것과 동등.
 * 목표 (`thisMonthTarget`) / 확정 상태 (`isConfirmed`) 는 폐기 — 응답 호환성 유지를 위해
 * `targetAmount = null`, `achievementRate = null`, `isConfirmed = false` 로 고정.
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
) {

    /**
     * 상단 KPI + 최근 6개월 월별 추이 조회.
     *
     * 권한 범위 거래처의 당월 마감실적 합산 + 전년 동월 비교 + 기준 진도율 (영업일 기반) 산출.
     * 목표 / 진도율은 폐기 — `totalTargetAmount = 0`, `overallAchievementRate = 0.0`.
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

        // RDS fetch — 당월 + 전년 동월 일괄 1 trip
        val accountSapCodes = accounts.mapNotNull { it.externalKey }
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDates(listOf(currentSalesDate, lastYearSalesDate), accountSapCodes)
            .associateBy { it.sapAccountCode to it.salesDate }

        val totalAchieved = accounts.sumOf { acc ->
            shipClosingSum(oroByKey[acc.externalKey to currentSalesDate])
        }
        val totalLastYearAchieved = accounts
            .sumOf { acc -> shipClosingSum(oroByKey[acc.externalKey to lastYearSalesDate]) }
            .takeIf { it > 0L }
        val lastYearRatio = if (totalLastYearAchieved == null || totalLastYearAchieved == 0L) null
        else (totalAchieved.toDouble() / totalLastYearAchieved.toDouble()) * 100.0

        return MonthlySalesDashboardSummaryResponse(
            salesYear = year,
            salesMonth = month,
            totalTargetAmount = 0L,
            totalAchievedAmount = totalAchieved,
            overallAchievementRate = 0.0,
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
     * 목표 / 진도율은 폐기.
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

        // RDS fetch — 당월 + 전년 + 1~조회월 누적 일괄 1 trip
        val accountSap = account.externalKey?.let { listOf(it) } ?: emptyList()
        val months = (1..month).toList()
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val currentRangeSalesDates = months.map { toSalesDate(year, it) }
        val previousRangeSalesDates = months.map { toSalesDate(year - 1, it) }
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDates(
                (currentRangeSalesDates + previousRangeSalesDates).distinct(),
                accountSap,
            )
            .associateBy { it.sapAccountCode to it.salesDate }
        val currentOro = oroByKey[account.externalKey to currentSalesDate]
        val lastYearOro = oroByKey[account.externalKey to lastYearSalesDate]

        val achieved = shipClosingSum(currentOro)

        val today = LocalDate.now()
        val isPastMonth = year < today.year || (year == today.year && month < today.monthValue)
        val categorySales = if (isPastMonth) buildCategorySales(currentOro) else emptyList()

        val lastYearAchieved = shipClosingSum(lastYearOro)
        val yearComparison = MonthlySalesDashboardDetailResponse.YearComparisonInfo(
            currentYear = achieved / MILLION,
            previousYear = lastYearAchieved / MILLION,
        )

        // 1월~조회월 누적 평균 (백만원 단위 절사) — RDS row 기반
        val currentAvg = if (months.isEmpty()) 0L
        else currentRangeSalesDates.sumOf { sd -> shipClosingSum(oroByKey[account.externalKey to sd]) } / months.size
        val previousAvg = if (months.isEmpty()) 0L
        else previousRangeSalesDates.sumOf { sd -> shipClosingSum(oroByKey[account.externalKey to sd]) } / months.size
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
            targetAmount = 0L,
            achievedAmount = achieved,
            achievementRate = 0.0,
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

        // RDS fetch — 당월 + 전년 동월 일괄 1 trip
        val currentSalesDate = toSalesDate(request.year, request.month)
        val lastYearSalesDate = toSalesDate(request.year - 1, request.month)
        val accountSapCodes = accounts.mapNotNull { it.externalKey }
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDates(listOf(currentSalesDate, lastYearSalesDate), accountSapCodes)
            .associateBy { it.sapAccountCode to it.salesDate }

        return accounts.map { account ->
            val currentOro = oroByKey[account.externalKey to currentSalesDate]
            val lastYearOro = oroByKey[account.externalKey to lastYearSalesDate]

            val achieved = shipClosingSum(currentOro)
            val lastYearAchieved = shipClosingSum(lastYearOro)
            val lastYearRatio = if (lastYearAchieved > 0)
                (achieved.toDouble() / lastYearAchieved.toDouble()) * 100.0 else null

            MonthlySalesDashboardListItem(
                accountId = account.id,
                accountName = account.name,
                sapAccountCode = account.externalKey,
                branchCode = account.branchCode,
                branchName = account.branchName,
                salesYear = request.year,
                salesMonth = request.month,
                targetAmount = null,
                totalAchievedAmount = achieved,
                achievementRate = null,
                ambientAchievedAmount = categoryAchieved(currentOro, SalesCategory.AMBIENT),
                noodleAchievedAmount = categoryAchieved(currentOro, SalesCategory.NOODLE),
                frozenRefrigeratedAchievedAmount = categoryAchieved(currentOro, SalesCategory.FROZEN_REFRIGERATED),
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
        val accountSapCodes = accounts.mapNotNull { it.externalKey }
        val allSalesDates = (keys + lastYearKeys).map { (y, m) -> toSalesDate(y, m) }.distinct()
        val oroByKey = monthlySalesHistoryGateway.findBySalesDates(allSalesDates, accountSapCodes)
            .groupBy { it.salesDate }

        return keys.map { (y, m) ->
            val currentSalesDate = toSalesDate(y, m)
            val lastYearSalesDate = toSalesDate(y - 1, m)
            val currentOroRows = oroByKey[currentSalesDate].orEmpty()
            val lastYearOroRows = oroByKey[lastYearSalesDate].orEmpty()
            MonthlySalesDashboardSummaryResponse.MonthlyTrendPoint(
                salesYear = y,
                salesMonth = m,
                targetAmount = 0L,
                achievedAmount = currentOroRows.sumOf { shipClosingSum(it) },
                lastYearAchievedAmount = if (lastYearOroRows.isEmpty()) null
                else lastYearOroRows.sumOf { shipClosingSum(it) },
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
    ): List<MonthlySalesDashboardDetailResponse.CategorySalesInfo> {
        if (oroRow == null) return emptyList()
        return SalesCategory.entries.map { category ->
            MonthlySalesDashboardDetailResponse.CategorySalesInfo(
                category = category.name,
                targetAmount = 0L,
                achievedAmount = categoryAchieved(oroRow, category),
                achievementRate = 0.0,
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
     * RDS row 의 카테고리 4종 `ShipClosingAmount` 합산 — 물류 배부 채널 누적 마감실적.
     *
     * SF Apex `IF_REST_MOBILE_MonthlySalesHistory.cls` 응답의 `ShipClosingSumAmount` 동등.
     * row 가 부재 시 0 반환.
     */
    private fun shipClosingSum(oro: MonthlySalesRow?): Long {
        if (oro == null) return 0L
        return listOfNotNull(
            oro.shipClosingAmount1, oro.shipClosingAmount2,
            oro.shipClosingAmount3, oro.shipClosingAmount4,
        ).sumOf { it.toLong() }
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

    /**
     * 여사원 투입 거래처의 마감실적 합산 — 투입현황 대시보드 매출현황 탭 (Spec 850, 결정 D7).
     *
     * 입력 account 집합의 당월/전년 동월 `ShipClosing` 마감실적을 RDS 1 trip 으로 일괄 합산.
     * 목표(target)/진도율은 신규 시스템 데이터 부재로 제외 — 실적 + 전년 비교만 산출 (D7).
     * row 부재 (미적재) 시 0 반환.
     * 부수 효과: 없음 (조회 전용).
     */
    fun sumInvestedAccountSales(accounts: List<Account>, year: Int, month: Int): InvestedAccountSales {
        val accountSapCodes = accounts.mapNotNull { it.externalKey }
        if (accountSapCodes.isEmpty()) {
            return InvestedAccountSales(actualAmount = 0L, lastYearAmount = 0L)
        }
        val currentSalesDate = toSalesDate(year, month)
        val lastYearSalesDate = toSalesDate(year - 1, month)
        val oroByKey = monthlySalesHistoryGateway
            .findBySalesDates(listOf(currentSalesDate, lastYearSalesDate), accountSapCodes)
            .associateBy { it.sapAccountCode to it.salesDate }

        val actual = accounts.sumOf { shipClosingSum(oroByKey[it.externalKey to currentSalesDate]) }
        val lastYear = accounts.sumOf { shipClosingSum(oroByKey[it.externalKey to lastYearSalesDate]) }
        return InvestedAccountSales(actualAmount = actual, lastYearAmount = lastYear)
    }

    /** 투입 거래처 매출 실적 합산 결과 (당월/전년 동월). */
    data class InvestedAccountSales(
        val actualAmount: Long,
        val lastYearAmount: Long,
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
