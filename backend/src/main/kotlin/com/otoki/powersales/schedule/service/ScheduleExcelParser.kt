package com.otoki.powersales.schedule.service

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class ScheduleExcelParser {

    companion object {
        private const val DATA_START_ROW = 3 // 0-based (4행, ScheduleTemplateGenerator.DATA_START_ROW와 동일)
        private const val MAX_ROWS = 500
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    data class ParsedRow(
        val rowNumber: Int, // Excel 행 번호 (1-based, 사용자 표시용)
        val employeeCode: String?,
        val employeeName: String?,
        val accountCode: String?,
        val accountName: String?,
        val typeOfWork3: String?,
        val typeOfWork4: String?,
        val typeOfWork5: String?,
        val startDateStr: String?,
        val endDateStr: String?,
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null
    )

    data class ParseResult(
        val rows: List<ParsedRow>,
        val totalRows: Int
    )

    fun parse(inputStream: InputStream): ParseResult {
        val workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0)

        val rows = mutableListOf<ParsedRow>()
        var rowIdx = DATA_START_ROW

        while (rowIdx <= sheet.lastRowNum) {
            val row = sheet.getRow(rowIdx)
            if (row == null || isEmptyRow(row)) {
                rowIdx++
                continue
            }

            val excelRowNumber = rowIdx + 1 // 1-based for user display

            // 11컬럼: A(0)소속, B(1)사번, C(2)이름, D(3)직위, E(4)거래처코드, F(5)거래처명,
            //         G(6)근무형태3, H(7)근무형태4, I(8)근무형태5, J(9)시작일, K(10)종료일
            val employeeCode = getCellStringValue(row, 1)
            val employeeName = getCellStringValue(row, 2)
            val accountCode = getCellStringValue(row, 4)
            val accountName = getCellStringValue(row, 5)
            val typeOfWork3 = getCellStringValue(row, 6)
            val typeOfWork4 = getCellStringValue(row, 7)
            val typeOfWork5 = getCellStringValue(row, 8)
            val startDateStr = getCellStringValue(row, 9)
            val endDateStr = getCellStringValue(row, 10)

            val startDate = parseDate(startDateStr)
            val endDate = parseDate(endDateStr)

            rows.add(
                ParsedRow(
                    rowNumber = excelRowNumber,
                    employeeCode = employeeCode,
                    employeeName = employeeName,
                    accountCode = accountCode,
                    accountName = accountName,
                    typeOfWork3 = typeOfWork3,
                    typeOfWork4 = typeOfWork4,
                    typeOfWork5 = typeOfWork5,
                    startDateStr = startDateStr,
                    endDateStr = endDateStr,
                    startDate = startDate,
                    endDate = endDate
                )
            )

            rowIdx++
        }

        workbook.close()
        return ParseResult(rows = rows, totalRows = rows.size)
    }

    private fun isEmptyRow(row: Row): Boolean {
        // B열(사번)과 E열(거래처코드) 모두 비어있으면 빈 행으로 판단
        val col1 = getCellStringValue(row, 1)
        val col4 = getCellStringValue(row, 4)
        return col1.isNullOrBlank() && col4.isNullOrBlank()
    }

    private fun getCellStringValue(row: Row, colIndex: Int): String? {
        val cell = row.getCell(colIndex) ?: return null
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue?.trim()?.ifBlank { null }
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.localDateTimeCellValue.toLocalDate()
                    date.format(DATE_FORMATTER)
                } else {
                    // 숫자를 정수 문자열로 변환 (사원번호 등)
                    val numValue = cell.numericCellValue
                    if (numValue == numValue.toLong().toDouble()) {
                        numValue.toLong().toString()
                    } else {
                        numValue.toString()
                    }
                }
            }
            CellType.BLANK -> null
            CellType.FORMULA -> cell.stringCellValue?.trim()?.ifBlank { null }
            else -> cell.toString().trim().ifBlank { null }
        }
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dateStr, DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
