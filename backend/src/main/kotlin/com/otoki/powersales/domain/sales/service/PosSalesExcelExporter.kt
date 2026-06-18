package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component

/**
 * POS매출 제품별 명세 엑셀 export — 조회 화면(`SalesQueryPage.tsx` 제품별 명세 테이블)과 동일 구성.
 *
 * 거래처 1곳 + 연월의 제품별 집계 전량을 단일 시트로 출력.
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class PosSalesExcelExporter : BaseExcelExporter<PosSalesResponse.ProductSales>() {

    override val sheetName = "POS매출"

    // 목록 테이블 컬럼 정합 (제품코드 / 제품명 / 바코드 / 납품수량(EA) / 금액(원)).
    override val headers = listOf("제품코드", "제품명", "바코드", "납품수량(EA)", "금액(원)")

    /**
     * 엑셀 워크북 생성 + 바이트 + 파일명 반환. 파일명 형식: `POS매출_{거래처명}_{yearMonth}.xlsx`.
     * 빈 items 일 경우 헤더 행만 존재.
     */
    fun export(customerName: String, yearMonth: String, items: List<PosSalesResponse.ProductSales>): ExcelResult {
        val safeName = customerName.ifBlank { "거래처" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        return export(items, "POS매출_${safeName}_${yearMonth}.xlsx")
    }

    override fun writeRow(row: Row, item: PosSalesResponse.ProductSales) {
        row.createCell(0).setCellValue(item.productCode)
        row.createCell(1).setCellValue(item.productName)
        row.createCell(2).setCellValue(item.barcode ?: "")
        row.createCell(3).setCellValue(item.quantity.toDouble())
        row.createCell(4).setCellValue(item.amount.toDouble())
    }
}
