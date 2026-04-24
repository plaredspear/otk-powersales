package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AlternativeHolidayApproveRequest
import com.otoki.powersales.admin.dto.request.AlternativeHolidayCreateRequest
import com.otoki.powersales.admin.dto.request.AlternativeHolidayRejectRequest
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.exception.*
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.leave.service.AlternativeHolidayValidator
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
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
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAlternativeHolidayService 테스트")
class AdminAlternativeHolidayServiceTest {

    @Mock private lateinit var alternativeHolidayRepository: AlternativeHolidayRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var validator: AlternativeHolidayValidator
    @Mock private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    @InjectMocks private lateinit var service: AdminAlternativeHolidayService

    // 2026-03-07 = 토요일, 2026-03-09 = 월요일, 2026-03-10 = 화요일
    private val saturday = LocalDate.of(2026, 3, 7)
    private val monday = LocalDate.of(2026, 3, 9)
    private val tuesday = LocalDate.of(2026, 3, 10)
    private val wednesday = LocalDate.of(2026, 3, 11)

    @Nested
    @DisplayName("createAlternativeHoliday - 대체휴무 신청")
    inner class CreateTests {

        @Test
        @DisplayName("정상 신청 - 토요일 근무 대휴 -> 신규 생성")
        fun create_success() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = saturday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode(request.employeeCode)).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            whenever(alternativeHolidayRepository.save(any<AlternativeHoliday>()))
                .thenAnswer { it.getArgument<AlternativeHoliday>(0) }

            val result = service.createAlternativeHoliday(request, 1L)

            assertThat(result.status).isEqualTo("신규")
        }

        @Test
        @DisplayName("사원 없음 - 존재하지 않는 사번 -> EmployeeNotFoundException")
        fun create_employeeNotFound() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "99999999",
                actualWorkDate = saturday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode("99999999")).thenReturn(Optional.empty())

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("신청일이 공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun create_targetDateIsHoliday() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = saturday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode("12345678")).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            doThrow(AltHolidayConfirmDateIsHolidayException()).whenever(validator).validateConfirmDate(monday)

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(AltHolidayConfirmDateIsHolidayException::class.java)
        }

        @Test
        @DisplayName("신청일이 주말 -> AltHolidayConfirmDateIsWeekendException")
        fun create_targetDateIsWeekend() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = saturday,
                targetAltHolidayDate = saturday
            )
            whenever(employeeRepository.findByEmployeeCode("12345678")).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            doThrow(AltHolidayConfirmDateIsWeekendException()).whenever(validator).validateConfirmDate(saturday)

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(AltHolidayConfirmDateIsWeekendException::class.java)
        }

        @Test
        @DisplayName("대상일이 평일(공휴일 아님) -> AltHolidayActualDateIsWeekdayException")
        fun create_actualDateIsWeekday() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = wednesday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode("12345678")).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            doThrow(AltHolidayActualDateIsWeekdayException()).whenever(validator).validateActualWorkDate(wednesday)

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(AltHolidayActualDateIsWeekdayException::class.java)
        }

        @Test
        @DisplayName("근무 스케줄 없음 -> AltHolidayNoWorkScheduleException")
        fun create_noWorkSchedule() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = saturday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode("12345678")).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            doThrow(AltHolidayNoWorkScheduleException()).whenever(validator).validateWorkScheduleExists(any(), any())

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(AltHolidayNoWorkScheduleException::class.java)
        }

        @Test
        @DisplayName("중복 신청 -> AltHolidayDuplicateException")
        fun create_duplicate() {
            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = saturday,
                targetAltHolidayDate = monday
            )
            whenever(employeeRepository.findByEmployeeCode("12345678")).thenReturn(Optional.of(createEmployee()))
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createAdmin()))
            doThrow(AltHolidayDuplicateException()).whenever(validator).validateNoDuplicate(10L, saturday)

            assertThatThrownBy { service.createAlternativeHoliday(request, 1L) }
                .isInstanceOf(AltHolidayDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("approveAlternativeHoliday - 대체휴무 승인")
    inner class ApproveTests {

        @Test
        @DisplayName("정상 승인 - 확정일 지정 없음 -> 신청일로 승인")
        fun approve_success_noConfirmDate() {
            val altHoliday = createAltHoliday(status = "신규")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))
            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(createEmployee()))
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>()))
                .thenAnswer { it.getArgument<TeamMemberSchedule>(0) }

            val result = service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest())

            assertThat(result.status).isEqualTo("승인")
            assertThat(result.confirmAltHolidayDate).isEqualTo(monday)
            verify(teamMemberScheduleRepository).save(any<TeamMemberSchedule>())
        }

        @Test
        @DisplayName("정상 승인 - 확정일 조정 -> 조정 사유 자동 기입")
        fun approve_success_withAdjustedDate() {
            val altHoliday = createAltHoliday(status = "신규")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))
            whenever(employeeRepository.findById(10L)).thenReturn(Optional.of(createEmployee()))
            whenever(teamMemberScheduleRepository.save(any<TeamMemberSchedule>()))
                .thenAnswer { it.getArgument<TeamMemberSchedule>(0) }

            val result = service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest(confirmAltHolidayDate = tuesday))

            assertThat(result.status).isEqualTo("승인")
            assertThat(result.confirmAltHolidayDate).isEqualTo(tuesday)
            assertThat(altHoliday.changeReason).isEqualTo("관리자 조정")
        }

        @Test
        @DisplayName("이미 승인된 건 -> AltHolidayInvalidStatusException")
        fun approve_invalidStatus() {
            val altHoliday = createAltHoliday(status = "승인")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))

            assertThatThrownBy { service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest()) }
                .isInstanceOf(AltHolidayInvalidStatusException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 ID -> AltHolidayNotFoundException")
        fun approve_notFound() {
            whenever(alternativeHolidayRepository.findById(99999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.approveAlternativeHoliday(99999L, AlternativeHolidayApproveRequest()) }
                .isInstanceOf(AltHolidayNotFoundException::class.java)
        }

        @Test
        @DisplayName("확정일이 공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun approve_confirmDateIsHoliday() {
            val altHoliday = createAltHoliday(status = "신규")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))
            doThrow(AltHolidayConfirmDateIsHolidayException()).whenever(validator).validateConfirmDate(tuesday)

            assertThatThrownBy {
                service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest(confirmAltHolidayDate = tuesday))
            }.isInstanceOf(AltHolidayConfirmDateIsHolidayException::class.java)
        }
    }

    @Nested
    @DisplayName("rejectAlternativeHoliday - 대체휴무 반려")
    inner class RejectTests {

        @Test
        @DisplayName("정상 반려 - 사유 입력 -> 반려 처리")
        fun reject_success() {
            val altHoliday = createAltHoliday(status = "신규")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))

            val result = service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "인력 부족"))

            assertThat(result.status).isEqualTo("반려")
            assertThat(altHoliday.changeReason).isEqualTo("인력 부족")
        }

        @Test
        @DisplayName("사유 없음 -> ChangeReasonRequiredException")
        fun reject_noReason() {
            val altHoliday = createAltHoliday(status = "신규")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))

            assertThatThrownBy {
                service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "  "))
            }.isInstanceOf(ChangeReasonRequiredException::class.java)
        }

        @Test
        @DisplayName("이미 승인된 건 반려 시도 -> AltHolidayInvalidStatusException")
        fun reject_invalidStatus() {
            val altHoliday = createAltHoliday(status = "승인")
            whenever(alternativeHolidayRepository.findById(1L)).thenReturn(Optional.of(altHoliday))

            assertThatThrownBy {
                service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "테스트"))
            }.isInstanceOf(AltHolidayInvalidStatusException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 ID -> AltHolidayNotFoundException")
        fun reject_notFound() {
            whenever(alternativeHolidayRepository.findById(99999L)).thenReturn(Optional.empty())

            assertThatThrownBy {
                service.rejectAlternativeHoliday(99999L, AlternativeHolidayRejectRequest(changeReason = "테스트"))
            }.isInstanceOf(AltHolidayNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = 10L,
        employeeCode: String = "12345678",
        name: String = "홍길동"
    ): Employee = Employee(
        id = id,
        sfid = "SF001",
        employeeCode = employeeCode,
        name = name,
        status = "재직"
    )

    private fun createAdmin(
        id: Long = 1L,
        employeeCode: String = "admin001",
        name: String = "관리자"
    ): Employee = Employee(
        id = id,
        sfid = "SF_ADMIN",
        employeeCode = employeeCode,
        name = name,
        status = "재직"
    )

    private fun createAltHoliday(
        id: Long = 1L,
        status: String = "신규"
    ): AlternativeHoliday = AlternativeHoliday(
        id = id,
        employeeId = 10L,
        employeeName = "홍길동",
        actualWorkDate = saturday,
        targetAltHolidayDate = monday,
        status = status,
        createdBy = "admin001"
    )
}
