package com.otoki.powersales.sap.inbound.dto

import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoDetail
import com.otoki.powersales.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterDetail
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import com.otoki.powersales.sap.inbound.dto.employee.FailureItem as EmployeeFailureItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem as SalesFailureItem

/**
 * SAP 인바운드 RESULT_DETAIL 페이로드의 직렬화 키 검증. (Spec #580 P1-B)
 *
 * 글로벌 Jackson 기본 NamingStrategy 가 LOWER_CAMEL_CASE 로 바뀌어도 SAP 응답 호환을 위해
 * 각 Detail DTO 의 `@JsonNaming(SnakeCaseStrategy)` 어노테이션으로 SnakeCase 직렬화가 보장되어야 한다.
 */
@DisplayName("SAP Detail DTO @JsonNaming 직렬화 검증")
class SapDetailJsonNamingTest {

    private val mapper = JsonMapper.builder().build()

    @Test
    @DisplayName("AttendInfoDetail 와 중첩 ScheduleConversionSummary 가 snake_case 로 직렬화된다")
    fun attendInfoDetail_serializes_snake_case() {
        val detail = AttendInfoDetail(
            successCount = 100,
            failureCount = 2,
            failures = listOf(SalesFailureItem(identifier = "K-1", reason = "duplicate")),
            chunks = listOf(ChunkResult(index = 0, status = "success", count = 50)),
            scheduleConversion = ScheduleConversionSummary(
                convertedScheduleCount = 30,
                deletedScheduleCount = 5,
                skippedEmployeeNotFound = 2,
                skippedJobFilter = 0,
                skippedAttendTypeFilter = 0,
                skippedIdempotent = 0
            )
        )

        val json = mapper.writeValueAsString(detail)

        assertThat(json).contains("\"success_count\":100")
        assertThat(json).contains("\"failure_count\":2")
        assertThat(json).contains("\"schedule_conversion\":")
        assertThat(json).contains("\"converted_schedule_count\":30")
        assertThat(json).contains("\"deleted_schedule_count\":5")
        assertThat(json).contains("\"skipped_employee_not_found\":2")
        assertThat(json).contains("\"skipped_job_filter\":0")
        assertThat(json).contains("\"skipped_attend_type_filter\":0")
        assertThat(json).contains("\"skipped_idempotent\":0")
        assertThat(json).doesNotContain("successCount")
        assertThat(json).doesNotContain("scheduleConversion")
        assertThat(json).doesNotContain("convertedScheduleCount")
    }

    @Test
    @DisplayName("EmployeeMasterDetail 가 snake_case (emp_code 포함) 로 직렬화된다")
    fun employeeMasterDetail_serializes_snake_case() {
        val detail = EmployeeMasterDetail(
            successCount = 5,
            failureCount = 1,
            failures = listOf(EmployeeFailureItem(empCode = "E001", reason = "missing field"))
        )

        val json = mapper.writeValueAsString(detail)

        assertThat(json).contains("\"success_count\":5")
        assertThat(json).contains("\"failure_count\":1")
        assertThat(json).contains("\"emp_code\":\"E001\"")
        assertThat(json).doesNotContain("successCount")
        assertThat(json).doesNotContain("empCode")
    }

    @Test
    @DisplayName("일반 DTO 는 기본 NamingStrategy(LOWER_CAMEL_CASE) 로 camelCase 직렬화된다")
    fun generalDto_serializes_camel_case() {
        data class GeneralResponseDto(
            val employeeCode: String,
            val orgName: String,
            val accessToken: String
        )

        val dto = GeneralResponseDto(employeeCode = "S00001", orgName = "영업1팀", accessToken = "eyJ...")
        val json = mapper.writeValueAsString(dto)

        assertThat(json).contains("\"employeeCode\":\"S00001\"")
        assertThat(json).contains("\"orgName\":\"영업1팀\"")
        assertThat(json).contains("\"accessToken\":\"eyJ...\"")
        assertThat(json).doesNotContain("employee_code")
        assertThat(json).doesNotContain("org_name")
        assertThat(json).doesNotContain("access_token")
    }
}
