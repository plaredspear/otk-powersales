package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.MonthlySalesDashboardListItem
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal

@DisplayName("MonthlySalesDashboardExcelExporter 테스트")
class MonthlySalesDashboardExcelExporterTest {

    private val exporter = MonthlySalesDashboardExcelExporter()

    private fun item(id: Long, name: String, isConfirmed: Boolean) = MonthlySalesDashboardListItem(
        accountId = id,
        accountName = name,
        distributionChannelLabel = "02 슈퍼",
        abcTypeLabel = "6111 이마트",
        sapAccountCode = "SAP$id",
        branchCode = "1000",
        branchName = "서울지점",
        salesYear = 2026,
        salesMonth = 5,
        targetAmount = 1_000_000L,
        totalAchievedAmount = 800_000L,
        achievementRate = 80.0,
        ambientTargetAmount = 250_000L,
        ambientAchievedAmount = 200_000L,
        noodleTargetAmount = 250_000L,
        noodleAchievedAmount = 200_000L,
        frozenRefrigeratedTargetAmount = 250_000L,
        frozenRefrigeratedAchievedAmount = 200_000L,
        oilFatTargetAmount = 250_000L,
        oilFatAchievedAmount = 200_000L,
        lastYearAchievedAmount = 700_000L,
        lastYearComparisonRatio = 114.3,
        isConfirmed = isConfirmed,
        displayHeadcount = BigDecimal("2.5"),
        eventHeadcount = BigDecimal("1.0"),
        totalHeadcount = BigDecimal("3.5"),
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
            // 화면 순서 정합: 지점명(0) 거래처명(1) 거래처코드(2) 진열(3) 행사(4) 총인원(5)
            assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("서울지점")
            assertThat(sheet.getRow(1).getCell(1).stringCellValue).isEqualTo("거래처A")
            assertThat(sheet.getRow(1).getCell(2).stringCellValue).isEqualTo("SAP1")
            assertThat(sheet.getRow(1).getCell(3).numericCellValue).isEqualTo(2.5)
            assertThat(sheet.getRow(1).getCell(4).numericCellValue).isEqualTo(1.0)
            assertThat(sheet.getRow(1).getCell(5).numericCellValue).isEqualTo(3.5)
            // 엑셀 전용 컬럼은 뒤로: 유통형태(22) 거래처유형(23)
            assertThat(sheet.getRow(1).getCell(22).stringCellValue).isEqualTo("02 슈퍼")
            assertThat(sheet.getRow(1).getCell(23).stringCellValue).isEqualTo("6111 이마트")
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
