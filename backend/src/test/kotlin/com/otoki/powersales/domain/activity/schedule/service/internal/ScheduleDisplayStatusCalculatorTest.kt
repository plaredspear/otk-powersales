package com.otoki.powersales.domain.activity.schedule.service.internal

import com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleDisplayStatusCalculator
import com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleValidLight
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * SF `DisplayWorkScheduleMaster__c` formula 필드 포팅 검증.
 *   - ValidConditionData__c (재직상태)
 *   - ValidData__c (유효데이터)
 *   - Valid__c (유효 신호등)
 */
class ScheduleDisplayStatusCalculatorTest {

    private val calc = ScheduleDisplayStatusCalculator()
    private val today = LocalDate.of(2026, 6, 3)

    private fun employee(
        status: String? = "재직",
        appLoginActive: Boolean? = true,
        endDate: LocalDate? = null,
    ): Employee = Employee(
        employeeCode = "20030001",
        name = "홍길동",
        status = status,
        appLoginActive = appLoginActive,
        endDate = endDate,
    )

    @Nested
    @DisplayName("재직상태 (ValidConditionData)")
    inner class EmploymentStatus {

        @Test
        fun `재직 사원은 재직`() {
            assertThat(calc.employmentStatus(employee(status = "재직"), today)).isEqualTo("재직")
        }

        @Test
        fun `휴직 사원은 휴직`() {
            assertThat(calc.employmentStatus(employee(status = "휴직"), today)).isEqualTo("휴직")
        }

        @Test
        fun `퇴직 + 퇴사일 과거면 퇴직+날짜`() {
            val e = employee(status = "퇴직", endDate = LocalDate.of(2026, 5, 1))
            assertThat(calc.employmentStatus(e, today)).isEqualTo("퇴직2026-05-01")
        }

        @Test
        fun `퇴직 + 퇴사일 미래면 퇴직예정+날짜`() {
            val e = employee(status = "퇴직", endDate = LocalDate.of(2026, 7, 1))
            assertThat(calc.employmentStatus(e, today)).isEqualTo("퇴직예정2026-07-01")
        }

        @Test
        fun `appLoginActive=false + 퇴사일 과거면 퇴직`() {
            val e = employee(status = "재직", appLoginActive = false, endDate = LocalDate.of(2026, 5, 1))
            assertThat(calc.employmentStatus(e, today)).isEqualTo("퇴직2026-05-01")
        }

        @Test
        fun `employee null 이면 null`() {
            assertThat(calc.employmentStatus(null, today)).isNull()
        }
    }

    @Nested
    @DisplayName("유효데이터 (ValidData)")
    inner class ValidData {

        @Test
        fun `재직 + 기간 진행 중이면 유효`() {
            val e = employee(status = "재직")
            val r = calc.validData(e, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 12, 31), today)
            assertThat(r).isEqualTo("유효")
        }

        @Test
        fun `재직 + 종료일 없는 진행 중이면 유효`() {
            val e = employee(status = "재직")
            val r = calc.validData(e, LocalDate.of(2026, 5, 1), null, today)
            assertThat(r).isEqualTo("유효")
        }

        @Test
        fun `재직 + 시작일 미래면 예정`() {
            val e = employee(status = "재직")
            val r = calc.validData(e, LocalDate.of(2026, 7, 1), null, today)
            assertThat(r).isEqualTo("예정")
        }

        @Test
        fun `종료일 과거면 종료`() {
            val e = employee(status = "재직")
            val r = calc.validData(e, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1), today)
            assertThat(r).isEqualTo("종료")
        }

        @Test
        fun `시작일 null 이면 null`() {
            assertThat(calc.validData(employee(), null, null, today)).isNull()
        }
    }

    @Nested
    @DisplayName("유효 신호등 (Valid)")
    inner class ValidLight {

        @Test
        fun `유효는 GREEN`() {
            assertThat(calc.validLight("유효")).isEqualTo(ScheduleValidLight.GREEN)
        }

        @Test
        fun `예정은 YELLOW`() {
            assertThat(calc.validLight("예정")).isEqualTo(ScheduleValidLight.YELLOW)
        }

        @Test
        fun `종료는 RED`() {
            assertThat(calc.validLight("종료")).isEqualTo(ScheduleValidLight.RED)
        }

        @Test
        fun `null 이면 null`() {
            assertThat(calc.validLight(null)).isNull()
        }
    }
}
