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

    /** 발령 레코드의 발령일 — 배치 실행일(today) 과 구분되도록 다른 날짜를 쓴다. */
    private val appointmentDate = LocalDate.of(2026, 3, 20)

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
            every { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc("100234") } returns
                appointment
            every {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, appointmentDate, codeMap
                )
            } just runs

            service.process(today)

            // 발령일자는 배치 실행일(today) 이 아니라 발령 레코드의 발령일이 적용된다.
            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, appointment, appointmentDate, codeMap
                )
            }
            verify(exactly = 0) {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, appointment, today, codeMap)
            }
        }

        @Test
        @DisplayName("발령일 없음 - 배치 실행일로 fallback")
        fun nullAppointDateFallsBackToToday() {
            val employee = createEmployee(crmWorkStartDate = today)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            val codeMap = emptyMap<String, String>()
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap

            val appointment = createAppointment(appointDate = null)
            every { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc("100234") } returns
                appointment
            every {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, appointment, today, codeMap)
            } just runs

            service.process(today)

            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(employee, appointment, today, codeMap)
            }
        }

        @Test
        @DisplayName("Appointment 없음 - crmWorkStartDate만 null 초기화")
        fun noAppointmentFound() {
            val employee = createEmployee(crmWorkStartDate = today)
            every { employeeRepository.findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(today) } returns
                listOf(employee)

            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns emptyMap()

            every { appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc("100234") } returns null

            service.process(today)

            assertThat(employee.crmWorkStartDate).isNull()
        }

        @Test
        @DisplayName("예약 발령 참조 보유 - 사원코드 조회 없이 참조된 발령을 반영")
        fun usesPostponedAppointmentReference() {
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

            service.process(today)

            verify {
                appointmentUserProfileUpdater.applyImmediateAppointment(
                    employee, reserved, appointmentDate, codeMap
                )
            }
            // 참조가 있으면 사원코드 기준 최신 발령 조회로 fallback 하지 않는다 —
            // 동일 발령일 다건에서 옛 발령을 집는 사고를 원천 차단하는 지점.
            verify(exactly = 0) {
                appointmentRepository.findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc(any())
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
