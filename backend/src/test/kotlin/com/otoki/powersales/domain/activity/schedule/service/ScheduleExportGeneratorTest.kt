package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.activity.schedule.service.ScheduleExportGenerator
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("ScheduleExportGenerator 테스트")
class ScheduleExportGeneratorTest {

    private val generator = ScheduleExportGenerator()

    @Test
    @DisplayName("헤더 11개 + 데이터 행 정확히 생성")
    fun generate_headersAndRows() {
        val employee = Employee(id = 1L, employeeCode = "20030001", name = "홍길동")
        val account = Account(id = 100, externalKey = "ACC001", name = "이마트 강남점")
        val schedule = DisplayWorkSchedule(
            id = 1L,
            employee = employee,
            account = account,
            typeOfWork3 = TypeOfWork3.FIXED,
            typeOfWork4 = SecondWorkType.ROOM_TEMP,
            typeOfWork5 = TypeOfWork5.REGULAR,
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 12, 31),
            confirmed = true,
            costCenterCode = "A10010",
            lastMonthRevenue = BigDecimal("3500000")
        )

        val bytes = generator.generate(listOf(schedule))

        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.sheetName).isEqualTo("진열스케줄")

        val headerRow = sheet.getRow(0)
        assertThat(headerRow.getCell(0).stringCellValue).isEqualTo("사원번호")
        assertThat(headerRow.getCell(1).stringCellValue).isEqualTo("사원명")
        assertThat(headerRow.getCell(2).stringCellValue).isEqualTo("거래처코드")
        assertThat(headerRow.getCell(3).stringCellValue).isEqualTo("거래처명")
        assertThat(headerRow.getCell(4).stringCellValue).isEqualTo("근무유형3")
        assertThat(headerRow.getCell(5).stringCellValue).isEqualTo("근무유형5")
        assertThat(headerRow.getCell(6).stringCellValue).isEqualTo("시작일")
        assertThat(headerRow.getCell(7).stringCellValue).isEqualTo("종료일")
        assertThat(headerRow.getCell(8).stringCellValue).isEqualTo("확정")
        assertThat(headerRow.getCell(9).stringCellValue).isEqualTo("조직코드")
        assertThat(headerRow.getCell(10).stringCellValue).isEqualTo("전월매출")

        val dataRow = sheet.getRow(1)
        assertThat(dataRow.getCell(0).stringCellValue).isEqualTo("20030001")
        assertThat(dataRow.getCell(1).stringCellValue).isEqualTo("홍길동")
        assertThat(dataRow.getCell(2).stringCellValue).isEqualTo("ACC001")
        assertThat(dataRow.getCell(3).stringCellValue).isEqualTo("이마트 강남점")
        assertThat(dataRow.getCell(4).stringCellValue).isEqualTo("고정")
        assertThat(dataRow.getCell(5).stringCellValue).isEqualTo("상시")
        assertThat(dataRow.getCell(6).stringCellValue).isEqualTo("2026-05-01")
        assertThat(dataRow.getCell(7).stringCellValue).isEqualTo("2026-12-31")
        assertThat(dataRow.getCell(8).stringCellValue).isEqualTo("확정")
        assertThat(dataRow.getCell(9).stringCellValue).isEqualTo("A10010")
        assertThat(dataRow.getCell(10).numericCellValue).isEqualTo(3500000.0)
        workbook.close()
    }

    @Test
    @DisplayName("빈 목록 - 헤더만 있는 시트 생성")
    fun generate_emptyList() {
        val bytes = generator.generate(emptyList())

        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        val sheet = workbook.getSheetAt(0)
        assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 행만
        workbook.close()
    }

    @Test
    @DisplayName("null 필드 - 빈 문자열 / 빈 셀로 처리")
    fun generate_nullFields() {
        val schedule = DisplayWorkSchedule(
            id = 1L,
            employee = null,
            account = null,
            typeOfWork3 = null,
            typeOfWork5 = null,
            startDate = null,
            endDate = null,
            confirmed = false,
            costCenterCode = null,
            lastMonthRevenue = null
        )

        val bytes = generator.generate(listOf(schedule))

        val workbook = XSSFWorkbook(ByteArrayInputStream(bytes))
        val sheet = workbook.getSheetAt(0)
        val dataRow = sheet.getRow(1)
        assertThat(dataRow.getCell(0).stringCellValue).isEmpty()
        assertThat(dataRow.getCell(8).stringCellValue).isEqualTo("미확정")
        assertThat(dataRow.getCell(10).stringCellValue).isEmpty()
        workbook.close()
    }
}
