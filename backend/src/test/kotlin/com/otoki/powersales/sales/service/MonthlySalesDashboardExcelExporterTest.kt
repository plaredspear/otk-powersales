package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.dto.response.MonthlySalesDashboardListItem
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("MonthlySalesDashboardExcelExporter 테스트")
class MonthlySalesDashboardExcelExporterTest {

    private val exporter = MonthlySalesDashboardExcelExporter()

    private fun item(id: Int, name: String, isConfirmed: Boolean) = MonthlySalesDashboardListItem(
        accountId = id,
        accountName = name,
        sapAccountCode = "SAP$id",
        branchCode = "1000",
        branchName = "서울지점",
        salesYear = 2026,
        salesMonth = 5,
        targetAmount = 1_000_000L,
        totalAchievedAmount = 800_000L,
        achievementRate = 80.0,
        ambientAchievedAmount = 200_000L,
        noodleAchievedAmount = 200_000L,
        frozenRefrigeratedAchievedAmount = 200_000L,
        oilFatAchievedAmount = 200_000L,
        lastYearAchievedAmount = 700_000L,
        lastYearComparisonRatio = 114.3,
        isConfirmed = isConfirmed,
    )

    @Test
    @DisplayName("빈 items → 헤더 1행만")
    fun emptyExport() {
        val result = exporter.export(2026, 5, emptyList())
        assertThat(result.filename).isEqualTo("monthly-sales-2026-05.xlsx")
        ByteArrayInputStream(result.bytes).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 1행 (0-based)
            wb.close()
        }
    }

    @Test
    @DisplayName("items N건 → 헤더 + N 행")
    fun nonEmptyExport() {
        val items = listOf(item(1, "거래처A", true), item(2, "거래처B", false), item(3, "거래처C", true))
        val result = exporter.export(2026, 5, items)
        ByteArrayInputStream(result.bytes).use { input ->
            val wb = WorkbookFactory.create(input)
            val sheet = wb.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(3) // 헤더 1 + 데이터 3 = 마지막 인덱스 3
            // 마감 컬럼은 마지막 (인덱스 15 — 거래처 SFID 컬럼 제거 후)
            assertThat(sheet.getRow(1).getCell(15).stringCellValue).isEqualTo("마감")
            assertThat(sheet.getRow(2).getCell(15).stringCellValue).isEqualTo("미마감")
            wb.close()
        }
    }

    @Test
    @DisplayName("파일명 형식 — 한 자리 월 zero-pad")
    fun filenameZeroPad() {
        val result = exporter.export(2026, 3, emptyList())
        assertThat(result.filename).isEqualTo("monthly-sales-2026-03.xlsx")
    }
}
