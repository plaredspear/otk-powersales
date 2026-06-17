package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyInputAdequacyItem
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyInputAdequacyResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.otoki.powersales.platform.common.util.excel.ExcelStyleSupport
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 월별 진열사원 투입적합성 — 연도 1~12월 매트릭스 조회 + 엑셀 export.
 *
 * 레거시 매핑: `MonthlyInputAdequacyController.cls` + Aura cmp `MonthlyInputAdequacy` (force-app/main/default).
 * 동작: 입력 연도의 1~12월 각각에 대해 [AdminSalesComparisonService.computeAccountSuitabilities] 를 호출하여 (사원, 거래처) 단위 적합성을 산출하고, 12개월 결과를 합쳐 매트릭스 행으로 빌드한다. 6개월 평균매출 + 1월 특수 로직 + 순회 자동 적합 + 환산 인원 / 기준 금액 비교는 모두 인접 서비스의 산식을 재사용.
 * 부수 효과: 없음 (조회 전용).
 *
 * 신규 도입 — 레거시 미존재 web 페이지 (`MonthlyInputAdequacyPage`) 신규 구현 동반. 레거시 UC-05 의 재직상태 "전체" (휴직·퇴직 포함) 옵션은 신규 정책 (재직 사원만 분석) 으로 미지원.
 */
@Service
@Transactional(readOnly = true)
class AdminMonthlyInputAdequacyService(
    private val adminSalesComparisonService: AdminSalesComparisonService
) {

    /**
     * 1~12월 매트릭스 조회.
     *
     * year 의 각 월에 대해 [AdminSalesComparisonService.computeAccountSuitabilities] 호출 → (사원, 거래처) 키로 그룹핑하여 12개월 적합성 라벨 배열을 빌드한다.
     * workingCategory3Filter 가 지정되면 사원 단위 추가 필터. 권한: `scope.isAllBranches` 면 사용자 입력 그대로 사용, 아니면 `scope.branchCodes` 와 교집합.
     */
    fun getMatrix(
        scope: DataScope,
        year: Int,
        costCenterCodes: List<String>,
        workingCategory3Filter: String?
    ): MonthlyInputAdequacyResponse {
        validateParams(year, costCenterCodes)
        val effectiveCodes = applyScope(scope, costCenterCodes)

        val rows = buildMatrixRows(year, effectiveCodes, workingCategory3Filter)
        return MonthlyInputAdequacyResponse(year, rows)
    }

    /**
     * 매트릭스 엑셀 export — 헤더 + 12개월 적합성 셀 + 색상.
     */
    fun exportMatrix(
        scope: DataScope,
        year: Int,
        costCenterCodes: List<String>,
        workingCategory3Filter: String?
    ): ExcelResult {
        val response = getMatrix(scope, year, costCenterCodes, workingCategory3Filter)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("월별투입적정성")

        val headerStyle = ExcelStyleSupport.primaryHeaderStyle(workbook)
        val headers = listOf(
            "소속", "근무형태3", "이름", "사번", "직위",
            "거래처유형", "거래처유형코드", "거래처명", "거래처코드"
        ) + (1..12).map { "${it}월" }

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
            excelRow.createCell(0).setCellValue(item.branchName)
            excelRow.createCell(1).setCellValue(item.workingCategory3 ?: "")
            excelRow.createCell(2).setCellValue(item.employeeName)
            excelRow.createCell(3).setCellValue(item.employeeCode)
            excelRow.createCell(4).setCellValue(item.title ?: "")
            excelRow.createCell(5).setCellValue(item.accountCategory)
            excelRow.createCell(6).setCellValue(item.accountCategoryCode ?: "")
            excelRow.createCell(7).setCellValue(item.accountName)
            excelRow.createCell(8).setCellValue(item.accountCode)
            item.monthlySuitability.forEachIndexed { monthIdx, label ->
                excelRow.createCell(9 + monthIdx).apply {
                    setCellValue(label)
                    if (label.isNotBlank()) {
                        cellStyle = workbook.createCellStyle().apply {
                            setFillForegroundColor(suitabilityColor(label))
                            fillPattern = FillPatternType.SOLID_FOREGROUND
                        }
                    }
                }
            }
        }
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = ExcelStyleSupport.workbookToBytes(workbook)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "${year}년도_월별투입적정성_${timestamp}.xlsx"
        return ExcelResult(bytes, filename)
    }

    /** (사원, 거래처, 근무형태3) 키로 1~12월 적합성을 모은 매트릭스 행 빌드. */
    private fun buildMatrixRows(
        year: Int,
        effectiveCodes: List<String>,
        workingCategory3Filter: String?
    ): List<MonthlyInputAdequacyItem> {
        data class RowKey(
            val employeeCode: String,
            val accountCode: String,
            val workingCategory3: String?
        )

        data class RowAcc(
            var branchName: String = "",
            var employeeName: String = "",
            var title: String? = null,
            var accountCategory: String = "",
            var accountCategoryCode: String? = null,
            var accountName: String = "",
            val monthlySuitability: Array<String> = Array(12) { "" }
        )

        val rowMap = LinkedHashMap<RowKey, RowAcc>()

        (1..12).forEach { month ->
            val accountSuitabilities = adminSalesComparisonService.computeAccountSuitabilities(year, month, effectiveCodes)

            accountSuitabilities.forEach { suit ->
                // 진열 상시 사원만 매트릭스 대상 (레거시 동등 — 진열 상시 조건 사원만 적합성 판정 대상)
                val displayPermanentItems = suit.allEmployeeItems.filter {
                    it.workingCategory1 == "진열" && it.workingCategory5 == "상시"
                }

                displayPermanentItems.forEach { empItem ->
                    if (workingCategory3Filter != null && empItem.workingCategory3 != workingCategory3Filter) return@forEach

                    val key = RowKey(
                        employeeCode = empItem.employeeCode,
                        accountCode = suit.accountCode,
                        workingCategory3 = empItem.workingCategory3
                    )
                    val acc = rowMap.getOrPut(key) {
                        RowAcc(
                            branchName = empItem.branchName,
                            employeeName = empItem.employeeName,
                            title = empItem.title,
                            accountCategory = suit.accountCategory,
                            accountCategoryCode = suit.accountCategoryCode,
                            accountName = suit.accountName,
                        )
                    }

                    val suitabilityLabel = adminSalesComparisonService.judgeSuitability(
                        workingCategory3 = empItem.workingCategory3,
                        avgClosingAmount = suit.avgClosingAmount,
                        totalDisplayConverted = suit.totalDisplayConvertedHeadcount,
                        fixedStandard = suit.fixedStandardAmount,
                        fixedMin = suit.fixedMinAmount,
                        bifurcationStandard = suit.bifurcationHalfStandardAmount,
                        bifurcationMin = suit.bifurcationHalfMinAmount
                    )
                    acc.monthlySuitability[month - 1] = suitabilityLabel
                }
            }
        }

        return rowMap.entries
            .map { (key, acc) ->
                MonthlyInputAdequacyItem(
                    branchName = acc.branchName,
                    workingCategory3 = key.workingCategory3,
                    employeeName = acc.employeeName,
                    employeeCode = key.employeeCode,
                    title = acc.title,
                    accountCategory = acc.accountCategory,
                    accountCategoryCode = acc.accountCategoryCode,
                    accountName = acc.accountName,
                    accountCode = key.accountCode,
                    monthlySuitability = acc.monthlySuitability.toList()
                )
            }
            .sortedWith(
                compareBy(
                    { it.branchName },
                    { it.employeeCode },
                    { it.accountName },
                    { it.workingCategory3 ?: "" }
                )
            )
    }

    /** 권한 범위와 사용자 입력 costCenterCodes 의 교집합 산출 — 인접 [AdminSalesComparisonService.applyScope] 와 동등. */
    private fun applyScope(scope: DataScope, costCenterCodes: List<String>): List<String> {
        if (scope.isAllBranches) return costCenterCodes
        val allowed = scope.branchCodes.toSet()
        val intersect = costCenterCodes.filter { it in allowed }
        if (intersect.isEmpty()) throw AdminForbiddenException()
        return intersect
    }

    private fun validateParams(year: Int, costCenterCodes: List<String>) {
        if (year !in 2020..2099) {
            throw InvalidParameterException("year는 2020~2099 범위여야 합니다")
        }
        if (costCenterCodes.isEmpty()) {
            throw InvalidParameterException("cost_center_codes는 필수입니다")
        }
    }

    private fun suitabilityColor(label: String): XSSFColor = when (label) {
        "적합" -> XSSFColor(byteArrayOf(0x66, 0xBB.toByte(), 0x6A), null)
        "경계" -> XSSFColor(byteArrayOf(0xFF.toByte(), 0xEB.toByte(), 0x3B), null)
        "재검토" -> XSSFColor(byteArrayOf(0xEF.toByte(), 0x53, 0x50), null)
        else -> XSSFColor(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()), null)
    }

}
