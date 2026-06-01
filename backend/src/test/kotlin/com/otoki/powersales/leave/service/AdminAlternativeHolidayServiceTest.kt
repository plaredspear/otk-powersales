package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.dto.request.AlternativeHolidayApproveRequest
import com.otoki.powersales.leave.dto.request.AlternativeHolidayCreateRequest
import com.otoki.powersales.leave.dto.request.AlternativeHolidayRejectRequest
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.exception.*
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.leave.enums.AltHolidayStatus
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.service.TeamMemberScheduleOwnerResolver
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

@DisplayName("AdminAlternativeHolidayService 테스트")
class AdminAlternativeHolidayServiceTest {

    private val alternativeHolidayRepository: AlternativeHolidayRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val validator: AlternativeHolidayValidator = mockk(relaxUnitFun = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver = mockk()

    private val service = AdminAlternativeHolidayService(
        alternativeHolidayRepository,
        employeeRepository,
        validator,
        teamMemberScheduleRepository,
        teamMemberScheduleOwnerResolver,
    )

    init {
        every { teamMemberScheduleOwnerResolver.resolveOwner(any()) } returns null
    }

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
            every { employeeRepository.findByEmployeeCode(request.employeeCode) } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { alternativeHolidayRepository.save(any<AlternativeHoliday>()) } answers { firstArg() }

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
            every { employeeRepository.findByEmployeeCode("99999999") } returns Optional.empty()

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
            every { employeeRepository.findByEmployeeCode("12345678") } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { validator.validateConfirmDate(monday) } throws AltHolidayConfirmDateIsHolidayException()

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
            every { employeeRepository.findByEmployeeCode("12345678") } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { validator.validateConfirmDate(saturday) } throws AltHolidayConfirmDateIsWeekendException()

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
            every { employeeRepository.findByEmployeeCode("12345678") } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { validator.validateActualWorkDate(wednesday) } throws AltHolidayActualDateIsWeekdayException()

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
            every { employeeRepository.findByEmployeeCode("12345678") } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { validator.validateWorkScheduleExists(any(), any()) } throws AltHolidayNoWorkScheduleException()

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
            every { employeeRepository.findByEmployeeCode("12345678") } returns Optional.of(createEmployee())
            every { employeeRepository.findById(1L) } returns Optional.of(createAdmin())
            every { validator.validateNoDuplicate(10L, saturday) } throws AltHolidayDuplicateException()

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
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)
            every { employeeRepository.findById(10L) } returns Optional.of(createEmployee())
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } answers { firstArg() }

            val result = service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest())

            assertThat(result.status).isEqualTo("승인")
            assertThat(result.confirmAltHolidayDate).isEqualTo(monday)
            verify { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) }
        }

        @Test
        @DisplayName("정상 승인 - 확정일 조정 -> 조정 사유 자동 기입")
        fun approve_success_withAdjustedDate() {
            val altHoliday = createAltHoliday(status = "신규")
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)
            every { employeeRepository.findById(10L) } returns Optional.of(createEmployee())
            every { teamMemberScheduleRepository.save(any<TeamMemberSchedule>()) } answers { firstArg() }

            val result = service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest(confirmAltHolidayDate = tuesday))

            assertThat(result.status).isEqualTo("승인")
            assertThat(result.confirmAltHolidayDate).isEqualTo(tuesday)
            assertThat(altHoliday.changeReason).isEqualTo("관리자 조정")
        }

        @Test
        @DisplayName("이미 승인된 건 -> AltHolidayInvalidStatusException")
        fun approve_invalidStatus() {
            val altHoliday = createAltHoliday(status = "승인")
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)

            assertThatThrownBy { service.approveAlternativeHoliday(1L, AlternativeHolidayApproveRequest()) }
                .isInstanceOf(AltHolidayInvalidStatusException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 ID -> AltHolidayNotFoundException")
        fun approve_notFound() {
            every { alternativeHolidayRepository.findById(99999L) } returns Optional.empty()

            assertThatThrownBy { service.approveAlternativeHoliday(99999L, AlternativeHolidayApproveRequest()) }
                .isInstanceOf(AltHolidayNotFoundException::class.java)
        }

        @Test
        @DisplayName("확정일이 공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun approve_confirmDateIsHoliday() {
            val altHoliday = createAltHoliday(status = "신규")
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)
            every { validator.validateConfirmDate(tuesday) } throws AltHolidayConfirmDateIsHolidayException()

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
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)

            val result = service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "인력 부족"))

            assertThat(result.status).isEqualTo("반려")
            assertThat(altHoliday.changeReason).isEqualTo("인력 부족")
        }

        @Test
        @DisplayName("사유 없음 -> ChangeReasonRequiredException")
        fun reject_noReason() {
            val altHoliday = createAltHoliday(status = "신규")
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)

            assertThatThrownBy {
                service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "  "))
            }.isInstanceOf(ChangeReasonRequiredException::class.java)
        }

        @Test
        @DisplayName("이미 승인된 건 반려 시도 -> AltHolidayInvalidStatusException")
        fun reject_invalidStatus() {
            val altHoliday = createAltHoliday(status = "승인")
            every { alternativeHolidayRepository.findById(1L) } returns Optional.of(altHoliday)

            assertThatThrownBy {
                service.rejectAlternativeHoliday(1L, AlternativeHolidayRejectRequest(changeReason = "테스트"))
            }.isInstanceOf(AltHolidayInvalidStatusException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 ID -> AltHolidayNotFoundException")
        fun reject_notFound() {
            every { alternativeHolidayRepository.findById(99999L) } returns Optional.empty()

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
        actualWorkDate = saturday,
        targetAltHolidayDate = monday,
        status = AltHolidayStatus.fromDisplayName(status),
        createdByEmpNo = "admin001"
    )
}
