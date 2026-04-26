package com.otoki.powersales.schedule.service

import com.otoki.powersales.sap.entity.Employee
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

@Component
class ScheduleTemplateGenerator {

    companion object {
        private val HEADERS = listOf(
            "소속", "*사번(필수입력)", "이름", "직위",
            "*거래처코드(필수입력)", "거래처명",
            "*근무형태3(필수입력)", "*근무형태4(필수입력)", "*근무형태5(필수입력)",
            "*시작일(필수입력)", "*종료일(선택입력)"
        )
        private val COLUMN_WIDTHS = listOf(10, 14, 10, 10, 20, 20, 20, 20, 20, 20, 20)
        private val GUIDE_TEXTS = listOf(
            "", "", "", "", "", "",
            "고정 or 격고 or 순회", "상온 or 냉동/냉장", "상시 or 임시",
            "yyyy-mm-dd", "yyyy-mm-dd"
        )
        private const val NOTICE_TEXT = "신규입력만 가능합니다. 수정이 필요한 경우 화면단에서 진행해주세요.(업로드시에는 파일 복호화를 해주시기 바랍니다.)"
        private val YELLOW_CELL_INDICES = setOf(1, 4, 6, 7, 8, 9) // B, E, G, H, I, J
        private val DATE_CELL_INDICES = setOf(9, 10) // J, K
        private const val DATA_START_ROW = 3 // 0-based (4행)
    }

    fun generate(employees: List<Employee>): ByteArray {
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

        // 노란 배경 스타일
        val yellowStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        // 날짜 서식 스타일
        val dateStyle = workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("yyyy-mm-dd")
        }

        // 노란 배경 + 날짜 서식
        val yellowDateStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            dataFormat = workbook.createDataFormat().getFormat("yyyy-mm-dd")
        }

        // 4행~: 사원 데이터
        employees.forEachIndexed { index, employee ->
            val rowIdx = DATA_START_ROW + index
            val row = sheet.createRow(rowIdx)

            // A: 소속
            row.createCell(0).setCellValue(employee.orgName ?: "")
            // B: 사번
            val cellB = row.createCell(1)
            cellB.setCellValue(employee.employeeCode)
            cellB.cellStyle = yellowStyle
            // C: 이름
            row.createCell(2).setCellValue(employee.name)
            // D: 직위
            row.createCell(3).setCellValue(employee.jikwee ?: "")

            // E(4): 노란 배경
            row.createCell(4).cellStyle = yellowStyle
            // F(5): 거래처명 (빈 셀)
            row.createCell(5)

            // G(6), H(7), I(8): 노란 배경
            for (colIdx in listOf(6, 7, 8)) {
                row.createCell(colIdx).cellStyle = yellowStyle
            }

            // J(9): 노란 + 날짜
            row.createCell(9).cellStyle = yellowDateStyle
            // K(10): 날짜
            row.createCell(10).cellStyle = dateStyle
        }

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

        // A1~K1 병합
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 10))
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
            if (text.isNotEmpty()) {
                cell.cellStyle = style
            }
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
            cell.cellStyle = if (header.startsWith("*")) requiredStyle else optionalStyle
        }
    }
}
