package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.schedule.dto.response.*
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.otoki.powersales.schedule.repository.EmployeeInputCriteriaMasterRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import com.otoki.powersales.common.util.TimeZones
import java.time.format.DateTimeFormatter

/**
 * 거래처별 진열사원 배치적합성 산출 + 조회 + 엑셀 export.
 *
 * 레거시 매핑: `SalesComparisonSearchController.getLowDataList` / `getSummaryItems` (force-app/main/default/classes).
 * 동작: 월별여사원 통합일정(`TeamMemberScheduleSearchService.search` — MFEIS 직접 조회) 결과를 기반으로 거래처별 진열 상시 환산인원 합 + 6개월 평균매출 + 투입기준마스터의 고정/격고 기준금액을 비교하여 배치적합성(적합/경계/재검토) 판정 후 집계/중간집계/상세 3종 응답을 빌드한다.
 * 부수 효과: 없음 (조회 전용).
 * 신규 도입 — 레거시 미존재 web 진입점(`DeploymentPage`) 신규 구현 동반.
 */
@Service
@Transactional(readOnly = true)
class AdminSalesComparisonService(
    private val teamMemberScheduleSearchService: TeamMemberScheduleSearchService,
    private val accountRepository: AccountRepository,
    private val employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository,
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository
) {

    /**
     * 거래처유형 picklist 조회 — `AccountCategoryMaster.useSearch=true` 인 항목만 `accountCode` 오름차순 반환.
     *
     * 레거시 매핑: `SalesComparisonSearchController.getCategoryList` (force-app/main/default/classes) — SOQL `WHERE useSearch__c = true ORDER BY AccountCode__c`.
     * 부수 효과: 없음 (조회 전용).
     */
    fun getSearchCategories(): List<SearchAccountCategoryItem> {
        return accountCategoryMasterRepository
            .findByUseSearchTrueAndIsDeletedNotOrderByAccountCode(true)
            .map { SearchAccountCategoryItem(accountCode = it.accountCode, name = it.name) }
    }

    /** 체인 거래처 (4종) — 일반 카테고리 대신 ABCType 으로 투입기준 조회. */
    private val chainAccountTypeNames = setOf("서원부산", "수협", "그랜드마트", "우리마트")

    /**
     * 집계 모드 조회 — 배치적합성 × 거래처 카테고리 거래처 수 집계표 산출.
     *
     * 근무형태1=진열 + 근무형태5=상시 조건의 사원 일정만 산출 대상. 거래처 코드 단위 중복 제거.
     * 권한: `scope.isAllBranches` 면 사용자 입력 `costCenterCodes` 그대로 사용, 아니면 `scope.branchCodes` 와 교집합으로 필터.
     */
    fun getSummary(scope: DataScope, year: Int, month: Int, costCenterCodes: List<String>): SalesComparisonSummaryResponse {
        validateParams(year, month, costCenterCodes)
        val effectiveCodes = applyScope(scope, costCenterCodes)

        val accountSuitabilities = computeAccountSuitabilities(year, month, effectiveCodes)
        val rows = buildSummaryRows(accountSuitabilities)
        val total = buildTotalRow(accountSuitabilities)

        return SalesComparisonSummaryResponse(year, month, rows, total)
    }

    /**
     * 중간집계 모드 조회 — 거래처별 행 + 적합성별 소계 + 전체 총계.
     *
     * accountIds 가 비어있으면 전체 범위 (집계 모드의 전체 셀에 해당). 비어있지 않으면 해당 거래처만 필터.
     */
    fun getMiddle(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        accountIds: List<Int>
    ): SalesComparisonMiddleResponse {
        validateParams(year, month, costCenterCodes)
        val effectiveCodes = applyScope(scope, costCenterCodes)

        val accountSuitabilities = computeAccountSuitabilities(year, month, effectiveCodes)
        val filtered = if (accountIds.isEmpty()) {
            accountSuitabilities
        } else {
            val accountIdSet = accountIds.toSet()
            accountSuitabilities.filter { it.account.id in accountIdSet }
        }

        val items = filtered.map { it.toMiddleItem() }
            .sortedWith(compareBy({ suitabilityOrder(it.suitability) }, { it.accountCategory }, { it.accountName }))
        val subtotals = buildMiddleSubtotals(filtered)
        val total = buildMiddleTotal(filtered)

        return SalesComparisonMiddleResponse(year, month, items, subtotals, total)
    }

    /**
     * 상세 모드 조회 — 사원별 행 + 총계.
     *
     * accountIds 가 비어있으면 전체 (상세 모드 단일 그리드). accountIds 지정 시 해당 거래처의 사원만 (집계 → 중간집계 → 상세 드릴다운 최하위).
     * workingCategory1Filter / workingCategory5Filter 가 지정되면 사원 단위 추가 필터.
     */
    fun getDetail(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        accountIds: List<Int>,
        workingCategory1Filter: String?,
        workingCategory5Filter: String?
    ): SalesComparisonDetailResponse {
        validateParams(year, month, costCenterCodes)
        val effectiveCodes = applyScope(scope, costCenterCodes)

        val accountSuitabilities = computeAccountSuitabilities(year, month, effectiveCodes)
        val accountIdSet = accountIds.toSet()

        val items = accountSuitabilities
            .filter { accountIdSet.isEmpty() || it.account.id in accountIdSet }
            .flatMap { suit -> suit.allEmployeeItems.map { suit to it } }
            .filter { (_, item) ->
                (workingCategory1Filter.isNullOrBlank() || item.workingCategory1 == workingCategory1Filter) &&
                    (workingCategory5Filter.isNullOrBlank() || item.workingCategory5 == workingCategory5Filter)
            }
            .map { (suit, item) -> suit.toDetailItem(item) }
            .sortedWith(
                compareBy(
                    { it.accountCategoryCode },
                    { it.accountName },
                    { suitabilityOrder(it.suitability) },
                    { it.employeeName }
                )
            )

        val total = buildDetailTotal(items)
        return SalesComparisonDetailResponse(year, month, items, total)
    }

    /**
     * 집계표 엑셀 export — 헤더 + 적합성 × 카테고리 셀 + 총계.
     */
    fun exportSummary(scope: DataScope, year: Int, month: Int, costCenterCodes: List<String>): ExcelResult {
        val response = getSummary(scope, year, month, costCenterCodes)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("배치적합성_집계")

        val headerStyle = createHeaderStyle(workbook)
        val intStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
        }
        val totalStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
            setFillForegroundColor(XSSFColor(byteArrayOf(0x8E.toByte(), 0x44, 0xAD.toByte()), null))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setFont(workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            })
        }

        val categoryColumns = AccountCategoryColumn.entries.map { it.displayName }
        val headers = listOf("구분", "전체") + categoryColumns

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        (response.rows + response.total).forEachIndexed { rowIdx, row ->
            val excelRow = sheet.createRow(rowIdx + 1)
            val isTotal = row.suitability == "총계"
            val labelStyle = if (isTotal) totalStyle else workbook.createCellStyle().apply {
                setFillForegroundColor(suitabilityColor(row.suitability))
                fillPattern = FillPatternType.SOLID_FOREGROUND
            }
            excelRow.createCell(0).apply {
                setCellValue(row.suitability)
                cellStyle = labelStyle
            }
            excelRow.createCell(1).apply {
                setCellValue(row.totalCount.toDouble())
                cellStyle = if (isTotal) totalStyle else intStyle
            }
            categoryColumns.forEachIndexed { idx, label ->
                excelRow.createCell(idx + 2).apply {
                    setCellValue((row.countsByCategory[label] ?: 0).toDouble())
                    cellStyle = if (isTotal) totalStyle else intStyle
                }
            }
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_월평균_매출대비_여사원배치_현황_집계_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    /**
     * 중간집계 엑셀 export — 거래처 행 + 적합성별 소계 + 총계.
     */
    fun exportMiddle(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        accountIds: List<Int>
    ): ExcelResult {
        val response = getMiddle(scope, year, month, costCenterCodes, accountIds)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("배치적합성_중간집계")

        val headerStyle = createHeaderStyle(workbook)
        val intStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
        }
        val decimal3Style = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.000")
        }

        val headers = listOf(
            "거래처지점명", "배치적합성", "월평균매출", "총 진열인원",
            "총 진열환산인원", "총 행사환산인원", "거래처유형", "거래처명", "거래처코드",
            "고정배치기준", "격고배치기준", "총 투입횟수", "총 환산일수", "당월매출", "EDI/POS"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        var rowIdx = 1
        response.items.forEach { item ->
            val excelRow = sheet.createRow(rowIdx++)
            excelRow.createCell(0).setCellValue(item.accountBranchName ?: "")
            excelRow.createCell(1).apply {
                setCellValue(item.suitability)
                cellStyle = workbook.createCellStyle().apply {
                    setFillForegroundColor(suitabilityColor(item.suitability))
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }
            }
            excelRow.createCell(2).apply { setCellValue(item.avgClosingAmount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(3).apply { setCellValue(item.totalDisplayHeadcount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(4).apply { setCellValue(item.totalDisplayConvertedHeadcount.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(5).apply { setCellValue(item.totalEventConvertedHeadcount.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(6).setCellValue(item.accountCategory)
            excelRow.createCell(7).setCellValue(item.accountName)
            excelRow.createCell(8).setCellValue(item.accountCode)
            excelRow.createCell(9).apply { setCellValue((item.fixedStandardAmount ?: BigDecimal.ZERO).toDouble()); cellStyle = intStyle }
            excelRow.createCell(10).apply { setCellValue((item.bifurcationHalfStandardAmount ?: BigDecimal.ZERO).toDouble()); cellStyle = intStyle }
            excelRow.createCell(11).apply { setCellValue(item.totalInputCount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(12).apply { setCellValue(item.totalEquivalentWorkingDays.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(13).apply { setCellValue(item.thisMonthSalesAmount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(14).setCellValue(item.ediPos ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_월평균_매출대비_여사원배치_현황_중간집계_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    /**
     * 상세 엑셀 export — 사원별 행 + 총계.
     */
    fun exportDetail(
        scope: DataScope,
        year: Int,
        month: Int,
        costCenterCodes: List<String>,
        accountIds: List<Int>,
        workingCategory1Filter: String?,
        workingCategory5Filter: String?
    ): ExcelResult {
        val response = getDetail(scope, year, month, costCenterCodes, accountIds, workingCategory1Filter, workingCategory5Filter)
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("배치적합성_상세")

        val headerStyle = createHeaderStyle(workbook)
        val intStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0")
        }
        val decimal3Style = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.000")
        }

        val headers = listOf(
            "거래처지점명", "배치적합성", "월평균매출", "총 진열인원",
            "총 진열환산인원", "총 행사환산인원", "거래처유형", "거래처유형코드",
            "거래처명", "거래처코드", "사원명", "사번", "직위",
            "근무형태1", "근무형태3", "근무형태4", "근무형태5",
            "고정배치기준", "격고배치기준", "투입횟수", "환산일수", "환산인원",
            "당월매출", "EDI/POS"
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
            val excelRow = sheet.createRow(idx + 1)
            excelRow.createCell(0).setCellValue(item.accountBranchName ?: "")
            excelRow.createCell(1).apply {
                setCellValue(item.suitability)
                cellStyle = workbook.createCellStyle().apply {
                    setFillForegroundColor(suitabilityColor(item.suitability))
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                }
            }
            excelRow.createCell(2).apply { setCellValue(item.avgClosingAmount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(3).apply { setCellValue(item.totalDisplayHeadcount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(4).apply { setCellValue(item.totalDisplayConvertedHeadcount.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(5).apply { setCellValue(item.totalEventConvertedHeadcount.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(6).setCellValue(item.accountCategory)
            excelRow.createCell(7).setCellValue(item.accountCategoryCode)
            excelRow.createCell(8).setCellValue(item.accountName)
            excelRow.createCell(9).setCellValue(item.accountCode)
            excelRow.createCell(10).setCellValue(item.employeeName)
            excelRow.createCell(11).setCellValue(item.employeeCode)
            excelRow.createCell(12).setCellValue(item.title ?: "")
            excelRow.createCell(13).setCellValue(item.workingCategory1)
            excelRow.createCell(14).setCellValue(item.workingCategory3 ?: "")
            excelRow.createCell(15).setCellValue(item.workingCategory4 ?: "")
            excelRow.createCell(16).setCellValue(item.workingCategory5 ?: "")
            excelRow.createCell(17).apply { setCellValue((item.fixedStandardAmount ?: BigDecimal.ZERO).toDouble()); cellStyle = intStyle }
            excelRow.createCell(18).apply { setCellValue((item.bifurcationHalfStandardAmount ?: BigDecimal.ZERO).toDouble()); cellStyle = intStyle }
            excelRow.createCell(19).apply { setCellValue(item.inputCount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(20).apply { setCellValue(item.equivalentWorkingDays.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(21).apply { setCellValue(item.convertedHeadcount.toDouble()); cellStyle = decimal3Style }
            excelRow.createCell(22).apply { setCellValue(item.thisMonthSalesAmount.toDouble()); cellStyle = intStyle }
            excelRow.createCell(23).setCellValue(item.ediPos ?: "")
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년${month}월_월평균_매출대비_여사원배치_현황_상세_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    // --- 내부 산출 로직 ---

    /**
     * MFEIS 검색 결과 항목(`TeamMemberScheduleResultItem`) → 배치적합성 산출 입력(`MonthlyIntegrationScheduleItem`) 매핑.
     *
     * `actualAmount`(6개월 평균 ABC 마감실적) → `avgClosingAmount`, `numberOfInputs` → `totalInputCount` 로 환원.
     * `workingCategory5` 는 MFEIS 저장값(상시/임시) 을 그대로 전달 (SF 정합 — 조회 시점 진열마스터 재조인 없음).
     */
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
            avgClosingAmount = actualAmount.toLong()
        )

    /**
     * 거래처별 배치적합성 산출 결과 (집계/중간집계/상세 공통 입력).
     *
     * `TeamMemberScheduleSearchService.search` 로 MFEIS(월별 여사원 통합일정) 검색 결과를 얻은 뒤 거래처별로 진열 상시 환산인원 합계를 산출하고 6개월 평균매출 / 투입기준마스터의 기준금액과 비교하여 적합/경계/재검토를 판정한다.
     *
     * SF 정합: 통합일정 모집단 / `WorkingCategory5`(상시·임시) 저장값 / 6개월 평균 ABC 마감실적은 모두 MFEIS 직접 조회로 SF `SalesComparisonSearchController` 와 동등. 과거 `AdminMonthlyIntegrationService.buildIntegrationItems` 기반(TeamMemberSchedule 동적 집계 + `attendance_log IS NOT NULL` 가드 + `display_work_schedule` 날짜범위 재조인) 은 SF 비동등으로 0건이 되는 원인이라 사용하지 않는다.
     */
    internal fun computeAccountSuitabilities(
        year: Int,
        month: Int,
        costCenterCodes: List<String>
    ): List<AccountSuitability> {
        val integrationItems = teamMemberScheduleSearchService
            .search(year = year.toString(), month = month.toString(), orgValues = costCenterCodes)
            .result
            .map { it.toMonthlyIntegrationItem() }
        if (integrationItems.isEmpty()) return emptyList()

        // 거래처 단위 그룹핑
        val accountCodeMap = integrationItems.groupBy { it.accountCode }
        val accountCodes = accountCodeMap.keys.filter { it.isNotBlank() }
        val accounts = if (accountCodes.isEmpty()) emptyList() else accountRepository.findByExternalKeyIn(accountCodes)
        val accountByCode = accounts.associateBy { it.externalKey }

        // 6개월 평균매출 (accountCode → avg amount) — MFEIS 검색 결과의 actualAmount(6개월 평균 ABC 마감실적) 재사용
        val avgClosingAmounts = integrationItems.associate { it.accountCode to it.avgClosingAmount }

        // 투입기준마스터 (진열 + confirmed = true + isDeleted != true) — 화면 단위 1회 조회.
        // SF `SalesComparisonSearchController` (cls:334-342) 의 유효기간 필터 정합 —
        // `StartDate__c <= 선택월 말일 AND (EndDate__c IS NULL OR EndDate__c >= 선택월 1일)`.
        // 선택 년월에 유효한 마스터만 판정에 사용한다 (유효기간 무관 매칭은 다른 기간 기준금액을
        // 끌어와 SF 와 판정이 어긋나는 원인).
        val selectedMonth = YearMonth.of(year, month)
        val selectedFirstDay: LocalDate = selectedMonth.atDay(1)
        val selectedLastDay: LocalDate = selectedMonth.atEndOfMonth()
        val criteriaList = employeeInputCriteriaMasterRepository
            .findByTypeOfWork1AndConfirmedTrueAndIsDeletedNot(TypeOfWork1.DISPLAY, true)
            .filter { master ->
                val start = master.startDate ?: return@filter false
                start <= selectedLastDay && (master.endDate?.let { it >= selectedFirstDay } ?: true)
            }

        // 거래처유형 코드 맵 — SF `categoryMap` (cls:101) 동등. AccountCategoryMaster.name → accountCode (예: "대형마트(3대)" → "01").
        // SF categoryMap 은 useSearch 필터 없이 전체 거래처유형을 담는다 (검색 제외 유형도 거래처 분류엔 필요).
        // Account.Type / ABCType 의 raw 저장값(= 거래처유형마스터 Name) 으로 직접 조회 — SF `categoryMap.get(Account__r.Type)` 정합.
        val categoryCodeByName = accountCategoryMasterRepository.findAll()
            .filter { it.isDeleted != true }
            .associate { it.name to it.accountCode }

        // SF 모집단 필터 (cls:168) — `WHERE ... AND Account__r.Type IN: categoryMap.keySet()`.
        // 거래처유형(Account.Type) 이 거래처유형마스터(categoryMap) 에 등록된 거래처만 모집단에 포함.
        // 미등록/null 유형 거래처는 SF SOQL 단계에서 제외되므로 신규도 동일하게 제외해야 적합/총계 카운트가 SF 와 일치한다.
        val categoryNames = categoryCodeByName.keys

        return accountCodeMap.entries.mapNotNull { (accountCode, items) ->
            val account = accountByCode[accountCode] ?: return@mapNotNull null

            // SF 모집단 필터 — Account.Type 이 거래처유형마스터에 없으면 제외 (cls:168 SOQL 동등).
            if (account.accountType?.displayName !in categoryNames) return@mapNotNull null

            val displayItems = items.filter { it.workingCategory1 == "진열" && it.workingCategory5 == "상시" }
            val eventItems = items.filter { it.workingCategory1 == "행사" }

            val totalDisplayConverted = displayItems
                .fold(BigDecimal.ZERO) { acc, it -> acc.add(it.convertedHeadcount) }
                .setScale(3, RoundingMode.HALF_UP)
            val totalEventConverted = eventItems
                .fold(BigDecimal.ZERO) { acc, it -> acc.add(it.convertedHeadcount) }
                .setScale(3, RoundingMode.HALF_UP)
            val totalDisplayHeadcount = displayItems.map { it.employeeCode }.distinct().size
            // SF 중간집계 표시값은 진열·상시 행만의 합 — 화면 검증(이마트 원주점 투입18/환산일18 = 진열행과 일치).
            // 행사 행을 포함하면 투입횟수·환산일수가 SF 보다 부풀려진다 (행사24 혼입 → 42).
            val totalInputCount = displayItems.sumOf { it.totalInputCount }
            val totalEquivalentDays = displayItems
                .fold(BigDecimal.ZERO) { acc, it -> acc.add(it.equivalentWorkingDays) }
                .setScale(3, RoundingMode.HALF_UP)
            val avgClosingAmount = avgClosingAmounts[accountCode] ?: 0L
            val thisMonthSalesAmount = avgClosingAmount  // 별도 당월 매출 컬럼 매핑 부재 — 6개월 평균 재사용 (TODO: SF 명세 확인 후 정정 후보)

            // SF 거래처유형 코드 산출 (cls:366-372) — 화면 카테고리 컬럼용.
            // 체인 특수 4종(ABCType__c)은 ABCType 으로, 그 외는 Account.Type 으로 categoryMap 변환.
            // SF 는 `categoryMap.get(ABCType)` 결과를 fallback 없이 그대로 customerTypeCode 에 넣는다 (cls:367).
            // 운영 마스터에 체인4종 Name 이 없으므로 결과는 null → getCategory(null) → 기타(others) 로 분류된다.
            // (과거 `?: typeCode` fallback 은 체인4종 거래처를 Account.Type(체인02)으로 되돌려 체인 컬럼을 SF 보다 부풀렸다.)
            val typeCode = account.accountType?.displayName?.let { categoryCodeByName[it] }
            val categoryColumnCode = if (account.abcType in chainAccountTypeNames) {
                categoryCodeByName[account.abcType]
            } else {
                typeCode
            }

            // SF 판정 마스터 매칭 (cls:466) — 항상 Account.Type 기준 코드 (ABCType 분기는 화면 카테고리에만 적용).
            val criteria = criteriaList.firstOrNull { it.accountCategorizedCode == typeCode }
            val fixedStandard = criteria?.fixed1PersonStandardAmount
            val fixedMin = criteria?.fixed1PersonMinAmountInRealmRange
            val bifurcationStandard = criteria?.bifurcationHalfPersonStandard
            val bifurcationMin = criteria?.bifurcationHalfPersonMinAmountInRealmRange

            // 진열 환산인원으로 평균매출 분배. 거래처별 적합성은 거래처 단위 1건 — 근무형태3 분기는 사원 단위 (상세) 에서.
            val accountCategory = categoryColumnFromCode(categoryColumnCode).displayName
            val accountCategoryCode = categoryColumnCode

            AccountSuitability(
                account = account,
                accountCode = accountCode,
                accountName = items.first().accountName,
                accountBranchName = items.first().accountBranchName,
                accountCategory = accountCategory,
                accountCategoryCode = accountCategoryCode,
                totalDisplayConvertedHeadcount = totalDisplayConverted,
                totalEventConvertedHeadcount = totalEventConverted,
                totalDisplayHeadcount = totalDisplayHeadcount,
                totalInputCount = totalInputCount,
                totalEquivalentWorkingDays = totalEquivalentDays,
                avgClosingAmount = avgClosingAmount,
                thisMonthSalesAmount = thisMonthSalesAmount,
                fixedStandardAmount = fixedStandard,
                fixedMinAmount = fixedMin,
                bifurcationHalfStandardAmount = bifurcationStandard,
                bifurcationHalfMinAmount = bifurcationMin,
                allEmployeeItems = items,
                ediPos = account.abcType
            )
        }
    }

    /**
     * 사원별 근무형태3(고정/격고/순회) + 거래처 진열 환산인원으로 배치적합성 판정.
     *
     * - 순회: 무조건 적합
     * - 고정: (월평균매출 / 진열 상시 환산인원 합계) 를 고정 기준금액과 비교
     * - 격고: 동일 비율을 격고 기준금액과 비교
     * - 기준금액 이상 → 적합, 최소금액 이상 기준금액 미만 → 경계, 최소금액 미만 → 재검토
     * - 환산인원 합 0 또는 데이터 부재 → 공백
     */
    internal fun judgeSuitability(
        workingCategory3: String?,
        avgClosingAmount: Long,
        totalDisplayConverted: BigDecimal,
        fixedStandard: BigDecimal?,
        fixedMin: BigDecimal?,
        bifurcationStandard: BigDecimal?,
        bifurcationMin: BigDecimal?
    ): String {
        if (workingCategory3 == "순회") return Suitability.FIT.displayName
        if (totalDisplayConverted.compareTo(BigDecimal.ZERO) == 0) return ""

        // SF `getCheckVal` (cls:529) `amt = amt / convertedCnt` — 정수 반올림 없이 Decimal 비교.
        // (과거 0-scale HALF_UP 반올림은 경계 케이스에서 적합↔경계 결과를 SF 와 다르게 만들었다.)
        val ratio = BigDecimal(avgClosingAmount).divide(totalDisplayConverted, 10, RoundingMode.HALF_UP)

        // SF `getCheckVal` (cls:531-541) — master/근무형태 미매칭 시 standard=min=0 (return 아님).
        // 그 결과 `ratio >= 0` 이 거의 항상 참 → 적합. (과거 null 시 공백 반환은 SF 와 달라 적합 카운트를 누락시켰다.)
        val standard = when (workingCategory3) {
            "고정" -> fixedStandard
            "격고" -> bifurcationStandard
            else -> null
        } ?: BigDecimal.ZERO
        val min = when (workingCategory3) {
            "고정" -> fixedMin
            "격고" -> bifurcationMin
            else -> null
        } ?: BigDecimal.ZERO

        return when {
            ratio >= standard -> Suitability.FIT.displayName            // SF cls:544 amt >= standardAmount
            min <= ratio && ratio < standard -> Suitability.BOUNDARY.displayName  // SF cls:546
            else -> Suitability.REVIEW.displayName                      // SF cls:550
        }
    }

    /**
     * 거래처별 단일 적합성 — SF `getSummaryItems` (cls:572-583) 의 worst-case 규칙 동등.
     *
     * 진열·상시 사원 각각을 사원 단위 근무형태3 으로 판정한 뒤, 가장 나쁜(forSort 최대) 결과로 거래처를 대표한다
     * (적합 < 경계 < 재검토). SF 는 거래처 안에 경계 사원이 1명이라도 있으면 그 거래처를 경계로 분류한다.
     * 진열·상시 사원이 없으면 공백 — 집계/총계 모두에서 제외 (SF `appropriateValues.contains('')` = false).
     */
    private fun AccountSuitability.computeAccountLevelSuitability(): String {
        val displayPermanent = allEmployeeItems.filter { it.workingCategory1 == "진열" && it.workingCategory5 == "상시" }
        if (displayPermanent.isEmpty()) return ""
        return displayPermanent
            .map { emp ->
                judgeSuitability(
                    workingCategory3 = emp.workingCategory3,
                    avgClosingAmount = avgClosingAmount,
                    totalDisplayConverted = totalDisplayConvertedHeadcount,
                    fixedStandard = fixedStandardAmount,
                    fixedMin = fixedMinAmount,
                    bifurcationStandard = bifurcationHalfStandardAmount,
                    bifurcationMin = bifurcationHalfMinAmount
                )
            }
            .filter { it.isNotBlank() }
            .maxByOrNull { suitabilityOrder(it) }   // forSort 최대 = worst-case (적합0 < 경계1 < 재검토2)
            ?: ""
    }

    private fun AccountSuitability.toMiddleItem(): SalesComparisonMiddleItem = SalesComparisonMiddleItem(
        accountId = account.id,
        accountCode = accountCode,
        accountName = accountName,
        accountBranchName = accountBranchName,
        accountCategory = accountCategory,
        suitability = computeAccountLevelSuitability(),
        avgClosingAmount = avgClosingAmount,
        totalDisplayHeadcount = totalDisplayHeadcount,
        totalDisplayConvertedHeadcount = totalDisplayConvertedHeadcount,
        totalEventConvertedHeadcount = totalEventConvertedHeadcount,
        fixedStandardAmount = fixedStandardAmount,
        bifurcationHalfStandardAmount = bifurcationHalfStandardAmount,
        totalInputCount = totalInputCount,
        totalEquivalentWorkingDays = totalEquivalentWorkingDays,
        thisMonthSalesAmount = thisMonthSalesAmount,
        ediPos = ediPos
    )

    private fun AccountSuitability.toDetailItem(
        item: com.otoki.powersales.schedule.dto.response.MonthlyIntegrationScheduleItem
    ): SalesComparisonDetailItem {
        val isDisplayPermanent = item.workingCategory1 == "진열" && item.workingCategory5 == "상시"
        val suitability = if (isDisplayPermanent) {
            judgeSuitability(
                workingCategory3 = item.workingCategory3,
                avgClosingAmount = avgClosingAmount,
                totalDisplayConverted = totalDisplayConvertedHeadcount,
                fixedStandard = fixedStandardAmount,
                fixedMin = fixedMinAmount,
                bifurcationStandard = bifurcationHalfStandardAmount,
                bifurcationMin = bifurcationHalfMinAmount
            )
        } else ""

        return SalesComparisonDetailItem(
            accountId = account.id,
            accountCode = accountCode,
            accountName = accountName,
            accountBranchName = accountBranchName,
            accountCategory = accountCategory,
            accountCategoryCode = accountCategoryCode,
            employeeCode = item.employeeCode,
            employeeName = item.employeeName,
            title = item.title,
            workingCategory1 = item.workingCategory1,
            workingCategory3 = item.workingCategory3,
            workingCategory4 = item.workingCategory4,
            workingCategory5 = item.workingCategory5,
            suitability = suitability,
            avgClosingAmount = avgClosingAmount,
            totalDisplayHeadcount = totalDisplayHeadcount,
            totalDisplayConvertedHeadcount = totalDisplayConvertedHeadcount,
            totalEventConvertedHeadcount = totalEventConvertedHeadcount,
            fixedStandardAmount = fixedStandardAmount,
            bifurcationHalfStandardAmount = bifurcationHalfStandardAmount,
            inputCount = item.totalInputCount,
            equivalentWorkingDays = item.equivalentWorkingDays,
            convertedHeadcount = item.convertedHeadcount,
            thisMonthSalesAmount = thisMonthSalesAmount,
            ediPos = ediPos
        )
    }

    private fun buildSummaryRows(accountSuitabilities: List<AccountSuitability>): List<SalesComparisonSummaryRow> {
        val grouped = accountSuitabilities
            .map { it to it.computeAccountLevelSuitability() }
            .filter { it.second.isNotBlank() }
            .groupBy({ it.second }, { it.first })

        return Suitability.entries.map { suit ->
            val accounts = grouped[suit.displayName] ?: emptyList()
            buildSummaryRowFromAccounts(suit.displayName, accounts)
        }
    }

    /**
     * 총계 행 — SF `getSummaryItems` 의 '총계' 누적 동등 (cls:567 동일 필터).
     *
     * 적합성이 공백("")인 거래처(진열·상시 사원 부재)는 제외하므로 총계 = 적합 + 경계 + 재검토.
     * (과거 전체 거래처 카운트는 공백 거래처를 총계에만 포함시켜 총계 ≠ 적합+경계+재검토 불일치를 유발했다.)
     */
    private fun buildTotalRow(accountSuitabilities: List<AccountSuitability>): SalesComparisonSummaryRow {
        val classified = accountSuitabilities.filter { it.computeAccountLevelSuitability().isNotBlank() }
        return buildSummaryRowFromAccounts("총계", classified)
    }

    private fun buildSummaryRowFromAccounts(
        suitabilityLabel: String,
        accounts: List<AccountSuitability>
    ): SalesComparisonSummaryRow {
        val byCategory = AccountCategoryColumn.entries.associate { col ->
            col.displayName to accounts.filter { it.accountCategory == col.displayName }
        }
        val countsByCategory = byCategory.mapValues { (_, list) -> list.distinctBy { it.accountCode }.size }
        val accountIdsByCategory = byCategory.mapValues { (_, list) -> list.map { it.account.id }.distinct() }
        val total = accounts.distinctBy { it.accountCode }.size
        return SalesComparisonSummaryRow(
            suitability = suitabilityLabel,
            totalCount = total,
            countsByCategory = countsByCategory,
            accountIdsByCategory = accountIdsByCategory
        )
    }

    private fun buildMiddleSubtotals(items: List<AccountSuitability>): List<SalesComparisonMiddleSubtotal> {
        val grouped = items.groupBy { it.computeAccountLevelSuitability() }
            .filterKeys { it.isNotBlank() }
        return Suitability.entries.mapNotNull { suit ->
            grouped[suit.displayName]?.let { sublist ->
                aggregateMiddle(suit.displayName, sublist)
            }
        }
    }

    private fun buildMiddleTotal(items: List<AccountSuitability>): SalesComparisonMiddleSubtotal {
        return aggregateMiddle("총계", items)
    }

    private fun aggregateMiddle(label: String, list: List<AccountSuitability>): SalesComparisonMiddleSubtotal {
        return SalesComparisonMiddleSubtotal(
            suitability = label,
            accountCount = list.distinctBy { it.accountCode }.size,
            avgClosingAmount = list.sumOf { it.avgClosingAmount },
            totalDisplayHeadcount = list.sumOf { it.totalDisplayHeadcount },
            totalDisplayConvertedHeadcount = list.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalDisplayConvertedHeadcount) }.setScale(3, RoundingMode.HALF_UP),
            totalEventConvertedHeadcount = list.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalEventConvertedHeadcount) }.setScale(3, RoundingMode.HALF_UP),
            totalInputCount = list.sumOf { it.totalInputCount },
            totalEquivalentWorkingDays = list.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalEquivalentWorkingDays) }.setScale(3, RoundingMode.HALF_UP),
            thisMonthSalesAmount = list.sumOf { it.thisMonthSalesAmount }
        )
    }

    private fun buildDetailTotal(items: List<SalesComparisonDetailItem>): SalesComparisonDetailTotal {
        return SalesComparisonDetailTotal(
            rowCount = items.size,
            totalDisplayHeadcount = items.sumOf { it.totalDisplayHeadcount },
            totalDisplayConvertedHeadcount = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalDisplayConvertedHeadcount) }.setScale(3, RoundingMode.HALF_UP),
            totalEventConvertedHeadcount = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.totalEventConvertedHeadcount) }.setScale(3, RoundingMode.HALF_UP),
            totalInputCount = items.sumOf { it.inputCount },
            totalEquivalentWorkingDays = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.equivalentWorkingDays) }.setScale(3, RoundingMode.HALF_UP),
            totalConvertedHeadcount = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.convertedHeadcount) }.setScale(3, RoundingMode.HALF_UP),
            totalThisMonthSalesAmount = items.sumOf { it.thisMonthSalesAmount }
        )
    }

    /**
     * 거래처유형 코드 → 매트릭스 컬럼 매핑 — SF `getCategory` (cls:663-685) 동등.
     *
     * AccountCategoryMaster.accountCode 값 기준: 01 대형마트 / 02 체인 / 03 백화점 / 05 농협 / 06 슈퍼 /
     * 07 대리점 / 08 홀세일 / 10 식자재 / 15 군납 / 그 외(미매칭·04·09·11~14·16+ 포함) → 기타.
     */
    private fun categoryColumnFromCode(code: String?): AccountCategoryColumn = when (code) {
        "01" -> AccountCategoryColumn.HYPER
        "02" -> AccountCategoryColumn.CHAIN
        "03" -> AccountCategoryColumn.DEPT
        "05" -> AccountCategoryColumn.NH
        "06" -> AccountCategoryColumn.SUPER
        "07" -> AccountCategoryColumn.DEALER
        "08" -> AccountCategoryColumn.WHOLESALE
        "10" -> AccountCategoryColumn.FOOD
        "15" -> AccountCategoryColumn.MILITARY
        else -> AccountCategoryColumn.OTHER
    }

    private fun suitabilityOrder(label: String): Int = when (label) {
        Suitability.FIT.displayName -> 0
        Suitability.BOUNDARY.displayName -> 1
        Suitability.REVIEW.displayName -> 2
        else -> 9
    }

    private fun suitabilityColor(label: String): XSSFColor = when (label) {
        Suitability.FIT.displayName -> XSSFColor(byteArrayOf(0x66, 0xBB.toByte(), 0x6A), null)
        Suitability.BOUNDARY.displayName -> XSSFColor(byteArrayOf(0xFF.toByte(), 0xEB.toByte(), 0x3B), null)
        Suitability.REVIEW.displayName -> XSSFColor(byteArrayOf(0xEF.toByte(), 0x53, 0x50), null)
        else -> XSSFColor(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), null)
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        setFillForegroundColor(XSSFColor(byteArrayOf(0x1E, 0x2F, 0x97.toByte()), null))
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
        setFont(workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        })
    }

    private fun workbookToBytes(workbook: XSSFWorkbook): ByteArray {
        return ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }
    }

    /**
     * 사용자 입력 costCenterCodes 를 권한 범위와 교집합으로 필터링.
     *
     * - `scope.isAllBranches=true`: 사용자 입력 그대로 사용 (전체 지점 권한)
     * - 그 외: `scope.branchCodes` 와 교집합. 교집합이 비어있으면 [AdminForbiddenException] (권한 범위 밖 코드만 입력)
     *
     * 레거시 매핑: `CurrentUserBranchNameList.getOrgList` 의 isAll 분기 동등.
     */
    internal fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

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

    /** 거래처 단위 산출 결과 — 집계/중간집계/상세 응답 빌드용 중간 모델. */
    internal data class AccountSuitability(
        val account: Account,
        val accountCode: String,
        val accountName: String,
        val accountBranchName: String?,
        val accountCategory: String,
        val accountCategoryCode: String?,
        val totalDisplayConvertedHeadcount: BigDecimal,
        val totalEventConvertedHeadcount: BigDecimal,
        val totalDisplayHeadcount: Int,
        val totalInputCount: Int,
        val totalEquivalentWorkingDays: BigDecimal,
        val avgClosingAmount: Long,
        val thisMonthSalesAmount: Long,
        val fixedStandardAmount: BigDecimal?,
        val fixedMinAmount: BigDecimal?,
        val bifurcationHalfStandardAmount: BigDecimal?,
        val bifurcationHalfMinAmount: BigDecimal?,
        val allEmployeeItems: List<MonthlyIntegrationScheduleItem>,
        val ediPos: String?
    )

    data class ExcelResult(
        val bytes: ByteArray,
        val filename: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ExcelResult
            return bytes.contentEquals(other.bytes) && filename == other.filename
        }

        override fun hashCode(): Int = bytes.contentHashCode() * 31 + filename.hashCode()
    }
}
