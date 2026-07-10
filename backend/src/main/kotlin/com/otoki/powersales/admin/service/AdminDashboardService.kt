package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.AgeGroupCount
import com.otoki.powersales.admin.dto.response.BasicStats
import com.otoki.powersales.admin.dto.response.ChannelStackRow
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.dto.response.EtcBreakdownItem
import com.otoki.powersales.admin.dto.response.SalesSummary
import com.otoki.powersales.admin.dto.response.StaffDeployment
import com.otoki.powersales.admin.dto.response.StaffTypeCount
import com.otoki.powersales.admin.dto.response.TotalByPosition
import com.otoki.powersales.admin.dto.response.WorkTypeChannelChart
import com.otoki.powersales.admin.dto.response.WorkTypeStats
import com.otoki.powersales.domain.org.employee.repository.DashboardEmployeeProjection
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.repository.DashboardDeploymentRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesAdminQueryService
import com.otoki.powersales.domain.sales.service.MonthlySalesAdminQueryService.InvestedAccountRef
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 투입현황 대시보드 집계 service (Spec 850).
 *
 * ## 책임
 * SF 레거시 '여사원 투입현황' 대시보드 3섹션(매출현황 / 여사원 투입현황 / 기본현황)을 신규 시스템
 * 도메인 서비스를 조합(orchestration)하여 채운다. 진입점 패키지라 도메인 집계는 직접 수행하지 않고
 * [MonthlyFemaleEmployeeIntegrationScheduleRepository] / [EmployeeRepository] /
 * [MonthlySalesAdminQueryService] 에 위임하고 메모리에서 합산만 한다.
 *
 * ## 레거시 매핑 + 신규 차이
 * - 환산인원: SF `MonthlyFemaleEmployeeIntegrationSchedule__c.ConvertedHeadcount__c`(Number 18,4) SUM 정합 — scale=4.
 * - 여사원 투입현황 탭 차트는 SF 레거시 대시보드(LAST_MONTH 필터)와 동일하게 **모두 전월(마감) 고정**
 *   (결정 D2 를 탭 전체로 확장). 매출현황/기본현황은 yearMonth 토글(당월 기본).
 * - 판촉/OSC 구분: `Employee.jobCode`("판촉직" / "OSC직"·"레이디직") — 레거시 `EmployeeTriggerHandler.cls:47` 정합(결정 D6).
 * - 매출현황 탭(salesSummary): 투입 거래처 기준 실적 + 목표 + 달성률 + 기준진도율(달력일) + 전년 비교.
 *   목표는 투입 거래처별 `SalesProgressRateMaster`(연·월 1행) 합계 총합, 달성률 = round(실적/목표×100).
 *   유통별 목표/진도율(channelSales)은 데이터 부재로 빈 리스트.
 *   실적 source 는 RDS `MonthlySalesHistory` ([MonthlySalesAdminQueryService] 경유) — 외부 ORORA view 직접 호출 아님.
 *   (ORORA view 는 RDS 적재 배치에서만 읽고, 화면/집계는 항상 RDS 적재본을 본다.)
 */
@Service
@Transactional(readOnly = true)
class AdminDashboardService(
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val employeeRepository: EmployeeRepository,
    private val monthlySalesAdminQueryService: MonthlySalesAdminQueryService,
) {

    /**
     * 대시보드 3섹션 집계 — yearMonth 미지정 시 당월.
     *
     * [effectiveCodes] 는 컨트롤러가 [DashboardBranchResolver.effectiveBranchCodes] 로 산출한
     * 조회 지점 코드 목록(전사 권한자는 34개 화이트리스트, 지점 사용자는 본인 지점). 빈 목록이면 조회 결과 0건.
     *
     * 여사원 투입현황은 전 차트 전월(마감) 고정(D2 확장). 매출현황은 실적+기준진도율만(D7).
     */
    fun getDashboard(
        effectiveCodes: List<String>,
        yearMonth: String?,
        branchNamesByCode: Map<String, String> = emptyMap(),
    ): DashboardResponse {
        val ym = yearMonth?.let { YearMonth.parse(it, YEAR_MONTH_FORMATTER) } ?: YearMonth.now()
        val previousYm = ym.minusMonths(1)
        // 조회 조건(지점)을 화면에 노출하기 위한 라벨 — 코드가 아니라 실제 지점명.
        // 단일 지점이면 그 지점명, 복수면 "OO 외 N", effectiveCodes 가 비면(전사 권한 전체) "전체".
        val branchName = resolveBranchLabel(effectiveCodes, branchNamesByCode)

        // 당월 MFEIS 는 매출/기본 두 섹션이 공유 — 1회만 조회해 재사용 (중복 trip 제거).
        // 전월 MFEIS 는 투입현황(D2 확장: 전 차트 전월 마감 고정)에서만 쓰여 해당 빌더가 자체 조회한다.
        val rows = mfeisRepository.findDeploymentDashboardRows(
            ym.year.toString(), ym.monthValue.toString(), effectiveCodes,
        )

        return DashboardResponse(
            salesSummary = buildSalesSummary(ym, branchName, rows),
            staffDeployment = buildStaffDeployment(ym, previousYm, branchName, effectiveCodes),
            basicStats = buildBasicStats(ym, branchName, effectiveCodes, rows),
        )
    }

    // ------------------- 매출현황 (D7: 실적 + 기준진도율) -------------------

    /**
     * 매출현황 — 투입 거래처의 당월/전년 마감실적 합산 (D7).
     *
     * 매출 실적은 RDS `MonthlySalesHistory` ([MonthlySalesAdminQueryService.sumInvestedAccountSales]
     * 경유) 에서 조회한다 — 외부 ORORA view 직접 호출이 아니다.
     */
    private fun buildSalesSummary(
        ym: YearMonth,
        branchName: String?,
        rows: List<DashboardDeploymentRow>,
    ): SalesSummary {
        // 투입 거래처 = 해당 월 MFEIS 에 등장하는 거래처(accountId 기준 distinct) — rows 는 getDashboard 에서 1회 조회분
        val accounts = rows
            .filter { it.accountId != null }
            .distinctBy { it.accountId }
            .map { InvestedAccountRef(id = it.accountId!!, externalKey = it.accountExternalKey) }
        val sales = monthlySalesAdminQueryService.sumInvestedAccountSales(accounts, ym.year, ym.monthValue)

        val lastYearRatio = if (sales.lastYearAmount == 0L) 0.0
        else (sales.actualAmount.toDouble() / sales.lastYearAmount.toDouble()) * 100.0

        // 달성률 = round(실적 / 목표 × 100). 목표 0(미등록) 이면 0.0 — 월 매출 요약([MonthlySalesAdminQueryService]) 정합.
        val progressRate = if (sales.targetAmount <= 0L) 0.0
        else Math.round(sales.actualAmount.toDouble() / sales.targetAmount * 100).toDouble()

        return SalesSummary(
            yearMonth = ym.format(YEAR_MONTH_FORMATTER),
            branchName = branchName,
            investedAccountCount = accounts.size,
            targetAmount = sales.targetAmount,
            actualAmount = sales.actualAmount,
            progressRate = progressRate,
            referenceProgressRate = calendarReferenceProgressRate(ym, LocalDate.now()),
            lastYearAmount = sales.lastYearAmount,
            lastYearRatio = lastYearRatio,
            channelSales = emptyList(), // 후속 — 유통별 목표/진도율 데이터 부재
            hasActualData = sales.hasActualData,
            hasLastYearData = sales.hasLastYearData,
            hasTargetData = sales.hasTargetData,
        )
    }

    /**
     * 달력일 기준 진도율 (결정 D4) — (경과 달력일 / 총 달력일) × 100.
     *
     * 과거월 100.0 / 미래월 0.0 / 당월은 오늘까지 경과일 비율. 영업일 아님 — 휴일 마스터 불요.
     */
    internal fun calendarReferenceProgressRate(ym: YearMonth, today: LocalDate): Double {
        val firstDay = ym.atDay(1)
        val lastDay = ym.atEndOfMonth()
        if (today.isAfter(lastDay)) return 100.0
        if (today.isBefore(firstDay)) return 0.0
        val totalDays = ym.lengthOfMonth()
        val elapsedDays = today.dayOfMonth
        return (elapsedDays.toDouble() / totalDays.toDouble()) * 100.0
    }

    // ------------------- 여사원 투입현황 -------------------

    private fun buildStaffDeployment(
        ym: YearMonth,
        previousYm: YearMonth,
        branchName: String?,
        effectiveCodes: List<String>,
    ): StaffDeployment {
        // SF 레거시 대시보드(LAST_MONTH 필터) 정합 — 투입현황 차트가 선택월의 전월(마감) 데이터 사용 (D2 확장)
        val previousRows = mfeisRepository.findDeploymentDashboardRows(
            previousYm.year.toString(), previousYm.monthValue.toString(), effectiveCodes,
        )

        // SF 는 근무유형1(진열/행사)로 리포트 2개를 분리한다 (WorkTypeForReport__c 스택 축도 진열=WC3 / 행사=WC4 로 갈림).
        val displayRows = previousRows.filter { it.workingCategory1 == WC1_DISPLAY }
        val eventRows = previousRows.filter { it.workingCategory1 == WC1_EVENT }

        return StaffDeployment(
            yearMonth = ym.format(YEAR_MONTH_FORMATTER),
            branchName = branchName,
            display = buildChannelChart(displayRows, DISPLAY_STACK_ORDER),
            event = buildChannelChart(eventRows, EVENT_STACK_ORDER),
        )
    }

    /**
     * 유통(거래처유형) × 근무형태 스택 누적 막대 1개 — SF 리포트 1개 대응.
     *
     * 스택 세그먼트 라벨은 [workTypeLabel] (SF WorkTypeForReport__c formula) 로 산출한다.
     * [presetOrder] 로 알려진 라벨을 먼저 고정 정렬하고, 그 외 실측 라벨(예: 신규 근무유형4 값)은 뒤에 이름순으로 붙인다.
     */
    private fun buildChannelChart(
        rows: List<DashboardDeploymentRow>,
        presetOrder: List<String>,
    ): WorkTypeChannelChart {
        // 등장하는 스택 라벨 집합 → preset 순서 우선, 그 외는 이름순으로 뒤에.
        val presentLabels = rows.mapTo(sortedSetOf()) { workTypeLabel(it) }
        val stackKeys = presetOrder.filter { it in presentLabels } +
            presentLabels.filter { it !in presetOrder }

        // 거래처유형(구분) × 스택라벨 별 환산인원 SUM.
        val byChannel = rows.groupBy { it.accountType ?: ACCOUNT_TYPE_UNKNOWN }
        val channelRows = byChannel
            .map { (channelName, group) ->
                val byLabel = group.groupBy { workTypeLabel(it) }
                ChannelStackRow(
                    channelName = channelName,
                    headcounts = stackKeys.map { key -> sumHeadcount(byLabel[key].orEmpty()) },
                )
            }
            .sortedBy { it.channelName }

        return WorkTypeChannelChart(
            stackKeys = stackKeys,
            rows = channelRows,
            totalHeadcount = sumHeadcount(rows),
        )
    }

    /**
     * SF `WorkTypeForReport__c`(근무형태3&4) formula 재현 — 스택 세그먼트 라벨.
     *
     * - 진열: 근무유형3 → "1.고정" / "2.격고" / "3.순회" (그 외/null → "기타")
     * - 행사: 근무유형4 = "상온" → "4.상온", 그 외 → "5.{근무유형4}" (null/빈 → "5.")
     */
    private fun workTypeLabel(row: DashboardDeploymentRow): String {
        return if (row.workingCategory1 == WC1_DISPLAY) {
            when (row.workingCategory3) {
                WC3_FIXED -> "1.고정"
                WC3_ALTERNATING -> "2.격고"
                WC3_VISITING -> "3.순회"
                else -> WORK_TYPE_LABEL_ETC
            }
        } else {
            val wc4 = row.workingCategory4
            if (wc4 == WC4_ROOM_TEMP) "4.상온" else "5.${wc4 ?: ""}"
        }
    }

    /** 환산인원 SUM — SF scale=4 정합 (HALF_UP). */
    private fun sumHeadcount(rows: List<DashboardDeploymentRow>): BigDecimal {
        return rows
            .fold(BigDecimal.ZERO) { acc, row -> acc + (row.convertedHeadcount ?: BigDecimal.ZERO) }
            .setScale(HEADCOUNT_SCALE, RoundingMode.HALF_UP)
    }

    // ------------------- 기본 현황 -------------------

    private fun buildBasicStats(
        ym: YearMonth,
        branchName: String?,
        effectiveCodes: List<String>,
        mfeisRows: List<DashboardDeploymentRow>,
    ): BasicStats {
        val employees = findEmployees(effectiveCodes)
        val asOf = ym.atEndOfMonth()

        // 판촉직/OSC직 (결정 D6 — Employee.jobCode). etc = 두 직군 외/null (모수는 사원 전체로 일치)
        val promotion = employees.count { it.jobCode == JOB_CODE_PROMOTION }
        val osc = employees.count { it.jobCode == JOB_CODE_OSC || it.jobCode == JOB_CODE_LADY }
        val staffTypeEtcEmployees = employees.filter {
            it.jobCode != JOB_CODE_PROMOTION && it.jobCode != JOB_CODE_OSC && it.jobCode != JOB_CODE_LADY
        }
        val staffTypeEtcBreakdown = buildEtcBreakdown(staffTypeEtcEmployees.map { it.jobCode })

        // 재직/휴직. 퇴직자는 모수에서 이미 제외됨(findEmployees). etc = status 가 재직/휴직 외이거나 null 인 사원
        val active = employees.count { it.status == STATUS_ACTIVE }
        val onLeave = employees.count { it.status == STATUS_ON_LEAVE }
        val positionEtcEmployees = employees.filter {
            it.status != STATUS_ACTIVE && it.status != STATUS_ON_LEAVE
        }
        val positionEtcBreakdown = buildEtcBreakdown(positionEtcEmployees.map { it.status })

        // 근무형태별 고정/격고/순회 — MFEIS 당월 근무형태 기준 환산인원 SUM (getDashboard 1회 조회분 공유)
        val byWc3 = mfeisRows.groupBy { it.workingCategory3 }

        return BasicStats(
            branchName = branchName,
            staffType = StaffTypeCount(
                promotion = promotion,
                osc = osc,
                etc = staffTypeEtcEmployees.size,
                etcBreakdown = staffTypeEtcBreakdown,
            ),
            totalByPosition = TotalByPosition(
                active = active,
                onLeave = onLeave,
                etc = positionEtcEmployees.size,
                etcBreakdown = positionEtcBreakdown,
            ),
            byAgeGroup = buildAgeGroups(employees, asOf),
            byWorkType = WorkTypeStats(
                fixed = sumHeadcount(byWc3[WC3_FIXED].orEmpty()),
                alternating = sumHeadcount(byWc3[WC3_ALTERNATING].orEmpty()),
                visiting = sumHeadcount(byWc3[WC3_VISITING].orEmpty()),
            ),
        )
    }

    /**
     * "기타" 항목 세부 내역 — 원본 값(jobCode/status)별 인원 수 집계.
     *
     * null/공백 값은 [ETC_LABEL_UNCLASSIFIED]("미분류") 로 합산. count 내림차순, 동수면 라벨 오름차순 정렬해
     * 툴팁 표시 순서를 안정화한다.
     */
    private fun buildEtcBreakdown(rawValues: List<String?>): List<EtcBreakdownItem> {
        return rawValues
            .groupingBy { it?.takeIf { v -> v.isNotBlank() } ?: ETC_LABEL_UNCLASSIFIED }
            .eachCount()
            .map { (label, count) -> EtcBreakdownItem(label = label, count = count) }
            .sortedWith(compareByDescending<EtcBreakdownItem> { it.count }.thenBy { it.label })
    }

    /** 연령대별 인원 수 — 만나이 정수 10세 단위 버킷. birthDate null/파싱불가는 "미상". */
    private fun buildAgeGroups(employees: List<DashboardEmployeeProjection>, asOf: LocalDate): List<AgeGroupCount> {
        return employees
            .groupBy { ageGroupLabel(calculateAge(it.birthDate, asOf)) }
            .map { (ageGroup, group) -> AgeGroupCount(ageGroup = ageGroup, count = group.size) }
            .sortedBy { ageGroupSortKey(it.ageGroup) }
    }

    private fun ageGroupLabel(age: Int?): String {
        if (age == null || age < 0) return AGE_GROUP_UNKNOWN
        val decade = (age / 10) * 10
        return "${decade}대"
    }

    private fun ageGroupSortKey(label: String): Int {
        if (label == AGE_GROUP_UNKNOWN) return Int.MAX_VALUE
        return label.removeSuffix("대").toIntOrNull() ?: Int.MAX_VALUE
    }

    /**
     * 만나이 정수 산출 — birthDate(`yyyy-MM-dd` String) 와 기준일 비교.
     *
     * 파싱 불가 / 미래 birthDate 는 null (호출부에서 "미상" 처리).
     */
    private fun calculateAge(birthDate: String?, asOf: LocalDate): Int? {
        if (birthDate.isNullOrBlank()) return null
        val date = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
        return Period.between(date, asOf).years.takeIf { it >= 0 }
    }

    // ------------------- helpers -------------------

    /**
     * 조회 조건 라벨 — effectiveCodes(실제 조회 지점) 를 지점명으로 변환해 응답 branchName 에 노출.
     *
     * - 빈 목록(조회 권한 지점 없음): "전체" (0건 결과의 표기)
     * - 단일 지점: 그 지점명 (맵 부재 시 코드 fallback)
     * - 복수 지점: "OO 외 N개" (사용자가 어느 범위인지 인지하도록)
     */
    private fun resolveBranchLabel(
        effectiveCodes: List<String>,
        branchNamesByCode: Map<String, String>,
    ): String? {
        if (effectiveCodes.isEmpty()) return BRANCH_LABEL_ALL
        val names = effectiveCodes.map { branchNamesByCode[it] ?: it }
        return if (names.size == 1) names.first()
        else "${names.first()} 외 ${names.size - 1}개"
    }

    private fun findEmployees(effectiveCodes: List<String>): List<DashboardEmployeeProjection> {
        // 기본현황은 jobCode/status/birthDate 만 쓰므로 projection 으로 적재 (entity 전 컬럼 회피).
        // 퇴직자는 재직 현황 모수에서 제외 (repository 쿼리 레벨). empty → 전사 조회.
        return employeeRepository.findDashboardBasicStatsProjection(effectiveCodes)
    }

    companion object {
        private val YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
        private const val HEADCOUNT_SCALE = 4

        private const val BRANCH_LABEL_ALL = "전체"

        private const val ACCOUNT_TYPE_UNKNOWN = "미상"
        private const val AGE_GROUP_UNKNOWN = "미상"
        private const val ETC_LABEL_UNCLASSIFIED = "미분류"
        private const val WORK_TYPE_LABEL_ETC = "기타" // 진열인데 WC3 가 고정/격고/순회 외/null

        // 근무유형1(WorkingCategory1) — SF 리포트 분리 축 (진열/행사)
        private const val WC1_DISPLAY = "진열"
        private const val WC1_EVENT = "행사"

        // 근무형태(WorkingCategory3) 표준값 (진열 스택 + basicStats 근무형태별)
        private const val WC3_FIXED = "고정"
        private const val WC3_ALTERNATING = "격고"
        private const val WC3_VISITING = "순회"

        // 근무유형4(WorkingCategory4) — 행사 스택 상온 판별값 (SF WorkTypeForReport__c: 상온→"4.상온")
        private const val WC4_ROOM_TEMP = "상온"

        // 스택 세그먼트 preset 정렬 순서 (SF WorkTypeForReport__c 값)
        private val DISPLAY_STACK_ORDER = listOf("1.고정", "2.격고", "3.순회", WORK_TYPE_LABEL_ETC)
        private val EVENT_STACK_ORDER = listOf("4.상온", "5.냉동", "5.냉장", "5.라면", "5.만두")

        // 재직상태(Employee.status) 표준값
        private const val STATUS_ACTIVE = "재직"
        private const val STATUS_ON_LEAVE = "휴직"

        // 판촉/OSC 판별값 (Employee.jobCode — 결정 D6)
        private const val JOB_CODE_PROMOTION = "판촉직"
        private const val JOB_CODE_OSC = "OSC직"
        private const val JOB_CODE_LADY = "레이디직" // 구 OSC (SAP A053, 2023-01-02 이전)
    }
}
