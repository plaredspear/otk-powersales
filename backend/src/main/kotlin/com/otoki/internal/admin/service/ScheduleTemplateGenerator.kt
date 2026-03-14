package com.otoki.internal.admin.service

import com.otoki.internal.sap.entity.User
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class ScheduleTemplateGenerator {

    companion object {
        private val HEADERS = listOf("*사원번호", "사원명", "*거래처코드", "거래처명", "*근무유형3", "*근무유형5", "*시작일", "종료일")
        private val COLUMN_WIDTHS = listOf(12, 10, 14, 20, 14, 14, 14, 14)
        private val GUIDE_TEXTS = listOf("(자동입력)", "(자동입력)", "거래처코드 입력", "(선택)", "고정 / 격고 / 순회", "상시 / 임시", "yyyy-MM-dd", "yyyy-MM-dd (선택)")
        private val REQUIRED_HEADER_INDICES = setOf(0, 2, 4, 5, 6) // *사원번호, *거래처코드, *근무유형3, *근무유형5, *시작일
        private val REQUIRED_INPUT_INDICES = setOf(2, 4, 5, 6) // C, E, F, G (사용자 입력 필수 컬럼)
        private const val NOTICE_TEXT = "신규입력 전용 양식입니다. 거래처코드~종료일 컬럼을 입력한 후 업로드하세요."
        private val WORK_TYPE3_OPTIONS = listOf("고정", "격고", "순회")
        private val WORK_TYPE5_OPTIONS = listOf("상시", "임시")
        private const val DATA_START_ROW = 3 // 0-based (4행)
        private const val DROPDOWN_END_ROW = 1003 // 0-based (1004행)
    }

    fun generate(employees: List<User>): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Template")

        // 컬럼 너비 설정
        COLUMN_WIDTHS.forEachIndexed { i, width ->
            sheet.setColumnWidth(i, width * 256)
        }

        // 1행: 안내 문구
        createNoticeRow(workbook, sheet)

        // 2행: 입력 가이드
        createGuideRow(workbook, sheet)

        // 3행: 헤더
        createHeaderRow(workbook, sheet)

        // G, H열 셀 서식: 텍스트 (날짜 자동변환 방지)
        val textStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("@")
        }

        // 필수 입력 셀용 스타일 (노란 배경 + 텍스트 서식)
        val yellowStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        val yellowTextStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            dataFormat = workbook.createDataFormat().getFormat("@")
        }

        // 4행~: 사원 데이터
        employees.forEachIndexed { index, user ->
            val rowIdx = DATA_START_ROW + index
            val row = sheet.createRow(rowIdx)

            // A: 사원번호
            row.createCell(0).setCellValue(user.employeeId)
            // B: 사원명
            row.createCell(1).setCellValue(user.name)

            // C, E, F열: 노란 배경 (필수 입력)
            for (colIdx in listOf(2, 4, 5)) {
                val cell = row.createCell(colIdx)
                cell.cellStyle = yellowStyle
            }
            // G열: 노란 배경 + 텍스트 서식
            row.createCell(6).cellStyle = yellowTextStyle
            // H열: 텍스트 서식
            row.createCell(7).cellStyle = textStyle
        }

        // 사원이 없어도 G, H열의 텍스트 서식은 드롭다운 범위에 적용
        for (rowIdx in DATA_START_ROW..DROPDOWN_END_ROW) {
            val row = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
            if (row.getCell(6) == null) {
                row.createCell(6).cellStyle = textStyle
            }
            if (row.getCell(7) == null) {
                row.createCell(7).cellStyle = textStyle
            }
        }

        // E열 드롭다운: 고정, 격고, 순회
        addDropdownValidation(sheet, DATA_START_ROW, DROPDOWN_END_ROW, 4, WORK_TYPE3_OPTIONS)

        // F열 드롭다운: 상시, 임시
        addDropdownValidation(sheet, DATA_START_ROW, DROPDOWN_END_ROW, 5, WORK_TYPE5_OPTIONS)

        val outputStream = ByteArrayOutputStream()
        workbook.use { it.write(outputStream) }
        return outputStream.toByteArray()
    }

    private fun createNoticeRow(workbook: XSSFWorkbook, sheet: Sheet) {
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue(NOTICE_TEXT)

        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        font.color = IndexedColors.RED.index
        style.setFont(font)
        cell.cellStyle = style

        // A1~H1 병합
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 7))
    }

    private fun createGuideRow(workbook: XSSFWorkbook, sheet: Sheet) {
        val row = sheet.createRow(1)
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        font.bold = true
        style.setFont(font)

        GUIDE_TEXTS.forEachIndexed { i, text ->
            val cell = row.createCell(i)
            cell.setCellValue(text)
            cell.cellStyle = style
        }
    }

    private fun createHeaderRow(workbook: XSSFWorkbook, sheet: Sheet) {
        val row = sheet.createRow(2)

        // 필수 컬럼: 주황 배경 + 흰색 글씨
        val requiredStyle = workbook.createCellStyle()
        requiredStyle.setFillForegroundColor(XSSFColor(byteArrayOf(0xC6.toByte(), 0x59.toByte(), 0x11.toByte()), null))
        requiredStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        val whiteFont = workbook.createFont()
        whiteFont.bold = true
        whiteFont.color = IndexedColors.WHITE.index
        requiredStyle.setFont(whiteFont)

        // 선택 컬럼: 남색 배경 + 흰색 글씨
        val optionalStyle = workbook.createCellStyle()
        optionalStyle.setFillForegroundColor(XSSFColor(byteArrayOf(0x1E.toByte(), 0x2F.toByte(), 0x97.toByte()), null))
        optionalStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        val whiteFontOptional = workbook.createFont()
        whiteFontOptional.bold = true
        whiteFontOptional.color = IndexedColors.WHITE.index
        optionalStyle.setFont(whiteFontOptional)

        HEADERS.forEachIndexed { i, header ->
            val cell = row.createCell(i)
            cell.setCellValue(header)
            cell.cellStyle = if (i in REQUIRED_HEADER_INDICES) requiredStyle else optionalStyle
        }
    }

    private fun addDropdownValidation(sheet: Sheet, startRow: Int, endRow: Int, colIndex: Int, options: List<String>) {
        val helper = sheet.dataValidationHelper
        val constraint = helper.createExplicitListConstraint(options.toTypedArray())
        val addressList = CellRangeAddressList(startRow, endRow, colIndex, colIndex)
        val validation = helper.createValidation(constraint, addressList)
        validation.showErrorBox = true
        sheet.addValidationData(validation)
    }
}
