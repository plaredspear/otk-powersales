package com.otoki.powersales.product.service

import com.otoki.powersales.product.repository.ProductRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream

/**
 * 선택 제품 엑셀 내려받기 (UC-05).
 *
 * 레거시 ProductToExcelController.cls 정책:
 * - "출고중지" 상태 제품은 결과에서 제외
 * - 형태구분 (TasteGift) 코드 "1" → "전용", "2" → "범용" 변환
 *
 * 신규 컬럼 구성은 명시 컬럼 (FieldSet 동적 컬럼 대신 — feedback_subagent_call_pattern 정책).
 */
@Service
@Transactional(readOnly = true)
class AdminProductExportService(
    private val productRepository: ProductRepository
) {

    fun exportSelectedProducts(productCodes: List<String>): ByteArray {
        val products = productRepository.findByProductCodeIn(productCodes)
            .filter { it.productStatus?.displayName != STATUS_DISCONTINUED }

        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("선택제품")
            val headerStyle = createHeaderStyle(workbook)

            val headerRow = sheet.createRow(0)
            HEADERS.forEachIndexed { idx, title ->
                val cell = headerRow.createCell(idx)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }

            products.forEachIndexed { rowIdx, product ->
                val row = sheet.createRow(rowIdx + 1)
                row.createCell(0).setCellValue(product.productCode ?: "")
                row.createCell(1).setCellValue(product.name ?: "")
                row.createCell(2).setCellValue(product.productCategory1 ?: "")
                row.createCell(3).setCellValue(product.productCategory2 ?: "")
                row.createCell(4).setCellValue(product.productCategory3 ?: "")
                row.createCell(5).setCellValue(product.storageCondition?.displayName ?: "")
                row.createCell(6).setCellValue(product.unit ?: "")
                row.createCell(7).setCellValue(product.launchDate?.toString() ?: "")
                row.createCell(8).setCellValue(product.standardUnitPrice?.toPlainString() ?: "")
                row.createCell(9).setCellValue(product.productStatus?.displayName ?: "")
                row.createCell(10).setCellValue(convertTasteGift(product.tasteGift))
            }

            HEADERS.indices.forEach { sheet.autoSizeColumn(it) }

            ByteArrayOutputStream().use { out ->
                workbook.write(out)
                return out.toByteArray()
            }
        }
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont().apply { bold = true }
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun convertTasteGift(code: String?): String = when (code) {
        "1" -> "전용"
        "2" -> "범용"
        else -> code ?: ""
    }

    companion object {
        private const val STATUS_DISCONTINUED = "출고중지"

        private val HEADERS = listOf(
            "제품코드", "제품명", "카테고리1", "카테고리2", "카테고리3",
            "보관방법", "단위", "출시일", "표준출고가", "제품상태", "형태구분"
        )
    }
}
