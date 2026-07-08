package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
    private lateinit var employeeRepository: EmployeeRepository

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
        val tomorrow = today.plus(1, ChronoUnit.DAYS)
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
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, PageRequest.of(0, 10))

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
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, PageRequest.of(0, 10))

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
                .findByEmployeeOrderByWorkingDateDescCreatedAtDesc(testEmployee, PageRequest.of(0, 10))

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

    @Nested
    @DisplayName("findMonthlyBy* — promotionEmployee fetch join")
    inner class MonthlyPromotionEmployeeFetchJoin {

        // 행사 일정의 promotionEmployee 를 fetch join 으로 즉시 로딩해 TeamScheduleDto.from 이
        // promotionEmployee.promotionId 를 채울 수 있어야 한다(행사 클릭 → 행사 상세 이동).
        // em.clear() 로 detach 한 뒤 promotionId 를 읽어도 LazyInitializationException 없이
        // 값이 나오면 fetch join 이 동작한 것이다.
        @Test
        @DisplayName("findMonthlyByEmployeeIds - 행사 일정의 promotionEmployee.promotionId 가 채워진다")
        fun findMonthlyByEmployeeIds_fetchesPromotionEmployee() {
            val today = LocalDate.now()
            val promotion = testEntityManager.persistAndFlush(
                Promotion(promotionNumber = "PM-EVT-1", startDate = today, endDate = today)
            )
            val pe = testEntityManager.persistAndFlush(PromotionEmployee(promotionId = promotion.id))
            val schedule = TeamMemberSchedule(
                employee = testEmployee,
                workingDate = today,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                promotionEmployee = pe
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                listOf(testEmployee.id), today, today, null
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].promotionEmployee?.promotionId).isEqualTo(promotion.id)
        }

        @Test
        @DisplayName("findMonthlyByAccountIds - 행사 일정의 promotionEmployee.promotionId 가 채워진다")
        fun findMonthlyByAccountIds_fetchesPromotionEmployee() {
            val today = LocalDate.now()
            val account = testEntityManager.persistAndFlush(Account(name = "행사거래처", externalKey = "EVT1"))
            val promotion = testEntityManager.persistAndFlush(
                Promotion(promotionNumber = "PM-EVT-2", startDate = today, endDate = today)
            )
            val pe = testEntityManager.persistAndFlush(PromotionEmployee(promotionId = promotion.id))
            val schedule = TeamMemberSchedule(
                employee = testEmployee,
                account = account,
                workingDate = today,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.EVENT,
                promotionEmployee = pe
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findMonthlyByAccountIds(
                listOf(account.id), today, today, null
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].promotionEmployee?.promotionId).isEqualTo(promotion.id)
        }

        // 출근로그를 fetch join 으로 즉시 로딩해 TeamScheduleDto.from 이 commuteDate
        // (=attendanceLog.attendanceDate) 로 출근 시각을 채울 수 있어야 한다.
        // em.clear() 로 detach 한 뒤 attendanceDate 를 읽어도 LazyInitializationException 없이
        // 값이 나오면 fetch join 이 동작한 것이다.
        @Test
        @DisplayName("findMonthlyByEmployeeIds - 출근등록 일정의 commuteDate(출근 시각) 가 채워진다")
        fun findMonthlyByEmployeeIds_fetchesAttendanceLog() {
            val today = LocalDate.now()
            val commuteAt = today.atTime(8, 30)
            val attendanceLog = testEntityManager.persistAndFlush(
                AttendanceLog(attendanceDate = commuteAt)
            )
            val schedule = TeamMemberSchedule(
                employee = testEmployee,
                workingDate = today,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                attendanceLog = attendanceLog
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                listOf(testEmployee.id), today, today, null
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].commuteDate).isEqualTo(commuteAt)
        }

        @Test
        @DisplayName("findMonthlyByAccountIds - 출근등록 일정의 commuteDate(출근 시각) 가 채워진다")
        fun findMonthlyByAccountIds_fetchesAttendanceLog() {
            val today = LocalDate.now()
            val account = testEntityManager.persistAndFlush(Account(name = "출근거래처", externalKey = "CMT1"))
            val commuteAt = today.atTime(9, 5)
            val attendanceLog = testEntityManager.persistAndFlush(
                AttendanceLog(attendanceDate = commuteAt)
            )
            val schedule = TeamMemberSchedule(
                employee = testEmployee,
                account = account,
                workingDate = today,
                workingType = WorkingType.WORK,
                workingCategory1 = WorkingCategory1.DISPLAY,
                attendanceLog = attendanceLog
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository.findMonthlyByAccountIds(
                listOf(account.id), today, today, null
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].commuteDate).isEqualTo(commuteAt)
        }
    }

    @Nested
    @DisplayName("findLatestAttendanceInfoByEmployeeIds - 최근 출근등록 근무형태")
    inner class FindLatestAttendanceCategories {

        /** attendanceLog 가 연결된(=출근등록된) TMS 1건을 persist 한다. */
        private fun persistAttendedSchedule(
            employee: Employee,
            workingDate: LocalDate,
            category1: WorkingCategory1?,
            category3: WorkingCategory3?,
            account: Account? = null,
        ): TeamMemberSchedule {
            val attendanceLog = testEntityManager.persistAndFlush(AttendanceLog())
            val schedule = testEntityManager.persistAndFlush(
                TeamMemberSchedule(
                    employee = employee,
                    workingDate = workingDate,
                    workingType = WorkingType.WORK,
                    workingCategory1 = category1,
                    workingCategory3 = category3,
                    account = account,
                    attendanceLog = attendanceLog,
                )
            )
            return schedule
        }

        @Test
        @DisplayName("사원별 가장 최근(workingDate 최신) 출근등록 1건의 category1/3 을 반환한다")
        fun returnsLatestPerEmployee() {
            val today = LocalDate.now()
            // 더 오래된 건
            persistAttendedSchedule(testEmployee, today.minusDays(3), WorkingCategory1.DISPLAY, WorkingCategory3.FIXED)
            // 더 최근 건 — 이게 반영되어야 한다
            persistAttendedSchedule(testEmployee, today, WorkingCategory1.EVENT, WorkingCategory3.PATROL)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            assertThat(result).containsKey(testEmployee.id)
            assertThat(result[testEmployee.id]!!.workingCategory1).isEqualTo("행사")
            assertThat(result[testEmployee.id]!!.workingCategory3).isEqualTo("순회")
        }

        @Test
        @DisplayName("가장 최근 출근등록 1건의 거래처명/거래처코드를 반환한다")
        fun returnsAccountOfLatest() {
            val today = LocalDate.now()
            val oldAccount = testEntityManager.persistAndFlush(Account(name = "옛거래처", externalKey = "1000001"))
            val recentAccount = testEntityManager.persistAndFlush(Account(name = "최근거래처", externalKey = "1000002"))
            persistAttendedSchedule(testEmployee, today.minusDays(2), WorkingCategory1.DISPLAY, WorkingCategory3.FIXED, oldAccount)
            persistAttendedSchedule(testEmployee, today, WorkingCategory1.EVENT, WorkingCategory3.PATROL, recentAccount)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            // 최근 일정의 거래처가 반영되어야 한다 (옛 거래처 아님)
            assertThat(result[testEmployee.id]!!.accountName).isEqualTo("최근거래처")
            assertThat(result[testEmployee.id]!!.accountCode).isEqualTo("1000002")
        }

        @Test
        @DisplayName("거래처 미연결 일정은 거래처명/코드가 null")
        fun nullAccountWhenUnlinked() {
            persistAttendedSchedule(testEmployee, LocalDate.now(), WorkingCategory1.DISPLAY, WorkingCategory3.FIXED, null)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            assertThat(result[testEmployee.id]!!.accountName).isNull()
            assertThat(result[testEmployee.id]!!.accountCode).isNull()
        }

        @Test
        @DisplayName("동일 working_date 에 출근등록 다건이면 가장 마지막 등록(id 최대) 1건을 채택한다")
        fun tieBreakByIdOnSameDate() {
            val today = LocalDate.now()
            // 같은 날 두 건 출근등록 — 나중에 persist 된(=id 큰) 행이 채택되어야 한다.
            persistAttendedSchedule(testEmployee, today, WorkingCategory1.DISPLAY, WorkingCategory3.FIXED)
            persistAttendedSchedule(testEmployee, today, WorkingCategory1.EVENT, WorkingCategory3.PATROL)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            assertThat(result[testEmployee.id]!!.workingCategory1).isEqualTo("행사")
            assertThat(result[testEmployee.id]!!.workingCategory3).isEqualTo("순회")
        }

        @Test
        @DisplayName("attendanceLog 가 없는(출근등록 안 된) 일정은 더 최근이어도 무시한다")
        fun ignoresSchedulesWithoutAttendanceLog() {
            val today = LocalDate.now()
            // 출근등록된 과거 건
            persistAttendedSchedule(testEmployee, today.minusDays(5), WorkingCategory1.DISPLAY, WorkingCategory3.FIXED)
            // attendanceLog 없는 더 최근 일정 (예약만 된 미래/오늘 일정) — 무시되어야 한다
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(
                    employee = testEmployee,
                    workingDate = today,
                    workingType = WorkingType.WORK,
                    workingCategory1 = WorkingCategory1.EVENT,
                    workingCategory3 = WorkingCategory3.PATROL,
                )
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            // attendanceLog 있는 과거 건(진열/고정)이 반영되어야 한다
            assertThat(result[testEmployee.id]!!.workingCategory1).isEqualTo("진열")
            assertThat(result[testEmployee.id]!!.workingCategory3).isEqualTo("고정")
        }

        @Test
        @DisplayName("employeeIds 에 포함되지 않은 사원은 결과에서 제외된다")
        fun filtersByEmployeeIds() {
            val today = LocalDate.now()
            val otherEmployee = testEntityManager.persistAndFlush(
                Employee(employeeCode = "EMP999", name = "다른사원")
            )
            persistAttendedSchedule(testEmployee, today, WorkingCategory1.DISPLAY, WorkingCategory3.FIXED)
            persistAttendedSchedule(otherEmployee, today, WorkingCategory1.EVENT, WorkingCategory3.PATROL)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            assertThat(result.keys).containsExactly(testEmployee.id)
        }

        @Test
        @DisplayName("두 사원의 최근일자가 서로 다를 때 날짜 교차 오염 없이 각자 최근 1건을 반환한다")
        fun noCrossContaminationBetweenEmployees() {
            val today = LocalDate.now()
            val empA = testEmployee
            val empB = testEntityManager.persistAndFlush(Employee(employeeCode = "EMPB", name = "사원B"))

            // A: 최근일자 = today (진열/고정). A 는 day-2 에도 출근등록(행사/순회) 이 있음.
            persistAttendedSchedule(empA, today.minusDays(2), WorkingCategory1.EVENT, WorkingCategory3.PATROL)
            persistAttendedSchedule(empA, today, WorkingCategory1.DISPLAY, WorkingCategory3.FIXED)
            // B: 최근일자 = today.minusDays(2) (행사/격고) — A 의 과거일자와 겹치는 날짜.
            persistAttendedSchedule(empB, today.minusDays(2), WorkingCategory1.EVENT, WorkingCategory3.ALTERNATE)
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(empA.id, empB.id))

            // A 는 today 의 진열/고정 (day-2 행사/순회 가 아님)
            assertThat(result[empA.id]!!.workingCategory1).isEqualTo("진열")
            assertThat(result[empA.id]!!.workingCategory3).isEqualTo("고정")
            // B 는 day-2 의 행사/격고
            assertThat(result[empB.id]!!.workingCategory1).isEqualTo("행사")
            assertThat(result[empB.id]!!.workingCategory3).isEqualTo("격고")
        }

        @Test
        @DisplayName("출근등록 이력이 없는 사원은 Map 에 키가 없다")
        fun noKeyWhenNoAttendance() {
            // 출근등록 없는 일정만 존재
            testEntityManager.persistAndFlush(
                TeamMemberSchedule(
                    employee = testEmployee,
                    workingDate = LocalDate.now(),
                    workingType = WorkingType.WORK,
                    workingCategory1 = WorkingCategory1.DISPLAY,
                )
            )
            testEntityManager.clear()

            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(listOf(testEmployee.id))

            assertThat(result).doesNotContainKey(testEmployee.id)
        }

        @Test
        @DisplayName("빈 employeeIds -> 빈 Map")
        fun emptyInput() {
            val result = teamMemberScheduleRepository
                .findLatestAttendanceInfoByEmployeeIds(emptyList())

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("findEmployees - 전문행사조 / 근무형태 매칭 employee_id IN 필터")
    inner class FindEmployeesPromotionAndWorkTypeIdFilter {

        // 근무형태(최근 출근등록 1건) 매칭 employee_id 산출은 native DISTINCT ON 쿼리
        // (TeamMemberScheduleRepository.findEmployeeIdsByLatestWorkType) 로 처리한다 — H2 미지원 PG 전용 문법이라
        // 여기서 실행 검증하지 않고 SQL 정적 검증만 한다(프로젝트 native query 검증 방침).
        // 본 테스트는 그 결과(employee_id 집합)를 받은 findEmployees 의 employee.id IN / 전문행사조 필터만 검증한다.

        @BeforeEach
        fun removeBaseEmployee() {
            // 바깥 setUp 이 만든 testEmployee(EMP001, 전문행사조 null) 는 본 필터 테스트의 모수에서 제외 —
            // '일반' 필터 등에 섞이지 않도록 삭제하고 격리된 사원만 다룬다.
            employeeRepository.delete(testEmployee)
            testEntityManager.flush()
            testEntityManager.clear()
        }

        private fun persistEmployee(
            code: String,
            promotionTeam: ProfessionalPromotionTeamType? = null,
        ): Employee = testEntityManager.persistAndFlush(
            Employee(employeeCode = code, name = "사원$code", professionalPromotionTeam = promotionTeam)
        )

        @Test
        @DisplayName("근무형태 매칭 집합으로 employee.id IN 필터 - 매칭된 사원만 조회")
        fun workTypeMatchedIdsFilter() {
            val e1 = persistEmployee("E1")
            val e2 = persistEmployee("E2")
            persistEmployee("E3")
            testEntityManager.clear()

            // native 매칭 결과가 {e1, e2} 라고 가정 — findEmployees 가 employee.id IN 으로 정확히 필터하는지.
            val result = employeeRepository.findEmployees(
                null, null, null, null, null,
                setOf(e1.id, e2.id), null, false,
                PageRequest.of(0, 20),
            )

            assertThat(result.content.map { it.employeeCode }).containsExactlyInAnyOrder("E1", "E2")
            assertThat(result.content.map { it.employeeCode }).doesNotContain("E3")
        }

        @Test
        @DisplayName("근무형태 매칭 0명이면 빈 결과 - employee.id IN (empty) 가 전건 조회로 새지 않아야 한다")
        fun emptyWorkTypeMatchReturnsEmpty() {
            persistEmployee("N1")
            persistEmployee("N2")
            testEntityManager.clear()

            // 매칭 0명(빈 집합) → 전체 사원이 새어 나오면 안 되고 반드시 빈 결과여야 한다(빈 IN 방어).
            val result = employeeRepository.findEmployees(
                null, null, null, null, null,
                emptySet(), null, false,
                PageRequest.of(0, 20),
            )

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isZero()
        }

        @Test
        @DisplayName("전문행사조 필터 - 지정 조의 사원만 조회")
        fun filterByPromotionTeam() {
            persistEmployee("P1", ProfessionalPromotionTeamType.RAMEN_SALE)
            persistEmployee("P2", ProfessionalPromotionTeamType.CURRY_PROMOTION)
            persistEmployee("P3", null)
            testEntityManager.clear()

            val result = employeeRepository.findEmployees(
                null, null, null, null, null,
                null, ProfessionalPromotionTeamType.RAMEN_SALE, false,
                PageRequest.of(0, 20),
            )

            assertThat(result.content.map { it.employeeCode }).containsExactly("P1")
        }

        @Test
        @DisplayName("전문행사조 '일반'(미배정) 필터 - professionalPromotionTeam IS NULL 인 사원만")
        fun filterByPromotionTeamGeneral() {
            persistEmployee("G1", null)
            persistEmployee("G2", ProfessionalPromotionTeamType.RAMEN_SALE)
            testEntityManager.clear()

            val result = employeeRepository.findEmployees(
                null, null, null, null, null,
                null, null, true,
                PageRequest.of(0, 20),
            )

            assertThat(result.content.map { it.employeeCode }).containsExactly("G1")
        }

        @Test
        @DisplayName("근무형태 매칭 집합 + 전문행사조 동시 필터 - 둘 다 만족하는 사원만")
        fun workTypeMatchedIdsAndPromotionTeam() {
            val match = persistEmployee("M1", ProfessionalPromotionTeamType.RAMEN_SALE)
            val wrongTeam = persistEmployee("M2", ProfessionalPromotionTeamType.CURRY_PROMOTION)
            testEntityManager.clear()

            // 근무형태 매칭이 {M1, M2} 여도 전문행사조(라면세일조) AND 조건으로 M1 만 남아야 한다.
            val result = employeeRepository.findEmployees(
                null, null, null, null, null,
                setOf(match.id, wrongTeam.id), ProfessionalPromotionTeamType.RAMEN_SALE, false,
                PageRequest.of(0, 20),
            )

            assertThat(result.content.map { it.employeeCode }).containsExactly("M1")
        }
    }
}
