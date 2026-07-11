package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListItem
import com.otoki.powersales.platform.common.util.excel.BaseExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.apache.poi.ss.usermodel.Row
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 전산실적 대시보드 명세 엑셀 export.
 *
 * [ElectronicSalesAdminQueryService.getListForExport] 산출물을 Apache POI 워크북으로 변환.
 * 시트 1개 (전산실적조회), 헤더 1행 + 데이터 N행. 거래처별 전산매출 금액/수량 합계.
 * 워크북 생성 / 헤더 스타일 / 직렬화는 [BaseExcelExporter] 가 담당하고, 본 클래스는 컬럼 정의만 책임진다.
 */
@Component
class ElectronicSalesDashboardExcelExporter : BaseExcelExporter<ElectronicSalesDashboardListItem>() {

    override val sheetName = "전산실적조회"

    // 기존 동작 정합 — 본 export 는 freeze pane 미적용이었음.
    override val freezeHeader = false

    override val headers = listOf(
        "거래처명", "SAP코드", "유통형태", "거래처유형", "지점코드", "지점명",
        "전산매출 금액", "전산매출 수량",
    )

    /**
     * 엑셀 워크북 생성 + 바이트 + 파일명 반환.
     * 파일명 형식: `electronic-sales-{startDate}-{endDate}.xlsx` (일 단위 기간).
     * 빈 items 일 경우 헤더 행만 존재.
     */
    fun export(startDate: LocalDate, endDate: LocalDate, items: List<ElectronicSalesDashboardListItem>): ExcelResult =
        export(items, "electronic-sales-${startDate}-${endDate}.xlsx")

    override fun writeRow(row: Row, item: ElectronicSalesDashboardListItem) {
        var col = 0
        row.createCell(col++).setCellValue(item.accountName ?: "")
        row.createCell(col++).setCellValue(item.sapAccountCode ?: "")
        row.createCell(col++).setCellValue(item.distributionChannel ?: "")
        row.createCell(col++).setCellValue(item.accountType ?: "")
        row.createCell(col++).setCellValue(item.branchCode ?: "")
        row.createCell(col++).setCellValue(item.branchName ?: "")
        row.createCell(col++).setCellValue(item.salesAmount.toDouble())
        row.createCell(col).setCellValue(item.salesQuantity.toDouble())
    }
}
