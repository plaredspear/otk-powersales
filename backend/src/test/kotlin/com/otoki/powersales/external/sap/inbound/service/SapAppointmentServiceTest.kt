package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.inbound.service.AppointmentUserProfileUpdater
import com.otoki.powersales.external.sap.inbound.service.SapAppointmentService
import com.otoki.powersales.external.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.schedule.service.AppointmentInsertService
import com.otoki.powersales.schedule.service.dto.AppointmentInsertCommand
import com.otoki.powersales.schedule.service.dto.AppointmentInsertFailedRow
import com.otoki.powersales.schedule.service.dto.AppointmentInsertResult
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("SapAppointmentService 어댑터 테스트")
class SapAppointmentServiceTest {

    private val appointmentInsertService: AppointmentInsertService = mockk()
    private val appointmentUserProfileUpdater: AppointmentUserProfileUpdater = mockk()
    private val service = SapAppointmentService(appointmentInsertService, appointmentUserProfileUpdater)

    private fun item(
        employeeCode: String? = "100123",
        afterOrgCode: String? = "11110",
        afterOrgName: String? = "서울지점",
        jobCode: String? = "J001",
        appointDate: String? = "20260401",
        workType: String? = "정규직"
    ): AppointmentRequestItem = AppointmentRequestItem(
        employeeCode = employeeCode,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jobCode = jobCode,
        appointDate = appointDate,
        workType = workType
    )

    private fun savedAppointment(empCode: String = "100123") = Appointment(
        id = 1L,
        employeeCode = empCode,
        empCodeExist = true,
        afterOrgCode = "11110",
        jobCode = "J001",
        appointDate = LocalDate.of(2026, 4, 1)
    )

    @Nested
    @DisplayName("insert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 적재 결과 → 후처리 호출")
        fun happy_domainSavedThenUpdater() {
            val saved = listOf(savedAppointment())
            every { appointmentInsertService.insert(any()) } returns
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    savedAppointments = saved
                )
            every { appointmentUserProfileUpdater.updateUserProfiles(saved) } just runs

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            verify { appointmentUserProfileUpdater.updateUserProfiles(saved) }
        }

        @Test
        @DisplayName("후처리 실패 - 적재는 유지, success_count 변경 없음")
        fun updaterFailure_savedKept() {
            every { appointmentInsertService.insert(any()) } returns
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    savedAppointments = listOf(savedAppointment())
                )
            every { appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()) } throws
                RuntimeException("updater failure")

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("savedAppointments 비어있을 시 후처리 호출 없음")
        fun emptySaved_noUpdaterCall() {
            every { appointmentInsertService.insert(any()) } returns
                AppointmentInsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(AppointmentInsertFailedRow(null, "EmployeeCode 필수")),
                    savedAppointments = emptyList()
                )

            service.insert(listOf(item(employeeCode = null)))

            verify(exactly = 0) { appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()) }
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            every { appointmentInsertService.insert(any()) } returns
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AppointmentInsertFailedRow("100456", "JobCode 필수")),
                    savedAppointments = listOf(savedAppointment())
                )
            every { appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()) } just runs

            val detail = service.insert(listOf(item(), item(employeeCode = "100456", jobCode = null)))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("100456")
            assertThat(detail.failures.single().reason).isEqualTo("JobCode 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파, 후처리 호출 없음 (audit 은 Aspect 책임)")
        fun domainThrow_propagated_noUpdater() {
            every { appointmentInsertService.insert(any()) } throws
                IllegalStateException("DB connection lost")

            assertThatThrownBy { service.insert(listOf(item())) }
                .isInstanceOf(IllegalStateException::class.java)

            verify(exactly = 0) { appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()) }
        }

        @Test
        @DisplayName("DTO 매핑: AppointmentRequestItem → AppointmentInsertCommand 15개 필드")
        fun dtoMapping_itemToCommand() {
            every { appointmentInsertService.insert(any()) } returns
                AppointmentInsertResult(1, 0, emptyList(), listOf(savedAppointment()))
            every { appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()) } just runs

            val items = listOf(
                AppointmentRequestItem(
                    employeeCode = "100123",
                    afterOrgCode = "11110",
                    afterOrgName = "서울지점",
                    jikchak = "D0052",
                    jikwee = "P10",
                    jobCode = "J001",
                    appointDate = "20260401",
                    workType = "정규직"
                )
            )

            service.insert(items)

            val captor = slot<List<AppointmentInsertCommand>>()
            verify { appointmentInsertService.insert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.employeeCode).isEqualTo("100123")
            assertThat(command.afterOrgCode).isEqualTo("11110")
            assertThat(command.jikchak).isEqualTo("D0052")
            assertThat(command.jikwee).isEqualTo("P10")
            assertThat(command.jobCode).isEqualTo("J001")
            assertThat(command.appointDate).isEqualTo("20260401")
            assertThat(command.workType).isEqualTo("정규직")
        }
    }
}
