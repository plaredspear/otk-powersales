package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.CategoryScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.CategoryScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationDetailResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationFilterOptionsResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationSourceScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberCategoryResultItem
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberScheduleResultItem
import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.config.CacheConfig
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val accountRepository: AccountRepository,
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway,
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
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
        costCenterCodes: List<String>,
        keyword: String? = null,
        accountKeyword: String? = null,
        distributionKeyword: String? = null,
        accountTypeKeyword: String? = null,
    ): MonthlyIntegrationScheduleResponse {
        validateParams(year, month, costCenterCodes)
        val sf = teamMemberScheduleSearchService.search(
            year = year.toString(),
            month = month.toString(),
            orgValues = costCenterCodes,
            keyword = keyword,
            accountKeyword = accountKeyword,
            distributionKeyword = distributionKeyword,
            accountTypeKeyword = accountTypeKeyword,
        )
        val items = sf.result.map { it.toMonthlyIntegrationItem() }
        return MonthlyIntegrationScheduleResponse(
            year = year,
            month = month,
            items = items,
            totalCount = items.size
        )
    }

    /**
     * 통합일정 조회조건 드롭다운 옵션 — 유통형태 / 거래처유형 목록 + 종속 매핑.
     *
     * Account 전체(미삭제)의 (유통형태, 거래처유형) 동시출현 distinct 4-튜플에서
     * 라벨을 조합해 (1) 유통형태 전체 목록, (2) 거래처유형 전체 목록,
     * (3) 유통형태 → 종속 거래처유형 목록 매핑을 구성한다.
     * 라벨 조합 규칙은 목록 화면([toResultItem])과 동일한 [Account] companion 정본을 재사용한다.
     *
     * 옵션 소스는 통합일정 검색 스코프(지점/월)가 아니라 Account 전역 코드 체계다 — 행사마스터
     * lookup(`findDistinctAccountTypes`)이 promotionLookupFilter/지점 스코프로 게이팅하는 것과 의도적으로
     * 다르다. 유통형태-거래처유형은 거래처 마스터의 코드 분류이므로 지점/월과 무관하게 전역 목록을 노출한다.
     *
     * 원천 Account 는 SAP inbound 거래처 마스터 적재(하루 1회)로만 갱신되므로, 매 화면 진입마다
     * 전역 distinct 스캔을 반복하지 않도록 Redis 캐시(24h TTL)에 얹는다. 무인자라 고정 key('all') 를 쓰며,
     * 적재 직후 [AccountUpsertService.upsert] 의 @CacheEvict 가 즉시 무효화한다(Organization 캐시군과 동일).
     */
    @Cacheable(value = [CacheConfig.CACHE_MONTHLY_INTEGRATION_FILTER_OPTIONS], key = "'ALL'")
    fun getFilterOptions(): MonthlyIntegrationFilterOptionsResponse {
        val pairs = accountRepository.findDistinctDistributionAbcPairs()

        val distributions = sortedSetOf<String>()
        val accountTypes = sortedSetOf<String>()
        val dependent = linkedMapOf<String, MutableSet<String>>()

        for (pair in pairs) {
            val distLabel = Account.distributionChannelLabel(pair.accountStatusCode, pair.accountType)
            val abcLabel = Account.abcTypeLabel(pair.abcTypeCode, pair.abcType)
            if (distLabel != null) distributions.add(distLabel)
            if (abcLabel != null) accountTypes.add(abcLabel)
            // 유통형태·거래처유형 둘 다 있는 경우에만 종속 매핑에 반영.
            if (distLabel != null && abcLabel != null) {
                dependent.getOrPut(distLabel) { sortedSetOf() }.add(abcLabel)
            }
        }

        return MonthlyIntegrationFilterOptionsResponse(
            distributions = distributions.toList(),
            accountTypes = accountTypes.toList(),
            dependentAccountTypes = dependent.mapValues { it.value.toList() },
        )
    }

    fun getCategorySchedule(
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        principal: WebUserPrincipal,
    ): CategoryScheduleResponse {
        validateParams(year, month, costCenterCodes)
        val sf = teamMemberCategorySearchService.search(
            year = year.toString(),
            month = month.toString(),
            orgValues = costCenterCodes,
            principal = principal,
        )
        // SF 정합 — 양월 0 지점도 행 유지 (setNull 상태 그대로, 수치는 null). mapNotNull 로 행을
        // 제거하면 SF (Aura 도 무필터 바인딩 — 지점명 + 빈 칸 행 표시) 와 달라진다.
        val items = sf.result.map { it.toCategoryItem() }
        return CategoryScheduleResponse(year = year, month = month, items = items)
    }

    fun exportMonthlyIntegration(
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        keyword: String? = null,
        accountKeyword: String? = null,
        distributionKeyword: String? = null,
        accountTypeKeyword: String? = null,
    ): ExcelResult {
        val response = getMonthlyIntegration(
            year, month, costCenterCodes, keyword, accountKeyword, distributionKeyword, accountTypeKeyword,
        )
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("통합일정")

        val headers = listOf(
            "소속", "거래처지점명", "거래처코드", "거래처명", "유통형태", "거래처유형", "사번", "직위", "이름",
            "근무형태1", "근무형태3", "근무형태4", "근무형태5",
            "총 투입횟수", "총 환산근무일수", "총 환산인원", "ABC마감실적"
        )

        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)
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
            row.createCell(4).setCellValue(item.distributionChannelLabel ?: "")
            row.createCell(5).setCellValue(item.abcTypeLabel ?: "")
            row.createCell(6).setCellValue(item.employeeCode)
            row.createCell(7).setCellValue(item.title ?: "")
            row.createCell(8).setCellValue(item.employeeName)
            row.createCell(9).setCellValue(item.workingCategory1)
            row.createCell(10).setCellValue(item.workingCategory3 ?: "")
            row.createCell(11).setCellValue(item.workingCategory4 ?: "")
            row.createCell(12).setCellValue(item.workingCategory5 ?: "")
            row.createCell(13).apply {
                setCellValue(item.totalInputCount.toDouble())
                cellStyle = intStyle
            }
            row.createCell(14).apply {
                setCellValue(item.equivalentWorkingDays.toDouble())
                cellStyle = decimal3Style
            }
            row.createCell(15).apply {
                setCellValue(item.convertedHeadcount.toDouble())
                cellStyle = decimal3Style
            }
            row.createCell(16).apply {
                setCellValue(item.avgClosingAmount.toDouble())
                cellStyle = intStyle
            }
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_여사원 통합일정 조회_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    fun exportCategorySchedule(
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        principal: WebUserPrincipal,
    ): ExcelResult {
        val response = getCategorySchedule(year, month, costCenterCodes, principal)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("근무형태별 여사원인원현황")

        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)
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

        // SF setNull 정합 — 양월 0 지점 행의 null 수치는 빈 셀로 출력 (화면의 빈 칸 표시와 동일)
        fun org.apache.poi.ss.usermodel.Row.createDecimalCell(
            col: Int,
            value: BigDecimal?,
            style: org.apache.poi.ss.usermodel.CellStyle,
        ) {
            createCell(col).apply {
                value?.let { setCellValue(it.toDouble()) }
                cellStyle = style
            }
        }

        response.items.forEachIndexed { rowIdx, item ->
            val row = sheet.createRow(rowIdx + 2)
            row.createCell(0).setCellValue(item.branchName)
            row.createDecimalCell(1, item.currentMonthTotal, decimal1Style)
            row.createDecimalCell(2, item.previousMonthTotal, decimal1Style)
            row.createDecimalCell(3, item.totalChange, decimal1Style)
            row.createDecimalCell(4, item.displayFixed, decimal3Style)
            row.createDecimalCell(5, item.displayAlternate, decimal3Style)
            row.createDecimalCell(6, item.displayPatrol, decimal3Style)
            row.createDecimalCell(7, item.currentMonthDisplayTotal, decimal3Style)
            row.createDecimalCell(8, item.previousMonthDisplayTotal, decimal3Style)
            row.createDecimalCell(9, item.displayChange, decimal3Style)
            row.createDecimalCell(10, item.eventAmbient, decimal3Style)
            row.createDecimalCell(11, item.eventFrozenChilled, decimal3Style)
            row.createDecimalCell(12, item.currentMonthEventTotal, decimal3Style)
            row.createDecimalCell(13, item.previousMonthEventTotal, decimal3Style)
            row.createDecimalCell(14, item.eventChange, decimal3Style)
        }

        header2Labels.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_근무형태별_인원현황_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    /**
     * MFEIS row 상세 — 집계 근거가 된 여사원일정 목록.
     *
     * 집계 근거는 `refreshIntegration` 이 각 근거 TMS row 에 세팅한 FK
     * (`monthly_female_employee_integration_schedule_id`) 로 역참조한다 (FK 기반 조회).
     * 단, FK 가 아직 채워지지 않은 row (재집계 미수행 / 마이그레이션 직후) 는 기존 externalKey
     * 재매칭으로 폴백한다 — 재집계 1회 후에는 FK 경로로 수렴한다.
     *
     * `dailyScheduleCount`(N) 는 모수 전체(본 키 조합 외 포함) 기준 — 환산근무일수 1/N 의 분모와 동일.
     * 따라서 근거 목록을 FK 로 얻더라도 N 분모 산출용 월 모수(사원+월 출근등록 전건) 는 별도로 조회한다.
     */
    fun getIntegrationDetail(id: Long): MonthlyIntegrationDetailResponse {
        val mfeis = monthlyIntegrationScheduleRepository.findByIdWithEmployeeAndAccount(id)
            ?: throw MonthlyIntegrationNotFoundException()
        val employee = mfeis.employee
        val account = mfeis.account
        val year = mfeis.year?.toIntOrNull()
        val month = mfeis.month?.toIntOrNull()

        val schedules = if (employee != null && year != null && month != null) {
            val ym = YearMonth.of(year, month)
            // N 분모(그날 사원 출근 row 수, 거래처 무관) 는 근거 조합 외 row 도 포함하는 월 전체 모수 기준.
            val population = teamMemberScheduleRepository
                .findAttendedSchedulesByEmployeeAndMonth(employee.id, ym.atDay(1), ym.atEndOfMonth())
            val rowCountByDate: Map<LocalDate, Int> = population.groupingBy { it.workingDate!! }.eachCount()

            // 근거 일정 — FK 역참조 우선, FK 미채움 row 는 externalKey 재매칭 폴백.
            val byFk = teamMemberScheduleRepository.findSchedulesByIntegrationScheduleId(id)
            val sourceRows = byFk.ifEmpty {
                population.filter { buildLegacyExternalKey(it, mfeis.year!!, mfeis.month!!) == mfeis.externalKey }
            }
            sourceRows
                .sortedWith(compareBy({ it.workingDate }, { it.id }))
                .map { row ->
                    val n = rowCountByDate[row.workingDate!!] ?: 1
                    MonthlyIntegrationSourceScheduleItem(
                        scheduleId = row.id,
                        workingDate = row.workingDate!!,
                        accountCode = row.account?.externalKey,
                        accountName = row.account?.name,
                        workingCategory1 = row.workingCategory1?.displayName,
                        workingCategory3 = row.workingCategory3?.displayName,
                        // MFEIS 근무유형4 = TMS.SecondWorkType (레거시 240304 정합)
                        workingCategory4 = row.secondWorkType,
                        workingCategory5 = row.workingCategory5?.displayName,
                        attendanceReportedAt = row.commuteDate,
                        dailyScheduleCount = n,
                        equivalentContribution = BigDecimal.ONE.divide(BigDecimal(n), 4, RoundingMode.HALF_UP),
                    )
                }
        } else {
            emptyList()
        }

        return MonthlyIntegrationDetailResponse(
            id = mfeis.id,
            year = year ?: 0,
            month = month ?: 0,
            branchName = mfeis.empBranchName?.takeIf { it.isNotBlank() } ?: employee?.orgName,
            employeeCode = employee?.employeeCode,
            employeeName = employee?.name,
            accountCode = account?.externalKey,
            accountName = account?.name,
            workingCategory1 = mfeis.workingCategory1,
            workingCategory3 = mfeis.workingCategory3,
            workingCategory4 = mfeis.workingCategory4,
            workingCategory5 = mfeis.workingCategory5,
            workingDaysMonth = mfeis.workingDaysMonth?.toInt() ?: 0,
            totalInputCount = mfeis.numberOfInputs?.toInt() ?: 0,
            equivalentWorkingDays = mfeis.equivalentNumberOfWorkingDays ?: BigDecimal.ZERO,
            convertedHeadcount = mfeis.convertedHeadcount ?: BigDecimal.ZERO,
            schedules = schedules,
        )
    }

    /**
     * MFEIS(월별여사원 통합일정) 실시간 재집계 — 사원×월 전체 조합.
     *
     * SF 레거시 `TeamMemberScheduleTriggerHandler.updateMonthlyFemaleEmployeeIntegrationSchedule`
     * (insert/update 경로) 동등:
     * - 모수: 사원+월의 출근등록(attendanceLog 연결)된 TMS row 전건 (`CommuteLogId != null AND AccountId != null`).
     *   출근되지 않은 스케줄은 집계에 포함하지 않는다 (스케줄 기반 아님 — 출근 실적 기반).
     * - 집계 키: 레거시 ExternalKey (년+월+거래처코드+costCenter+사번+근무유형1+근무유형3
     *   +근무유형4(SecondWorkType)+근무유형5+전문판촉팀). null 컴포넌트는 Apex 문자열 연결 동등 "null" 리터럴,
     *   월은 zero-pad 없음 — SF 마이그레이션 row 의 external_key 포맷과 일치시켜 upsert 연속성 유지.
     * - 환산근무일수: row 별 1/N (N = 그날 사원의 출근 row 수, 거래처 무관) 전정밀도 누적 후 최종 4자리 HALF_UP.
     * - 당월근무일수: 사원+costCenter 별 distinct 근무일 수 (거래처 무관).
     * - 총투입횟수: 거래처+근무유형 조합(costCenter 무관) 별 distinct 근무일 수.
     * - 환산인원: 미반올림 환산근무일수 / 당월근무일수 (4자리 HALF_UP).
     * - 기존 row 는 ExternalKey 매칭 갱신 (EmpBranchName 비공백 값 유지 — 레거시 250409 지점이동 이력관리),
     *   재집계 후 키 조합이 사라진 row 와 모수 0건 시 잔여 row 는 삭제 (레거시 deleteRecordsSet + NumberOfInputs=0 동등).
     * - spec #680 §5.3 self-trigger 3필드 (accountConvertedHeadcount / thisMonthAmount /
     *   employeeInputCriteriaMaster) 는 키 조합 row 단위로 기존 로직 유지.
     *
     * 사원×월 전체를 한 번에 재계산하므로, 같은 날 다른 거래처 조합의 1/N 기여 변화도 함께 반영된다
     * (레거시 trigger 가 사원+월 전 조합을 재집계하는 것과 동일).
     */
    @Transactional
    fun refreshIntegration(employeeId: Long, yearMonth: YearMonth) {
        val from = yearMonth.atDay(1)
        val to = yearMonth.atEndOfMonth()
        val yearStr = yearMonth.year.toString()
        // 레거시 Month__c = String.valueOf(month()) — zero-pad 없음 ("6"). 마이그레이션 row 포맷 정합.
        val monthStr = yearMonth.monthValue.toString()

        val population = teamMemberScheduleRepository.findAttendedSchedulesByEmployeeAndMonth(employeeId, from, to)
        val existingRows = monthlyIntegrationScheduleRepository.findByEmployeeIdAndYearAndMonth(
            employeeId, yearStr, monthStr
        )

        if (population.isEmpty()) {
            detachIntegrationFk(existingRows)
            monthlyIntegrationScheduleRepository.deleteAll(existingRows)
            return
        }

        // 그날 투입건수 N — 레거시 countMap (사원+근무일별 모수 row 수, 거래처 무관)
        val rowCountByDate: Map<LocalDate, Int> = population.groupingBy { it.workingDate!! }.eachCount()

        // 당월근무일수 — 레거시 getWorkingDaysMonth (사원+costCenter 별 distinct 근무일, 거래처 무관)
        val workingDaysByCostCenter: Map<String?, Int> = population
            .groupBy { it.costCenterCode }
            .mapValues { (_, rows) -> rows.mapNotNull { it.workingDate }.distinct().size }

        // 총투입횟수 — 레거시 getNumberOfTradesEntered (거래처+근무유형 조합 별 distinct 근무일, costCenter 무관)
        val inputDaysByCombo: Map<List<Any?>, Int> = population
            .groupBy { inputsComboKey(it) }
            .mapValues { (_, rows) -> rows.mapNotNull { it.workingDate }.distinct().size }

        val groups = population.groupBy { buildLegacyExternalKey(it, yearStr, monthStr) }
        val existingByKey = existingRows
            .filter { it.externalKey != null }
            .associateBy { it.externalKey!! }

        for ((externalKey, rows) in groups) {
            val rep = rows.first()
            val employee = rep.employee
            val account = rep.account

            // 환산근무일수 / 환산인원 — SF 레거시(`TeamMemberScheduleTriggerHandler.cls:981,769`) 완전 동형.
            // SF 는 `1.0/N` 과 `Σ(1/N)/D` 를 IEEE-754 Double 로 중간 반올림 없이 계산하고,
            // Number(18,4) 필드 저장 시점에만 4자리 round-half-up 을 1회 적용한다.
            // BigDecimal 로 row 별 선반올림하면 `N × (1/N) = 1` 의 Double 대칭 소거가 깨져
            // 사원 1인 환산인원 합계가 0.9999 / 1.0001 로 어긋난다 → Double 산술로 재현.
            var equivalentRawDouble = 0.0
            for (row in rows) {
                val n = rowCountByDate[row.workingDate!!] ?: 1
                equivalentRawDouble += 1.0 / n.toDouble()
            }
            // 환산근무일수 = Σ(1/N) — 저장 직전 4자리 반올림 1회 (SF Number(18,4) 저장 동등)
            val equivalentWorkingDays = BigDecimal(equivalentRawDouble).setScale(4, RoundingMode.HALF_UP)

            val workingDaysMonth = workingDaysByCostCenter[rep.costCenterCode] ?: 0
            // 환산인원 = Σ(1/N) / D — 미반올림 Double 나눗셈 후 저장 직전 4자리 반올림 1회
            val convertedHeadcount = if (workingDaysMonth > 0) {
                BigDecimal(equivalentRawDouble / workingDaysMonth.toDouble())
                    .setScale(4, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            val numberOfInputs = BigDecimal.valueOf((inputDaysByCombo[inputsComboKey(rep)] ?: 0).toLong())

            val workingCategory1 = rep.workingCategory1?.displayName
            val workingCategory3 = rep.workingCategory3?.displayName
            // 레거시 record 필드 동등 — WorkingCategory5__c 는 TMS 자체 컬럼 (DisplayWorkSchedule 유추 아님)
            val workingCategory5 = rep.workingCategory5?.displayName

            // spec #680 §5.3 — self-trigger 3필드 자동 set (legacy
            // `MonthlyEmpIntegrationSchTriggerHandler.setAccountConvertedHeadcount` 동등).
            // 가드: workingCategory5='상시' AND workingCategory1/3 not null. 그 외 row 는 3필드 모두 null.
            val applyThreeFields = workingCategory5 == "상시" &&
                workingCategory1 != null && workingCategory3 != null
            val existing = existingByKey[externalKey]

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
            val employeeInputCriteriaMaster: EmployeeInputCriteriaMaster? =
                if (applyThreeFields && account != null) {
                    val accountTypeName = account.accountType
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

            val persisted: MonthlyFemaleEmployeeIntegrationSchedule = if (existing != null) {
                // 레거시 ExternalKey upsert 동등 — 집계 필드만 갱신, sfid/name/EDI_POS 등 미집계 필드 보존
                existing.workingDaysMonth = BigDecimal(workingDaysMonth)
                existing.numberOfInputs = numberOfInputs
                existing.equivalentNumberOfWorkingDays = equivalentWorkingDays
                existing.convertedHeadcount = convertedHeadcount
                existing.accountConvertedHeadcount = accountConvertedHeadcount
                existing.thisMonthAmount = thisMonthAmount
                existing.employeeInputCriteriaMaster = employeeInputCriteriaMaster
                if (existing.empBranchName.isNullOrBlank()) {
                    // 레거시 250409 — 기존 비공백 EmpBranchName 은 유지 (지점 이동 이력관리), 공백/미설정만 채움
                    existing.empBranchName = employee?.orgName
                }
                monthlyIntegrationScheduleRepository.save(existing)
            } else {
                val record = MonthlyFemaleEmployeeIntegrationSchedule(
                    externalKey = externalKey,
                    year = yearStr,
                    month = monthStr,
                    employee = employee,
                    account = account,
                    costCenterCode = rep.costCenterCode,
                    workingCategory1 = workingCategory1,
                    workingCategory3 = workingCategory3,
                    // 레거시 WorkingCategory4__c = TMS.SecondWorkType__c (240304)
                    workingCategory4 = rep.secondWorkType,
                    workingCategory5 = workingCategory5,
                    // 레거시 `TeamMemberScheduleTriggerHandler.cls:874` 동등 — TMS 의 전문행사조 값을
                    // 무변환 복사 (SF 3계층 모두 Text). 미배정 사원은 AttendanceService 가 TMS 에
                    // '일반' 을 stamp 하므로 그대로 '일반' 이 실린다 (마이그레이션 row 와 동일 값).
                    professionalPromotionTeam = rep.professionalPromotionTeam,
                    empBranchName = employee?.orgName,
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

            // 집계 근거 FK 세팅 — 상세(집계 근거 일정) 조회를 externalKey 재매칭 대신 FK 역참조로 전환.
            // 이 externalKey 그룹에 속한 근거 TMS row 전건을 본 MFEIS row 로 연결한다.
            // rows 는 같은 트랜잭션에서 조회한 영속 entity 라 dirty checking 으로 commit 시 자동 UPDATE
            // (명시적 save 불필요). persisted 는 save/upsert 를 거쳐 PK 확정된 managed entity.
            rows.forEach { row ->
                row.monthlyFemaleEmployeeIntegrationSchedule = persisted
            }
        }

        // stale 삭제 — 재집계 후 키 조합이 사라진 기존 row (레거시 deleteRecordsSet + NumberOfInputs=0 동등).
        // 삭제 전, 해당 row 를 근거로 가리키던 TMS FK 를 끊는다 (dangling FK 방지).
        // 벌크 detach 실행 시 auto-flush 로 위 FK 재세팅(dirty)이 먼저 DB 반영되므로, 이번 재집계에서
        // 다른 MFEIS 로 옮겨간 row 는 stale id 매칭에서 빠져 잘못 detach 되지 않는다.
        val staleRows = existingRows.filter { it.externalKey == null || it.externalKey !in groups.keys }
        detachIntegrationFk(staleRows)
        monthlyIntegrationScheduleRepository.deleteAll(staleRows)
    }

    /**
     * 삭제 예정 MFEIS row 를 근거로 가리키던 TMS 의 FK 를 끊는다 (dangling FK 방지).
     * QueryDSL 벌크 update 1쿼리로 일괄 null 처리 후 MFEIS 를 삭제해야 FK 제약 위반이 없다.
     */
    private fun detachIntegrationFk(mfeisRows: List<MonthlyFemaleEmployeeIntegrationSchedule>) {
        if (mfeisRows.isEmpty()) return
        teamMemberScheduleRepository.detachIntegrationScheduleByIds(mfeisRows.map { it.id })
    }

    /**
     * 레거시 `TeamMemberScheduleTriggerHandler.getExternalKey` 동등 — MFEIS 집계 키.
     * Apex 문자열 연결의 null 은 "null" 리터럴로 남는다 (마이그레이션 row 의 external_key 포맷 정합).
     * 컴포넌트 순서: 년 + 월(무패딩) + 거래처코드 + costCenter + 사번 + 근무유형1 + 근무유형3
     * + 근무유형4(SecondWorkType) + 근무유형5 + 전문판촉팀.
     */
    private fun buildLegacyExternalKey(schedule: TeamMemberSchedule, yearStr: String, monthStr: String): String {
        return yearStr + monthStr +
            (schedule.account?.externalKey ?: "null") +
            (schedule.costCenterCode ?: "null") +
            (schedule.employee?.employeeCode ?: "null") +
            (schedule.workingCategory1?.displayName ?: "null") +
            (schedule.workingCategory3?.displayName ?: "null") +
            (schedule.secondWorkType ?: "null") +
            (schedule.workingCategory5?.displayName ?: "null") +
            (schedule.professionalPromotionTeam ?: "null")
    }

    /**
     * 레거시 `getNumberOfTradesEntered` 의 countSet 키 — 거래처+근무유형 조합 (costCenter 미포함).
     * 같은 조합의 같은 날 중복 row 는 distinct 날짜 집계로 1회만 계산된다.
     */
    private fun inputsComboKey(schedule: TeamMemberSchedule): List<Any?> = listOf(
        schedule.account?.id,
        schedule.workingCategory1,
        schedule.workingCategory3,
        schedule.secondWorkType,
        schedule.workingCategory5,
        schedule.professionalPromotionTeam,
    )

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

    private fun calculateAvgClosingAmounts(
        year: Int,
        month: Int,
        accounts: List<Account>
    ): Map<Long, Long> {
        if (accounts.isEmpty()) return emptyMap()
        val externalKeyToId: Map<String, Long> = accounts
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
        return histories.groupBy { externalKeyToId[it.sapAccountCode] ?: 0L }
            .filter { it.key != 0L }
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

}

class InvalidParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class MonthlyIntegrationNotFoundException : BusinessException(
    errorCode = "MONTHLY_INTEGRATION_NOT_FOUND",
    message = "월별 여사원 통합일정을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

// SF `ScheduleSearchByTeamMember` row → web admin 화면 DTO 변환.
// `numberOfInputs` 는 MFEIS 가 BigDecimal 로 보유, 화면은 Int 로 표시 (행 수 의미).
private fun TeamMemberScheduleResultItem.toMonthlyIntegrationItem(): MonthlyIntegrationScheduleItem =
    MonthlyIntegrationScheduleItem(
        id = mfeisId,
        branchName = orgName ?: "",
        accountBranchName = accountBranchName,
        accountCode = accountCode ?: "",
        accountName = accountName ?: "",
        distributionChannelLabel = distributionChannelLabel,
        abcTypeLabel = abcTypeLabel,
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
// SF `setNull()` 정합 — 양월 0 지점 행은 수치 전부 null 로 유지 (행 자체는 보존).
// 양 월 모두 0 인 row 는 SF `setNull()` 로 모든 수치가 null → 화면에서 제외 (returns null).
private fun TeamMemberCategoryResultItem.toCategoryItem(): CategoryScheduleItem =
    CategoryScheduleItem(
        branchName = branchName,
        currentMonthTotal = currentMonthTotal,
        previousMonthTotal = lastMonthTotal,
        totalChange = totalIncrease,
        displayFixed = fix,
        displayAlternate = store,
        displayPatrol = rotate,
        currentMonthDisplayTotal = currentExhibitionTotal,
        previousMonthDisplayTotal = lastExhibitionTotal,
        displayChange = exhibitionIncrease,
        eventAmbient = roomTemperature,
        eventFrozenChilled = refrigerationAndFreezing,
        currentMonthEventTotal = currentEventTotal,
        previousMonthEventTotal = lastEventTotal,
        eventChange = eventIncrease,
    )
