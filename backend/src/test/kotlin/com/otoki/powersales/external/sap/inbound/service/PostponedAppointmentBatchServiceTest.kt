package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PostponedAppointmentBatchService 테스트 (#692)")
class PostponedAppointmentBatchServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val appointmentRepository: AppointmentRepository = mockk()
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater = mockk()

    private lateinit var service: PostponedAppointmentBatchService

    private val today = LocalDate.of(2026, 3, 22)

    @BeforeEach
    fun setUp() {
        service = PostponedAppointmentBatchService(
            employeeRepository, appointmentRepository, appointmentUserProfileUpdater
        )
    }

    @Nested
    @DisplayName("process")
    inner class ProcessTests {

        @Test
        @DisplayName("대상 없음 - 아무 처리 없이 종료")
        fun noTargets() {
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                emptyList()

            service.process(today)
        }

        @Test
        @DisplayName("정상 처리 - Appointment 조회 후 즉시 반영 수행")
        fun normalProcess() {
            val employee = createEmployee(crmWorkStartDate = today)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            val codeMap = mapOf("H20020:D0052" to "조장")
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap

            val appointment = createAppointment()
            every { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc("100234") } returns
                appointment
            every {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, appointment, today, codeMap)
            } just runs

            service.process(today)

            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, today, codeMap
                )
            }
        }

        @Test
        @DisplayName("Appointment 없음 - crmWorkStartDate만 null 초기화")
        fun noAppointmentFound() {
            val employee = createEmployee(crmWorkStartDate = today)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns emptyMap()

            every { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDesc("100234") } returns null

            service.process(today)

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
        appointDate = LocalDate.of(2026, 3, 20)
    )
}
