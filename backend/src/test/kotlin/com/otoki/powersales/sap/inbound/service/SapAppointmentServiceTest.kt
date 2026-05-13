package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.inbound.dto.appointment.AppointmentRequestItem
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.schedule.service.AppointmentInsertService
import com.otoki.powersales.schedule.service.dto.AppointmentInsertCommand
import com.otoki.powersales.schedule.service.dto.AppointmentInsertFailedRow
import com.otoki.powersales.schedule.service.dto.AppointmentInsertResult
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAppointmentService 어댑터 테스트")
class SapAppointmentServiceTest {

    @Mock
    private lateinit var appointmentInsertService: AppointmentInsertService

    @Mock
    private lateinit var appointmentUserProfileUpdater: AppointmentUserProfileUpdater

    @InjectMocks
    private lateinit var service: SapAppointmentService

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
            whenever(appointmentInsertService.insert(any())).thenReturn(
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    savedAppointments = saved
                )
            )

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            verify(appointmentUserProfileUpdater).updateUserProfiles(saved)
        }

        @Test
        @DisplayName("후처리 실패 - 적재는 유지, success_count 변경 없음")
        fun updaterFailure_savedKept() {
            whenever(appointmentInsertService.insert(any())).thenReturn(
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    savedAppointments = listOf(savedAppointment())
                )
            )
            whenever(appointmentUserProfileUpdater.updateUserProfiles(any<List<Appointment>>()))
                .thenThrow(RuntimeException("updater failure"))

            val detail = service.insert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("savedAppointments 비어있을 시 후처리 호출 없음")
        fun emptySaved_noUpdaterCall() {
            whenever(appointmentInsertService.insert(any())).thenReturn(
                AppointmentInsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(AppointmentInsertFailedRow(null, "EmployeeCode 필수")),
                    savedAppointments = emptyList()
                )
            )

            service.insert(listOf(item(employeeCode = null)))

            verify(appointmentUserProfileUpdater, never()).updateUserProfiles(any<List<Appointment>>())
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            whenever(appointmentInsertService.insert(any())).thenReturn(
                AppointmentInsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AppointmentInsertFailedRow("100456", "JobCode 필수")),
                    savedAppointments = listOf(savedAppointment())
                )
            )

            val detail = service.insert(listOf(item(), item(employeeCode = "100456", jobCode = null)))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("100456")
            assertThat(detail.failures.single().reason).isEqualTo("JobCode 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파, 후처리 호출 없음 (audit 은 Aspect 책임)")
        fun domainThrow_propagated_noUpdater() {
            whenever(appointmentInsertService.insert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.insert(listOf(item())) }
                .isInstanceOf(IllegalStateException::class.java)

            verify(appointmentUserProfileUpdater, never()).updateUserProfiles(any<List<Appointment>>())
        }

        @Test
        @DisplayName("DTO 매핑: AppointmentRequestItem → AppointmentInsertCommand 15개 필드")
        fun dtoMapping_itemToCommand() {
            whenever(appointmentInsertService.insert(any())).thenReturn(
                AppointmentInsertResult(1, 0, emptyList(), listOf(savedAppointment()))
            )
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

            val captor = argumentCaptor<List<AppointmentInsertCommand>>()
            verify(appointmentInsertService).insert(captor.capture())
            val command = captor.firstValue.single()
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
