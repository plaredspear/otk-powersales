package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListItem
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

@DisplayName("ElectronicSalesDashboardExcelExporter 테스트")
class ElectronicSalesDashboardExcelExporterTest {

    private val exporter = ElectronicSalesDashboardExcelExporter()

    private val start = LocalDate.of(2026, 5, 1)
    private val end = LocalDate.of(2026, 5, 31)

    private fun item(id: Long, name: String) = ElectronicSalesDashboardListItem(
        accountId = id,
        accountName = name,
        sapAccountCode = "SAP$id",
        branchCode = "1000",
        branchName = "서울지점",
        salesAmount = 1_500_000L,
        salesQuantity = 320L,
    )

    @Test
    @DisplayName("빈 items → 헤더 1행만")
    fun emptyExport() {
        val result = exporter.export(start, end, emptyList())
        assertThat(result.filename).isEqualTo("electronic-sales-2026-05-01-2026-05-31.xlsx")
        ByteArrayInputStream(result.bytes).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 1행 (0-based)
            wb.close()
        }
    }

    @Test
    @DisplayName("items N건 → 헤더 + N 행 + 셀값 매핑")
    fun nonEmptyExport() {
        val items = listOf(item(1, "거래처A"), item(2, "거래처B"))
        val result = exporter.export(start, end, items)
        ByteArrayInputStream(result.bytes).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(2) // 헤더 1 + 데이터 2
            assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("거래처A")
            // 전산매출 금액(4) / 수량(5) 컬럼 — 매출연도/매출월 컬럼 제거 후 좌측으로 이동
            assertThat(sheet.getRow(1).getCell(4).numericCellValue).isEqualTo(1_500_000.0)
            assertThat(sheet.getRow(1).getCell(5).numericCellValue).isEqualTo(320.0)
            wb.close()
        }
    }

    @Test
    @DisplayName("파일명 형식 — 일 단위 기간 (yyyy-MM-dd)")
    fun filenameDateRange() {
        val result = exporter.export(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 4, 2), emptyList())
        assertThat(result.filename).isEqualTo("electronic-sales-2026-03-05-2026-04-02.xlsx")
    }
}
