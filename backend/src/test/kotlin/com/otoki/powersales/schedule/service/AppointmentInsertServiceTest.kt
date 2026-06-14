package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.schedule.repository.AppointmentRepository
import com.otoki.powersales.schedule.service.dto.AppointmentInsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AppointmentInsertService 테스트")
class AppointmentInsertServiceTest {

    private val appointmentRepository: AppointmentRepository = mockk(relaxUnitFun = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxUnitFun = true)
    private val service = AppointmentInsertService(appointmentRepository, employeeRepository)

    @org.junit.jupiter.api.BeforeEach
    fun setUpDefaults() {
        every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
    }

    private fun command(
        employeeCode: String? = "100123",
        afterOrgCode: String? = "11110",
        afterOrgName: String? = "서울지점",
        jobCode: String? = "J001",
        appointDate: String? = "20260401",
        workType: String? = "정규직"
    ): AppointmentInsertCommand = AppointmentInsertCommand(
        employeeCode = employeeCode,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = null,
        jikwee = null,
        jikgub = null,
        workType = workType,
        manageType = null,
        jobCode = jobCode,
        workArea = null,
        jikjong = null,
        appointDate = appointDate,
        jobName = null,
        ordDetailCode = null,
        ordDetailNode = null
    )

    private fun mockSaveAll() {
        every { appointmentRepository.saveAll(any<List<Appointment>>()) } answers { firstArg<List<Appointment>>() }
    }

    private fun employee(empCode: String): Employee = Employee(employeeCode = empCode, name = "테스트사원")

    @Nested
    @DisplayName("insert - Happy Path")
    inner class InsertHappy {

        @Test
        @DisplayName("정상 1건 - INSERT, success_count=1, empCodeExist=true")
        fun insert_success_empCodeExists() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(employee("100123"))
            val savedSlot = slot<List<Appointment>>()
            every { appointmentRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<Appointment>>() }

            val result = service.insert(listOf(command()))

            verify { appointmentRepository.saveAll(any<List<Appointment>>()) }
            val saved = savedSlot.captured.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.empCodeExist).isTrue()
            assertThat(saved.appointDate).isEqualTo(LocalDate.of(2026, 4, 1))
            assertThat(saved.afterOrgCode).isEqualTo("11110")
            assertThat(saved.jobCode).isEqualTo("J001")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(result.savedAppointments).hasSize(1)
        }

        @Test
        @DisplayName("EmployeeCode 미매칭 - empCodeExist=false, INSERT 진행")
        fun insert_success_empCodeMissing() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("999999")) } returns emptyList()
            val savedSlot = slot<List<Appointment>>()
            every { appointmentRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<Appointment>>() }

            val result = service.insert(listOf(command(employeeCode = "999999")))

            verify { appointmentRepository.saveAll(any<List<Appointment>>()) }
            assertThat(savedSlot.captured.single().empCodeExist).isFalse()
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("같은 페이로드 재호출 - 중복 INSERT (멱등성 미보장, 의도된 동작)")
        fun insert_duplicateAllowed() {
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
            mockSaveAll()

            service.insert(listOf(command()))
            service.insert(listOf(command()))

            verify(exactly = 2) { appointmentRepository.saveAll(any<List<Appointment>>()) }
        }
    }

    @Nested
    @DisplayName("insert - Error Path")
    inner class InsertError {

        @Test
        @DisplayName("EmployeeCode 누락 - failures, INSERT 안 함")
        fun insert_missingEmployeeCode() {
            val result = service.insert(listOf(command(employeeCode = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("EmployeeCode 필수")
            verify(exactly = 0) { appointmentRepository.saveAll(any<List<Appointment>>()) }
        }

        @Test
        @DisplayName("JobCode 누락 - failures")
        fun insert_missingJobCode() {
            val result = service.insert(listOf(command(jobCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("JobCode 필수")
        }

        @Test
        @DisplayName("AppointDate 형식 오류 - failures")
        fun insert_invalidAppointDate() {
            val result = service.insert(listOf(command(appointDate = "2026-04-01")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("AppointDate YYYYMMDD 형식 오류")
        }

        @Test
        @DisplayName("부분 실패 - 정상 1건 + JobCode 누락 1건")
        fun insert_partialFailure() {
            every { employeeRepository.findByEmployeeCodeIn(any()) } returns listOf(employee("100123"))
            mockSaveAll()

            val result = service.insert(listOf(command(), command(employeeCode = "100456", jobCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
        }
    }
}
