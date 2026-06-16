package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListItem
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

/**
 * 월매출 대시보드 명세 엑셀 export.
 *
 * `MonthlySalesAdminQueryService.getListForExport` 의 산출물을 Apache POI 워크북으로 변환.
 * 시트 1개 (월매출조회), 헤더 1행 + 데이터 N행. 카테고리 4종(상온/라면/냉동냉장/유지)은 목표/실적 쌍으로 배치.
 */
@Component
class MonthlySalesDashboardExcelExporter {

    /**
     * 엑셀 워크북 생성 + 바이트 + 파일명 반환.
     *
     * 파일명 형식: `monthly-sales-{year}-{month:02}.xlsx`. 빈 items 일 경우 헤더 행만 존재.
     */
    fun export(year: Int, month: Int, items: List<MonthlySalesDashboardListItem>): ExcelResult {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("월매출조회")
        val headerStyle = createHeaderStyle(workbook)

        val headers = listOf(
            "거래처명", "SAP코드", "지점코드", "지점명",
            "매출연도", "매출월",
            "목표", "실적", "진도율(%)",
            "상온 목표", "상온 실적",
            "라면 목표", "라면 실적",
            "냉동냉장 목표", "냉동냉장 실적",
            "유지 목표", "유지 실적",
            "전년 동월 실적", "전년 대비(%)", "마감"
        )

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { idx, name ->
            headerRow.createCell(idx).apply {
                setCellValue(name)
                cellStyle = headerStyle
            }
        }

        items.forEachIndexed { rowIdx, item ->
            val row = sheet.createRow(rowIdx + 1)
            var col = 0
            row.createCell(col++).setCellValue(item.accountName ?: "")
            row.createCell(col++).setCellValue(item.sapAccountCode ?: "")
            row.createCell(col++).setCellValue(item.branchCode ?: "")
            row.createCell(col++).setCellValue(item.branchName ?: "")
            row.createCell(col++).setCellValue(item.salesYear.toDouble())
            row.createCell(col++).setCellValue(item.salesMonth.toDouble())
            row.createCell(col++).setCellValue(item.targetAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.totalAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.achievementRate ?: 0.0)
            row.createCell(col++).setCellValue(item.ambientTargetAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.ambientAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.noodleTargetAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.noodleAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.frozenRefrigeratedTargetAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.frozenRefrigeratedAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.oilFatTargetAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.oilFatAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.lastYearAchievedAmount?.toDouble() ?: 0.0)
            row.createCell(col++).setCellValue(item.lastYearComparisonRatio ?: 0.0)
            row.createCell(col).setCellValue(if (item.isConfirmed) "마감" else "미마감")
        }

        repeat(headers.size) { sheet.autoSizeColumn(it) }

        val bytes = ByteArrayOutputStream().use { out ->
            workbook.write(out)
            workbook.close()
            out.toByteArray()
        }
        val filename = "monthly-sales-${year}-${month.toString().padStart(2, '0')}.xlsx"
        return ExcelResult(bytes, filename)
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
