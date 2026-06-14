package com.otoki.powersales.domain.org.leave.service

import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AdminAnnualLeaveService 테스트")
class AdminAnnualLeaveServiceTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val adminAnnualLeaveService = AdminAnnualLeaveService(
        teamMemberScheduleRepository,
        employeeRepository,
    )

    // --- Helper ---

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        name: String = "홍길동",
        orgName: String? = "서울1팀"
    ) = Employee(id = id, employeeCode = employeeCode, name = name, orgName = orgName)

    private fun createSchedule(
        id: Long = 1L,
        employeeId: Long? = 1L,
        employeeCode: String? = null,
        employeeName: String? = null,
        employeeOrgName: String? = null,
        workingDate: LocalDate? = LocalDate.of(2026, 3, 5),
        workingType: WorkingType? = WorkingType.ANNUAL_LEAVE
    ) = TeamMemberSchedule(
        id = id,
        employee = employeeId?.let {
            Employee(
                id = it,
                employeeCode = employeeCode ?: "EMP$it",
                name = employeeName ?: "테스트$it",
                orgName = employeeOrgName
            )
        },
        workingDate = workingDate,
        workingType = workingType
    )

    @Nested
    @DisplayName("getSummary - 연차 현황 조회")
    inner class GetSummaryTests {

        @Test
        @DisplayName("성공 - orgCode 없음 → 전체 사원 연차 반환")
        fun noOrgCode_returnsAllEmployees() {
            val schedule1 = createSchedule(id = 1L, employeeId = 1L, employeeCode = "EMP001", employeeName = "홍길동", employeeOrgName = "서울1팀", workingDate = LocalDate.of(2026, 3, 5))
            val schedule2 = createSchedule(id = 2L, employeeId = 1L, employeeCode = "EMP001", employeeName = "홍길동", employeeOrgName = "서울1팀", workingDate = LocalDate.of(2026, 3, 10))
            val schedule3 = createSchedule(id = 3L, employeeId = 2L, employeeCode = "EMP002", employeeName = "김철수", employeeOrgName = "부산1팀", workingDate = LocalDate.of(2026, 3, 15))

            every {
                teamMemberScheduleRepository.findAnnualLeaveByDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
            } returns listOf(schedule1, schedule2, schedule3)

            val result = adminAnnualLeaveService.getSummary("2026-03", null)

            assertThat(result).hasSize(2)

            val emp1 = result.find { it.employeeCode == "EMP001" }!!
            assertThat(emp1.employeeName).isEqualTo("홍길동")
            assertThat(emp1.orgName).isEqualTo("서울1팀")
            assertThat(emp1.annualLeaveDays).hasSize(2)
            assertThat(emp1.totalCount).isEqualTo(2)
            assertThat(emp1.annualLeaveDays[0].date).isEqualTo("2026-03-05")
            assertThat(emp1.annualLeaveDays[1].date).isEqualTo("2026-03-10")

            val emp2 = result.find { it.employeeCode == "EMP002" }!!
            assertThat(emp2.employeeName).isEqualTo("김철수")
            assertThat(emp2.totalCount).isEqualTo(1)
        }

        @Test
        @DisplayName("성공 - orgCode 지정 → 해당 조직 사원만 반환")
        fun withOrgCode_returnsFilteredEmployees() {
            val employee1 = createEmployee(id = 1L, employeeCode = "EMP001", name = "홍길동", orgName = "서울1팀")
            every { employeeRepository.findByOrgName("서울1팀") } returns listOf(employee1)

            val schedule1 = createSchedule(id = 1L, employeeId = 1L, employeeCode = "EMP001", employeeName = "홍길동", employeeOrgName = "서울1팀", workingDate = LocalDate.of(2026, 3, 5))
            every {
                teamMemberScheduleRepository.findAnnualLeaveByDateRangeAndEmployeeIds(
                    LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), listOf(1L)
                )
            } returns listOf(schedule1)

            val result = adminAnnualLeaveService.getSummary("2026-03", "서울1팀")

            assertThat(result).hasSize(1)
            assertThat(result[0].employeeCode).isEqualTo("EMP001")
            assertThat(result[0].employeeName).isEqualTo("홍길동")
            assertThat(result[0].orgName).isEqualTo("서울1팀")
            assertThat(result[0].totalCount).isEqualTo(1)
        }

        @Test
        @DisplayName("빈 결과 - 스케줄 없음 → 빈 리스트 반환")
        fun noSchedules_returnsEmptyList() {
            every {
                teamMemberScheduleRepository.findAnnualLeaveByDateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
            } returns emptyList()

            val result = adminAnnualLeaveService.getSummary("2026-03", null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("빈 결과 - orgCode에 해당하는 사용자 없음 → 빈 리스트 반환")
        fun orgCodeWithNoUsers_returnsEmptyList() {
            every { employeeRepository.findByOrgName("존재하지않는팀") } returns emptyList()

            val result = adminAnnualLeaveService.getSummary("2026-03", "존재하지않는팀")

            assertThat(result).isEmpty()
        }
    }
}
