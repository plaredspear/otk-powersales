package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.schedule.service.MyScheduleService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@DisplayName("MyScheduleService 테스트")
class MyScheduleServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val myScheduleService = MyScheduleService(
        employeeRepository,
        displayWorkScheduleRepository,
        teamMemberScheduleRepository,
    )

    // ========== 월간 일정 조회 Tests ==========

    @Nested
    @DisplayName("월간 일정 조회")
    inner class GetMonthlySchedule {

        @Test
        @DisplayName("성공 - 진열 근무일은 안전점검 게이트 없이 마스터 기간이면 표시 (여사원 본인 계획 화면)")
        fun getMonthlySchedule_displayShownWithoutSafetyGate() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val account = Account(id = 10L, name = "거래처")
            // 단일일 진열 마스터 3건(8/1, 8/4, 8/10)
            val masters = listOf(
                createMockSchedule(account = account, startDate = LocalDate.of(2020, 8, 1)),
                createMockSchedule(account = account, startDate = LocalDate.of(2020, 8, 4)),
                createMockSchedule(account = account, startDate = LocalDate.of(2020, 8, 10))
            )

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId),
                eq(LocalDate.of(2020, 8, 1)),
                eq(LocalDate.of(2020, 8, 31))
            ) } returns masters
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then — 안전점검 제출 여부와 무관하게 마스터가 있는 3일 모두 근무일
            assertThat(result.year).isEqualTo(2020)
            assertThat(result.month).isEqualTo(8)
            assertThat(result.workDays).hasSize(31) // 8월은 31일
            assertThat(result.workDays.filter { it.hasWork }.map { it.date })
                .containsExactly("2020-08-01", "2020-08-04", "2020-08-10")
            assertThat(result.annualLeaveCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 마스터 기간(여러 날)의 모든 날이 근무일로 표시 (안전점검 게이트 없음)")
        fun getMonthlySchedule_multiDayPeriodShownEveryDay() {
            // Given
            val userId = 1L
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val account = Account(id = 10L, name = "거래처")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            // 8/4 ~ 8/6 기간 마스터 1건
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId), eq(LocalDate.of(2020, 8, 1)), eq(LocalDate.of(2020, 8, 31))
            ) } returns listOf(
                createMockSchedule(account = account, startDate = LocalDate.of(2020, 8, 4), endDate = LocalDate.of(2020, 8, 6))
            )
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, 2020, 8)

            // Then — 기간(8/4~8/6) 전체가 근무일 (안전점검 제출과 무관)
            assertThat(result.workDays.filter { it.hasWork }.map { it.date })
                .containsExactly("2020-08-04", "2020-08-05", "2020-08-06")
        }

        @Test
        @DisplayName("성공 - 보고완료/총건 카운트 (안전점검일 거래처 수 = total, 출근등록 = completed)")
        fun getMonthlySchedule_reportCounts() {
            // Given
            val userId = 1L
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val date = LocalDate.of(2020, 8, 4)
            val accA = Account(id = 10L, name = "거래처A")
            val accB = Account(id = 20L, name = "거래처B")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            // 8/4 담당 거래처 2건
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId), any(), any()
            ) } returns listOf(
                createMockSchedule(account = accA, startDate = date),
                createMockSchedule(account = accB, startDate = date)
            )
            // 거래처A 만 출근등록(attendanceLog) 완료
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns listOf(
                createMockMemberSchedule(
                    workingDate = date,
                    workingType = WorkingType.WORK,
                    account = accA,
                    attendanceLog = AttendanceLog(id = 100L),
                    workingCategory1 = WorkingCategory1.DISPLAY
                )
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, 2020, 8)

            // Then — 총건 2, 보고완료 1
            val day4 = result.workDays.first { it.date == "2020-08-04" }
            assertThat(day4.hasWork).isTrue()
            assertThat(day4.totalCount).isEqualTo(2)
            assertThat(day4.completedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("성공 - 행사(EVENT)는 안전점검 게이트 없이 hasWork 로 표시")
        fun getMonthlySchedule_unionsEventDays() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val account = Account(id = 10L, name = "거래처")
            val eventAccount = Account(id = 30L, name = "행사거래처")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            // 진열: 8/4 만
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId),
                eq(LocalDate.of(2020, 8, 1)),
                eq(LocalDate.of(2020, 8, 31))
            ) } returns listOf(createMockSchedule(account = account, startDate = LocalDate.of(2020, 8, 4)))
            // TMS: 8/10 행사(EVENT) → 안전점검 없이도 표시, 8/15 진열(DISPLAY) → 진열 마스터에 없으면 미표시
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns listOf(
                createMockMemberSchedule(
                    workingDate = LocalDate.of(2020, 8, 10),
                    account = eventAccount,
                    workingCategory1 = WorkingCategory1.EVENT
                ),
                createMockMemberSchedule(
                    workingDate = LocalDate.of(2020, 8, 15),
                    account = account,
                    workingCategory1 = WorkingCategory1.DISPLAY
                )
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then — 진열(8/4) ∪ 행사(8/10) 만 hasWork
            assertThat(result.workDays.filter { it.hasWork }.map { it.date })
                .containsExactlyInAnyOrder("2020-08-04", "2020-08-10")
            // 진열 카테고리 TMS(8/15)는 진열 마스터에 없으므로 마커 아님
            assertThat(result.workDays[14].date).isEqualTo("2020-08-15")
            assertThat(result.workDays[14].hasWork).isFalse()
        }

        @Test
        @DisplayName("성공 - 근무일이 없는 경우")
        fun getMonthlySchedule_noWorkDays_success() {
            // Given
            val userId = 1L
            val year = 2020
            val month = 8
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId),
                any(),
                any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.year).isEqualTo(2020)
            assertThat(result.month).isEqualTo(8)
            assertThat(result.workDays).hasSize(31)
            assertThat(result.workDays.all { !it.hasWork }).isTrue()
            assertThat(result.annualLeaveCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 2월(윤년 아님) 28일 반환")
        fun getMonthlySchedule_february_nonLeapYear() {
            // Given
            val userId = 1L
            val year = 2021
            val month = 2
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId),
                any(),
                any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.workDays).hasSize(28)
        }

        @Test
        @DisplayName("성공 - 대휴 2건, 연차 1건 → substituteHolidayCount=2, annualLeaveCount=1")
        fun getMonthlySchedule_withSubstituteHoliday() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 4
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId), any(), any()
            ) } returns listOf(createMockSchedule(startDate = LocalDate.of(2026, 4, 1)))
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
                any()
            ) } returns listOf(
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 5), workingType = WorkingType.ALT_HOLIDAY),
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 12), workingType = WorkingType.ALT_HOLIDAY),
                createMockMemberSchedule(workingDate = LocalDate.of(2026, 4, 20), workingType = WorkingType.ANNUAL_LEAVE)
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.substituteHolidayCount).isEqualTo(2)
            assertThat(result.annualLeaveCount).isEqualTo(1)
        }

        @Test
        @DisplayName("성공 - 대휴 0건 → substituteHolidayCount=0")
        fun getMonthlySchedule_noSubstituteHoliday() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 4
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId), any(), any()
            ) } returns emptyList()
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)), any(), any(), any()
            ) } returns emptyList()

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.substituteHolidayCount).isEqualTo(0)
        }

        @Test
        @DisplayName("성공 - 연차 날은 근무일 아님(hasWork=false)이고 workingType='연차' 마커만 표시")
        fun getMonthlySchedule_withAnnualLeave() {
            // Given
            val userId = 1L
            val year = 2026
            val month = 3
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val account = Account(id = 10L, name = "거래처")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(
                eq(userId), any(), any()
            ) } returns listOf(
                createMockSchedule(account = account, startDate = LocalDate.of(2026, 3, 5)),
                createMockSchedule(account = account, startDate = LocalDate.of(2026, 3, 10))
            )
            every { teamMemberScheduleRepository.findMonthlyByEmployeeIds(
                eq(listOf(userId)),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                any()
            ) } returns listOf(
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 5), WorkingType.ANNUAL_LEAVE),
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 10), WorkingType.WORK),
                createMockMemberSchedule("20030117", LocalDate.of(2026, 3, 20), WorkingType.ANNUAL_LEAVE)
            )

            // When
            val result = myScheduleService.getMonthlySchedule(userId, year, month)

            // Then
            assertThat(result.annualLeaveCount).isEqualTo(2)
            // 3/5 → 연차라 hasWork=false (거래처 집계 없음), workingType 마커만
            val day5 = result.workDays.first { it.date == "2026-03-05" }
            assertThat(day5.workingType).isEqualTo("연차")
            assertThat(day5.hasWork).isFalse()
            assertThat(day5.totalCount).isEqualTo(0)
            // 3/10 → workingType = WorkingType.WORK
            val day10 = result.workDays.first { it.date == "2026-03-10" }
            assertThat(day10.workingType).isEqualTo("근무")
            // 스케줄 없는 날 → workingType=null
            val day1 = result.workDays.first { it.date == "2026-03-01" }
            assertThat(day1.workingType).isNull()
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getMonthlySchedule_userNotFound() {
            // Given
            val userId = 999L
            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThrows<EmployeeNotFoundException> {
                myScheduleService.getMonthlySchedule(userId, 2020, 8)
            }
        }
    }

    // ========== 일간 일정 상세 조회 Tests ==========

    @Nested
    @DisplayName("일간 일정 상세 조회")
    inner class GetDailySchedule {

        @Test
        @DisplayName("성공 - 일정 있음 (일반 근무)")
        fun getDailySchedule_withSchedules_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val mockSchedules = listOf(
                createMockSchedule(
                    typeOfWork1 = TypeOfWork1.DISPLAY,
                    typeOfWork3 = TypeOfWork3.ROTATION,
                    typeOfWork5 = TypeOfWork5.REGULAR,
                    account = Account(id = 10L, name = "(주)이마트트레이더스명지점"),
                    startDate = date
                ),
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, startDate = date),
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, startDate = date)
            )

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.WORK))
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, date) } returns mockSchedules

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.date).isEqualTo("2020-08-04")
            assertThat(result.dayOfWeek).isEqualTo("화")
            assertThat(result.memberName).isEqualTo("최금주")
            assertThat(result.employeeCode).isEqualTo("20030117")
            assertThat(result.workingType).isNull()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(3)
            assertThat(result.reportProgress.workType).isEqualTo("진열")
            assertThat(result.accounts).hasSize(3)
            assertThat(result.accounts.all { !it.isRegistered }).isTrue()
            // 레거시 myDaily.jsp 정합: 거래처명 + typeOfWork1/typeOfWork5/typeOfWork3
            val first = result.accounts.first()
            assertThat(first.accountId).isEqualTo(10L)
            assertThat(first.accountName).isEqualTo("(주)이마트트레이더스명지점")
            assertThat(first.workType1).isEqualTo("진열")
            assertThat(first.workType2).isEqualTo("상시")
            assertThat(first.workType3).isEqualTo("순회")
        }

        @Test
        @DisplayName("성공 - 행사 전용일 → 행사 거래처(EVENT TMS)에서 소싱")
        fun getDailySchedule_eventOnlyDay_sourcesEventAccounts() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 10)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val eventAccount = Account(id = 30L, name = "행사거래처")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            // 진열은 없음 → 행사 거래처만 표시되어야 함
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, date) } returns emptyList()
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(
                createMockMemberSchedule(
                    workingDate = date,
                    workingType = WorkingType.WORK,
                    account = eventAccount,
                    workingCategory1 = WorkingCategory1.EVENT
                )
            )

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountId).isEqualTo(30L)
            assertThat(result.accounts[0].accountName).isEqualTo("행사거래처")
            assertThat(result.accounts[0].workType1).isEqualTo("행사")
            assertThat(result.accounts[0].isRegistered).isFalse()
            assertThat(result.reportProgress.total).isEqualTo(1)
            assertThat(result.reportProgress.workType).isEqualTo("행사")
        }

        @Test
        @DisplayName("성공 - 진열·행사 동일 거래처는 accountId 기준 1건으로 dedup")
        fun getDailySchedule_dedupsSharedAccountById() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 11)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val sharedAccount = Account(id = 40L, name = "공통거래처")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, date) } returns listOf(
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, account = sharedAccount, startDate = date)
            )
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(
                createMockMemberSchedule(
                    workingDate = date,
                    workingType = WorkingType.WORK,
                    account = sharedAccount,
                    attendanceLog = AttendanceLog(id = 200L),
                    workingCategory1 = WorkingCategory1.EVENT
                )
            )

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then — 같은 거래처는 1건으로 합쳐지고 출근완료 반영
            assertThat(result.accounts).hasSize(1)
            assertThat(result.accounts[0].accountId).isEqualTo(40L)
            assertThat(result.accounts[0].isRegistered).isTrue()
        }

        @Test
        @DisplayName("성공 - 출근 등록(attendanceLog) 완료 거래처는 isRegistered=true, completed 반영")
        fun getDailySchedule_registeredAccount_reflectsCompleted() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")
            val registeredAccount = Account(id = 10L, name = "등록완료거래처")
            val pendingAccount = Account(id = 20L, name = "미등록거래처")
            val mockSchedules = listOf(
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, account = registeredAccount, startDate = date),
                createMockSchedule(typeOfWork1 = TypeOfWork1.DISPLAY, account = pendingAccount, startDate = date)
            )
            // 거래처 10만 출근 등록(attendanceLog) 완료
            val mockMemberSchedules = listOf(
                createMockMemberSchedule(
                    workingDate = date,
                    workingType = WorkingType.WORK,
                    account = registeredAccount,
                    attendanceLog = AttendanceLog(id = 100L)
                )
            )

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns mockMemberSchedules
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, date) } returns mockSchedules

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.reportProgress.total).isEqualTo(2)
            assertThat(result.reportProgress.completed).isEqualTo(1)
            assertThat(result.accounts.first { it.accountId == 10L }.isRegistered).isTrue()
            assertThat(result.accounts.first { it.accountId == 20L }.isRegistered).isFalse()
        }

        @Test
        @DisplayName("성공 - 일정 없음")
        fun getDailySchedule_noSchedule_success() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2020, 8, 4)
            val mockUser = createMockEmployee(userId, "최금주", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns emptyList()
            every { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(userId, date) } returns emptyList()

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isNull()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(0)
            assertThat(result.reportProgress.workType).isEmpty()
            assertThat(result.accounts).isEmpty()
        }

        @Test
        @DisplayName("성공 - 대휴 날짜 → workingType='대휴', accounts 빈 리스트")
        fun getDailySchedule_substituteHoliday() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2026, 4, 5)
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.ALT_HOLIDAY))

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isEqualTo("대휴")
            assertThat(result.accounts).isEmpty()
            assertThat(result.reportProgress.completed).isEqualTo(0)
            assertThat(result.reportProgress.total).isEqualTo(0)
            assertThat(result.reportProgress.workType).isEmpty()
            verify(exactly = 0) { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(any(), any()) }
        }

        @Test
        @DisplayName("성공 - 연차 날짜 → workingType='연차', accounts 빈 리스트")
        fun getDailySchedule_annualLeave() {
            // Given
            val userId = 1L
            val date = LocalDate.of(2026, 4, 10)
            val mockUser = createMockEmployee(userId, "김여사", "20030117", sfid = "a0B000000012345")

            every { employeeRepository.findById(userId) } returns Optional.of(mockUser)
            every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(userId, date) } returns listOf(createMockMemberSchedule(workingDate = date, workingType = WorkingType.ANNUAL_LEAVE))

            // When
            val result = myScheduleService.getDailySchedule(userId, date)

            // Then
            assertThat(result.workingType).isEqualTo("연차")
            assertThat(result.accounts).isEmpty()
            assertThat(result.reportProgress.total).isEqualTo(0)
            verify(exactly = 0) { displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(any(), any()) }
        }

        @Test
        @DisplayName("실패 - 사용자 없음")
        fun getDailySchedule_userNotFound() {
            // Given
            val userId = 999L
            val date = LocalDate.of(2020, 8, 4)

            every { employeeRepository.findById(userId) } returns Optional.empty()

            // When & Then
            assertThrows<EmployeeNotFoundException> {
                myScheduleService.getDailySchedule(userId, date)
            }
        }
    }

    // ========== Helper Methods ==========

    private fun createMockEmployee(userId: Long, name: String, employeeCode: String, sfid: String? = null): Employee {
        return Employee(
            id = userId,
            employeeCode = employeeCode,
            password = "encoded",
            name = name,
            orgName = "서울지점",
            sfid = sfid
        )
    }

    private fun createMockMemberSchedule(
        employeeCode: String = "20030117",
        workingDate: LocalDate = LocalDate.now(),
        workingType: WorkingType = WorkingType.WORK,
        account: Account? = null,
        attendanceLog: AttendanceLog? = null,
        workingCategory1: WorkingCategory1? = null
    ): TeamMemberSchedule {
        return TeamMemberSchedule(
            employee = Employee(id = 1L, employeeCode = employeeCode, name = "테스트"),
            workingDate = workingDate,
            workingType = workingType,
            account = account,
            attendanceLog = attendanceLog,
            workingCategory1 = workingCategory1
        )
    }

    private fun createMockSchedule(
        id: Long = 0,
        typeOfWork1: TypeOfWork1? = TypeOfWork1.DISPLAY,
        typeOfWork3: TypeOfWork3? = null,
        typeOfWork5: TypeOfWork5? = null,
        account: Account? = null,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = startDate
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            id = id,
            typeOfWork1 = typeOfWork1,
            typeOfWork3 = typeOfWork3,
            typeOfWork5 = typeOfWork5,
            account = account,
            startDate = startDate,
            endDate = endDate
        )
    }
}
