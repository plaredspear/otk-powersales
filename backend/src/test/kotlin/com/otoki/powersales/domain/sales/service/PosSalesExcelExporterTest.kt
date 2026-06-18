package com.otoki.powersales.domain.sales.service

import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("PosSalesExcelExporter 테스트")
class PosSalesExcelExporterTest {

    private val exporter = PosSalesExcelExporter()

    private fun item(
        productCode: String = "01101123",
        barcode: String? = "8801045123456",
    ) = PosSalesResponse.ProductSales(
        productCode = productCode,
        productName = "갈릭 아이올리소스 240g",
        barcode = barcode,
        amount = 3500L,
        quantity = 10L,
    )

    @Test
    @DisplayName("헤더 5개 + 데이터 행이 목록 컬럼 순서대로 생성 + 파일명에 거래처/연월")
    fun export_headersAndRows() {
        val result = exporter.export("사과마을", "202602", listOf(item()))

        assertThat(result.filename).isEqualTo("POS매출_사과마을_202602.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("POS매출")

        val header = sheet.getRow(0)
        assertThat(header.getCell(0).stringCellValue).isEqualTo("제품코드")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("제품명")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("바코드")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("납품수량(EA)")
        assertThat(header.getCell(4).stringCellValue).isEqualTo("금액(원)")

        val row = sheet.getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEqualTo("01101123")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("갈릭 아이올리소스 240g")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("8801045123456")
        assertThat(row.getCell(3).numericCellValue).isEqualTo(10.0)
        assertThat(row.getCell(4).numericCellValue).isEqualTo(3500.0)
        workbook.close()
    }

    @Test
    @DisplayName("빈 목록 - 헤더만 있는 시트 생성")
    fun export_emptyList() {
        val result = exporter.export("사과마을", "202602", emptyList())

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        assertThat(workbook.getSheetAt(0).lastRowNum).isEqualTo(0) // 헤더 행만
        workbook.close()
    }

    @Test
    @DisplayName("null 바코드는 빈 셀 / 빈 거래처명은 '거래처' fallback + 파일명 금지문자 치환")
    fun export_nullBarcodeAndFilenameSanitize() {
        val result = exporter.export("이마트/강남", "202602", listOf(item(barcode = null)))

        // 파일명 금지문자(/) 가 _ 로 치환
        assertThat(result.filename).isEqualTo("POS매출_이마트_강남_202602.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val row = workbook.getSheetAt(0).getRow(1)
        assertThat(row.getCell(2).stringCellValue).isEmpty()
        workbook.close()

        val blankNameResult = exporter.export("", "202602", listOf(item()))
        assertThat(blankNameResult.filename).isEqualTo("POS매출_거래처_202602.xlsx")
    }
}
