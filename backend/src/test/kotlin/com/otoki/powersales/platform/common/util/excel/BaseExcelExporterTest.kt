package com.otoki.powersales.platform.common.util.excel

import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("BaseExcelExporter 테스트")
class BaseExcelExporterTest {

    private data class Sample(val name: String, val age: Int)

    private val exporter = object : BaseExcelExporter<Sample>() {
        override val sheetName = "샘플시트"
        override val defaultFilename = "샘플.xlsx"
        override val headers = listOf("이름", "나이")
        override fun writeRow(row: Row, item: Sample) {
            row.createCell(0).setCellValue(item.name)
            row.createCell(1).setCellValue(item.age.toDouble())
        }
    }

    @Test
    @DisplayName("헤더 1행 + 데이터 N행 + 시트명 + 파일명 생성")
    fun export_producesHeaderAndRows() {
        val result = exporter.export(listOf(Sample("홍길동", 30), Sample("김철수", 25)))

        assertThat(result.filename).isEqualTo("샘플.xlsx")
        // xlsx (ZIP) magic "PK"
        assertThat(result.bytes[0]).isEqualTo('P'.code.toByte())
        assertThat(result.bytes[1]).isEqualTo('K'.code.toByte())

        XSSFWorkbook(ByteArrayInputStream(result.bytes)).use { wb ->
            val sheet = wb.getSheet("샘플시트")
            assertThat(sheet).isNotNull
            assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
            assertThat(sheet.getRow(0).getCell(1).stringCellValue).isEqualTo("나이")
            assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("홍길동")
            assertThat(sheet.getRow(1).getCell(1).numericCellValue).isEqualTo(30.0)
            assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("김철수")
            assertThat(sheet.lastRowNum).isEqualTo(2) // 헤더 + 2행
        }
    }

    @Test
    @DisplayName("동적 파일명 오버로드 - 전달한 파일명으로 ExcelResult 생성")
    fun export_withDynamicFilename() {
        val result = exporter.export(listOf(Sample("홍길동", 30)), "샘플_2026-06.xlsx")

        assertThat(result.filename).isEqualTo("샘플_2026-06.xlsx")
        XSSFWorkbook(ByteArrayInputStream(result.bytes)).use { wb ->
            assertThat(wb.getSheet("샘플시트").getRow(1).getCell(0).stringCellValue).isEqualTo("홍길동")
        }
    }

    @Test
    @DisplayName("빈 목록 - 헤더만 있는 워크북 생성")
    fun export_emptyList_headerOnly() {
        val result = exporter.export(emptyList())

        XSSFWorkbook(ByteArrayInputStream(result.bytes)).use { wb ->
            val sheet = wb.getSheet("샘플시트")
            assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 행만
            assertThat(sheet.getRow(0).getCell(0).stringCellValue).isEqualTo("이름")
        }
    }
}
