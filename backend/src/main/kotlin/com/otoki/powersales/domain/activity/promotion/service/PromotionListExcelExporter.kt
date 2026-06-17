package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionListItem
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream

/**
 * 행사마스터 목록 엑셀 export — 목록 화면(`AdminPromotionService.getPromotions`) 컬럼과 동일 구성.
 *
 * 페이징 없이 현재 검색 조건의 가시 범위 전량을 단일 시트로 출력 (호출 측에서 최대 건수 제한).
 * 컬럼: 행사번호 / 행사명 / 거래처 / 대표제품 / 시작일 / 종료일 / 거래처코드 / 행사유형 / 매대위치 /
 *       제품유형 / CC코드 / 목표금액 / 실적금액 / 작성일자 / 작성자 (목록 테이블 컬럼 정합).
 */
@Component
class PromotionListExcelExporter {

    fun export(items: List<PromotionListItem>): ExcelResult {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("행사마스터")
        val headerStyle = createHeaderStyle(workbook)

        val headers = listOf(
            "행사번호", "행사명", "거래처", "대표제품", "시작일", "종료일", "거래처코드",
            "행사유형", "매대위치", "제품유형", "CC Code", "목표금액", "실적금액(원)",
            "작성 일자", "작성자",
        )
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }
        sheet.createFreezePane(0, 1)

        items.forEachIndexed { index, item ->
            writeRow(sheet.createRow(index + 1), item)
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val bytes = workbookToBytes(workbook)
        return ExcelResult(bytes, "행사마스터.xlsx")
    }

    private fun writeRow(row: Row, item: PromotionListItem) {
        row.createCell(0).setCellValue(item.promotionNumber)
        row.createCell(1).setCellValue(item.promotionName ?: "")
        row.createCell(2).setCellValue(item.accountName ?: "")
        row.createCell(3).setCellValue(item.primaryProductName ?: "")
        row.createCell(4).setCellValue(item.startDate.toString())
        row.createCell(5).setCellValue(item.endDate.toString())
        row.createCell(6).setCellValue(item.accountCode ?: "")
        row.createCell(7).setCellValue(item.promotionType ?: "")
        row.createCell(8).setCellValue(item.standLocation ?: "")
        row.createCell(9).setCellValue(item.category1 ?: "")
        row.createCell(10).setCellValue(item.costCenterCode ?: "")
        row.createCell(11).setCellValue(item.targetAmount ?: 0.0)
        row.createCell(12).setCellValue(item.actualAmount ?: 0.0)
        row.createCell(13).setCellValue(item.createdAt.toString())
        row.createCell(14).setCellValue(item.createdByName ?: "")
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
