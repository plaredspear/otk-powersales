package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryAccountMonthlyStat
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryAccountStat
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryEmployeeAccountResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryMonthlyStat
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryPeriodSummaryItem
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryPeriodSummaryResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * 기간별 근무내역(개인) 집계 서비스.
 *
 * 근무기간 조회(월별근무내역 목록)와 동일하게 [TeamMemberSchedule] 을 원천으로 하되,
 * 단일 월이 아닌 기간(시작년월~종료년월) 전체를 지점 스코프 내 전체 여사원에 대해 조회하고
 * 여사원(사번)별로 집계해 1행으로 만든다. 통합일정과 유사한 레이아웃이나 집계 원천이 다르다.
 *
 * 출근 등록(attendanceLog) 된 일정만 집계 — 월별근무내역 목록(EmployeeWorkHistoryService.getMonthlyHistory) 과 정합.
 */
@Service
@Transactional(readOnly = true)
class WorkHistoryPeriodSummaryService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /**
     * 기간별 근무내역 집계 조회.
     *
     * year/month 검증 (2020~2099 / 1~12) + 기간 순서 검증 후 [from 1일, to 말일] 환산하여 조회.
     * 지점 스코프: scope.isAllBranches 면 무제한, 아니면 scope.branchCodes (costCenterCode) 로 제한.
     */
    fun getSummary(
        scope: DataScope,
        fromYearMonth: String,
        toYearMonth: String,
        costCenterCodes: List<String>,
        keyword: String?,
    ): WorkHistoryPeriodSummaryResponse {
        val fromYm = parseYearMonth(fromYearMonth, "fromYearMonth")
        val toYm = parseYearMonth(toYearMonth, "toYearMonth")
        validateRange(fromYm, toYm)

        val from = fromYm.atDay(1)
        val to = toYm.atEndOfMonth()
        val branchCodes = resolveBranchCodes(scope, costCenterCodes)
        // 권한 스코프 밖이거나 선택 지점이 없어 조회 가능 지점이 비면 빈 결과.
        if (!scope.isAllBranches && branchCodes.isEmpty()) {
            return WorkHistoryPeriodSummaryResponse(
                fromYearMonth = fromYm.format(YEAR_MONTH_FORMAT),
                toYearMonth = toYm.format(YEAR_MONTH_FORMAT),
                items = emptyList(),
                totalCount = 0,
            )
        }

        val schedules = teamMemberScheduleRepository.findWorkHistoryForPeriod(
            from = from,
            to = to,
            branchCodes = branchCodes,
            keyword = keyword?.trim()?.takeIf { it.isNotEmpty() },
        )

        val items = aggregate(schedules, fromYm, toYm)
        return WorkHistoryPeriodSummaryResponse(
            fromYearMonth = fromYm.format(YEAR_MONTH_FORMAT),
            toYearMonth = toYm.format(YEAR_MONTH_FORMAT),
            items = items,
            totalCount = items.size,
        )
    }

    /**
     * 특정 여사원 1명의 기간 내 거래처별 근무 집계 조회.
     *
     * 기간 검증은 [getSummary] 와 동일 (yyyy-MM / 2020~2099 / 순서 / 최대 6개월).
     * 지점 스코프: scope.isAllBranches 면 무제한, 아니면 scope.branchCodes 로 제한 —
     * 스코프 밖 여사원의 사번을 지정해도 조회 행이 없어 빈 결과가 된다.
     * 거래처 미연결 행(연차/대휴 등)은 accountName=null 1행으로 묶는다.
     * 정렬: 총 근무일수 내림차순 → 거래처명 오름차순, 거래처 미연결 행은 맨 뒤.
     */
    fun getAccountSummary(
        scope: DataScope,
        employeeCode: String,
        fromYearMonth: String,
        toYearMonth: String,
    ): WorkHistoryEmployeeAccountResponse {
        val fromYm = parseYearMonth(fromYearMonth, "fromYearMonth")
        val toYm = parseYearMonth(toYearMonth, "toYearMonth")
        validateRange(fromYm, toYm)

        val trimmedCode = employeeCode.trim()
        if (trimmedCode.isEmpty()) {
            throw InvalidParameterException("employeeCode 는 필수입니다")
        }

        val branchCodes = if (scope.isAllBranches) emptyList() else scope.branchCodes
        val schedules = if (!scope.isAllBranches && branchCodes.isEmpty()) {
            emptyList()
        } else {
            teamMemberScheduleRepository.findWorkHistoryForPeriodByEmployee(
                employeeCode = trimmedCode,
                from = fromYm.atDay(1),
                to = toYm.atEndOfMonth(),
                branchCodes = branchCodes,
            )
        }

        // 통합일정(MFEIS) B그룹 지표 산출용 월 전체 모수.
        // 환산근무일수의 분모 N(그날 사원의 거래처 무관 출근 수)과 당월근무일수는 거래처 필터가 걸린
        // 표시용 조회로는 구할 수 없어, 사원 1명 × 기간 전체를 별도 1회 조회한다 (getIntegrationDetail 동일 패턴).
        // 이 모수는 MFEIS 집계 모수와 동일하게 account != null 출근 행만 포함한다.
        val employeeId = schedules.firstOrNull()?.employee?.id
        val bMetrics: Map<Long, AccountBMetrics> = if (employeeId != null) {
            computeAccountBMetrics(employeeId, fromYm, toYm)
        } else {
            emptyMap()
        }

        val items = schedules
            .groupBy { it.account?.id }
            .map { (accountId, rows) ->
                val acc = rows.first().account
                val s = stat(rows)
                val b = accountId?.let { bMetrics[it] }
                WorkHistoryAccountStat(
                    accountName = acc?.name,
                    accountExternalKey = acc?.externalKey,
                    accountBranchName = acc?.branchName,
                    distributionChannelLabel = acc?.distributionChannelLabel(),
                    abcTypeLabel = acc?.abcTypeLabel(),
                    totalWorkingDays = s.totalWorkingDays,
                    displayDays = s.displayDays,
                    eventDays = s.eventDays,
                    workDays = s.workDays,
                    annualLeaveDays = s.annualLeaveDays,
                    altHolidayDays = s.altHolidayDays,
                    totalInputCount = b?.totalInputCount ?: 0,
                    equivalentWorkingDays = b?.equivalentWorkingDays ?: BigDecimal.ZERO,
                    monthlyStats = b?.monthlyStats ?: emptyList(),
                )
            }
            .sortedWith(
                compareBy<WorkHistoryAccountStat> { it.accountName == null && it.accountExternalKey == null }
                    .thenByDescending { it.totalWorkingDays }
                    .thenBy { it.accountName ?: "" },
            )

        return WorkHistoryEmployeeAccountResponse(
            fromYearMonth = fromYm.format(YEAR_MONTH_FORMAT),
            toYearMonth = toYm.format(YEAR_MONTH_FORMAT),
            employeeCode = trimmedCode,
            employeeName = schedules.firstOrNull()?.employee?.name,
            items = items,
            totalCount = items.size,
        )
    }

    /**
     * 거래처별 통합일정(MFEIS) B그룹 지표 산출 — 사원 1명 × 기간.
     *
     * MFEIS 재집계([AdminMonthlyIntegrationService.refreshIntegration]) 의 산출식을 조회 시점에 재현한다.
     * 모수는 사원+월의 출근등록(account != null) TMS 전건이며, N(그날 출근 수)/당월근무일수는 월 단위로 계산한다.
     *
     * 반환: accountId → (기간 합산 투입횟수/환산근무일수 + 월별 분해). 월별 분해에만 환산인원을 담는다
     * (분모가 월마다 달라 합산 불가).
     */
    private fun computeAccountBMetrics(
        employeeId: Long,
        fromYm: YearMonth,
        toYm: YearMonth,
    ): Map<Long, AccountBMetrics> {
        val population = teamMemberScheduleRepository.findAttendedSchedulesByEmployeeAndMonth(
            employeeId = employeeId,
            from = fromYm.atDay(1),
            to = toYm.atEndOfMonth(),
        )
        if (population.isEmpty()) return emptyMap()

        // 월(yyyy-MM) → 거래처 → 월별 지표. 월 단위로 N/당월근무일수를 계산해야 하므로 월로 먼저 나눈다.
        // accountId → 월별 지표 리스트 (오름차순)
        val perAccountMonthly = mutableMapOf<Long, MutableList<WorkHistoryAccountMonthlyStat>>()

        val byMonth = population.groupBy { YearMonth.from(it.workingDate!!) }
        for (ym in byMonth.keys.sorted()) {
            val monthRows = byMonth.getValue(ym)

            // 그날 투입건수 N — 사원+근무일별 모수 row 수 (거래처 무관). refreshIntegration 동등.
            val rowCountByDate: Map<LocalDate, Int> = monthRows.groupingBy { it.workingDate!! }.eachCount()
            // 당월근무일수 — 사원+costCenter 별 distinct 근무일 (거래처 무관). refreshIntegration 동등.
            val workingDaysByCostCenter: Map<String?, Int> = monthRows
                .groupBy { it.costCenterCode }
                .mapValues { (_, rows) -> rows.mapNotNull { it.workingDate }.distinct().size }

            val byAccount = monthRows.groupBy { it.account?.id }
            for ((accountId, accRows) in byAccount) {
                if (accountId == null) continue // 모수는 account != null 이나 방어적으로 스킵

                // 환산근무일수 — Σ(1/N) 전정밀도 누적 후 최종 4자리 HALF_UP (refreshIntegration 동등).
                var equivalentRaw = BigDecimal.ZERO
                for (row in accRows) {
                    val n = rowCountByDate[row.workingDate!!] ?: 1
                    equivalentRaw = equivalentRaw.add(BigDecimal.ONE.divide(BigDecimal(n), 16, RoundingMode.HALF_UP))
                }
                val equivalentWorkingDays = equivalentRaw.setScale(4, RoundingMode.HALF_UP)

                // 환산인원 — 미반올림 환산근무일수 ÷ 당월근무일수 (거래처 대표 costCenter 기준).
                val rep = accRows.first()
                val workingDaysMonth = workingDaysByCostCenter[rep.costCenterCode] ?: 0
                val convertedHeadcount = if (workingDaysMonth > 0) {
                    equivalentRaw.divide(BigDecimal(workingDaysMonth), 4, RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }

                // 총투입횟수 — 이 거래처+월 의 근무유형 조합별 distinct 근무일 수 합 (refreshIntegration 동등).
                val totalInputCount = accRows
                    .groupBy { inputsComboKey(it) }
                    .values
                    .sumOf { comboRows -> comboRows.mapNotNull { it.workingDate }.distinct().size }

                // 근무형태1/3/4/5 대표값 — 이 거래처+월 최다 조합의 대표 row 값.
                val repByCombo = accRows
                    .groupBy { inputsComboKey(it) }
                    .maxByOrNull { it.value.size }
                    ?.value?.first() ?: rep

                perAccountMonthly.getOrPut(accountId) { mutableListOf() }.add(
                    WorkHistoryAccountMonthlyStat(
                        yearMonth = ym.format(YEAR_MONTH_FORMAT),
                        totalWorkingDays = accRows.size,
                        totalInputCount = totalInputCount,
                        equivalentWorkingDays = equivalentWorkingDays,
                        convertedHeadcount = convertedHeadcount,
                        workingCategory1 = repByCombo.workingCategory1?.displayName,
                        workingCategory3 = repByCombo.workingCategory3?.displayName,
                        workingCategory4 = repByCombo.secondWorkType,
                        workingCategory5 = repByCombo.workingCategory5?.displayName,
                    )
                )
            }
        }

        // 기간 합산 (투입횟수/환산근무일수는 합산 가능) + 월별 분해 (단일 월이면 빈 리스트).
        return perAccountMonthly.mapValues { (_, monthly) ->
            val sorted = monthly.sortedBy { it.yearMonth }
            AccountBMetrics(
                totalInputCount = sorted.sumOf { it.totalInputCount },
                equivalentWorkingDays = sorted
                    .fold(BigDecimal.ZERO) { acc, m -> acc.add(m.equivalentWorkingDays) }
                    .setScale(4, RoundingMode.HALF_UP),
                monthlyStats = if (sorted.size > 1) sorted else emptyList(),
            )
        }
    }

    /**
     * 통합일정 총투입횟수 조합 키 — 거래처+근무유형 조합 (costCenter 미포함).
     * [AdminMonthlyIntegrationService.inputsComboKey] 동등.
     */
    private fun inputsComboKey(schedule: TeamMemberSchedule): List<Any?> = listOf(
        schedule.account?.id,
        schedule.workingCategory1,
        schedule.workingCategory3,
        schedule.secondWorkType,
        schedule.workingCategory5,
        schedule.professionalPromotionTeam,
    )

    /** [computeAccountBMetrics] 반환용 — 거래처 1개의 B그룹 지표 (기간 합산 + 월별 분해). */
    private data class AccountBMetrics(
        val totalInputCount: Int,
        val equivalentWorkingDays: BigDecimal,
        val monthlyStats: List<WorkHistoryAccountMonthlyStat>,
    )

    /**
     * 기간별 근무내역 집계 엑셀 export — 조회와 동일 필터/스코프.
     */
    fun exportSummary(
        scope: DataScope,
        fromYearMonth: String,
        toYearMonth: String,
        costCenterCodes: List<String>,
        keyword: String?,
    ): ExcelResult {
        val response = getSummary(scope, fromYearMonth, toYearMonth, costCenterCodes, keyword)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("기간별근무내역")
        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)

        val headers = listOf(
            "소속지점", "사번", "이름", "직위",
            "총 근무일수", "근무 거래처 수", "진열", "행사", "근무", "연차", "대휴",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        response.items.forEachIndexed { idx, item ->
            val row = sheet.createRow(idx + 1)
            row.createCell(0).setCellValue(item.orgName ?: "")
            row.createCell(1).setCellValue(item.employeeCode ?: "")
            row.createCell(2).setCellValue(item.employeeName ?: "")
            row.createCell(3).setCellValue(item.title ?: "")
            row.createCell(4).setCellValue(item.totalWorkingDays.toDouble())
            row.createCell(5).setCellValue(item.workingAccountCount.toDouble())
            row.createCell(6).setCellValue(item.displayDays.toDouble())
            row.createCell(7).setCellValue(item.eventDays.toDouble())
            row.createCell(8).setCellValue(item.workDays.toDouble())
            row.createCell(9).setCellValue(item.annualLeaveDays.toDouble())
            row.createCell(10).setCellValue(item.altHolidayDays.toDouble())
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val filename = "기간별근무내역_%s_%s.xlsx".format(response.fromYearMonth, response.toYearMonth)
        return ExcelResult(bytes, filename)
    }

    /**
     * 일정 행을 여사원(사번)별로 집계.
     * 사번 미보유(null) 행은 집계 대상에서 제외 (여사원 식별 불가).
     * 정렬은 repository orderBy (orgName, employeeCode) 를 그룹 첫 등장 순서로 보존.
     *
     * 조회 기간이 2개월 이상이면 각 여사원의 월별 분해(monthlyBreakdown)를 함께 채운다 (yyyy-MM 오름차순,
     * 데이터 있는 월만). 단일 월이면 분해가 합계와 동일하므로 빈 리스트.
     */
    private fun aggregate(
        schedules: List<TeamMemberSchedule>,
        fromYm: YearMonth,
        toYm: YearMonth,
    ): List<WorkHistoryPeriodSummaryItem> {
        val multiMonth = fromYm.isBefore(toYm)
        val grouped = schedules
            .filter { !it.employee?.employeeCode.isNullOrBlank() }
            .groupBy { it.employee?.employeeCode!! }

        return grouped.map { (_, rows) ->
            val emp = rows.first().employee
            val total = stat(rows)
            val breakdown = if (multiMonth) {
                rows.groupBy { yearMonthOf(it) }
                    .toSortedMap()
                    .map { (ym, monthRows) ->
                        val s = stat(monthRows)
                        WorkHistoryMonthlyStat(
                            yearMonth = ym,
                            totalWorkingDays = s.totalWorkingDays,
                            workingAccountCount = s.workingAccountCount,
                            displayDays = s.displayDays,
                            eventDays = s.eventDays,
                            workDays = s.workDays,
                            annualLeaveDays = s.annualLeaveDays,
                            altHolidayDays = s.altHolidayDays,
                        )
                    }
            } else {
                emptyList()
            }

            WorkHistoryPeriodSummaryItem(
                orgName = emp?.orgName,
                employeeCode = emp?.employeeCode,
                employeeName = emp?.name,
                title = emp?.jikwee,
                totalWorkingDays = total.totalWorkingDays,
                workingAccountCount = total.workingAccountCount,
                displayDays = total.displayDays,
                eventDays = total.eventDays,
                workDays = total.workDays,
                annualLeaveDays = total.annualLeaveDays,
                altHolidayDays = total.altHolidayDays,
                monthlyBreakdown = breakdown,
            )
        }
    }

    /** 일정 행 집합의 근무 통계 산출 (합계/월별 공통). */
    private fun stat(rows: List<TeamMemberSchedule>): Stat = Stat(
        totalWorkingDays = rows.size,
        workingAccountCount = rows.mapNotNull { it.account?.id }.distinct().size,
        displayDays = rows.count { it.workingCategory1 == WorkingCategory1.DISPLAY },
        eventDays = rows.count { it.workingCategory1 == WorkingCategory1.EVENT },
        workDays = rows.count { it.workingType == WorkingType.WORK },
        annualLeaveDays = rows.count { it.workingType == WorkingType.ANNUAL_LEAVE },
        altHolidayDays = rows.count { it.workingType == WorkingType.ALT_HOLIDAY },
    )

    /** 일정의 근무일자 → yyyy-MM 문자열. workingDate null 은 "unknown" 으로 묶어 분해에 노출. */
    private fun yearMonthOf(schedule: TeamMemberSchedule): String =
        schedule.workingDate?.let { YearMonth.from(it).format(YEAR_MONTH_FORMAT) } ?: "unknown"

    private data class Stat(
        val totalWorkingDays: Int,
        val workingAccountCount: Int,
        val displayDays: Int,
        val eventDays: Int,
        val workDays: Int,
        val annualLeaveDays: Int,
        val altHolidayDays: Int,
    )

    /**
     * 조회 대상 지점 코드(costCenterCode) 산출.
     * - 사용자가 지점을 선택하면 권한 스코프와 교집합 (전사 권한자는 선택 그대로).
     * - 선택이 없으면 권한 스코프 전체 (전사 권한자는 빈 리스트 = 무제한).
     */
    private fun resolveBranchCodes(scope: DataScope, requested: List<String>): List<String> {
        val cleaned = requested.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when {
            cleaned.isEmpty() -> if (scope.isAllBranches) emptyList() else scope.branchCodes
            scope.isAllBranches -> cleaned
            else -> cleaned.filter { it in scope.branchCodes }
        }
    }

    private fun parseYearMonth(value: String, paramName: String): YearMonth {
        return try {
            YearMonth.parse(value, YEAR_MONTH_FORMAT)
        } catch (e: DateTimeParseException) {
            throw InvalidParameterException("$paramName 형식이 올바르지 않습니다 (yyyy-MM): $value")
        }
    }

    private fun validateRange(from: YearMonth, to: YearMonth) {
        if (from.year !in 2020..2099 || to.year !in 2020..2099) {
            throw InvalidParameterException("조회 기간은 2020~2099 범위여야 합니다")
        }
        if (from.isAfter(to)) {
            throw InvalidParameterException("시작년월은 종료년월보다 이후일 수 없습니다")
        }
        // 시작~종료 포함 최대 MAX_RANGE_MONTHS 개월. (차이 개월 수 + 1 = 포함 개월 수)
        val inclusiveMonths = ChronoUnit.MONTHS.between(from, to) + 1
        if (inclusiveMonths > MAX_RANGE_MONTHS) {
            throw InvalidParameterException("조회 기간은 최대 ${MAX_RANGE_MONTHS}개월까지 가능합니다")
        }
    }

    companion object {
        private val YEAR_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        private const val MAX_RANGE_MONTHS = 6L
    }
}
