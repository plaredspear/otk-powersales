package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.service.AppointmentInsertService
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

@DisplayName("AppointmentInsertService 테스트")
class AppointmentInsertServiceTest {

    private val appointmentRepository: AppointmentRepository = mockk(relaxUnitFun = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxUnitFun = true)
    private val transactionManager: PlatformTransactionManager = mockk(relaxed = true)
    private val service = AppointmentInsertService(appointmentRepository, employeeRepository, transactionManager)

    private val savedRows = mutableListOf<Appointment>()

    @BeforeEach
    fun setUpDefaults() {
        savedRows.clear()
        every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
        every { employeeRepository.existsByEmployeeCodeIsNull() } returns false
        val seq = AtomicLong(0)
        every { appointmentRepository.getNextAppointmentNameSeq() } answers { seq.incrementAndGet() }
        every { appointmentRepository.save(any<Appointment>()) } answers {
            firstArg<Appointment>().also { savedRows += it }
        }
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

    private fun employee(empCode: String): Employee = Employee(employeeCode = empCode, name = "테스트사원")

    @Nested
    @DisplayName("insert - Happy Path")
    inner class InsertHappy {

        @Test
        @DisplayName("정상 1건 - INSERT, success_count=1, empCodeExist=true")
        fun insert_success_empCodeExists() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(employee("100123"))

            val result = service.insert(listOf(command()))

            verify { appointmentRepository.save(any<Appointment>()) }
            val saved = savedRows.single()
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

            val result = service.insert(listOf(command(employeeCode = "999999")))

            assertThat(savedRows.single().empCodeExist).isFalse()
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("같은 페이로드 재호출 - 중복 INSERT (멱등성 미보장, 의도된 동작)")
        fun insert_duplicateAllowed() {
            service.insert(listOf(command()))
            service.insert(listOf(command()))

            verify(exactly = 2) { appointmentRepository.save(any<Appointment>()) }
        }

        @Test
        @DisplayName("name 채번 - SF AutoNumber AP{00000000} 정합 포맷")
        fun insert_nameAutoNumber() {
            service.insert(listOf(command(), command()))

            assertThat(savedRows.map { it.name }).containsExactly("AP00000001", "AP00000002")
        }
    }

    @Nested
    @DisplayName("insert - 레거시 정합 (수신 필드 명시 필수/형식 검증 제거 — 전 행 raw INSERT)")
    inner class InsertLegacyAlignment {

        @Test
        @DisplayName("EmployeeCode 누락 - 검증 없이 raw 적재 (empCodeExist=false), employeeCode=null")
        fun insert_missingEmployeeCode_stored() {
            val result = service.insert(listOf(command(employeeCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            val saved = savedRows.single()
            assertThat(saved.employeeCode).isNull()
            assertThat(saved.empCodeExist).isFalse()
        }

        @Test
        @DisplayName("JobCode 누락 - 검증 없이 raw 적재, jobCode=null")
        fun insert_missingJobCode_stored() {
            val result = service.insert(listOf(command(jobCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedRows.single().jobCode).isNull()
        }

        @Test
        @DisplayName("AppointDate 형식 오류 - 거부하지 않고 2999-12-31 센티넬로 흡수 (레거시 전체실패 버그 미재현)")
        fun insert_invalidAppointDate_sentinel() {
            val result = service.insert(listOf(command(appointDate = "2026-04-01")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedRows.single().appointDate).isEqualTo(LocalDate.of(2999, 12, 31))
        }

        @Test
        @DisplayName("AppointDate 빈값 / 00000000 - 2999-12-31 센티넬 저장 (레거시 convertStringToDate 정합)")
        fun insert_emptyAppointDateSentinel() {
            val result = service.insert(
                listOf(
                    command(appointDate = ""),
                    command(appointDate = "00000000")
                )
            )

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedRows).allMatch { it.appointDate == LocalDate.of(2999, 12, 31) }
        }

        @Test
        @DisplayName("필수 필드 모두 누락 행도 전부 적재 (검증 게이트 없음)")
        fun insert_allMissing_allStored() {
            val result = service.insert(
                listOf(command(), command(employeeCode = null, jobCode = null, appointDate = "bad"))
            )

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("빈 문자열('') 필드 - null 정규화 저장 (SF 플랫폼 '' → null 정합), 공백만은 그대로")
        fun insert_emptyStringNormalizedToNull() {
            val result = service.insert(
                listOf(command(employeeCode = "", afterOrgCode = "", afterOrgName = " ", jobCode = ""))
            )

            assertThat(result.successCount).isEqualTo(1)
            val saved = savedRows.single()
            assertThat(saved.employeeCode).isNull()
            assertThat(saved.afterOrgCode).isNull()
            assertThat(saved.jobCode).isNull()
            // 공백만 있는 문자열은 SF 도 그대로 저장하므로 정규화하지 않는다.
            assertThat(saved.afterOrgName).isEqualTo(" ")
            // '' 사번은 사원 집합에 존재할 수 없으므로 empCodeExist=false (contains('') 정합).
            assertThat(saved.empCodeExist).isFalse()
        }

        @Test
        @DisplayName("사번 null + EmpCode null 사원 존재 - empCodeExist=true (SF contains(null) 정합)")
        fun insert_nullEmployeeCode_nullEmployeeExists() {
            every { employeeRepository.existsByEmployeeCodeIsNull() } returns true

            service.insert(listOf(command(employeeCode = null)))

            assertThat(savedRows.single().empCodeExist).isTrue()
        }

        @Test
        @DisplayName("사번 '' 은 EmpCode null 사원이 있어도 empCodeExist=false (contains('') ≠ contains(null))")
        fun insert_emptyEmployeeCode_notMatchedToNullEmployee() {
            every { employeeRepository.existsByEmployeeCodeIsNull() } returns true

            service.insert(listOf(command(employeeCode = "")))

            assertThat(savedRows.single().empCodeExist).isFalse()
        }
    }

    @Nested
    @DisplayName("insert - 행 격리 (SF Database.insert allOrNone=false 정합)")
    inner class InsertRowIsolation {

        @Test
        @DisplayName("한 행 저장 실패 - 실패 행만 failures, 나머지 행은 정상 적재")
        fun insert_rowFailureIsolated() {
            every { appointmentRepository.save(any<Appointment>()) } answers {
                val row = firstArg<Appointment>()
                if (row.employeeCode == "BAD001") throw RuntimeException("DB 제약 위반")
                row.also { savedRows += it }
            }

            val result = service.insert(
                listOf(
                    command(employeeCode = "100123"),
                    command(employeeCode = "BAD001"),
                    command(employeeCode = "100456")
                )
            )

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("BAD001")
            assertThat(result.savedAppointments.map { it.employeeCode })
                .containsExactly("100123", "100456")
        }
    }
}
