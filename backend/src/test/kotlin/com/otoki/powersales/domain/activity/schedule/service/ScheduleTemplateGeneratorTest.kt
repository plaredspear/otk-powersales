package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.service.ScheduleTemplateGenerator
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("ScheduleTemplateGenerator 테스트")
class ScheduleTemplateGeneratorTest {

    private val generator = ScheduleTemplateGenerator()

    @Nested
    @DisplayName("generate - Excel 생성")
    inner class GenerateTests {

        @Test
        @DisplayName("11개 컬럼 헤더 - 레거시와 동일한 헤더명")
        fun generate_headerColumns() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val headerRow = sheet.getRow(2)
            assertThat(headerRow.getCell(0).stringCellValue).isEqualTo("소속")
            assertThat(headerRow.getCell(1).stringCellValue).isEqualTo("*사번(필수입력)")
            assertThat(headerRow.getCell(2).stringCellValue).isEqualTo("이름")
            assertThat(headerRow.getCell(3).stringCellValue).isEqualTo("직위")
            assertThat(headerRow.getCell(4).stringCellValue).isEqualTo("*거래처코드(필수입력)")
            assertThat(headerRow.getCell(5).stringCellValue).isEqualTo("거래처명")
            assertThat(headerRow.getCell(6).stringCellValue).isEqualTo("*근무형태3(필수입력)")
            assertThat(headerRow.getCell(7).stringCellValue).isEqualTo("*근무형태4(필수입력)")
            assertThat(headerRow.getCell(8).stringCellValue).isEqualTo("*근무형태5(필수입력)")
            assertThat(headerRow.getCell(9).stringCellValue).isEqualTo("*시작일(필수입력)")
            assertThat(headerRow.getCell(10).stringCellValue).isEqualTo("*종료일(선택입력)")

            workbook.close()
        }

        @Test
        @DisplayName("1행 안내문구 - 레거시와 동일")
        fun generate_noticeText() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            assertThat(sheet.getRow(0).getCell(0).stringCellValue)
                .isEqualTo("신규입력만 가능합니다. 수정이 필요한 경우 화면단에서 진행해주세요.(업로드시에는 파일 복호화를 해주시기 바랍니다.)")

            workbook.close()
        }

        @Test
        @DisplayName("1행 병합 없음 - SF 레거시(writeExcelFile)는 A1 셀에만 값을 넣고 병합하지 않음")
        fun generate_noMergedRegion() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            assertThat(sheet.numMergedRegions).isEqualTo(0)

            workbook.close()
        }

        @Test
        @DisplayName("첫 행 고정 - SF 레거시 stickyRowsCount: 1 정합")
        fun generate_freezePane() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val pane = sheet.paneInformation
            assertThat(pane).isNotNull
            assertThat(pane.horizontalSplitPosition.toInt()).isEqualTo(1)
            assertThat(pane.verticalSplitPosition.toInt()).isEqualTo(0)

            workbook.close()
        }

        @Test
        @DisplayName("2행 가이드 - G~K열에만 텍스트, A~F열 빈 문자열")
        fun generate_guideTexts() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val guideRow = sheet.getRow(1)
            // A~F: 빈 문자열
            for (i in 0..5) {
                assertThat(guideRow.getCell(i).stringCellValue).isEmpty()
            }
            // G~K: 가이드 텍스트
            assertThat(guideRow.getCell(6).stringCellValue).isEqualTo("고정 or 격고 or 순회")
            assertThat(guideRow.getCell(7).stringCellValue).isEqualTo("상온 or 냉동/냉장")
            assertThat(guideRow.getCell(8).stringCellValue).isEqualTo("상시 or 임시")
            assertThat(guideRow.getCell(9).stringCellValue).isEqualTo("yyyy-mm-dd")
            assertThat(guideRow.getCell(10).stringCellValue).isEqualTo("yyyy-mm-dd")

            workbook.close()
        }

        @Test
        @DisplayName("데이터 프리필 - 소속, 사번, 이름, 직위 4개 자동 채움")
        fun generate_prefillData() {
            val employees = listOf(
                createEmployee(employeeCode = "20030001", name = "홍길동", orgName = "서울지점", jikwee = "사원"),
                createEmployee(id = 2L, employeeCode = "20030002", name = "김철수", orgName = "부산지점", jikwee = null)
            )

            val bytes = generator.generate(employees)
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // 4행: 첫 번째 사원
            assertThat(sheet.getRow(3).getCell(0).stringCellValue).isEqualTo("서울지점")  // A: 소속
            assertThat(sheet.getRow(3).getCell(1).stringCellValue).isEqualTo("20030001") // B: 사번
            assertThat(sheet.getRow(3).getCell(2).stringCellValue).isEqualTo("홍길동")    // C: 이름
            assertThat(sheet.getRow(3).getCell(3).stringCellValue).isEqualTo("사원")      // D: 직위

            // 5행: 두 번째 사원 (직위 null → 빈 문자열)
            assertThat(sheet.getRow(4).getCell(0).stringCellValue).isEqualTo("부산지점")
            assertThat(sheet.getRow(4).getCell(3).stringCellValue).isEqualTo("")

            // E~K: 사용자 입력 컬럼은 비어있음
            assertThat(sheet.getRow(3).getCell(4).stringCellValue ?: "").isEmpty()

            workbook.close()
        }

        @Test
        @DisplayName("노란 배경 - B, E, G, H, I, J 셀")
        fun generate_yellowBackground() {
            val employees = listOf(createEmployee())

            val bytes = generator.generate(employees)
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val dataRow = sheet.getRow(3)
            val yellowIndices = listOf(1, 4, 6, 7, 8, 9) // B, E, G, H, I, J
            for (colIdx in yellowIndices) {
                val cell = dataRow.getCell(colIdx)
                assertThat(cell.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
                assertThat(cell.cellStyle.fillForegroundColor).isEqualTo(IndexedColors.YELLOW.index)
            }

            workbook.close()
        }

        @Test
        @DisplayName("헤더 스타일 - * 접두사 주황 배경, 없으면 남색 배경")
        fun generate_headerStyles() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val headerRow = sheet.getRow(2)

            // 소속 (A): 남색 배경 (no *)
            val optionalCell = headerRow.getCell(0)
            assertThat(optionalCell.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
            assertThat(optionalCell.cellStyle.font.bold).isTrue()

            // *사번(필수입력) (B): 주황 배경
            val requiredCell = headerRow.getCell(1)
            assertThat(requiredCell.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
            assertThat(requiredCell.cellStyle.font.bold).isTrue()

            workbook.close()
        }

        @Test
        @DisplayName("사원 없음 - 헤더만 있는 템플릿")
        fun generate_emptyEmployees() {
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            assertThat(sheet.getRow(0).getCell(0).stringCellValue).contains("신규입력")
            assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("소속")

            workbook.close()
        }

        @Test
        @DisplayName("드롭다운 없음 - 데이터 유효성 검사 미설정")
        fun generate_noDropdownValidation() {
            val bytes = generator.generate(listOf(createEmployee()))
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            val validations = sheet.dataValidations
            assertThat(validations).isEmpty()

            workbook.close()
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "20030001",
        name: String = "테스트사원",
        costCenterCode: String = "1234",
        orgName: String = "테스트팀",
        jikwee: String? = "사원"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        costCenterCode = costCenterCode,
        orgName = orgName,
        jikwee = jikwee,
        role = null,
        appLoginActive = true,
        status = "재직"
    )
}
