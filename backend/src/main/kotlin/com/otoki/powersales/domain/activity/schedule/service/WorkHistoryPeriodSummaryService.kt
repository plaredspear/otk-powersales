package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryAccountMonthlyStat
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryAccountStat
import com.otoki.powersales.domain.activity.schedule.dto.response.WorkHistoryEmployeeAccountResponse
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
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
 * 기간별 근무내역(개인) — 특정 여사원 1명의 거래처별 근무 집계 서비스.
 *
 * 근무기간 조회(월별근무내역 목록)와 동일하게 [TeamMemberSchedule](출근 등록 기준) 을 원천으로,
 * 선택한 여사원의 기간(시작년월~종료년월) 근무 행을 거래처 단위로 집계한다. 거래처별로 통합일정(MFEIS)
 * B그룹 지표(투입횟수/환산근무일수/환산인원/근무형태)를 조회 시점에 재현해 함께 제공한다.
 */
@Service
@Transactional(readOnly = true)
class WorkHistoryPeriodSummaryService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
) {

    /**
     * 특정 여사원 1명의 기간 내 거래처별 근무 집계 조회.
     *
     * 기간 검증은 yyyy-MM / 2020~2099 / 순서 / 최대 6개월.
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

    /** 일정 행 집합의 근무 통계 산출 (거래처별 집계 stat 공통). */
    private fun stat(rows: List<TeamMemberSchedule>): Stat = Stat(
        totalWorkingDays = rows.size,
        workingAccountCount = rows.mapNotNull { it.account?.id }.distinct().size,
        displayDays = rows.count { it.workingCategory1 == WorkingCategory1.DISPLAY },
        eventDays = rows.count { it.workingCategory1 == WorkingCategory1.EVENT },
        workDays = rows.count { it.workingType == WorkingType.WORK },
        annualLeaveDays = rows.count { it.workingType == WorkingType.ANNUAL_LEAVE },
        altHolidayDays = rows.count { it.workingType == WorkingType.ALT_HOLIDAY },
    )

    private data class Stat(
        val totalWorkingDays: Int,
        val workingAccountCount: Int,
        val displayDays: Int,
        val eventDays: Int,
        val workDays: Int,
        val annualLeaveDays: Int,
        val altHolidayDays: Int,
    )

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
