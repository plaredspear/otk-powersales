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
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.Optional

@DisplayName("PostponedAppointmentBatchService 테스트 (#692)")
class PostponedAppointmentBatchServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater = mockk()
    private val transactionManager: PlatformTransactionManager = mockk(relaxed = true)

    private lateinit var service: PostponedAppointmentBatchService

    private val today = LocalDate.of(2026, 3, 22)

    /** 발령 레코드의 발령일 — 배치 실행일(today) 과 구분되도록 다른 날짜를 쓴다. */
    private val appointmentDate = LocalDate.of(2026, 3, 20)

    @BeforeEach
    fun setUp() {
        service = PostponedAppointmentBatchService(
            employeeRepository, appointmentUserProfileUpdater, transactionManager
        )
    }

    private fun stubEmployees(vararg employees: Employee) {
        every { employeeRepository.findByCrmWorkStartDateAndPostponedAppointmentIsNotNull(today) } returns
            employees.toList()
        employees.forEach { employee ->
            every { employeeRepository.findById(employee.id) } returns Optional.of(employee)
        }
    }

    @Nested
    @DisplayName("process")
    inner class ProcessTests {

        @Test
        @DisplayName("대상 없음 - 아무 처리 없이 종료")
        fun noTargets() {
            every { employeeRepository.findByCrmWorkStartDateAndPostponedAppointmentIsNotNull(today) } returns
                emptyList()

            service.process(today)
        }

        @Test
        @DisplayName("예약 발령 참조 보유 - 배치 전용 반영(applyPostponedAppointment) 호출, User 캐시 미갱신")
        fun appliesReferencedAppointment() {
            val reserved = createAppointment()
            val employee = createEmployee(crmWorkStartDate = today, postponedAppointment = reserved)
            stubEmployees(employee)

            val codeMap = mapOf("H10060:A055" to "OSC직")
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap
            every {
                appointmentUserProfileUpdater.applyPostponedAppointment(employee, reserved, codeMap)
            } just runs

            service.process(today)

            verify {
                appointmentUserProfileUpdater.applyPostponedAppointment(employee, reserved, codeMap)
            }
            // SF 배치 정합 — 트리거 즉시 경로 함수를 쓰지 않고, User(Profile) 갱신도 하지 않는다
            // (SF PostponedAppointmentBatch 는 UserRole 갱신을 주석 처리해 의도적으로 미수행).
            verify(exactly = 0) {
                appointmentUserProfileUpdater.applyImmediateAppointment(any(), any(), any(), any())
            }
            verify(exactly = 0) {
                appointmentUserProfileUpdater.updateUserProfileCache(any())
            }
        }

        @Test
        @DisplayName("참조 없음(방어) - 아무것도 수정하지 않고 skip")
        fun noReferenceSkipsWithoutModification() {
            // 조회 조건(참조 non-null)상 도달 불가한 방어 경로 — SF 는 이런 건을 아예 조회하지 않고
            // 영영 건드리지 않으므로, 신규도 예약 표시를 포함해 아무것도 수정하지 않는다.
            val employee = createEmployee(crmWorkStartDate = today, postponedAppointment = null)
            stubEmployees(employee)
            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns emptyMap()

            service.process(today)

            assertThat(employee.crmWorkStartDate).isEqualTo(today)
            // 어떤 발령도 추측해서 반영하지 않는다 — 이 추측이 직무코드 회귀 사고의 원인이었다.
            verify(exactly = 0) {
                appointmentUserProfileUpdater.applyImmediateAppointment(any(), any(), any(), any())
            }
        }

        @Test
        @DisplayName("한 행 반영 실패 - 실패 행만 skip, 나머지 행은 정상 반영 (SF allOrNone=false 행 격리)")
        fun rowFailureIsolated() {
            val reserved1 = createAppointment(employeeCode = "100001")
            val reserved2 = createAppointment(employeeCode = "100002")
            val failing = createEmployee(id = 1L, employeeCode = "100001", crmWorkStartDate = today, postponedAppointment = reserved1)
            val healthy = createEmployee(id = 2L, employeeCode = "100002", crmWorkStartDate = today, postponedAppointment = reserved2)
            stubEmployees(failing, healthy)

            every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns emptyMap()
            every {
                appointmentUserProfileUpdater.applyPostponedAppointment(failing, reserved1, any())
            } throws RuntimeException("반영 실패")
            every {
                appointmentUserProfileUpdater.applyPostponedAppointment(healthy, reserved2, any())
            } just runs

            service.process(today)

            // 실패 행이 나머지 행의 반영을 막지 않는다.
            verify {
                appointmentUserProfileUpdater.applyPostponedAppointment(healthy, reserved2, any())
            }
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "100234",
        crmWorkStartDate: LocalDate? = null,
        postponedAppointment: Appointment? = null
    ): Employee = Employee(
        id = id,
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
