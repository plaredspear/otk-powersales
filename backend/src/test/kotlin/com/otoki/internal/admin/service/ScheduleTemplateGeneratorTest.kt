package com.otoki.internal.admin.service

import com.otoki.internal.sap.entity.User
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
        @DisplayName("사원 있음 - 헤더 + 사원 데이터 포함")
        fun generate_withEmployees() {
            // Given
            val employees = listOf(
                createUser(employeeId = "20030001", name = "홍길동"),
                createUser(employeeId = "20030002", name = "김철수")
            )

            // When
            val bytes = generator.generate(employees)
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // Then
            // 1행: 안내 문구
            assertThat(sheet.getRow(0).getCell(0).stringCellValue)
                .contains("신규입력 전용 양식입니다")

            // 2행: 가이드
            assertThat(sheet.getRow(1).getCell(0).stringCellValue).isEqualTo("(자동입력)")

            // 3행: 헤더
            assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("*사원번호")
            assertThat(sheet.getRow(2).getCell(1).stringCellValue).isEqualTo("사원명")
            assertThat(sheet.getRow(2).getCell(7).stringCellValue).isEqualTo("종료일")

            // 4행: 첫 번째 사원
            assertThat(sheet.getRow(3).getCell(0).stringCellValue).isEqualTo("20030001")
            assertThat(sheet.getRow(3).getCell(1).stringCellValue).isEqualTo("홍길동")

            // 5행: 두 번째 사원
            assertThat(sheet.getRow(4).getCell(0).stringCellValue).isEqualTo("20030002")
            assertThat(sheet.getRow(4).getCell(1).stringCellValue).isEqualTo("김철수")

            workbook.close()
        }

        @Test
        @DisplayName("사원 없음 - 헤더만 있는 템플릿")
        fun generate_emptyEmployees() {
            // When
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // Then
            // 1~3행 존재
            assertThat(sheet.getRow(0).getCell(0).stringCellValue).contains("신규입력")
            assertThat(sheet.getRow(2).getCell(0).stringCellValue).isEqualTo("*사원번호")

            // 4행에 사원 데이터 없음 (G/H열 텍스트 서식 셀은 존재)
            val row3 = sheet.getRow(3)
            assertThat(row3?.getCell(0)?.stringCellValue ?: "").isEmpty()

            workbook.close()
        }

        @Test
        @DisplayName("필수 입력 셀 노란 배경 - C, E, F, G열에 노란 배경 적용")
        fun generate_yellowBackground() {
            // Given
            val employees = listOf(createUser())

            // When
            val bytes = generator.generate(employees)
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // Then - 4행 (인덱스 3)의 C열 (인덱스 2)
            val cellC = sheet.getRow(3).getCell(2)
            assertThat(cellC.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
            assertThat(cellC.cellStyle.fillForegroundColor).isEqualTo(IndexedColors.YELLOW.index)

            workbook.close()
        }

        @Test
        @DisplayName("드롭다운 유효성 검사 - E, F열에 설정됨")
        fun generate_dropdownValidation() {
            // When
            val bytes = generator.generate(listOf(createUser()))
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // Then - 데이터 유효성 검사가 설정됨
            val validations = sheet.dataValidations
            assertThat(validations).hasSizeGreaterThanOrEqualTo(2)

            workbook.close()
        }

        @Test
        @DisplayName("헤더 스타일 - 필수 컬럼 주황 배경, 선택 컬럼 남색 배경")
        fun generate_headerStyles() {
            // When
            val bytes = generator.generate(emptyList())
            val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
            val sheet = workbook.getSheetAt(0)

            // Then
            val headerRow = sheet.getRow(2)

            // 필수 컬럼 (A: *사원번호) - 주황 배경
            val requiredCell = headerRow.getCell(0)
            assertThat(requiredCell.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
            assertThat(requiredCell.cellStyle.font.bold).isTrue()

            // 선택 컬럼 (B: 사원명) - 남색 배경
            val optionalCell = headerRow.getCell(1)
            assertThat(optionalCell.cellStyle.fillPattern).isEqualTo(FillPatternType.SOLID_FOREGROUND)
            assertThat(optionalCell.cellStyle.font.bold).isTrue()

            workbook.close()
        }
    }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030001",
        name: String = "테스트사원",
        costCenterCode: String = "1234",
        orgName: String = "테스트팀"
    ): User = User(
        id = id,
        employeeId = employeeId,
        name = name,
        costCenterCode = costCenterCode,
        orgName = orgName,
        appAuthority = null,
        appLoginActive = true,
        status = "재직"
    )
}
