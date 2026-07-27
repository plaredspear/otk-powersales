package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.PostponedAppointmentNotFoundException
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("AdminEmployeeAppointmentConfirmService 테스트")
class AdminEmployeeAppointmentConfirmServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater = mockk()

    private val service = AdminEmployeeAppointmentConfirmService(
        employeeRepository, appointmentUserProfileUpdater
    )

    @Test
    @DisplayName("유예 발령 참조 보유 - 수동 확정 반영(applyManualConfirmAppointment) + User 캐시 갱신")
    fun confirmsReferencedAppointment() {
        val reserved = createAppointment()
        val employee = createEmployee(postponedAppointment = reserved)
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        val codeMap = mapOf("H10060:A049" to "판촉직")
        every { appointmentUserProfileUpdater.loadSystemCodeMap() } returns codeMap
        every {
            appointmentUserProfileUpdater.applyManualConfirmAppointment(employee, reserved, codeMap)
        } just runs
        every { appointmentUserProfileUpdater.updateUserProfileCache(employee) } just runs

        val response = service.confirmPostponedAppointment(1L)

        verify { appointmentUserProfileUpdater.applyManualConfirmAppointment(employee, reserved, codeMap) }
        // SF 수동 확정은 AppointmentTriggerHanlder.updateUser(@future) 를 호출한다 — 배치와 달리 User 갱신 수행.
        verify { appointmentUserProfileUpdater.updateUserProfileCache(employee) }
        assertThat(response.id).isEqualTo(1L)
        // 트리거/배치 반영 함수를 오용하지 않는다 (제3 변형 전용 함수 사용).
        verify(exactly = 0) {
            appointmentUserProfileUpdater.applyImmediateAppointment(any(), any(), any(), any())
        }
        verify(exactly = 0) {
            appointmentUserProfileUpdater.applyPostponedAppointment(any(), any(), any())
        }
    }

    @Test
    @DisplayName("유예 발령 참조 없음 - 409 예외, 아무것도 반영하지 않음 (SF checkPostponedAppointment 차단 동등)")
    fun rejectsWhenNoReservation() {
        val employee = createEmployee(postponedAppointment = null)
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        assertThrows<PostponedAppointmentNotFoundException> {
            service.confirmPostponedAppointment(1L)
        }

        verify(exactly = 0) {
            appointmentUserProfileUpdater.applyManualConfirmAppointment(any(), any(), any())
        }
    }

    @Test
    @DisplayName("사원 없음 - 404 예외")
    fun rejectsWhenEmployeeMissing() {
        every { employeeRepository.findWithEmployeeInfoById(99L) } returns null

        assertThrows<EmployeeNotFoundException> {
            service.confirmPostponedAppointment(99L)
        }
    }

    private fun createEmployee(
        postponedAppointment: Appointment? = null
    ): Employee = Employee(
        id = 1L,
        employeeCode = "100234",
        name = "테스트사원",
        crmWorkStartDate = postponedAppointment?.appointDate,
        postponedAppointment = postponedAppointment
    )

    private fun createAppointment(): Appointment = Appointment(
        employeeCode = "100234",
        empCodeExist = true,
        afterOrgCode = "1111",
        afterOrgName = "테스트지점",
        jikchak = "D0052",
        jobCode = "A049",
        appointDate = LocalDate.of(2026, 3, 1)
    )
}
