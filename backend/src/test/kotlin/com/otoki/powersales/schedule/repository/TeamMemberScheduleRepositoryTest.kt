package com.otoki.powersales.schedule.repository

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.schedule.entity.AttendanceLog
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
import com.otoki.powersales.platform.common.config.QueryDslConfig
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

    @Nested
    @DisplayName("aggregateDailySummaryByEmployeeIds - 무필터 일별 요약 DB 집계")
    inner class AggregateDailySummaryByEmployeeIds {

        @Test
        @DisplayName("날짜별로 진열/행사 expected·actual + 연차/대휴를 집계한다")
        fun aggregates_per_date() {
            val date = LocalDate.of(2026, 4, 1)
            val attendance = testEntityManager.persistAndFlush(AttendanceLog())

            // 진열(WORK, 비행사) 2건 — 그중 1건 출근
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY, attendanceLog = attendance)
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY)
            )
            // 행사(WORK, EVENT) 1건 — 출근 없음
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.EVENT)
            )
            // 연차 1건, 대휴 1건
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.ANNUAL_LEAVE)
            )
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.ALT_HOLIDAY)
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .aggregateDailySummaryByEmployeeIds(listOf(testEmployee.id), date, date)

            assertThat(result).hasSize(1)
            val summary = result[0]
            // 날짜는 ISO(YYYY-MM-DD) 문자열로 내려와야 한다 (프론트 정합).
            assertThat(summary.date).isEqualTo("2026-04-01")
            assertThat(summary.displayExpected).isEqualTo(2)
            assertThat(summary.displayActual).isEqualTo(1)
            assertThat(summary.promotionExpected).isEqualTo(1)
            assertThat(summary.promotionActual).isEqualTo(0)
            assertThat(summary.annualLeave).isEqualTo(1)
            assertThat(summary.compensatoryLeave).isEqualTo(1)
        }

        @Test
        @DisplayName("category1 이 null 인 WORK 는 진열(expected)로 집계된다")
        fun null_category_counts_as_display() {
            val date = LocalDate.of(2026, 4, 2)
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = date, workingType = WorkingType.WORK, workingCategory1 = null)
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .aggregateDailySummaryByEmployeeIds(listOf(testEmployee.id), date, date)

            assertThat(result).hasSize(1)
            assertThat(result[0].displayExpected).isEqualTo(1)
            assertThat(result[0].promotionExpected).isEqualTo(0)
        }

        @Test
        @DisplayName("기간 밖 / 다른 사원 일정은 집계에서 제외된다")
        fun excludes_out_of_range_and_other_employee() {
            val other = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP_X", name = "타사원"))
            val from = LocalDate.of(2026, 4, 1)
            val to = LocalDate.of(2026, 4, 30)
            // 기간 안, 본인
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = LocalDate.of(2026, 4, 15), workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY)
            )
            // 기간 밖
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = testEmployee, workingDate = LocalDate.of(2026, 5, 1), workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY)
            )
            // 다른 사원
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(employee = other, workingDate = LocalDate.of(2026, 4, 16), workingType = WorkingType.WORK, workingCategory1 = WorkingCategory1.DISPLAY)
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .aggregateDailySummaryByEmployeeIds(listOf(testEmployee.id), from, to)

            assertThat(result).hasSize(1)
            assertThat(result[0].date).isEqualTo("2026-04-15")
            assertThat(result[0].displayExpected).isEqualTo(1)
        }

        @Test
        @DisplayName("employeeIds 가 비어 있으면 빈 결과를 반환한다")
        fun empty_employee_ids() {
            val result = teamMemberScheduleRepository
                .aggregateDailySummaryByEmployeeIds(emptyList(), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30))
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findDistinctScheduledAccounts - 부서장 전체조회(레거시 selectAllAccount)")
    inner class FindDistinctScheduledAccounts {

        private fun scheduleFor(account: Account, date: LocalDate = LocalDate.now()) {
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(
                    employee = testEmployee,
                    account = account,
                    workingDate = date,
                    workingType = WorkingType.WORK
                )
            )
        }

        @Test
        @DisplayName("일정 잡힌 거래처만 반환하고, 동일 거래처가 여러 일정이어도 1건으로 중복 제거한다")
        fun distinct_only_scheduled() {
            val scheduled = testEntityManager.persistAndFlush(Account(name = "스케줄거래처", externalKey = "1000001"))
            val unscheduled = testEntityManager.persistAndFlush(Account(name = "미스케줄거래처", externalKey = "1000002"))
            // 같은 거래처에 일정 2건 → distinct 검증
            scheduleFor(scheduled)
            scheduleFor(scheduled, LocalDate.now().minusDays(1))
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findDistinctScheduledAccounts(null, 100)

            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("스케줄거래처")
            assertThat(result.map { it.id }).doesNotContain(unscheduled.id)
        }

        @Test
        @DisplayName("keyword 는 거래처명 + 거래처코드(externalKey)로 검색한다")
        fun keyword_matches_name_and_code() {
            val byName = testEntityManager.persistAndFlush(Account(name = "이마트월배점", externalKey = "1000091"))
            val byCode = testEntityManager.persistAndFlush(Account(name = "GS더프레시동탄점", externalKey = "1090298"))
            val noMatch = testEntityManager.persistAndFlush(Account(name = "롯데프레시향동점", externalKey = "1079385"))
            scheduleFor(byName)
            scheduleFor(byCode)
            scheduleFor(noMatch)
            testEntityManager.clear()

            // 거래처명 일부
            assertThat(teamMemberScheduleRepository.findDistinctScheduledAccounts("이마트", 100).map { it.name })
                .containsExactly("이마트월배점")
            // 거래처코드 일부
            assertThat(teamMemberScheduleRepository.findDistinctScheduledAccounts("1090298", 100).map { it.name })
                .containsExactly("GS더프레시동탄점")
        }

        @Test
        @DisplayName("limit 만큼만 반환하고 거래처명 오름차순 정렬한다")
        fun limit_and_order() {
            val c = testEntityManager.persistAndFlush(Account(name = "C거래처", externalKey = "3"))
            val a = testEntityManager.persistAndFlush(Account(name = "A거래처", externalKey = "1"))
            val b = testEntityManager.persistAndFlush(Account(name = "B거래처", externalKey = "2"))
            scheduleFor(c); scheduleFor(a); scheduleFor(b)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findDistinctScheduledAccounts(null, 2)

            assertThat(result.map { it.name }).containsExactly("A거래처", "B거래처")
        }
    }
}
