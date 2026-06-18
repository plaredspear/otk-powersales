package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryResponse
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

@DisplayName("PPTHistoryExcelExporter 테스트")
class PPTHistoryExcelExporterTest {

    private val exporter = PPTHistoryExcelExporter()

    private fun item(
        name: String? = "PH0000001",
        oldValue: ProfessionalPromotionTeamType? = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
        newValue: ProfessionalPromotionTeamType? = ProfessionalPromotionTeamType.RAMEN_SALE,
    ) = PPTMasterHistoryResponse(
        id = 1L,
        name = name,
        employeeId = 10L,
        employeeName = "홍길동",
        employeeCode = "12345678",
        orgName = "서울지점",
        oldValue = oldValue,
        newValue = newValue,
        changedAt = LocalDateTime.of(2026, 5, 1, 14, 30),
    )

    @Test
    @DisplayName("헤더 7개 + 데이터 행이 목록 컬럼 순서대로 생성")
    fun export_headersAndRows() {
        val result = exporter.export(listOf(item()), "전문행사조이력.xlsx")

        assertThat(result.filename).isEqualTo("전문행사조이력.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("전문행사조이력")

        val header = sheet.getRow(0)
        assertThat(header.getCell(0).stringCellValue).isEqualTo("전문행사조 이력번호")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("소속")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("사번")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("사원")
        assertThat(header.getCell(4).stringCellValue).isEqualTo("변경 전")
        assertThat(header.getCell(5).stringCellValue).isEqualTo("변경 후")
        assertThat(header.getCell(6).stringCellValue).isEqualTo("변경 시점")

        val row = sheet.getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEqualTo("PH0000001")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("서울지점")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("12345678")
        assertThat(row.getCell(3).stringCellValue).isEqualTo("홍길동")
        assertThat(row.getCell(4).stringCellValue)
            .isEqualTo(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING.displayName)
        assertThat(row.getCell(5).stringCellValue)
            .isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE.displayName)
        assertThat(row.getCell(6).stringCellValue).isEqualTo("2026-05-01 14:30")
        workbook.close()
    }

    @Test
    @DisplayName("빈 목록 - 헤더만 있는 시트 생성")
    fun export_emptyList() {
        val result = exporter.export(emptyList())

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0) // 헤더 행만
        workbook.close()
    }

    @Test
    @DisplayName("null 이력번호 / 변경 전 값은 빈 셀로 처리")
    fun export_nullFields() {
        val result = exporter.export(listOf(item(name = null, oldValue = null)))

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val row = workbook.getSheetAt(0).getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEmpty()
        assertThat(row.getCell(4).stringCellValue).isEmpty()
        workbook.close()
    }
}
