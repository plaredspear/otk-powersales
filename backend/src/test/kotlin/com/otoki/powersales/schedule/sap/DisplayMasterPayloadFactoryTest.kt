package com.otoki.powersales.schedule.sap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@DisplayName("DisplayMasterPayloadFactory — 진열 마스터(DISPLAY) SAP 페이로드 빌더")
class DisplayMasterPayloadFactoryTest {

    private val factory = DisplayMasterPayloadFactory()
    private val objectMapper = ObjectMapper()
    private val today: LocalDate = LocalDate.of(2026, 5, 4)

    @Test
    @DisplayName("페이로드 키 셋 정합성 — 키 7개 출력 (Cat2/Cat4 부재)")
    fun build_payloadKeysAndValues() {
        val row = baseRow().copy(
            employeeCode = "100123",
            accountExternalKey = "ACC001",
            typeOfWork1 = "진열",
            typeOfWork3 = "정상",
            typeOfWork5 = "전문판촉팀"
        )

        val payload = factory.build(listOf(row), today)
        val json = objectMapper.writeValueAsString(payload)

        assertThat(payload.request).hasSize(1)
        val item = payload.request.single()
        assertThat(item.CompanyCode).isEqualTo("1000")
        assertThat(item.EmployeeCode).isEqualTo("100123")
        assertThat(item.SAPAccountCode).isEqualTo("ACC001")
        assertThat(item.WorkDate).isEqualTo("20260504")
        assertThat(item.WorkingCategory1).isEqualTo("진열")
        assertThat(item.WorkingCategory3).isEqualTo("정상")
        assertThat(item.WorkingCategory5).isEqualTo("전문판촉팀")

        // P1 의 WorkingCategory2/4 키는 출력 자체가 없어야 한다
        assertThat(json).doesNotContain("WorkingCategory2")
        assertThat(json).doesNotContain("WorkingCategory4")
    }

    @Test
    @DisplayName("WorkDate — 배치 실행 시점 today 의 yyyyMMdd")
    fun build_workDateIsBatchToday() {
        val row = baseRow()
        val payload = factory.build(listOf(row), LocalDate.of(2026, 1, 9))
        assertThat(payload.request.single().WorkDate).isEqualTo("20260109")
    }

    @Test
    @DisplayName("CompanyCode — SapConstants.OTOKI_COMPANY_CODE 고정 \"1000\"")
    fun build_companyCodeIsConstant() {
        val payload = factory.build(listOf(baseRow()), today)
        assertThat(payload.request.single().CompanyCode).isEqualTo("1000")
    }

    private fun baseRow() = DisplayMasterSapPayloadRow(
        displayWorkScheduleId = 1L,
        employeeCode = "EMP001",
        accountExternalKey = "ACC001",
        typeOfWork1 = "진열",
        typeOfWork3 = "정상",
        typeOfWork5 = "전문판촉팀"
    )
}
