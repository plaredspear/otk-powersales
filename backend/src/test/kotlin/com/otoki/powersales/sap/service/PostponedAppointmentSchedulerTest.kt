package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.entity.Appointment
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.entity.SystemCodeMaster
import com.otoki.powersales.sap.repository.AppointmentRepository
import com.otoki.powersales.sap.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("PostponedAppointmentScheduler 테스트")
class PostponedAppointmentSchedulerTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var appointmentRepository: AppointmentRepository

    @Mock
    private lateinit var appointmentUserProfileUpdater: AppointmentUserProfileUpdater

    private lateinit var scheduler: PostponedAppointmentScheduler

    private val today = LocalDate.of(2026, 3, 22)

    @BeforeEach
    fun setUp() {
        scheduler = PostponedAppointmentScheduler(
            employeeRepository, appointmentRepository, appointmentUserProfileUpdater
        )
    }

    @Nested
    @DisplayName("processPostponedAppointments")
    inner class ProcessTests {

        @Test
        @DisplayName("대상 없음 - 아무 처리 없이 종료")
        fun noTargets() {
            whenever(employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today))
                .thenReturn(emptyList())

            scheduler.processPostponedAppointments(today)
        }

        @Test
        @DisplayName("정상 처리 - Appointment 조회 후 즉시 반영 수행")
        fun normalProcess() {
            val employee = createEmployee(crmWorkStartDate = today)
            whenever(employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today))
                .thenReturn(listOf(employee))

            val codeMap = mapOf("H20020:D0052" to "조장")
            whenever(appointmentUserProfileUpdater.loadSystemCodeMap()).thenReturn(codeMap)

            val appointment = createAppointment()
            whenever(appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc("100234"))
                .thenReturn(appointment)

            scheduler.processPostponedAppointments(today)

            org.mockito.kotlin.verify(appointmentUserProfileUpdater).applyImmediateAppointment(
                employee, appointment, today, codeMap
            )
        }

        @Test
        @DisplayName("Appointment 없음 - crmWorkStartDate만 null 초기화")
        fun noAppointmentFound() {
            val employee = createEmployee(crmWorkStartDate = today)
            whenever(employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today))
                .thenReturn(listOf(employee))

            whenever(appointmentUserProfileUpdater.loadSystemCodeMap()).thenReturn(emptyMap())

            whenever(appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc("100234"))
                .thenReturn(null)

            scheduler.processPostponedAppointments(today)

            assertThat(employee.crmWorkStartDate).isNull()
        }
    }

    private fun createEmployee(
        employeeCode: String = "100234",
        crmWorkStartDate: LocalDate? = null
    ): Employee = Employee(
        id = 1L,
        employeeCode = employeeCode,
        name = "테스트사원",
        crmWorkStartDate = crmWorkStartDate
    )

    private fun createAppointment(
        employeeCode: String = "100234"
    ): Appointment = Appointment(
        employeeCode = employeeCode,
        empCodeExist = true,
        afterOrgCode = "1111",
        afterOrgName = "테스트지점",
        jikchak = "D0052",
        jobCode = "A055",
        appointDate = "20260320"
    )
}
