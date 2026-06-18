package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListItem
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("EmployeeListExcelExporter 테스트")
class EmployeeListExcelExporterTest {

    private val exporter = EmployeeListExcelExporter()

    private fun item(
        employeeCode: String? = "10000001",
        appLoginActive: Boolean? = true,
    ) = EmployeeListItem(
        id = 1L,
        employeeCode = employeeCode,
        name = "홍길동",
        status = "재직",
        gender = "여",
        orgName = "서울지점",
        costCenterCode = "A001",
        role = "여사원",
        startDate = "2020-01-01",
        endDate = null,
        appLoginActive = appLoginActive,
        workPhone = "02-123-4567",
        jikchak = "팀원",
        jikwee = "사원",
        jikgub = "5급",
        jobCode = "판촉직",
        appointmentDate = "2021-03-01",
        ordDetailNode = "정기발령",
        jikjong = "판촉",
        workEmail = "hong@otoki.com",
        phone = "010-1234-5678",
        age = "30",
        yearsOfService = "6",
    )

    @Test
    @DisplayName("헤더 20개 + 데이터 행이 목록 컬럼 순서대로 생성")
    fun export_headersAndRows() {
        val result = exporter.export(listOf(item()), "여사원현황.xlsx")

        assertThat(result.filename).isEqualTo("여사원현황.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("여사원현황")

        val header = sheet.getRow(0)
        assertThat(header.getCell(0).stringCellValue).isEqualTo("사번")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("이름")
        assertThat(header.getCell(11).stringCellValue).isEqualTo("이메일(회사)")
        assertThat(header.getCell(12).stringCellValue).isEqualTo("전화번호(HP)")
        assertThat(header.getCell(17).stringCellValue).isEqualTo("만나이")
        assertThat(header.getCell(18).stringCellValue).isEqualTo("근속년수")
        assertThat(header.getCell(19).stringCellValue).isEqualTo("앱활성")

        val row = sheet.getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEqualTo("10000001")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("홍길동")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("여")
        assertThat(row.getCell(3).stringCellValue).isEqualTo("재직")
        assertThat(row.getCell(11).stringCellValue).isEqualTo("hong@otoki.com")
        assertThat(row.getCell(12).stringCellValue).isEqualTo("010-1234-5678")
        assertThat(row.getCell(17).stringCellValue).isEqualTo("30")
        assertThat(row.getCell(18).stringCellValue).isEqualTo("6")
        assertThat(row.getCell(19).stringCellValue).isEqualTo("활성")
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
    @DisplayName("null 사번은 빈 셀 / appLoginActive=false 는 비활성 표기")
    fun export_nullAndInactive() {
        val result = exporter.export(listOf(item(employeeCode = null, appLoginActive = false)))

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val row = workbook.getSheetAt(0).getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEmpty()
        assertThat(row.getCell(19).stringCellValue).isEqualTo("비활성")
        workbook.close()
    }
}
