package com.otoki.powersales.schedule.repository

import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.employee.entity.Employee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.common.config.QueryDslConfig
import java.time.LocalDate

/**
 * TeamMemberScheduleRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class TeamMemberScheduleRepositoryTest {

    @Autowired
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testEmployee: Employee

    @BeforeEach
    fun setUp() {
        teamMemberScheduleRepository.deleteAll()
        testEntityManager.clear()
        testEmployee = testEntityManager.persistAndFlush(
            Employee(employeeCode = "EMP001", name = "테스트사원")
        )
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 해당 날짜 일정이 있으면 일정 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_withSchedules() {
        // Given
        val today = LocalDate.now()
        val teamMemberSchedule1 = TeamMemberSchedule(
            employee = testEmployee,
            workingDate = today,
            workingType = WorkingType.WORK
        )
        val teamMemberSchedule2 = TeamMemberSchedule(
            employee = testEmployee,
            workingDate = today,
            workingType = WorkingType.WORK
        )
        testEntityManager.persistAndFlush(teamMemberSchedule1)
        testEntityManager.persistAndFlush(teamMemberSchedule2)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(testEmployee.id, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.workingType }).containsExactlyInAnyOrder(WorkingType.WORK, WorkingType.WORK)
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 다른 날짜 일정만 있으면 빈 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_differentDate() {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plus(1, java.time.temporal.ChronoUnit.DAYS)
        val teamMemberSchedule = TeamMemberSchedule(
            employee = testEmployee,
            workingDate = tomorrow,
            workingType = WorkingType.WORK
        )
        testEntityManager.persistAndFlush(teamMemberSchedule)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(testEmployee.id, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 다른 사원의 일정은 조회되지 않는다")
    fun findByEmployeeIdAndWorkingDate_differentEmployee() {
        // Given
        val today = LocalDate.now()
        val otherEmployee = testEntityManager.persistAndFlush(
            Employee(employeeCode = "EMP099", name = "다른사원")
        )
        val teamMemberSchedule = TeamMemberSchedule(
            employee = otherEmployee,
            workingDate = today,
            workingType = WorkingType.WORK
        )
        testEntityManager.persistAndFlush(teamMemberSchedule)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(testEmployee.id, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 일정이 전혀 없으면 빈 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_noSchedules() {
        // When
        val result = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(testEmployee.id, LocalDate.now())

        // Then
        assertThat(result).isEmpty()
    }

    @Nested
    @DisplayName("findByWorkingDateAndEmployeeIn")
    inner class FindByWorkingDateAndEmployeeIn {

        @Test
        @DisplayName("팀 스케줄 조회 - 3명의 사원 스케줄 반환")
        fun findByWorkingDateAndEmployeeIn_threeEmployees() {
            // Given
            val today = LocalDate.now()
            val employee1 = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP011", name = "사원1"))
            val employee2 = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP022", name = "사원2"))
            val employee3 = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP033", name = "사원3"))

            val teamMemberSchedule1 = TeamMemberSchedule(
                employee = employee1,
                workingDate = today,
                workingType = WorkingType.WORK
            )
            val teamMemberSchedule2 = TeamMemberSchedule(
                employee = employee2,
                workingDate = today,
                workingType = WorkingType.WORK
            )
            val teamMemberSchedule3 = TeamMemberSchedule(
                employee = employee3,
                workingDate = today,
                workingType = WorkingType.ANNUAL_LEAVE
            )
            testEntityManager.persistAndFlush(teamMemberSchedule1)
            testEntityManager.persistAndFlush(teamMemberSchedule2)
            testEntityManager.persistAndFlush(teamMemberSchedule3)
            testEntityManager.clear()

            // When
            val result = teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(
                today,
                listOf(employee1, employee2, employee3)
            )

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.employee?.id })
                .containsExactlyInAnyOrder(employee1.id, employee2.id, employee3.id)
        }

        @Test
        @DisplayName("빈 결과 - 일치하는 사원 없음")
        fun findByWorkingDateAndEmployeeIn_noMatchingEmployees() {
            // Given
            val today = LocalDate.now()
            val otherEmployee = testEntityManager.persistAndFlush(
                Employee(employeeCode = "EMP099", name = "다른사원")
            )
            val searchEmployee1 = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP011", name = "검색1"))
            val searchEmployee2 = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP022", name = "검색2"))
            val teamMemberSchedule = TeamMemberSchedule(
                employee = otherEmployee,
                workingDate = today,
                workingType = WorkingType.WORK
            )
            testEntityManager.persistAndFlush(teamMemberSchedule)
            testEntityManager.clear()

            // When
            val result = teamMemberScheduleRepository.findByWorkingDateAndEmployeeIn(
                today,
                listOf(searchEmployee1, searchEmployee2)
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("updateAttendanceLog - attendance_log id-FK 업데이트 (Spec #789)")
    inner class UpdateAttendanceLogTests {

        @Test
        @DisplayName("정상 업데이트 - id 일치 시 attendance_log id-FK 변경")
        fun updateAttendanceLog_success() {
            // Given
            val attendanceLog = testEntityManager.persistAndFlush(com.otoki.powersales.schedule.entity.AttendanceLog())
            val teamMemberSchedule = TeamMemberSchedule(
                sfid = "SF789",
                employee = testEmployee,
                workingDate = LocalDate.now(),
                workingType = WorkingType.WORK
            )
            testEntityManager.persistAndFlush(teamMemberSchedule)
            testEntityManager.clear()

            // When
            teamMemberScheduleRepository.updateAttendanceLog(teamMemberSchedule.id, attendanceLog.id)
            testEntityManager.clear()

            // Then
            val updated = teamMemberScheduleRepository.findById(teamMemberSchedule.id)
            assertThat(updated).isPresent
            assertThat(updated.get().attendanceLog?.id).isEqualTo(attendanceLog.id)
        }

        @Test
        @DisplayName("존재하지 않는 schedule id - 에러 없이 0건 업데이트")
        fun updateAttendanceLog_nonExistentSchedule() {
            // When & Then (에러 없이 실행)
            teamMemberScheduleRepository.updateAttendanceLog(99999L, 1L)
        }
    }

    @Nested
    @DisplayName("findByEmployeeOrderByWorkingDateDescCreatedAtDesc")
    inner class FindByEmployeeOrderByWorkingDateDescCreatedAtDesc {

        @Test
        @DisplayName("working_date 내림차순으로 정렬된 결과를 반환한다")
        fun ordered_by_working_date_desc() {
            val day1 = LocalDate.of(2026, 1, 10)
            val day2 = LocalDate.of(2026, 2, 15)
            val day3 = LocalDate.of(2026, 3, 20)
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = day1, workingType = WorkingType.WORK)
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = day3, workingType = WorkingType.WORK)
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = day2, workingType = WorkingType.WORK)
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, org.springframework.data.domain.PageRequest.of(0, 10))

            assertThat(result.map { it.workingDate }).containsExactly(day3, day2, day1)
        }

        @Test
        @DisplayName("Pageable limit 만큼만 반환한다")
        fun limit_respected() {
            for (d in 1..15) {
                testEntityManager.persistAndFlush(
                    TeamMemberSchedule(
                        employee = testEmployee,
                        workingDate = LocalDate.of(2026, 1, d),
                        workingType = WorkingType.WORK,
                    )
                )
            }
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, org.springframework.data.domain.PageRequest.of(0, 10))

            assertThat(result).hasSize(10)
            assertThat(result.first().workingDate).isEqualTo(LocalDate.of(2026, 1, 15))
            assertThat(result.last().workingDate).isEqualTo(LocalDate.of(2026, 1, 6))
        }

        @Test
        @DisplayName("다른 사원 일정은 포함되지 않는다")
        fun other_employee_excluded() {
            val otherEmployee = testEntityManager.persistAndFlush(
                Employee(employeeCode = "EMP_OTHER", name = "다른사원")
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = LocalDate.of(2026, 1, 10), workingType = WorkingType.WORK)
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = otherEmployee, workingDate = LocalDate.of(2026, 1, 20), workingType = WorkingType.WORK)
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, org.springframework.data.domain.PageRequest.of(0, 10))

            assertThat(result).hasSize(1)
            assertThat(result[0].employee?.id).isEqualTo(testEmployee.id)
        }
    }
}
