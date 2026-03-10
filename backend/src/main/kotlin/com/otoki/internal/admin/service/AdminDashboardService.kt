package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.entity.MonthlySalesHistory
import com.otoki.internal.sap.repository.MonthlySalesHistoryRepository
import com.otoki.internal.common.entity.StoreSchedule
import com.otoki.internal.common.repository.StoreScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Service
@Transactional(readOnly = true)
class AdminDashboardService(
    private val dataScopeHolder: DataScopeHolder,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val storeScheduleRepository: StoreScheduleRepository,
    private val accountRepository: AccountRepository,
    private val userRepository: UserRepository
) {

    fun getDashboard(yearMonth: String?, branchCode: String?): DashboardResponse {
        val scope = dataScopeHolder.require()
        val effectiveScope = applyBranchFilter(scope, branchCode)
        val ym = yearMonth ?: YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val branchName = resolveBranchName(effectiveScope)

        return DashboardResponse(
            salesSummary = buildSalesSummary(effectiveScope, ym, branchName),
            staffDeployment = buildStaffDeployment(effectiveScope, ym, branchName),
            basicStats = buildBasicStats(effectiveScope, branchName)
        )
    }

    private fun applyBranchFilter(scope: DataScope, branchCode: String?): DataScope {
        if (branchCode == null) return scope
        if (!scope.isAllBranches && branchCode !in scope.branchCodes) {
            return scope
        }
        return DataScope(branchCodes = listOf(branchCode), isAllBranches = false)
    }

    private fun resolveBranchName(scope: DataScope): String? {
        if (scope.isAllBranches) return null
        if (scope.branchCodes.isEmpty()) return null
        val account = accountRepository.findByBranchCodeIn(scope.branchCodes).firstOrNull()
        return account?.branchName
    }

    // --- Sales Summary ---

    private fun buildSalesSummary(scope: DataScope, yearMonth: String, branchName: String?): SalesSummary {
        val parts = yearMonth.split("-")
        val salesYear = parts[0]
        val salesMonth = parts[1]

        val salesData = fetchSalesData(scope, salesYear, salesMonth)
        val lastYearData = fetchSalesData(scope, (salesYear.toInt() - 1).toString(), salesMonth)

        val targetAmount = salesData.sumOf { it.targetMonthResults?.toLong() ?: 0L }
        val actualAmount = salesData.sumOf { it.shipClosingAmount?.toLong() ?: 0L }
        val progressRate = if (targetAmount > 0) actualAmount.toDouble() / targetAmount * 100 else 0.0
        val referenceProgressRate = calculateReferenceProgressRate(salesYear.toInt(), salesMonth.toInt())
        val lastYearAmount = lastYearData.sumOf { it.shipClosingAmount?.toLong() ?: 0L }
        val lastYearRatio = if (lastYearAmount > 0) actualAmount.toDouble() / lastYearAmount * 100 else 0.0

        val channelSales = buildChannelSales(salesData)

        return SalesSummary(
            yearMonth = yearMonth,
            branchName = branchName,
            targetAmount = targetAmount,
            actualAmount = actualAmount,
            progressRate = round2(progressRate),
            referenceProgressRate = round2(referenceProgressRate),
            lastYearAmount = lastYearAmount,
            lastYearRatio = round2(lastYearRatio),
            channelSales = channelSales
        )
    }

    private fun fetchSalesData(scope: DataScope, salesYear: String, salesMonth: String): List<MonthlySalesHistory> {
        return if (scope.isAllBranches) {
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonth(salesYear, salesMonth)
        } else {
            if (scope.branchCodes.isEmpty()) return emptyList()
            val accounts = accountRepository.findByBranchCodeIn(scope.branchCodes)
            val externalKeys = accounts.mapNotNull { it.externalKey }
            if (externalKeys.isEmpty()) return emptyList()
            monthlySalesHistoryRepository.findBySalesYearAndSalesMonthAndAccountExternalKeyIn(
                salesYear, salesMonth, externalKeys
            )
        }
    }

    private fun buildChannelSales(salesData: List<MonthlySalesHistory>): List<ChannelSalesItem> {
        val accountExternalKeys = salesData.mapNotNull { it.accountExternalKey }.distinct()
        val accountMap = if (accountExternalKeys.isNotEmpty()) {
            accountRepository.findAll()
                .filter { it.externalKey != null && it.externalKey in accountExternalKeys }
                .associateBy { it.externalKey!! }
        } else {
            emptyMap()
        }

        val grouped = salesData.groupBy { record ->
            val account = record.accountExternalKey?.let { accountMap[it] }
            classifyChannel(account?.abcType)
        }

        return grouped.map { (channelName, records) ->
            val target = records.sumOf { it.targetMonthResults?.toLong() ?: 0L }
            val actual = records.sumOf {
                (it.abcClosingAmount1?.toLong() ?: 0L) +
                (it.abcClosingAmount2?.toLong() ?: 0L) +
                (it.abcClosingAmount3?.toLong() ?: 0L)
            }
            val rate = if (target > 0) actual.toDouble() / target * 100 else 0.0
            ChannelSalesItem(
                channelName = channelName,
                targetAmount = target,
                actualAmount = actual,
                progressRate = round2(rate)
            )
        }.sortedBy { it.channelName }
    }

    private fun classifyChannel(abcType: String?): String {
        return when {
            abcType == null -> "기타"
            abcType.contains("대형마트") || abcType.contains("할인점") -> "대형마트"
            abcType.contains("슈퍼") -> "슈퍼"
            abcType.contains("편의점") -> "편의점"
            else -> "기타"
        }
    }

    private fun calculateReferenceProgressRate(year: Int, month: Int): Double {
        val today = LocalDate.now()
        val ym = YearMonth.of(year, month)
        val totalDays = ym.lengthOfMonth()
        val currentDay = if (today.year == year && today.monthValue == month) today.dayOfMonth else totalDays
        return currentDay.toDouble() / totalDays * 100
    }

    // --- Staff Deployment ---

    private fun buildStaffDeployment(scope: DataScope, yearMonth: String, branchName: String?): StaffDeployment {
        val ym = YearMonth.parse(yearMonth)
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        val schedules = fetchScheduleData(scope, monthStart, monthEnd)
        val accountSfids = schedules.mapNotNull { it.account }.distinct()
        val accountMap = if (accountSfids.isNotEmpty()) {
            accountRepository.findBySfidIn(accountSfids).associateBy { it.sfid }
        } else {
            emptyMap()
        }

        val byAccountType = buildByAccountType(schedules, accountMap)
        val byWorkType = buildByWorkType(schedules)
        val byChannelAndWorkType = buildByChannelAndWorkType(schedules, accountMap)

        val prevYm = ym.minusMonths(1)
        val prevStart = prevYm.atDay(1)
        val prevEnd = prevYm.atEndOfMonth()
        val prevSchedules = fetchScheduleData(scope, prevStart, prevEnd)
        val prevByWorkType = buildByWorkType(prevSchedules)

        return StaffDeployment(
            yearMonth = yearMonth,
            branchName = branchName,
            byAccountType = byAccountType,
            byWorkType = byWorkType,
            byChannelAndWorkType = byChannelAndWorkType,
            previousMonth = PreviousMonthData(byWorkType = prevByWorkType)
        )
    }

    private fun fetchScheduleData(scope: DataScope, monthStart: LocalDate, monthEnd: LocalDate): List<StoreSchedule> {
        return if (scope.isAllBranches) {
            storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(monthEnd, monthStart)
        } else {
            if (scope.branchCodes.isEmpty()) return emptyList()
            val accounts = accountRepository.findByBranchCodeIn(scope.branchCodes)
            val accountSfids = accounts.mapNotNull { it.sfid }
            if (accountSfids.isEmpty()) return emptyList()
            storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndAccountIn(monthEnd, monthStart, accountSfids)
        }
    }

    private fun buildByAccountType(schedules: List<StoreSchedule>, accountMap: Map<String?, Account>): List<AccountTypeCount> {
        val employeesByAccountType = schedules
            .groupBy { schedule ->
                val account = schedule.account?.let { accountMap[it] }
                classifyChannel(account?.abcType)
            }
            .mapValues { (_, records) -> records.mapNotNull { it.fullName }.distinct().size }

        return employeesByAccountType.map { (type, count) ->
            AccountTypeCount(accountType = type, count = count)
        }.sortedBy { it.accountType }
    }

    private fun buildByWorkType(schedules: List<StoreSchedule>): List<WorkTypeCount> {
        val employeesByWorkType = schedules
            .groupBy { normalizeWorkType(it.typeOfWork1) }
            .mapValues { (_, records) -> records.mapNotNull { it.fullName }.distinct().size }

        return employeesByWorkType.map { (type, count) ->
            WorkTypeCount(workType = type, count = count)
        }.sortedBy { it.workType }
    }

    private fun buildByChannelAndWorkType(
        schedules: List<StoreSchedule>,
        accountMap: Map<String?, Account>
    ): List<ChannelWorkTypeItem> {
        val grouped = schedules.groupBy { schedule ->
            val account = schedule.account?.let { accountMap[it] }
            classifyChannel(account?.abcType)
        }

        return grouped.map { (channelName, records) ->
            val byWork = records.groupBy { normalizeWorkType(it.typeOfWork1) }
            ChannelWorkTypeItem(
                channelName = channelName,
                fixed = byWork["고정"]?.mapNotNull { it.fullName }?.distinct()?.size ?: 0,
                alternating = byWork["격고"]?.mapNotNull { it.fullName }?.distinct()?.size ?: 0,
                visiting = byWork["순회"]?.mapNotNull { it.fullName }?.distinct()?.size ?: 0
            )
        }.sortedBy { it.channelName }
    }

    private fun normalizeWorkType(typeOfWork: String?): String {
        return when {
            typeOfWork == null -> "미배정"
            typeOfWork.contains("고정") -> "고정"
            typeOfWork.contains("격고") -> "격고"
            typeOfWork.contains("순회") -> "순회"
            else -> "기타"
        }
    }

    // --- Basic Stats ---

    private fun buildBasicStats(scope: DataScope, branchName: String?): BasicStats {
        val activeUsers = fetchUsersByStatus(scope, "재직")
        val onLeaveUsers = fetchUsersByStatus(scope, "휴직")

        val promotionCount = activeUsers.size
        val oscCount = 0 // Phase 1: 모두 판촉직

        val byAgeGroup = buildByAgeGroup(activeUsers)
        val byWorkType = buildUserWorkTypeStats(activeUsers)

        return BasicStats(
            branchName = branchName,
            staffType = StaffTypeCount(promotion = promotionCount, osc = oscCount),
            totalByPosition = TotalByPosition(active = activeUsers.size, onLeave = onLeaveUsers.size),
            byAgeGroup = byAgeGroup,
            byWorkType = byWorkType
        )
    }

    private fun fetchUsersByStatus(scope: DataScope, status: String): List<User> {
        return if (scope.isAllBranches) {
            userRepository.findByStatus(status)
        } else {
            if (scope.branchCodes.isEmpty()) return emptyList()
            userRepository.findByCostCenterCodeInAndStatus(scope.branchCodes, status)
        }
    }

    private fun buildByAgeGroup(users: List<User>): List<AgeGroupCount> {
        val today = LocalDate.now()
        val grouped = users.groupBy { user ->
            val age = calculateAge(user.birthDate, today)
            if (age == null) "기타" else classifyAgeGroup(age)
        }
        return grouped.map { (group, list) ->
            AgeGroupCount(ageGroup = group, count = list.size)
        }.sortedBy { it.ageGroup }
    }

    private fun calculateAge(birthDateStr: String?, today: LocalDate): Int? {
        if (birthDateStr.isNullOrBlank()) return null
        return try {
            val birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ChronoUnit.YEARS.between(birthDate, today).toInt()
        } catch (_: Exception) {
            null
        }
    }

    private fun classifyAgeGroup(age: Int): String {
        return when {
            age < 20 -> "20대 미만"
            age < 30 -> "20대"
            age < 40 -> "30대"
            age < 50 -> "40대"
            age < 60 -> "50대"
            else -> "60대 이상"
        }
    }

    private fun buildUserWorkTypeStats(users: List<User>): WorkTypeStats {
        val userSfids = users.mapNotNull { it.sfid }
        if (userSfids.isEmpty()) return WorkTypeStats(fixed = 0, alternating = 0, visiting = 0)

        val today = LocalDate.now()
        val ym = YearMonth.from(today)
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        val schedules = storeScheduleRepository.findByConfirmedTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(monthEnd, monthStart)
        val userSchedules = schedules.filter { it.fullName in userSfids }

        val byWorkType = userSchedules
            .groupBy { normalizeWorkType(it.typeOfWork1) }
            .mapValues { (_, records) -> records.mapNotNull { it.fullName }.distinct().size }

        return WorkTypeStats(
            fixed = byWorkType["고정"] ?: 0,
            alternating = byWorkType["격고"] ?: 0,
            visiting = byWorkType["순회"] ?: 0
        )
    }

    private fun round2(value: Double): Double {
        return Math.round(value * 10.0) / 10.0
    }
}
