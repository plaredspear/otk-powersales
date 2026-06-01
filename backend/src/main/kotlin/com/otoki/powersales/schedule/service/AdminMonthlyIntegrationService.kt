package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.leave.repository.HolidayMasterRepository
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.EmployeeInputCriteriaMasterRepository
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
import java.time.LocalDateTime
import com.otoki.powersales.common.util.TimeZones
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
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val holidayMasterRepository: HolidayMasterRepository,
    private val branchCodeExpander: BranchCodeExpander,
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository,
    private val employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository,
    private val teamMemberScheduleSearchService: TeamMemberScheduleSearchService,
    private val teamMemberCategorySearchService: TeamMemberCategorySearchService,
) {

    // 조회 (`getMonthlyIntegration` / `getCategorySchedule`) 는 SF `ScheduleSearchByTeamMember` /
    // `CategorySearchByTeamMember` 와 동등하게 MFEIS 를 직접 IN 필터로 조회한다 (`TeamMemberSchedule*SearchService`).
    // 기존 `buildIntegrationItems` 의 TeamMemberSchedule 동적 집계 + `attendance_log IS NOT NULL` 가드는
    // SF 정합 차원에서 0건이 되는 원인이라 사용하지 않는다. (Write 흐름 `refreshIntegration` 은 그대로 유지.)
    fun getMonthlyIntegration(
        year: Int,
        month: Int,
        costCenterCodes: List<String>
    ): MonthlyIntegrationScheduleResponse {
        validateParams(year, month, costCenterCodes)
        val sf = teamMemberScheduleSearchService.search(
            year = year.toString(),
            month = month.toString(),
            orgValues = costCenterCodes,
        )
        val items = sf.result.map { it.toMonthlyIntegrationItem() }
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
        val sf = teamMemberCategorySearchService.search(
            year = year.toString(),
            month = month.toString(),
            orgValues = costCenterCodes,
        )
        val items = sf.result.mapNotNull { it.toCategoryItem() }
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
        val filename = "${year}년${month}월_여사원 통합일정 조회_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    fun exportCategorySchedule(year: Int, month: Int, costCenterCodes: List<String>): ExcelResult {
        val response = getCategorySchedule(year, month, costCenterCodes)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("근무형태별 여사원인원현황")

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
        val numberOfInputs = BigDecimal.valueOf(schedules.size.toLong())

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

        val employee = schedules.first().employee
        val account = schedules.first().account
        val first = schedules.first()
        val workingCategory1 = first.workingCategory1?.displayName
        val workingCategory3 = first.workingCategory3?.displayName
        // workingCategory5 는 DisplayWorkSchedule.typeOfWork5 에서 representative 값 추출
        // (기존 resolveWorkingCategory5 와 동일 매칭 룰 — confirmed=true + workingDate in [startDate, endDate])
        val workingCategory5 = resolveWorkingCategory5(schedules).values.firstOrNull { it != null }

        // spec #680 §5.3 — self-trigger 3필드 자동 set (legacy
        // `MonthlyEmpIntegrationSchTriggerHandler.setAccountConvertedHeadcount` 동등).
        // 가드: workingCategory5='상시' AND workingCategory1/3 not null AND year/month not null
        // 그 외 row 는 3필드 모두 null.
        val applyThreeFields = workingCategory5 == "상시" &&
            workingCategory1 != null && workingCategory3 != null

        // accountConvertedHeadcount = 본 거래처+근무유형1+년월 의 (자기 자신 제외) 다른 MFEIS row 의
        // convertedHeadcount 합산 + 본 row 의 새 convertedHeadcount.
        // (legacy 는 listNew + 기존 DB row 합산 — bypass 로 다른 row 의 accountConvertedHeadcount
        // 갱신은 미발생. 본 spec 도 동일하게 본 row 만 set, 다른 row 의 stale 갱신은 비범위)
        val accountConvertedHeadcount: BigDecimal? = if (applyThreeFields && account != null) {
            val others = monthlyIntegrationScheduleRepository
                .findByAccountIdAndWorkingCategory1AndYearAndMonth(account.id, workingCategory1, yearStr, monthStr)
                .filter { it.id != (existing?.id ?: -1L) }
            val othersSum = others
                .mapNotNull { it.convertedHeadcount }
                .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
            othersSum.add(convertedHeadcount).setScale(4, RoundingMode.HALF_UP)
        } else null

        // thisMonthAmount — Q12 옵션 1: batch persist 결과 우선 + 동기 fallback
        val thisMonthAmount: BigDecimal? = if (applyThreeFields && account != null) {
            val batchPersisted = existing?.thisMonthAmount
            if (batchPersisted != null) {
                batchPersisted
            } else {
                val avgMap = calculateAvgClosingAmounts(
                    yearMonth.year, yearMonth.monthValue, listOf(account)
                )
                avgMap[account.id]?.let { BigDecimal(it) }
            }
        } else null

        // employeeInputCriteriaMaster lookup — 운영 정합 dev 검증 2026-05-26.
        // legacy `MonthlyEmpIntegrationSchTriggerHandler.cls:73-80` 의 `criteriaMap.get(Account.Type + Year + Month)`
        // 동등 — `Account.accountType.displayName` 으로 AccountCategoryMaster.name 매칭 후 EICM 활성 기간 lookup.
        // 활성 기간 referenceDate 는 검색월 첫날 (yearMonth.atDay(1)) 사용.
        // (legacy 는 본 Trigger 호출 시점의 sysdate 기반 currentDate 산출 — 신규는 검색 대상 월 기준이 정확)
        val employeeInputCriteriaMaster: com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster? =
            if (applyThreeFields && account != null) {
                val accountTypeName = account.accountType?.displayName
                if (accountTypeName != null) {
                    val category = accountCategoryMasterRepository.findByName(accountTypeName)
                    if (category != null) {
                        employeeInputCriteriaMasterRepository.findActiveByCategoryAndTypeOfWork1(
                            categoryId = category.id,
                            typeOfWork1 = TypeOfWork1.DISPLAY,
                            referenceDate = yearMonth.atDay(1),
                        )
                    } else null
                } else null
            } else null

        if (existing != null) {
            monthlyIntegrationScheduleRepository.delete(existing)
        }

        val record = MonthlyFemaleEmployeeIntegrationSchedule(
            year = yearStr,
            month = monthStr,
            employee = employee,
            account = account,
            workingDaysMonth = BigDecimal(workingDaysMonth),
            numberOfInputs = numberOfInputs,
            equivalentNumberOfWorkingDays = equivalentWorkingDays,
            convertedHeadcount = convertedHeadcount,
            accountConvertedHeadcount = accountConvertedHeadcount,
            thisMonthAmount = thisMonthAmount,
            employeeInputCriteriaMaster = employeeInputCriteriaMaster,
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
        // 1. Cost center expansion — 조직 계층 펼침 + BranchMapping 이력 합집합 (SF Util.getIncludedBranchCode 동등)
        val orgExpanded = organizationRepository.expandCostCenterCodes(costCenterCodes)
        val expandedCodes = branchCodeExpander.expand(orgExpanded).toList()
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

        // 9. Calculate 6-month ABC closing amount — Q12 옵션 1 (#680): MFEIS persist 우선 + 동기 fallback.
        // batch 가 매월 1일 03시에 전월 기준 this_month_amount 를 persist 한다 (MfeisThisMonthRevenueBatch).
        // 본 조회 시점에 employee+account+year+month 의 MFEIS 가 존재 + thisMonthAmount not null 이면 그 값 사용,
        // 그 외 (당월 / persist 미적용 / 등) 는 calculateAvgClosingAmounts 동기 계산 fallback.
        val yearStr = year.toString()
        val monthStr = String.format("%02d", month)
        val employeeAccountPersistedAmounts: Map<Pair<Long, Int>, BigDecimal> = grouped.keys
            .map { it.employeeId to it.accountId }
            .distinct()
            .mapNotNull { (empId, accId) ->
                val mfeis = monthlyIntegrationScheduleRepository
                    .findByEmployeeIdAndAccountIdAndYearAndMonth(empId, accId, yearStr, monthStr)
                val amount = mfeis?.thisMonthAmount ?: return@mapNotNull null
                (empId to accId) to amount
            }
            .toMap()
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

            // Q12 옵션 1 (#680): MFEIS persist 우선 + 동기 fallback
            val persisted = employeeAccountPersistedAmounts[key.employeeId to key.accountId]
            val avgAmount = persisted?.toLong()
                ?: account?.id?.let { avgClosingAmounts[it] }
                ?: 0L
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
        val externalKeyToId: Map<String, Int> = accounts
            .filter { it.externalKey != null }
            .associate { it.externalKey!! to it.id }
        if (externalKeyToId.isEmpty()) return emptyMap()

        val now = YearMonth.now()
        val searchYm = YearMonth.of(year, month)

        // Determine 6-month range — legacy `UpdateThisMonthRevenueBatch.execute` 동등.
        val (startYm, endYm) = if (searchYm == now) {
            searchYm.minusMonths(6) to searchYm.minusMonths(1)
        } else {
            searchYm.minusMonths(5) to searchYm
        }

        // 매출월 "YYYYMM" 6자 문자열 리스트 생성 (게이트웨이가 SalesYear/SalesMonth picklist 로 변환)
        val salesDates = mutableListOf<String>()
        var current = startYm
        while (!current.isAfter(endYm)) {
            salesDates.add("%d%02d".format(current.year, current.monthValue))
            current = current.plusMonths(1)
        }

        val histories = monthlySalesHistoryGateway.findBySalesDates(salesDates, externalKeyToId.keys)

        // Group by account ID and calculate average
        // spec #680 Q2 옵션 2 — legacy `UpdateThisMonthRevenueBatch.cls:execute` + `get6MonthsAvg` 동등.
        // 양수 (ClosingAmount > 0) 월만 합산 + divider = 양수 count (legacy `dividerMap_6M`).
        // 0/음수 매출 월은 제외 (legacy 의 batch 외 컨트롤러는 미적용이라 비대칭 — 신규는 일관 정책).
        return histories.groupBy { externalKeyToId[it.sapAccountCode] ?: 0 }
            .filter { it.key != 0 }
            .mapValues { (_, rows) ->
                val positives = rows
                    .mapNotNull { it.abcClosingAmount1 }
                    .filter { it > BigDecimal.ZERO }
                val divider = positives.size
                if (divider > 0) {
                    positives
                        .fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
                        .divide(BigDecimal(divider), 0, RoundingMode.HALF_UP)
                        .toLong()
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
                "진열" -> if (item.workingCategory5?.contains("상시") == true) {
                    when (item.workingCategory3) {
                        "고정" -> sums.displayFixed = sums.displayFixed.add(headcount)
                        "격고" -> sums.displayAlternate = sums.displayAlternate.add(headcount)
                        "순회" -> sums.displayPatrol = sums.displayPatrol.add(headcount)
                    }
                }
                "행사" -> when (item.workingCategory4) {
                    "상온", "라면" -> sums.eventAmbient = sums.eventAmbient.add(headcount)
                    "냉동", "냉장", "만두", "냉동/냉장" -> sums.eventFrozenChilled = sums.eventFrozenChilled.add(headcount)
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

// SF `ScheduleSearchByTeamMember` row → web admin 화면 DTO 변환.
// `numberOfInputs` 는 MFEIS 가 BigDecimal 로 보유, 화면은 Int 로 표시 (행 수 의미).
private fun TeamMemberScheduleResultItem.toMonthlyIntegrationItem(): MonthlyIntegrationScheduleItem =
    MonthlyIntegrationScheduleItem(
        branchName = orgName ?: "",
        accountBranchName = accountBranchName,
        accountCode = accountCode ?: "",
        accountName = accountName ?: "",
        employeeCode = employeeNumber ?: "",
        title = title,
        employeeName = employeeName ?: "",
        workingCategory1 = workingCategory1 ?: "",
        workingCategory3 = workingCategory3,
        workingCategory4 = workingCategory4,
        workingCategory5 = workingCategory5,
        totalInputCount = numberOfInputs?.toInt() ?: 0,
        equivalentWorkingDays = equivalentNumberOfWorkingDays,
        convertedHeadcount = convertedHeadcount,
        avgClosingAmount = actualAmount.toLong(),
    )

// SF `CategorySearchByTeamMember` row → web admin 카테고리 DTO 변환.
// 양 월 모두 0 인 row 는 SF `setNull()` 로 모든 수치가 null → 화면에서 제외 (returns null).
private fun TeamMemberCategoryResultItem.toCategoryItem(): CategoryScheduleItem? {
    val curTotal = currentMonthTotal ?: return null
    val prevTotal = lastMonthTotal ?: java.math.BigDecimal.ZERO
    return CategoryScheduleItem(
        branchName = branchName,
        currentMonthTotal = curTotal,
        previousMonthTotal = prevTotal,
        totalChange = totalIncrease ?: java.math.BigDecimal.ZERO,
        displayFixed = fix ?: java.math.BigDecimal.ZERO,
        displayAlternate = store ?: java.math.BigDecimal.ZERO,
        displayPatrol = rotate ?: java.math.BigDecimal.ZERO,
        currentMonthDisplayTotal = currentExhibitionTotal ?: java.math.BigDecimal.ZERO,
        previousMonthDisplayTotal = lastExhibitionTotal ?: java.math.BigDecimal.ZERO,
        displayChange = exhibitionIncrease ?: java.math.BigDecimal.ZERO,
        eventAmbient = roomTemperature ?: java.math.BigDecimal.ZERO,
        eventFrozenChilled = refrigerationAndFreezing ?: java.math.BigDecimal.ZERO,
        currentMonthEventTotal = currentEventTotal ?: java.math.BigDecimal.ZERO,
        previousMonthEventTotal = lastEventTotal ?: java.math.BigDecimal.ZERO,
        eventChange = eventIncrease ?: java.math.BigDecimal.ZERO,
    )
}
