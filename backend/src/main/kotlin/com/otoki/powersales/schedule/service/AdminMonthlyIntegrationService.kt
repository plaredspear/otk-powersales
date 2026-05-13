package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.entity.WorkingCategory3
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminMonthlyIntegrationService(
    private val organizationRepository: OrganizationRepository,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val holidayMasterRepository: HolidayMasterRepository
) {

    fun getMonthlyIntegration(
        year: Int,
        month: Int,
        costCenterCodes: List<String>
    ): MonthlyIntegrationScheduleResponse {
        validateParams(year, month, costCenterCodes)
        val items = buildIntegrationItems(year, month, costCenterCodes)
        return MonthlyIntegrationScheduleResponse(
            year = year,
            month = month,
            items = items,
            totalCount = items.size
        )
    }

    fun getCategorySchedule(
        year: Int,
        month: Int,
        costCenterCodes: List<String>
    ): CategoryScheduleResponse {
        validateParams(year, month, costCenterCodes)

        val currentItems = buildIntegrationItems(year, month, costCenterCodes)
        val prevYearMonth = YearMonth.of(year, month).minusMonths(1)
        val prevItems = buildIntegrationItems(prevYearMonth.year, prevYearMonth.monthValue, costCenterCodes)

        val items = buildCategoryItems(currentItems, prevItems)
        return CategoryScheduleResponse(year = year, month = month, items = items)
    }

    fun exportMonthlyIntegration(year: Int, month: Int, costCenterCodes: List<String>): ExcelResult {
        val response = getMonthlyIntegration(year, month, costCenterCodes)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("통합일정")

        val headers = listOf(
            "소속", "거래처지점명", "거래처코드", "거래처명", "사번", "직위", "이름",
            "근무형태1", "근무형태3", "근무형태4", "근무형태5",
            "총 투입횟수", "총 환산근무일수", "총 환산인원", "ABC마감실적"
        )

        val headerStyle = createHeaderStyle(workbook)
        val intStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
        }
        val decimal3Style = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.000")
        }

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        response.items.forEachIndexed { rowIdx, item ->
            val row = sheet.createRow(rowIdx + 1)
            row.createCell(0).setCellValue(item.branchName)
            row.createCell(1).setCellValue(item.accountBranchName ?: "")
            row.createCell(2).setCellValue(item.accountCode)
            row.createCell(3).setCellValue(item.accountName)
            row.createCell(4).setCellValue(item.employeeCode)
            row.createCell(5).setCellValue(item.title ?: "")
            row.createCell(6).setCellValue(item.employeeName)
            row.createCell(7).setCellValue(item.workingCategory1)
            row.createCell(8).setCellValue(item.workingCategory3 ?: "")
            row.createCell(9).setCellValue(item.workingCategory4 ?: "")
            row.createCell(10).setCellValue(item.workingCategory5 ?: "")
            row.createCell(11).apply {
                setCellValue(item.totalInputCount.toDouble())
                cellStyle = intStyle
            }
            row.createCell(12).apply {
                setCellValue(item.equivalentWorkingDays.toDouble())
                cellStyle = decimal3Style
            }
            row.createCell(13).apply {
                setCellValue(item.convertedHeadcount.toDouble())
                cellStyle = decimal3Style
            }
            row.createCell(14).apply {
                setCellValue(item.avgClosingAmount.toDouble())
                cellStyle = intStyle
            }
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_여사원_통합일정_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    fun exportCategorySchedule(year: Int, month: Int, costCenterCodes: List<String>): ExcelResult {
        val response = getCategorySchedule(year, month, costCenterCodes)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("근무형태별 인원현황")

        val headerStyle = createHeaderStyle(workbook)
        val decimal1Style = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.0")
        }
        val decimal3Style = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.000")
        }

        // 1단 헤더
        val header1 = sheet.createRow(0)
        val header1Labels = listOf(
            "총계", "총계", "총계", "총계",
            "진열", "진열", "진열", "진열", "진열", "진열",
            "행사", "행사", "행사", "행사", "행사"
        )
        header1Labels.forEachIndexed { i, label ->
            header1.createCell(i).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        // Merge 1단 헤더
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 3))
        sheet.addMergedRegion(CellRangeAddress(0, 0, 4, 9))
        sheet.addMergedRegion(CellRangeAddress(0, 0, 10, 14))

        // 2단 헤더
        val header2 = sheet.createRow(1)
        val header2Labels = listOf(
            "지점명", "당월총합계", "전월마감합계", "전체증감수",
            "고정", "격고", "순회", "당월진열합계", "전월진열합계", "진열증감수",
            "상온", "냉동/냉장", "당월행사합계", "전월행사합계", "행사증감수"
        )
        header2Labels.forEachIndexed { i, label ->
            header2.createCell(i).apply {
                setCellValue(label)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 2)

        response.items.forEachIndexed { rowIdx, item ->
            val row = sheet.createRow(rowIdx + 2)
            row.createCell(0).setCellValue(item.branchName)
            row.createCell(1).apply { setCellValue(item.currentMonthTotal.toDouble()); cellStyle = decimal1Style }
            row.createCell(2).apply { setCellValue(item.previousMonthTotal.toDouble()); cellStyle = decimal1Style }
            row.createCell(3).apply { setCellValue(item.totalChange.toDouble()); cellStyle = decimal1Style }
            row.createCell(4).apply { setCellValue(item.displayFixed.toDouble()); cellStyle = decimal3Style }
            row.createCell(5).apply { setCellValue(item.displayAlternate.toDouble()); cellStyle = decimal3Style }
            row.createCell(6).apply { setCellValue(item.displayPatrol.toDouble()); cellStyle = decimal3Style }
            row.createCell(7).apply { setCellValue(item.currentMonthDisplayTotal.toDouble()); cellStyle = decimal3Style }
            row.createCell(8).apply { setCellValue(item.previousMonthDisplayTotal.toDouble()); cellStyle = decimal3Style }
            row.createCell(9).apply { setCellValue(item.displayChange.toDouble()); cellStyle = decimal3Style }
            row.createCell(10).apply { setCellValue(item.eventAmbient.toDouble()); cellStyle = decimal3Style }
            row.createCell(11).apply { setCellValue(item.eventFrozenChilled.toDouble()); cellStyle = decimal3Style }
            row.createCell(12).apply { setCellValue(item.currentMonthEventTotal.toDouble()); cellStyle = decimal3Style }
            row.createCell(13).apply { setCellValue(item.previousMonthEventTotal.toDouble()); cellStyle = decimal3Style }
            row.createCell(14).apply { setCellValue(item.eventChange.toDouble()); cellStyle = decimal3Style }
        }

        header2Labels.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_근무형태별_인원현황_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    @Transactional
    fun refreshIntegration(employeeId: Long, accountId: Int, yearMonth: YearMonth) {
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        val schedules = teamMemberScheduleRepository.findWorkSchedulesByEmployeeAndAccountAndMonth(
            employeeId, accountId, from, to
        )

        val yearStr = yearMonth.year.toString()
        val monthStr = String.format("%02d", yearMonth.monthValue)
        val existing = monthlyIntegrationScheduleRepository.findByEmployeeIdAndAccountIdAndYearAndMonth(
            employeeId, accountId, yearStr, monthStr
        )

        if (schedules.isEmpty()) {
            if (existing != null) {
                monthlyIntegrationScheduleRepository.delete(existing)
            }
            return
        }

        val workingDaysMonth = schedules.map { it.workingDate!! }.distinct().size
        val numberOfInputs = schedules.size.toLong()

        var equivalentWorkingDays = BigDecimal.ZERO
        for (schedule in schedules) {
            val accountCountOnDate = teamMemberScheduleRepository.countWorkSchedulesByEmployeeAndDateAndWorkingType(
                employeeId, schedule.workingDate!!
            )
            val coefficient = when (schedule.workingCategory3) {
                WorkingCategory3.FIXED -> BigDecimal.ONE
                WorkingCategory3.ALTERNATE -> BigDecimal("0.5")
                WorkingCategory3.PATROL -> if (accountCountOnDate > 0) {
                    BigDecimal.ONE.divide(BigDecimal(accountCountOnDate), 4, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO
                else -> BigDecimal.ONE
            }
            equivalentWorkingDays = equivalentWorkingDays.add(coefficient)
        }
        equivalentWorkingDays = equivalentWorkingDays.setScale(4, RoundingMode.HALF_UP)

        val businessDays = calculateBusinessDays(yearMonth)
        val convertedHeadcount = if (businessDays > 0) {
            equivalentWorkingDays.divide(BigDecimal(businessDays), 4, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        if (existing != null) {
            monthlyIntegrationScheduleRepository.delete(existing)
        }

        val employee = schedules.first().employee
        val account = schedules.first().account

        val record = MonthlyFemaleEmployeeIntegrationSchedule(
            year = yearStr,
            month = monthStr,
            employee = employee,
            account = account,
            workingDaysMonth = BigDecimal(workingDaysMonth),
            numberOfInputs = numberOfInputs,
            equivalentNumberOfWorkingDays = equivalentWorkingDays,
            convertedHeadcount = convertedHeadcount
        )
        monthlyIntegrationScheduleRepository.save(record)
    }

    internal fun calculateBusinessDays(yearMonth: YearMonth): Int {
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()

        var weekdays = 0
        var date = from
        while (!date.isAfter(to)) {
            val dow = date.dayOfWeek
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
                weekdays++
            }
            date = date.plusDays(1)
        }

        val holidays = holidayMasterRepository.findByHolidayDateBetween(from, to)
        val holidayWeekdays = holidays.count { h ->
            val dow = h.holidayDate.dayOfWeek
            dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY
        }

        return weekdays - holidayWeekdays
    }

    // --- Private helpers ---

    private fun validateParams(year: Int, month: Int, costCenterCodes: List<String>) {
        if (year !in 2020..2099) {
            throw InvalidParameterException("year는 2020~2099 범위여야 합니다")
        }
        if (month !in 1..12) {
            throw InvalidParameterException("month는 1~12 범위여야 합니다")
        }
        if (costCenterCodes.isEmpty()) {
            throw InvalidParameterException("cost_center_codes는 필수입니다")
        }
    }

    internal fun buildIntegrationItems(
        year: Int,
        month: Int,
        costCenterCodes: List<String>
    ): List<MonthlyIntegrationScheduleItem> {
        // 1. Cost center expansion
        val expandedCodes = organizationRepository.expandCostCenterCodes(costCenterCodes)
        if (expandedCodes.isEmpty()) return emptyList()

        // 2. Find employees with these cost center codes
        val employees = employeeRepository.findByCostCenterCodeInAndStatus(expandedCodes, "재직")
        if (employees.isEmpty()) return emptyList()

        val employeeMap = employees.associateBy { it.id }
        val employeeIds = employees.map { it.id }

        // 3. Query schedule records
        val ym = YearMonth.of(year, month)
        val from = ym.atDay(1)
        val to = ym.atEndOfMonth()

        val records = teamMemberScheduleRepository.findIntegrationScheduleRecords(employeeIds, from, to)
        if (records.isEmpty()) return emptyList()

        // 4. Resolve working_category5 from display_work_schedule
        val category5Map = resolveWorkingCategory5(records)

        // 5. Calculate equivalent working days denominator:
        //    for each employee + each working_date → count of distinct accounts
        val employeeDateAccountCount = records
            .groupBy { it.employee!!.id to it.workingDate!! }
            .mapValues { (_, recs) -> recs.map { it.account!!.id }.distinct().size }

        // 6. Calculate monthly working days per employee (distinct working dates for the employee in month)
        val monthlyWorkingDays = records
            .groupBy { it.employee!!.id }
            .mapValues { (_, recs) -> recs.map { it.workingDate!! }.distinct().size }

        // 7. Grouping key → aggregate
        data class GroupKey(
            val employeeId: Long,
            val accountId: Int,
            val workingCategory1: String?,
            val workingCategory3: String?,
            val workingCategory4: String?,
            val workingCategory5: String?
        )

        val grouped = records.groupBy { rec ->
            GroupKey(
                employeeId = rec.employee!!.id,
                accountId = rec.account!!.id,
                workingCategory1 = rec.workingCategory1?.displayName,
                workingCategory3 = rec.workingCategory3?.displayName,
                workingCategory4 = rec.workingCategory4,
                workingCategory5 = category5Map[rec.id]
            )
        }

        // 8. Collect account IDs for ABC closing amount
        val accountIds = grouped.keys.map { it.accountId }.distinct()
        val accounts = accountRepository.findByIdIn(accountIds).associateBy { it.id }

        // 9. Calculate 6-month ABC closing amount
        val avgClosingAmounts = calculateAvgClosingAmounts(year, month, accounts.values.toList())

        // 10. Get org name map for branch names
        val orgNameMap = getOrgNameMap(expandedCodes)

        // 11. Build items
        val items = grouped.map { (key, recs) ->
            val employee = employeeMap[key.employeeId]
            val account = accounts[key.accountId]

            val totalInputCount = recs.map { it.workingDate!! }.distinct().size

            var equivalentWorkingDays = BigDecimal.ZERO
            for (rec in recs) {
                val n = employeeDateAccountCount[rec.employee!!.id to rec.workingDate!!] ?: 1
                equivalentWorkingDays = equivalentWorkingDays.add(
                    BigDecimal.ONE.divide(BigDecimal(n), 6, RoundingMode.HALF_UP)
                )
            }
            equivalentWorkingDays = equivalentWorkingDays.setScale(3, RoundingMode.HALF_UP)

            val empMonthlyDays = monthlyWorkingDays[key.employeeId] ?: 1
            val convertedHeadcount = if (empMonthlyDays > 0) {
                equivalentWorkingDays.divide(BigDecimal(empMonthlyDays), 3, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

            val avgAmount = account?.id?.let { avgClosingAmounts[it] } ?: 0L
            val branchName = employee?.costCenterCode?.let { orgNameMap[it] } ?: employee?.orgName ?: ""

            MonthlyIntegrationScheduleItem(
                branchName = branchName,
                accountBranchName = account?.branchName,
                accountCode = account?.externalKey ?: "",
                accountName = account?.name ?: "",
                employeeCode = employee?.employeeCode ?: "",
                title = null,
                employeeName = employee?.name ?: "",
                workingCategory1 = key.workingCategory1 ?: "",
                workingCategory3 = key.workingCategory3,
                workingCategory4 = key.workingCategory4,
                workingCategory5 = key.workingCategory5,
                totalInputCount = totalInputCount,
                equivalentWorkingDays = equivalentWorkingDays,
                convertedHeadcount = convertedHeadcount,
                avgClosingAmount = avgAmount
            )
        }.sortedWith(compareBy({ it.branchName }, { it.accountCode }, { it.employeeCode }))

        return items
    }

    private fun resolveWorkingCategory5(records: List<TeamMemberSchedule>): Map<Long, String?> {
        val employeeAccountPairs = records.map { (it.employee!!.id to it.account!!.id) }.distinct()

        if (employeeAccountPairs.isEmpty()) return emptyMap()

        val employeeIds = employeeAccountPairs.map { it.first }.distinct()
        val accountIds = employeeAccountPairs.map { it.second }.distinct()

        val displaySchedules = displayWorkScheduleRepository.findByEmployeeIdsAndAccountIds(
            employeeIds, accountIds
        )

        val result = mutableMapOf<Long, String?>()
        for (rec in records) {
            val matching = displaySchedules.find { dws ->
                dws.employee?.id == rec.employee?.id &&
                    dws.account?.id == rec.account?.id &&
                    dws.confirmed == true &&
                    rec.workingDate != null &&
                    dws.startDate != null &&
                    dws.endDate != null &&
                    !rec.workingDate!!.isBefore(dws.startDate) &&
                    !rec.workingDate!!.isAfter(dws.endDate)
            }
            result[rec.id] = matching?.typeOfWork5?.displayName
        }
        return result
    }

    private fun calculateAvgClosingAmounts(
        year: Int,
        month: Int,
        accounts: List<Account>
    ): Map<Int, Long> {
        if (accounts.isEmpty()) return emptyMap()

        val now = YearMonth.now()
        val searchYm = YearMonth.of(year, month)

        // Determine 6-month range
        val (startYm, endYm) = if (searchYm == now) {
            searchYm.minusMonths(6) to searchYm.minusMonths(1)
        } else {
            searchYm.minusMonths(5) to searchYm
        }

        // Collect all year-month pairs
        val yearMonthPairs = mutableListOf<YearMonth>()
        var current = startYm
        while (!current.isAfter(endYm)) {
            yearMonthPairs.add(current)
            current = current.plusMonths(1)
        }

        val salesYears = yearMonthPairs.map { it.year.toString() }.distinct()
        val allHistory = monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(
            accounts, salesYears
        )

        // Filter to valid year-month range
        val validYmStrings = yearMonthPairs.map {
            "${it.year}" to String.format("%02d", it.monthValue)
        }.toSet()

        val filtered = allHistory.filter { h ->
            h.salesYear != null && h.salesMonth != null &&
                (h.salesYear!! to h.salesMonth!!) in validYmStrings
        }

        // Group by account ID and calculate average
        return filtered.groupBy { it.account?.id ?: 0 }
            .filter { it.key != 0 }
            .mapValues { (_, histories) ->
                val sum = histories.sumOf { it.abcClosingAmount1 ?: 0.0 }
                val divider = histories.size
                if (divider > 0) {
                    BigDecimal(sum / divider).setScale(0, RoundingMode.HALF_UP).toLong()
                } else {
                    0L
                }
            }
    }

    private fun getOrgNameMap(costCenterCodes: List<String>): Map<String, String> {
        val orgs = organizationRepository.searchForAdmin(null, null, costCenterCodes)
        return orgs
            .filter { it.costCenterLevel5 != null && it.orgNameLevel5 != null }
            .associate { it.costCenterLevel5!! to it.orgNameLevel5!! }
    }

    private fun buildCategoryItems(
        currentItems: List<MonthlyIntegrationScheduleItem>,
        prevItems: List<MonthlyIntegrationScheduleItem>
    ): List<CategoryScheduleItem> {
        val currentByBranch = groupByBranchCategory(currentItems)
        val prevByBranch = groupByBranchCategory(prevItems)

        val allBranches = (currentByBranch.keys + prevByBranch.keys).distinct()

        return allBranches.mapNotNull { branch ->
            val cur = currentByBranch[branch] ?: BranchCategorySums()
            val prev = prevByBranch[branch] ?: BranchCategorySums()

            val curDisplayTotal = (cur.displayFixed + cur.displayAlternate + cur.displayPatrol)
                .setScale(3, RoundingMode.HALF_UP)
            val prevDisplayTotal = (prev.displayFixed + prev.displayAlternate + prev.displayPatrol)
                .setScale(3, RoundingMode.HALF_UP)
            val curEventTotal = (cur.eventAmbient + cur.eventFrozenChilled)
                .setScale(3, RoundingMode.HALF_UP)
            val prevEventTotal = (prev.eventAmbient + prev.eventFrozenChilled)
                .setScale(3, RoundingMode.HALF_UP)

            val curTotal = (curDisplayTotal + curEventTotal).setScale(1, RoundingMode.HALF_UP)
            val prevTotal = (prevDisplayTotal + prevEventTotal).setScale(1, RoundingMode.HALF_UP)

            // Skip if both are zero
            if (curTotal.compareTo(BigDecimal.ZERO) == 0 && prevTotal.compareTo(BigDecimal.ZERO) == 0) {
                return@mapNotNull null
            }

            CategoryScheduleItem(
                branchName = branch,
                currentMonthTotal = curTotal,
                previousMonthTotal = prevTotal,
                totalChange = (curTotal - prevTotal).setScale(1, RoundingMode.HALF_UP),
                displayFixed = cur.displayFixed.setScale(3, RoundingMode.HALF_UP),
                displayAlternate = cur.displayAlternate.setScale(3, RoundingMode.HALF_UP),
                displayPatrol = cur.displayPatrol.setScale(3, RoundingMode.HALF_UP),
                currentMonthDisplayTotal = curDisplayTotal,
                previousMonthDisplayTotal = prevDisplayTotal,
                displayChange = (curDisplayTotal - prevDisplayTotal).setScale(3, RoundingMode.HALF_UP),
                eventAmbient = cur.eventAmbient.setScale(3, RoundingMode.HALF_UP),
                eventFrozenChilled = cur.eventFrozenChilled.setScale(3, RoundingMode.HALF_UP),
                currentMonthEventTotal = curEventTotal,
                previousMonthEventTotal = prevEventTotal,
                eventChange = (curEventTotal - prevEventTotal).setScale(3, RoundingMode.HALF_UP)
            )
        }.sortedBy { it.branchName }
    }

    private data class BranchCategorySums(
        var displayFixed: BigDecimal = BigDecimal.ZERO,
        var displayAlternate: BigDecimal = BigDecimal.ZERO,
        var displayPatrol: BigDecimal = BigDecimal.ZERO,
        var eventAmbient: BigDecimal = BigDecimal.ZERO,
        var eventFrozenChilled: BigDecimal = BigDecimal.ZERO
    )

    private fun groupByBranchCategory(
        items: List<MonthlyIntegrationScheduleItem>
    ): Map<String, BranchCategorySums> {
        val result = mutableMapOf<String, BranchCategorySums>()

        for (item in items) {
            val sums = result.getOrPut(item.branchName) { BranchCategorySums() }
            val headcount = item.convertedHeadcount

            when (item.workingCategory1) {
                "진열" -> when (item.workingCategory3) {
                    "고정" -> sums.displayFixed = sums.displayFixed.add(headcount)
                    "격고" -> sums.displayAlternate = sums.displayAlternate.add(headcount)
                    "순회" -> sums.displayPatrol = sums.displayPatrol.add(headcount)
                }
                "행사" -> when (item.workingCategory4) {
                    "상온", "라면" -> sums.eventAmbient = sums.eventAmbient.add(headcount)
                    "냉동", "냉장", "만두", "냉동냉장" -> sums.eventFrozenChilled = sums.eventFrozenChilled.add(headcount)
                }
            }
        }

        return result
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        setFillForegroundColor(XSSFColor(byteArrayOf(0x1E, 0x2F, 0x97.toByte()), null))
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
        setFont(workbook.createFont().apply {
            bold = true
            color = org.apache.poi.ss.usermodel.IndexedColors.WHITE.index
        })
    }

    data class ExcelResult(
        val bytes: ByteArray,
        val filename: String
    )
}

class InvalidParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)
