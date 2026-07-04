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
        status: String? = "재직",
        endDate: String? = null,
        professionalPromotionTeam: String = "일반",
        workType1: String? = "진열",
        workType3: String? = "고정",
        workAccountName: String? = "테스트마트",
        workAccountCode: String? = "ACC001",
    ) = EmployeeListItem(
        id = 1L,
        employeeCode = employeeCode,
        name = "홍길동",
        status = status,
        gender = "여",
        orgName = "서울지점",
        costCenterCode = "A001",
        role = "여사원",
        startDate = "2020-01-01",
        endDate = endDate,
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
        professionalPromotionTeam = professionalPromotionTeam,
        workType1 = workType1,
        workType3 = workType3,
        workAccountName = workAccountName,
        workAccountCode = workAccountCode,
    )

    @Test
    @DisplayName("헤더 19개 + 데이터 행이 목록 컬럼 순서대로 생성 (직책·이메일·전화번호·만나이 제외)")
    fun export_headersAndRows() {
        val result = exporter.export(
            listOf(item(professionalPromotionTeam = "라면세일조", workType1 = "진열", workType3 = "고정")),
            "여사원현황.xlsx",
        )

        assertThat(result.filename).isEqualTo("여사원현황.xlsx")

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("여사원현황")

        val header = sheet.getRow(0)
        assertThat(header.getCell(0).stringCellValue).isEqualTo("사번")
        assertThat(header.getCell(1).stringCellValue).isEqualTo("이름")
        assertThat(header.getCell(2).stringCellValue).isEqualTo("상태")
        assertThat(header.getCell(3).stringCellValue).isEqualTo("소속")
        assertThat(header.getCell(4).stringCellValue).isEqualTo("전문행사조")
        assertThat(header.getCell(5).stringCellValue).isEqualTo("근무형태1")
        assertThat(header.getCell(6).stringCellValue).isEqualTo("근무형태3")
        assertThat(header.getCell(7).stringCellValue).isEqualTo("근무거래처")
        assertThat(header.getCell(8).stringCellValue).isEqualTo("거래처코드")
        assertThat(header.getCell(9).stringCellValue).isEqualTo("권한")
        assertThat(header.getCell(10).stringCellValue).isEqualTo("직종명")
        assertThat(header.getCell(11).stringCellValue).isEqualTo("직위")
        assertThat(header.getCell(12).stringCellValue).isEqualTo("직급")
        assertThat(header.getCell(13).stringCellValue).isEqualTo("발령일")
        assertThat(header.getCell(17).stringCellValue).isEqualTo("근속년수")
        assertThat(header.getCell(18).stringCellValue).isEqualTo("앱활성")

        val row = sheet.getRow(1)
        assertThat(row.getCell(0).stringCellValue).isEqualTo("10000001")
        assertThat(row.getCell(1).stringCellValue).isEqualTo("홍길동")
        assertThat(row.getCell(2).stringCellValue).isEqualTo("재직")
        assertThat(row.getCell(3).stringCellValue).isEqualTo("서울지점")
        assertThat(row.getCell(4).stringCellValue).isEqualTo("라면세일조")
        assertThat(row.getCell(5).stringCellValue).isEqualTo("진열")
        assertThat(row.getCell(6).stringCellValue).isEqualTo("고정")
        assertThat(row.getCell(7).stringCellValue).isEqualTo("테스트마트")
        assertThat(row.getCell(8).stringCellValue).isEqualTo("ACC001")
        assertThat(row.getCell(9).stringCellValue).isEqualTo("여사원")
        assertThat(row.getCell(17).stringCellValue).isEqualTo("6")
        assertThat(row.getCell(18).stringCellValue).isEqualTo("활성")
        workbook.close()
    }

    @Test
    @DisplayName("근무형태1/근무형태3/근무거래처/거래처코드 null 은 '-' 로 출력")
    fun export_workInfoNull() {
        val result = exporter.export(
            listOf(item(workType1 = null, workType3 = null, workAccountName = null, workAccountCode = null)),
        )

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val row = workbook.getSheetAt(0).getRow(1)
        assertThat(row.getCell(5).stringCellValue).isEqualTo("-") // 근무형태1
        assertThat(row.getCell(6).stringCellValue).isEqualTo("-") // 근무형태3
        assertThat(row.getCell(7).stringCellValue).isEqualTo("-") // 근무거래처
        assertThat(row.getCell(8).stringCellValue).isEqualTo("-") // 거래처코드
        workbook.close()
    }

    @Test
    @DisplayName("재직 사원은 퇴사일 미표시 / 퇴직 사원은 퇴사일 표시")
    fun export_endDateHiddenWhenActive() {
        val result = exporter.export(
            listOf(
                item(status = "재직", endDate = "2025-12-31"),
                item(status = "퇴직", endDate = "2025-12-31"),
            ),
        )

        val workbook = XSSFWorkbook(ByteArrayInputStream(result.bytes))
        val sheet = workbook.getSheetAt(0)
        // 퇴사일은 16번 컬럼
        assertThat(sheet.getRow(1).getCell(16).stringCellValue).isEmpty()
        assertThat(sheet.getRow(2).getCell(16).stringCellValue).isEqualTo("2025-12-31")
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
        assertThat(row.getCell(18).stringCellValue).isEqualTo("비활성")
        workbook.close()
    }
}
