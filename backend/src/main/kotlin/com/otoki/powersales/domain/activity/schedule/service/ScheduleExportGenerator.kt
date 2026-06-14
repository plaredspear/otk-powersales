package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter

/**
 * UC-08 진열사원 스케줄 Excel 다운로드 생성기.
 * 레거시 SF `ExcelIO_ExportDisplayWorkScheduleMaster2.page` (ecio 관리형 패키지) 의 list view 컬럼셋과 동등 매핑.
 * 신규 화면 `DisplaySchedulePage.tsx` 의 listColumns 11종과 1:1 정합.
 */
@Component
class ScheduleExportGenerator {

    companion object {
        private val HEADERS = listOf(
            "사원번호", "사원명", "거래처코드", "거래처명",
            "근무유형3", "근무유형5", "시작일", "종료일",
            "확정", "조직코드", "전월매출"
        )
        private val COLUMN_WIDTHS = listOf(12, 10, 14, 20, 10, 10, 12, 12, 8, 10, 14)
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    fun generate(schedules: List<DisplayWorkSchedule>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("진열스케줄")

        COLUMN_WIDTHS.forEachIndexed { i, width ->
            sheet.setColumnWidth(i, width * 256)
        }

        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }

        val headerRow = sheet.createRow(0)
        HEADERS.forEachIndexed { i, header ->
            val cell = headerRow.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        schedules.forEachIndexed { index, schedule ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(schedule.employee?.employeeCode ?: "")
            row.createCell(1).setCellValue(schedule.employee?.name ?: "")
            row.createCell(2).setCellValue(schedule.account?.externalKey ?: "")
            row.createCell(3).setCellValue(schedule.account?.name ?: "")
            row.createCell(4).setCellValue(schedule.typeOfWork3?.displayName ?: "")
            row.createCell(5).setCellValue(schedule.typeOfWork5?.displayName ?: "")
            row.createCell(6).setCellValue(schedule.startDate?.format(DATE_FORMAT) ?: "")
            row.createCell(7).setCellValue(schedule.endDate?.format(DATE_FORMAT) ?: "")
            row.createCell(8).setCellValue(if (schedule.confirmed == true) "확정" else "미확정")
            row.createCell(9).setCellValue(schedule.costCenterCode ?: "")
            val revenue = schedule.lastMonthRevenue?.toLong()
            if (revenue != null) {
                row.createCell(10).setCellValue(revenue.toDouble())
            } else {
                row.createCell(10).setCellValue("")
            }
        }

        val outputStream = ByteArrayOutputStream()
        workbook.use { it.write(outputStream) }
        return outputStream.toByteArray()
    }
}
