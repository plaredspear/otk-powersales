package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AttendInfoToScheduleConverter 테스트")
class AttendInfoToScheduleConverterTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val converter = AttendInfoToScheduleConverter(employeeRepository, teamMemberScheduleRepository)

    private fun attendInfo(
        id: Long = 0,
        employeeCode: String = "100123",
        startDate: String = "20260427",
        endDate: String = "20260427",
        attendType: String? = "14",
        status: String? = "N"
    ): AttendInfo = AttendInfo(
        id = id,
        employeeCode = employeeCode,
        startDate = startDate,
        endDate = endDate,
        attendType = attendType,
        status = status
    )

    private fun employee(
        id: Long = 1L,
        employeeCode: String = "100123",
        jobCode: String? = "판촉직"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "테스트사원"
    ).apply { this.jobCode = jobCode }

    @Nested
    @DisplayName("convert - Happy Path")
    inner class ConvertHappy {

        @Test
        @DisplayName("빈 입력 - ZERO 반환, 리포지토리 호출 없음")
        fun emptyInput() {
            val result = converter.convert(emptyList())

            assertThat(result).isEqualTo(ScheduleConversionSummary.ZERO)
            verify(exactly = 0) { employeeRepository.findByEmployeeCodeIn(any()) }
        }

        @Test
        @DisplayName("정상 변환 (Status='N') - 3일 범위, converted=3")
        fun convert_status_N_threeDays() {
            val emp = employee()
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                    eq(emp), any(), eq(WorkingType.ANNUAL_LEAVE)
                )
            } returns false
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>()
            }

            val result = converter.convert(
                listOf(
                    attendInfo(startDate = "20260427", endDate = "20260429", status = "N")
                )
            )

            assertThat(result.convertedScheduleCount).isEqualTo(3)
            assertThat(result.deletedScheduleCount).isEqualTo(0)
            assertThat(result.skippedIdempotent).isEqualTo(0)
            val captor = slot<List<TeamMemberSchedule>>()
            verify { teamMemberScheduleRepository.saveAll(capture(captor)) }
            val saved = captor.captured
            assertThat(saved).hasSize(3)
            assertThat(saved.map { it.workingDate }).containsExactly(
                LocalDate.of(2026, 4, 27),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 4, 29)
            )
            assertThat(saved).allMatch { it.workingType == WorkingType.ANNUAL_LEAVE && it.employee == emp }
        }

        @Test
        @DisplayName("Status='Y' - 동일 직원·기간 일정 모두 삭제, deleted_count 누적")
        fun delete_status_Y() {
            val emp = employee()
            val existing = listOf<TeamMemberSchedule>(
                TeamMemberSchedule().apply {
                    workingDate = LocalDate.of(2026, 4, 27); workingType = WorkingType.ANNUAL_LEAVE; this.employee = emp
                },
                TeamMemberSchedule().apply {
                    workingDate = LocalDate.of(2026, 4, 28); workingType = WorkingType.ANNUAL_LEAVE; this.employee = emp
                },
                TeamMemberSchedule().apply {
                    workingDate = LocalDate.of(2026, 4, 29); workingType = WorkingType.ANNUAL_LEAVE; this.employee = emp
                }
            )
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    eq(emp),
                    eq(LocalDate.of(2026, 4, 27)),
                    eq(LocalDate.of(2026, 4, 29)),
                    eq(WorkingType.ANNUAL_LEAVE)
                )
            } returns existing
            every { teamMemberScheduleRepository.deleteAll(existing) } returns Unit

            val result = converter.convert(
                listOf(
                    attendInfo(startDate = "20260427", endDate = "20260429", status = "Y")
                )
            )

            assertThat(result.deletedScheduleCount).isEqualTo(3)
            assertThat(result.convertedScheduleCount).isEqualTo(0)
            verify { teamMemberScheduleRepository.deleteAll(existing) }
        }

        @Test
        @DisplayName("소문자 status 'n' / 'y' - 대문자와 동일하게 처리")
        fun lowercaseStatus_handled() {
            val emp = employee()
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any())
            } returns false
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>()
            }
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    any(), any(), any(), any()
                )
            } returns emptyList()
            every { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) } returns Unit

            val result = converter.convert(
                listOf(
                    attendInfo(startDate = "20260427", endDate = "20260427", status = "n"),
                    attendInfo(startDate = "20260428", endDate = "20260428", status = "y")
                )
            )

            assertThat(result.convertedScheduleCount).isEqualTo(1)
            assertThat(result.deletedScheduleCount).isEqualTo(0)
        }

        @Test
        @DisplayName("멱등성 - 이미 존재하는 일정은 skipped_idempotent 증가")
        fun idempotent_skip() {
            val emp = employee()
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                    eq(emp), eq(LocalDate.of(2026, 4, 27)), eq(WorkingType.ANNUAL_LEAVE)
                )
            } returns true
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                    eq(emp), eq(LocalDate.of(2026, 4, 28)), eq(WorkingType.ANNUAL_LEAVE)
                )
            } returns false
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>()
            }

            val result = converter.convert(
                listOf(attendInfo(startDate = "20260427", endDate = "20260428", status = "N"))
            )

            assertThat(result.convertedScheduleCount).isEqualTo(1)
            assertThat(result.skippedIdempotent).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("convert - 필터")
    inner class ConvertFilters {

        @Test
        @DisplayName("AttendType 필터 미통과 - skipped_attend_type_filter 증가")
        fun attendType_outOfAnnualLeaveCodes() {
            val result = converter.convert(
                listOf(
                    attendInfo(attendType = "01"),
                    attendInfo(attendType = "02"),
                    attendInfo(attendType = "99")
                )
            )

            assertThat(result.skippedAttendTypeFilter).isEqualTo(3)
            assertThat(result.convertedScheduleCount).isEqualTo(0)
            verify(exactly = 0) { employeeRepository.findByEmployeeCodeIn(any()) }
            verify(exactly = 0) { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("직무 필터 미통과 - skipped_job_filter 증가 (Status='N' 분기만 적용)")
        fun jobFilter_skip() {
            val emp = employee(jobCode = "영업직")
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)

            val result = converter.convert(
                listOf(attendInfo(status = "N"))
            )

            assertThat(result.skippedJobFilter).isEqualTo(1)
            assertThat(result.convertedScheduleCount).isEqualTo(0)
            verify(exactly = 0) { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("Status='Y' 분기 - 직무 필터 미적용, 영업직도 삭제 진행")
        fun statusY_noJobFilter() {
            val emp = employee(jobCode = "영업직")
            val existing = listOf(
                TeamMemberSchedule().apply {
                    workingDate = LocalDate.of(2026, 4, 27); workingType = WorkingType.ANNUAL_LEAVE; this.employee = emp
                }
            )
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    eq(emp), any(), any(), eq(WorkingType.ANNUAL_LEAVE)
                )
            } returns existing
            every { teamMemberScheduleRepository.deleteAll(existing) } returns Unit

            val result = converter.convert(
                listOf(attendInfo(status = "Y"))
            )

            assertThat(result.deletedScheduleCount).isEqualTo(1)
            assertThat(result.skippedJobFilter).isEqualTo(0)
            verify { teamMemberScheduleRepository.deleteAll(existing) }
        }

        @Test
        @DisplayName("Status 가 N/Y 외 - 카운트 미증가 silent skip")
        fun unknownStatus_silentSkip() {
            val result = converter.convert(
                listOf(attendInfo(status = "정상"))
            )

            assertThat(result).isEqualTo(ScheduleConversionSummary.ZERO)
            verify(exactly = 0) { employeeRepository.findByEmployeeCodeIn(any()) }
        }
    }

    @Nested
    @DisplayName("convert - 부분 실패")
    inner class ConvertPartial {

        @Test
        @DisplayName("직원 매핑 실패 - 해당 레코드 skip, 다른 레코드는 변환 진행")
        fun employeeNotFound_partialContinue() {
            val emp = employee(employeeCode = "100123")
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any())
            } returns false
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>()
            }

            val result = converter.convert(
                listOf(
                    attendInfo(employeeCode = "UNKNOWN", status = "N"),
                    attendInfo(employeeCode = "100123", status = "N")
                )
            )

            assertThat(result.skippedEmployeeNotFound).isEqualTo(1)
            assertThat(result.convertedScheduleCount).isEqualTo(1)
        }

        @Test
        @DisplayName("Status='Y' 분기 직원 매핑 실패 - skipped_employee_not_found 증가")
        fun statusY_employeeNotFound() {
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()

            val result = converter.convert(
                listOf(attendInfo(employeeCode = "UNKNOWN", status = "Y"))
            )

            assertThat(result.skippedEmployeeNotFound).isEqualTo(1)
            assertThat(result.deletedScheduleCount).isEqualTo(0)
        }

        @Test
        @DisplayName("일자 역전 (start > end) - skip")
        fun invalidDateRange_skip() {
            val emp = employee()
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)

            val result = converter.convert(
                listOf(
                    attendInfo(startDate = "20260429", endDate = "20260427", status = "N")
                )
            )

            assertThat(result.convertedScheduleCount).isEqualTo(0)
            assertThat(result.skippedJobFilter).isEqualTo(0)
            verify(exactly = 0) { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) }
        }

        @Test
        @DisplayName("AttendType 필터 미통과 + Status='N' 정상건 혼합 - 정상건만 변환")
        fun mixed_filterAndConvert() {
            val emp = employee()
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(emp)
            every {
                teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(any(), any(), any())
            } returns false
            every { teamMemberScheduleRepository.saveAll(any<List<TeamMemberSchedule>>()) } answers {
                firstArg<List<TeamMemberSchedule>>()
            }

            val result = converter.convert(
                listOf(
                    attendInfo(attendType = "01", status = "정상"),
                    attendInfo(attendType = "14", status = "N")
                )
            )

            assertThat(result.skippedAttendTypeFilter).isEqualTo(1)
            assertThat(result.convertedScheduleCount).isEqualTo(1)
        }
    }
}
