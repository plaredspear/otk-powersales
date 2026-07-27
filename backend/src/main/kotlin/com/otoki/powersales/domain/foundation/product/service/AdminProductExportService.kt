package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream

/**
 * 제품 목록 엑셀 내려받기 (UC-05).
 *
 * 대상은 **화면에서 체크한 제품이 아니라 현재 조회 조건에 해당하는 전체 제품** — 목록 조회
 * ([com.otoki.powersales.domain.foundation.product.service.AdminProductService.getProducts]) 와
 * 동일한 `searchForAdmin` 술어를 재사용해 화면 결과와 파일 내용이 어긋나지 않게 한다.
 * 페이징 없이 [EXPORT_MAX_ROWS] 단일 페이지로 조회하고 초과분은 잘라낸다 (다른 export 서비스 정합).
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

    fun exportByCondition(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?
    ): ByteArray {
        val products = productRepository.searchForAdmin(
            keyword = keyword,
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            pageable = PageRequest.of(0, EXPORT_MAX_ROWS)
        ).content.filter { it.productStatus != ProductStatus.OUT_OF_STOCK }

        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("제품")
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
                // 목록/lookup 응답과 동일하게 화면 표시명 — 값이 없으면 "판매중".
                row.createCell(9).setCellValue(product.productStatus?.label ?: ProductStatus.DEFAULT_LABEL)
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
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        return style
    }

    private fun convertTasteGift(code: String?): String = when (code) {
        "1" -> "전용"
        "2" -> "범용"
        else -> code ?: ""
    }

    companion object {
        /** 다른 목록 export 서비스와 동일한 단일 조회 상한. 초과분은 잘라낸다. */
        const val EXPORT_MAX_ROWS = 50_000

        private val HEADERS = listOf(
            "제품코드", "제품명", "카테고리1", "카테고리2", "카테고리3",
            "보관방법", "단위", "출시일", "표준출고가", "제품상태", "형태구분"
        )
    }
}
