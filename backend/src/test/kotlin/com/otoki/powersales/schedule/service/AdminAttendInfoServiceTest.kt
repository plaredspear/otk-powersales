package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.sap.inbound.service.AttendInfoToScheduleConverter
import com.otoki.powersales.schedule.dto.request.AdminAttendInfoCreateRequest
import com.otoki.powersales.schedule.dto.request.AdminAttendInfoUpdateRequest
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.AttendInfoNotFoundException
import com.otoki.powersales.schedule.exception.InvalidAttendInfoDateException
import com.otoki.powersales.schedule.exception.InvalidAttendInfoDateRangeException
import com.otoki.powersales.schedule.exception.InvalidAttendInfoStatusException
import com.otoki.powersales.schedule.exception.InvalidAttendInfoTypeException
import com.otoki.powersales.schedule.repository.AttendInfoRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAttendInfoService 테스트")
class AdminAttendInfoServiceTest {

    @Mock
    private lateinit var attendInfoRepository: AttendInfoRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Mock
    private lateinit var attendInfoToScheduleConverter: AttendInfoToScheduleConverter

    @InjectMocks
    private lateinit var service: AdminAttendInfoService

    @Nested
    @DisplayName("create - 보정 등록")
    inner class CreateTests {

        @Test
        @DisplayName("정상 등록 + Converter 호출")
        fun create_success_callsConverter() {
            val employee = newEmployee()
            whenever(employeeRepository.findByEmployeeCode("E001"))
                .thenReturn(Optional.of(employee))
            whenever(attendInfoRepository.save(any<AttendInfo>())).thenAnswer { invocation ->
                invocation.getArgument<AttendInfo>(0)
            }
            whenever(attendInfoToScheduleConverter.convert(any()))
                .thenReturn(ScheduleConversionSummary(3, 0, 0, 0, 0, 0))
            whenever(
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    any(), any(), any(), any()
                )
            ).thenReturn(emptyList())

            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
                reason = "SAP 적재 누락 보정 등록",
            )

            val result = service.create(request)

            assertThat(result.employeeCode).isEqualTo("E001")
            assertThat(result.attendType).isEqualTo("14")
            assertThat(result.attendTypeName).isEqualTo("연차")
            assertThat(result.conversionSummary?.convertedScheduleCount).isEqualTo(3)
            verify(attendInfoToScheduleConverter).convert(any())
        }

        @Test
        @DisplayName("status 무효 값 - 차단")
        fun create_invalidStatus_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "Z",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoStatusException::class.java)
        }

        @Test
        @DisplayName("attendType 무효 코드 - 차단")
        fun create_invalidAttendType_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "99",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoTypeException::class.java)
        }

        @Test
        @DisplayName("날짜 형식 오류 - 차단")
        fun create_invalidDateFormat_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "2026-05-18",
                endDate = "20260522",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoDateException::class.java)
        }

        @Test
        @DisplayName("종료일 < 시작일 - 차단")
        fun create_endBeforeStart_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260522",
                endDate = "20260518",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoDateRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("update - 수정 + cascade")
    inner class UpdateTests {

        @Test
        @DisplayName("수정 시 기존 연차 일정 cascade 삭제 후 신규 INSERT")
        fun update_cascadesDeleteAndInsert() {
            val employee = newEmployee()
            val existing = AttendInfo(
                id = 1L,
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            whenever(attendInfoRepository.findById(1L)).thenReturn(Optional.of(existing))
            whenever(employeeRepository.findByEmployeeCode("E001"))
                .thenReturn(Optional.of(employee))
            val oldSchedules = listOf(
                newSchedule(101L, employee, LocalDate.of(2026, 5, 18)),
                newSchedule(102L, employee, LocalDate.of(2026, 5, 19)),
            )
            whenever(
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 22), WorkingType.ANNUAL_LEAVE
                )
            ).thenReturn(oldSchedules)
            whenever(
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 27), WorkingType.ANNUAL_LEAVE
                )
            ).thenReturn(emptyList())
            whenever(attendInfoToScheduleConverter.convert(any()))
                .thenReturn(ScheduleConversionSummary(3, 0, 0, 0, 0, 0))

            val request = AdminAttendInfoUpdateRequest(
                startDate = "20260525",
                endDate = "20260527",
                reason = "기간 변경 보정",
            )

            val result = service.update(1L, request)

            assertThat(result.startDate).isEqualTo("20260525")
            assertThat(result.endDate).isEqualTo("20260527")
            verify(teamMemberScheduleRepository).deleteAll(oldSchedules)
            verify(attendInfoToScheduleConverter).convert(any())
            assertThat(result.conversionSummary?.deletedScheduleCount).isEqualTo(2)
            assertThat(result.conversionSummary?.convertedScheduleCount).isEqualTo(3)
        }

        @Test
        @DisplayName("미존재 id - 404")
        fun update_notFound_throws() {
            whenever(attendInfoRepository.findById(999L)).thenReturn(Optional.empty())
            assertThatThrownBy {
                service.update(999L, AdminAttendInfoUpdateRequest(status = "Y"))
            }.isInstanceOf(AttendInfoNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("delete - 삭제 + cascade")
    inner class DeleteTests {

        @Test
        @DisplayName("삭제 시 연결 연차 일정 cascade 삭제")
        fun delete_cascadesAnnualLeaveSchedules() {
            val employee = newEmployee()
            val existing = AttendInfo(
                id = 5L,
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            whenever(attendInfoRepository.findById(5L)).thenReturn(Optional.of(existing))
            whenever(employeeRepository.findByEmployeeCode("E001"))
                .thenReturn(Optional.of(employee))
            val oldSchedules = listOf(
                newSchedule(201L, employee, LocalDate.of(2026, 5, 18)),
                newSchedule(202L, employee, LocalDate.of(2026, 5, 19)),
                newSchedule(203L, employee, LocalDate.of(2026, 5, 20)),
            )
            whenever(
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 22), WorkingType.ANNUAL_LEAVE
                )
            ).thenReturn(oldSchedules)

            val result = service.delete(5L)

            assertThat(result.deletedScheduleCount).isEqualTo(3)
            verify(teamMemberScheduleRepository).deleteAll(oldSchedules)
            verify(attendInfoRepository).delete(existing)
        }

        @Test
        @DisplayName("연차류 외 코드 - cascade 0건")
        fun delete_nonAnnualLeave_noCascade() {
            val existing = AttendInfo(
                id = 6L,
                employeeCode = "E002",
                attendType = "99",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            whenever(attendInfoRepository.findById(6L)).thenReturn(Optional.of(existing))

            val result = service.delete(6L)

            assertThat(result.deletedScheduleCount).isEqualTo(0)
            verify(teamMemberScheduleRepository, never()).deleteAll(any<List<TeamMemberSchedule>>())
            verify(attendInfoRepository).delete(existing)
        }
    }

    private fun newEmployee() = Employee(
        id = 100L,
        employeeCode = "E001",
        name = "홍길동",
    ).apply {
        jobCode = "판촉직"
    }

    private fun newSchedule(id: Long, employee: Employee, date: LocalDate): TeamMemberSchedule {
        val schedule = TeamMemberSchedule(id = id)
        schedule.employee = employee
        schedule.workingDate = date
        schedule.workingType = WorkingType.ANNUAL_LEAVE
        return schedule
    }
}
