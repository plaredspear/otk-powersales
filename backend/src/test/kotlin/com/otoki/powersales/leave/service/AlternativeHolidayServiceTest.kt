package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.leave.exception.AltHolidayConfirmDateIsHolidayException
import com.otoki.powersales.leave.exception.AltHolidayDuplicateException
import com.otoki.powersales.leave.exception.EmployeeNotFoundException
import com.otoki.powersales.leave.repository.AlternativeHolidayRepository
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.EmployeeRepository
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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AlternativeHolidayService 테스트")
class AlternativeHolidayServiceTest {

    @Mock private lateinit var alternativeHolidayRepository: AlternativeHolidayRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var validator: AlternativeHolidayValidator
    @InjectMocks private lateinit var service: AlternativeHolidayService

    // 2026-03-07 = 토요일, 2026-03-09 = 월요일
    private val saturday = LocalDate.of(2026, 3, 7)
    private val monday = LocalDate.of(2026, 3, 9)

    @Nested
    @DisplayName("createAlternativeHoliday - 사원 본인 대체휴무 신청")
    inner class CreateTests {

        @Test
        @DisplayName("정상 신청 - 토요일 근무 대휴 -> 신규 생성, createdBy=본인 사번")
        fun create_success() {
            val employee = createEmployee()
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(alternativeHolidayRepository.save(any<AlternativeHoliday>()))
                .thenAnswer { it.getArgument<AlternativeHoliday>(0) }

            val result = service.createAlternativeHoliday(1L, saturday, monday)

            assertThat(result.status).isEqualTo("신규")
            assertThat(result.actualWorkDate).isEqualTo(saturday)
            assertThat(result.targetAltHolidayDate).isEqualTo(monday)
        }

        @Test
        @DisplayName("사용자 없음 - 존재하지 않는 userId -> EmployeeNotFoundException")
        fun create_userNotFound() {
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.createAlternativeHoliday(999L, saturday, monday) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("검증 실패 - 신청일이 공휴일 -> AltHolidayConfirmDateIsHolidayException")
        fun create_validationFails() {
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            doThrow(AltHolidayConfirmDateIsHolidayException()).whenever(validator).validateConfirmDate(monday)

            assertThatThrownBy { service.createAlternativeHoliday(1L, saturday, monday) }
                .isInstanceOf(AltHolidayConfirmDateIsHolidayException::class.java)
        }

        @Test
        @DisplayName("중복 신청 -> AltHolidayDuplicateException")
        fun create_duplicate() {
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(createEmployee()))
            doThrow(AltHolidayDuplicateException()).whenever(validator).validateNoDuplicate(1L, saturday)

            assertThatThrownBy { service.createAlternativeHoliday(1L, saturday, monday) }
                .isInstanceOf(AltHolidayDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("getAlternativeHolidays - 사원 본인 이력 조회")
    inner class GetTests {

        @Test
        @DisplayName("정상 조회 - 기간 지정 -> 해당 기간 이력 반환")
        fun get_withDateRange() {
            val employee = createEmployee()
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            val altHoliday = createAltHoliday()
            whenever(alternativeHolidayRepository.findByEmployeeIdAndActualWorkDateBetweenOrderByCreatedAtDesc(
                1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)
            )).thenReturn(listOf(altHoliday))

            val result = service.getAlternativeHolidays(1L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))

            assertThat(result).hasSize(1)
            assertThat(result[0].actualWorkDate).isEqualTo(saturday)
        }

        @Test
        @DisplayName("기간 미지정 - 최근 3개월~오늘 기본값 적용")
        fun get_defaultDateRange() {
            val employee = createEmployee()
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))
            whenever(alternativeHolidayRepository.findByEmployeeIdAndActualWorkDateBetweenOrderByCreatedAtDesc(
                org.mockito.kotlin.eq(1L), any(), any()
            )).thenReturn(emptyList())

            val result = service.getAlternativeHolidays(1L, null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("사용자 없음 -> EmployeeNotFoundException")
        fun get_userNotFound() {
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { service.getAlternativeHolidays(999L, null, null) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        name: String = "홍길동"
    ): Employee = Employee(
        id = id,
        sfid = "SF001",
        employeeCode = employeeCode,
        name = name,
        status = "재직"
    )

    private fun createAltHoliday(
        id: Long = 1L,
        status: String = "신규"
    ): AlternativeHoliday = AlternativeHoliday(
        id = id,
        employeeId = 1L,
        employeeName = "홍길동",
        actualWorkDate = saturday,
        targetAltHolidayDate = monday,
        status = status,
        createdBy = "12345678"
    )
}
