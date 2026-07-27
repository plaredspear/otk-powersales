package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
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
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater = mockk()

    private lateinit var service: PostponedAppointmentBatchService

    private val today = LocalDate.of(2026, 3, 22)

    /** 발령 레코드의 발령일 — 배치 실행일(today) 과 구분되도록 다른 날짜를 쓴다. */
    private val appointmentDate = LocalDate.of(2026, 3, 20)

    @BeforeEach
    fun setUp() {
        service = PostponedAppointmentBatchService(
            employeeRepository, appointmentUserProfileUpdater
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
        @DisplayName("예약 발령 참조 보유 - 참조된 발령을 발령일자 기준으로 반영")
        fun appliesReferencedAppointment() {
            val reserved = createAppointment()
            val employee = createEmployee(crmWorkStartDate = today, postponedAppointment = reserved)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            val codeMap = mapOf("H10060:A055" to "OSC직")
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap
            every {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, reserved, appointmentDate, codeMap
                )
            } just runs
            every { appointmentUserProfileUpdater.updateUserProfileCache(employee) } just runs

            service.process(today)

            // 발령일자는 배치 실행일(today) 이 아니라 발령 레코드의 발령일이 적용된다.
            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, reserved, appointmentDate, codeMap
                )
            }
            verify(exactly = 0) {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, reserved, today, codeMap)
            }
        }

        @Test
        @DisplayName("발령일 없음 - 배치 실행일로 fallback")
        fun nullAppointDateFallsBackToToday() {
            val reserved = createAppointment(appointDate = null)
            val employee = createEmployee(crmWorkStartDate = today, postponedAppointment = reserved)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            val codeMap = emptyMap<String, String>()
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap
            every {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, reserved, today, codeMap)
            } just runs
            every { appointmentUserProfileUpdater.updateUserProfileCache(employee) } just runs

            service.process(today)

            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, reserved, today, codeMap)
            }
        }

        @Test
        @DisplayName("참조 없음 - 인사정보 미반영, 예약 표시만 해제")
        fun noReferenceOnlyClearsReservation() {
            // SF 는 start 쿼리에서 PostponedAppointment__c != null 로 이런 건을 배제한다.
            // 신규는 잔여 예약을 정리하기 위해 조회는 하되, 반영 없이 예약 표시만 해제한다.
            val employee = createEmployee(crmWorkStartDate = today, postponedAppointment = null)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns emptyMap()

            service.process(today)

            assertThat(employee.crmWorkStartDate).isNull()
            // 어떤 발령도 추측해서 반영하지 않는다 — 이 추측이 직무코드 회귀 사고의 원인이었다.
            verify(exactly = 0) {
                appointmentUserProfileUpdater.applyImmediateAppointment(any(), any(), any(), any())
            }
        }
    }

    private fun createEmployee(
        employeeCode: String = "100234",
        crmWorkStartDate: LocalDate? = null,
        postponedAppointment: Appointment? = null
    ): Employee = Employee(
        id = 1L,
        employeeCode = employeeCode,
        name = "테스트사원",
        crmWorkStartDate = crmWorkStartDate,
        postponedAppointment = postponedAppointment
    )

    private fun createAppointment(
        employeeCode: String = "100234",
        appointDate: LocalDate? = appointmentDate
    ): Appointment = Appointment(
        employeeCode = employeeCode,
        empCodeExist = true,
        afterOrgCode = "1111",
        afterOrgName = "테스트지점",
        jikchak = "D0052",
        jobCode = "A055",
        appointDate = appointDate
    )
}
