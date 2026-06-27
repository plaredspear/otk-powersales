package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate

@DisplayName("ScheduleListExcelExporter 테스트")
class ScheduleListExcelExporterTest {

    private val exporter = ScheduleListExcelExporter()

    private fun item(
        confirmed: Boolean? = true,
        lastMonthRevenue: Long? = 3_500_000L,
    ) = ScheduleListItemDto(
        id = 1L,
        employeeId = 10L,
        employeeCode = "20030001",
        employeeName = "홍길동",
        branchName = "서울지점",
        employmentStatus = "재직",
        validData = "유효",
        valid = "GREEN",
        accountId = 100L,
        accountCode = "ACC001",
        accountName = "이마트 강남점",
        accountType = "대형마트",
        accountStatus = "정상",
        typeOfWork3 = "고정",
        typeOfWork4 = "상온",
        typeOfWork5 = "상시",
        startDate = LocalDate.of(2026, 5, 1),
        endDate = LocalDate.of(2026, 12, 31),
        confirmed = confirmed,
        costCenterCode = "A10010",
        lastMonthRevenue = lastMonthRevenue,
    )

    @Test
    @DisplayName("헤더 14개 + 데이터 행이 목록 컬럼 순서대로 생성")
    fun export_headersAndRows() {
        val result = exporter.export(listOf(item()), "진열스케줄.xlsx")

        assertThat(result.filename).isEqualTo("진열스케줄.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("진열스케줄")

        val header = sheet.getRow(0)
        assertThat(header.getCell(0).stringCellValue).isEqualTo("지점명")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("사번")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("성명")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("재직상태")
        assertThat(header.getCell(4).stringCellValue).isEqualTo("거래처코드")
        assertThat(header.getCell(5).stringCellValue).isEqualTo("거래명")
        assertThat(header.getCell(6).stringCellValue).isEqualTo("거래처유형")
        assertThat(header.getCell(7).stringCellValue).isEqualTo("근무형태3")
        assertThat(header.getCell(8).stringCellValue).isEqualTo("근무형태5")
        assertThat(header.getCell(9).stringCellValue).isEqualTo("시작일")
        assertThat(header.getCell(10).stringCellValue).isEqualTo("종료일")
        assertThat(header.getCell(11).stringCellValue).isEqualTo("거래처상태")
        assertThat(header.getCell(12).stringCellValue).isEqualTo("전월매출")
        assertThat(header.getCell(13).stringCellValue).isEqualTo("확정")

        val row = sheet.getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEqualTo("서울지점")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("20030001")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("홍길동")
        assertThat(row.getCell(3).stringCellValue).isEqualTo("재직")
        assertThat(row.getCell(4).stringCellValue).isEqualTo("ACC001")
        assertThat(row.getCell(5).stringCellValue).isEqualTo("이마트 강남점")
        assertThat(row.getCell(6).stringCellValue).isEqualTo("대형마트")
        assertThat(row.getCell(7).stringCellValue).isEqualTo("고정")
        assertThat(row.getCell(8).stringCellValue).isEqualTo("상시")
        assertThat(row.getCell(9).stringCellValue).isEqualTo("2026-05-01")
        assertThat(row.getCell(10).stringCellValue).isEqualTo("2026-12-31")
        assertThat(row.getCell(11).stringCellValue).isEqualTo("정상")
        assertThat(row.getCell(12).numericCellValue).isEqualTo(3_500_000.0)
        assertThat(row.getCell(13).stringCellValue).isEqualTo("확정")
        workbook.close()
    }

    @Test
    @DisplayName("빈 목록 - 헤더만 있는 시트 생성")
    fun export_emptyList() {
        val result = exporter.export(emptyList())

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 행만
        workbook.close()
    }

    @Test
    @DisplayName("null 전월매출은 빈 셀 / 미확정 표기")
    fun export_nullRevenueAndUnconfirmed() {
        val result = exporter.export(listOf(item(confirmed = false, lastMonthRevenue = null)))

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val row = workbook.getSheetAt(0).getRow(1)
        assertThat(row.getCell(12).stringCellValue).isEmpty()
        assertThat(row.getCell(13).stringCellValue).isEqualTo("미확정")
        workbook.close()
    }
}
