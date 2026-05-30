package com.otoki.powersales.schedule.service

import com.otoki.powersales.schedule.dto.response.ConvertedHeadcountReportGroup
import com.otoki.powersales.schedule.dto.response.ConvertedHeadcountReportResult
import com.otoki.powersales.schedule.dto.response.ConvertedHeadcountReportRow
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * 거래처유형별 환산인원 현황 보고서 조회 + 엑셀 export (Spec #847).
 *
 * 레거시 매핑: SF Report 5변형 — `new_report_nNq`(1-1) / `X12_only_aM6`(1-2) / `X14_cwi`(1-4) /
 * `X15_Uyw`(1-5) / `X21_E94`((2팀)2-1). 기준 객체 MonthlyFemaleEmployeeIntegrationSchedule__c.
 * Matrix: 집계 = SUM(ConvertedHeadcount__c), 열그룹 = AccountType(구분)→WorkingCategory1, 행그룹 = 지점→연월.
 * variant 별 근무유형5 / 위탁제외 / 2팀(CostCenterCode) 필터 + 지점 기준(1-x: 여사원 소속 / 2-1: 거래처) 차이.
 * 부수 효과: 없음 (조회 전용). SF scope=organization = 전사.
 *
 * 신규 차이: 기존 [AdminMonthlyIntegrationService] 통합일정 조회와 별개 보고서 — 거래처유형/근무유형1 그룹핑 +
 *   환산인원 합산 + variant 분기 + 엑셀.
 */
@Service
@Transactional(readOnly = true)
class AdminConvertedHeadcountReportService(
    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
) {

    /**
     * 환산인원 현황 조회 — variant 별 필터 + 구분×근무유형1×지점×연월 그룹핑 + SUM(환산인원) + 소계/총계.
     */
    fun getReport(variant: ConvertedHeadcountReportVariant, year: String, month: String): ConvertedHeadcountReportResult {
        val rows = mfeisRepository.findConvertedHeadcountReport(
            year = year,
            month = month,
            workingCategory5In = variant.workingCategory5In,
            includeNullWc5 = variant.includeNullWc5,
            excludeConsignment = variant.excludeConsignment,
            costCenterCode = variant.costCenterCode,
            accountTypeFilter = variant.accountTypeFilter,
        )

        // 구분 × 근무유형1 (× 근무유형3) × 지점 × 연월 단위 SUM(환산인원) 집계.
        // 근무유형3 은 variant.includeWorkingCategory3 인 경우에만 집계 키에 포함 (그 외 null 로 평탄화).
        val aggregated = rows
            .groupBy {
                AggKey(
                    accountType = it.account?.accountType?.displayName,
                    workingCategory1 = it.workingCategory1,
                    workingCategory3 = if (variant.includeWorkingCategory3) it.workingCategory3 else null,
                    branchName = branchNameOf(it, variant),
                    yearMonth = yearMonthOf(it),
                )
            }
            .map { (key, group) ->
                ConvertedHeadcountReportRow(
                    accountType = key.accountType,
                    workingCategory1 = key.workingCategory1,
                    workingCategory3 = key.workingCategory3,
                    branchName = key.branchName,
                    yearMonth = key.yearMonth,
                    convertedHeadcount = group.sumOf { it.convertedHeadcount ?: BigDecimal.ZERO },
                )
            }

        // 구분(accountType) 그룹 + 그룹별 소계. 정렬: 구분 Asc → 근무유형1 → 근무유형3 → 지점 → 연월 (SF groupings 정합)
        val groups = aggregated
            .groupBy { it.accountType }
            .toSortedMap(nullsLast(naturalOrder()))
            .map { (accountType, groupRows) ->
                ConvertedHeadcountReportGroup(
                    accountType = accountType ?: "",
                    subtotalHeadcount = groupRows.sumOf { it.convertedHeadcount },
                    rows = groupRows.sortedWith(
                        compareBy<ConvertedHeadcountReportRow, String?>(nullsLast()) { it.workingCategory1 }
                            .thenBy(nullsLast()) { it.workingCategory3 }
                            .thenBy(nullsLast()) { it.branchName }
                            .thenBy(nullsLast()) { it.yearMonth },
                    ),
                )
            }

        return ConvertedHeadcountReportResult(
            variant = variant.name,
            year = year,
            month = month,
            includeWorkingCategory3 = variant.includeWorkingCategory3,
            groups = groups,
            totalHeadcount = groups.sumOf { it.subtotalHeadcount },
        )
    }

    /**
     * 환산인원 현황 엑셀 export — 구분/근무유형1/지점/연월/환산인원 + 그룹 소계 + 총계.
     */
    fun exportReport(variant: ConvertedHeadcountReportVariant, year: String, month: String): ExcelResult {
        val report = getReport(variant, year, month)

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("환산인원현황")
        val headerStyle = createHeaderStyle(workbook)

        // 근무유형3 컬럼은 variant.includeWorkingCategory3 인 경우에만 표시 (대리점 3종 + 대형마트 X3_rq9).
        val wc3 = report.includeWorkingCategory3
        val headers = buildList {
            add("구분")
            add("근무유형1")
            if (wc3) add("근무유형3")
            add("지점")
            add("연월")
            add("환산인원")
        }
        val hcCol = headers.lastIndex // 환산인원 컬럼 인덱스
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        var rowIdx = 1
        report.groups.forEach { group ->
            group.rows.forEach { r ->
                val row = sheet.createRow(rowIdx++)
                var c = 0
                row.createCell(c++).setCellValue(r.accountType ?: "")
                row.createCell(c++).setCellValue(r.workingCategory1 ?: "")
                if (wc3) row.createCell(c++).setCellValue(r.workingCategory3 ?: "")
                row.createCell(c++).setCellValue(r.branchName ?: "")
                row.createCell(c++).setCellValue(r.yearMonth ?: "")
                row.createCell(c).setCellValue(r.convertedHeadcount.toDouble())
            }
            // 그룹 소계 행
            val subtotalRow = sheet.createRow(rowIdx++)
            subtotalRow.createCell(0).setCellValue("${group.accountType} 소계")
            subtotalRow.createCell(hcCol).setCellValue(group.subtotalHeadcount.toDouble())
        }
        // 전체 총계 행
        val totalRow = sheet.createRow(rowIdx)
        totalRow.createCell(0).setCellValue("합계")
        totalRow.createCell(hcCol).setCellValue(report.totalHeadcount.toDouble())

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        return ExcelResult(bytes, "${variant.reportName}_${year}-${month}.xlsx")
    }

    /** variant 별 지점 기준 — 1-x: 여사원 소속(empBranchName) / 2-1: 거래처(account.branchName). */
    private fun branchNameOf(m: MonthlyFemaleEmployeeIntegrationSchedule, variant: ConvertedHeadcountReportVariant): String? =
        if (variant.useAccountBranch) m.account?.branchName else m.empBranchName

    /** SF DateForReport__c (Month) = year-month 재구성. */
    private fun yearMonthOf(m: MonthlyFemaleEmployeeIntegrationSchedule): String? {
        val y = m.year ?: return null
        val mo = m.month ?: return y
        return "$y-${mo.padStart(2, '0')}"
    }

    private data class AggKey(
        val accountType: String?,
        val workingCategory1: String?,
        val workingCategory3: String?,
        val branchName: String?,
        val yearMonth: String?,
    )

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

    data class ExcelResult(
        val bytes: ByteArray,
        val filename: String,
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
