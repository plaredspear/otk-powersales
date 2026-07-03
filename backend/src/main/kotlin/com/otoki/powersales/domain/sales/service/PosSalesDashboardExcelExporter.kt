package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * POS매출 대시보드 거래처별 명세 엑셀 export.
 *
 * [PosSalesAdminQueryService.getListForExport] 산출물을 Apache POI 워크북으로 변환.
 * 시트 1개 (POS매출조회), 헤더 1행 + 데이터 N행. 거래처별 POS매출 금액/수량 합계.
 * 전산실적 [ElectronicSalesDashboardExcelExporter] 와 동일 구성.
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class PosSalesDashboardExcelExporter : BaseExcelExporter<PosSalesDashboardListItem>() {

    override val sheetName = "POS매출조회"

    override val headers = listOf(
        "거래처명", "SAP코드", "지점코드", "지점명",
        "POS매출 금액(원)", "POS매출 수량(EA)",
    )

    /**
     * 엑셀 워크북 생성 + 바이트 + 파일명 반환.
     * 파일명 형식: `pos-sales-{startDate}-{endDate}.xlsx` (일 단위 기간).
     * 빈 items 일 경우 헤더 행만 존재.
     */
    fun export(startDate: LocalDate, endDate: LocalDate, items: List<PosSalesDashboardListItem>): ExcelResult =
        export(items, "pos-sales-${startDate}-${endDate}.xlsx")

    override fun writeRow(row: Row, item: PosSalesDashboardListItem) {
        var col = 0
        row.createCell(col++).setCellValue(item.accountName ?: "")
        row.createCell(col++).setCellValue(item.sapAccountCode ?: "")
        row.createCell(col++).setCellValue(item.branchCode ?: "")
        row.createCell(col++).setCellValue(item.branchName ?: "")
        row.createCell(col++).setCellValue(item.salesAmount.toDouble())
        row.createCell(col).setCellValue(item.salesQuantity.toDouble())
    }
}
