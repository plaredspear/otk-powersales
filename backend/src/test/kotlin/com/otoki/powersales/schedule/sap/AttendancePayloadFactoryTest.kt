package com.otoki.powersales.schedule.sap

import com.otoki.powersales.schedule.entity.SecondWorkType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AttendancePayloadFactory — 일반 출근(REGULAR) SAP 페이로드 빌더")
class AttendancePayloadFactoryTest {

    private val factory = AttendancePayloadFactory()
    private val today: LocalDate = LocalDate.of(2026, 5, 4)
    private val yesterday: LocalDate = today.minusDays(1)

    @Test
    @DisplayName("페이로드 키 셋 정합성 — 모든 키 8개가 정해진 매핑으로 출력된다")
    fun build_payloadKeysAndValues() {
        val row = baseRow().copy(
            workingDate = today,
            employeeCode = "100123",
            accountExternalKey = "ACC001",
            workingCategory1 = "근무",
            workingCategory2 = "행사",
            workingCategory3 = "정상",
            secondWorkType = null
        )

        val payload = factory.build(listOf(row), today)

        assertThat(payload.request).hasSize(1)
        val item = payload.request.single()
        assertThat(item.CompanyCode).isEqualTo("1000")
        assertThat(item.EmployeeCode).isEqualTo("100123")
        assertThat(item.SAPAccountCode).isEqualTo("ACC001")
        assertThat(item.WorkDate).isEqualTo("20260504")
        assertThat(item.WorkingCategory1).isEqualTo("근무")
        assertThat(item.WorkingCategory2).isEqualTo("행사")
        assertThat(item.WorkingCategory3).isEqualTo("정상")
        assertThat(item.WorkingCategory4).isNull()
    }

    @Test
    @DisplayName("WorkDate 형식 — yyyyMMdd 8자리 문자열")
    fun build_workDateFormat() {
        val row = baseRow().copy(workingDate = LocalDate.of(2026, 1, 9))
        val payload = factory.build(listOf(row), today)
        assertThat(payload.request.single().WorkDate).isEqualTo("20260109")
    }

    @Test
    @DisplayName("WorkingCategory4 — today row 는 secondWorkType 이 있어도 null 출력")
    fun build_workingCategory4_todayIsAlwaysNull() {
        val row = baseRow().copy(workingDate = today, secondWorkType = SecondWorkType.ROOM_TEMP)
        val payload = factory.build(listOf(row), today)
        assertThat(payload.request.single().WorkingCategory4).isNull()
    }

    @Test
    @DisplayName("WorkingCategory4 — yesterday 보정 row 는 secondWorkType displayName 그대로 전달")
    fun build_workingCategory4_yesterdayCarriesSecondWorkType() {
        val row = baseRow().copy(workingDate = yesterday, secondWorkType = SecondWorkType.ROOM_TEMP)
        val payload = factory.build(listOf(row), today)
        assertThat(payload.request.single().WorkingCategory4).isEqualTo("상온")
    }

    @Test
    @DisplayName("CompanyCode — SapConstants.OTOKI_COMPANY_CODE 고정 \"1000\"")
    fun build_companyCodeIsConstant() {
        val payload = factory.build(listOf(baseRow()), today)
        assertThat(payload.request.single().CompanyCode).isEqualTo("1000")
    }

    private fun baseRow() = AttendanceSapPayloadRow(
        attendanceLogId = 1L,
        workingDate = today,
        employeeCode = "EMP001",
        accountExternalKey = "ACC001",
        workingCategory1 = "근무",
        workingCategory2 = null,
        workingCategory3 = null,
        secondWorkType = null
    )
}
