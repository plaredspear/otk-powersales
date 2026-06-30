package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

/**
 * EmployeeUpsertService — origin 처리 검증.
 *
 * 레거시 IF_REST_SAP_EmployeeMaster 정합으로 origin=MANUAL 보호 게이트(구 Spec #579)를 제거했다.
 * SAP 인바운드는 origin 구분 없이 EmpCode 기준으로 전 행을 upsert 하며, 신규 INSERT 시 origin 은 SAP default 를 유지한다.
 */
@DisplayName("EmployeeUpsertService - origin 처리 테스트")
class EmployeeUpsertServiceManualOriginTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

    private val service = EmployeeUpsertService(
        employeeRepository,
        systemCodeMasterRepository,
        eventPublisher,
        userRepository,
    )

    private fun command(
        employeeCode: String = "ADMIN-001",
        employeeName: String = "신규이름",
        lockingFlag: String? = null
    ) = EmployeeUpsertCommand(
        employeeCode = employeeCode,
        employeeName = employeeName,
        gender = null,
        homePhone = null,
        workPhone = null,
        workEmail = null,
        email = null,
        startDate = null,
        endDate = null,
        status = null,
        birthdate = null,
        orgCode = null,
        lockingFlag = lockingFlag
    )

    private fun stubSaveAllCapture(): CapturingSlot<List<Employee>> {
        val slot = slot<List<Employee>>()
        every { employeeRepository.saveAll(capture(slot)) } answers { firstArg<List<Employee>>() }
        return slot
    }

    @Nested
    @DisplayName("origin 무관 전 행 upsert (보호 게이트 제거 — 레거시 정합)")
    inner class OriginAgnostic {

        @Test
        @DisplayName("기존 origin=MANUAL 직원도 갱신됨 - save 호출, 카운트 반영")
        fun manualOriginUpdated() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "기존관리자").apply {
                origin = EmployeeOrigin.MANUAL
                appLoginActive = false
            }
            every { employeeRepository.findByEmployeeCodeIn(listOf("ADMIN-001")) } returns listOf(manualEmployee)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(employeeName = "갱신된이름", lockingFlag = "Y")))

            assertThat(manualEmployee.name).isEqualTo("갱신된이름")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.single().employeeCode).isEqualTo("ADMIN-001")
        }

        @Test
        @DisplayName("SAP 직원 정상 갱신 - origin=SAP 행은 기존 동작 유지")
        fun sapOriginUpdated() {
            val sapEmployee = Employee(employeeCode = "E001", name = "기존이름").apply {
                origin = EmployeeOrigin.SAP
            }
            every { employeeRepository.findByEmployeeCodeIn(listOf("E001")) } returns listOf(sapEmployee)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            stubSaveAllCapture()

            val result = service.upsert(
                listOf(command(employeeCode = "E001", employeeName = "변경된이름"))
            )

            assertThat(sapEmployee.name).isEqualTo("변경된이름")
            assertThat(result.successCount).isEqualTo(1)
            verify { employeeRepository.saveAll(any<List<Employee>>()) }
        }

        @Test
        @DisplayName("신규 INSERT - 매칭되는 기존 행 없음 -> origin=SAP default 유지")
        fun newInsertHasSapOrigin() {
            every { employeeRepository.findByEmployeeCodeIn(listOf("E999")) } returns emptyList()
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(employeeCode = "E999")))

            val saved = savedSlot.captured.single()
            assertThat(saved.origin).isEqualTo(EmployeeOrigin.SAP)
        }

        @Test
        @DisplayName("정상 1건 + MANUAL 1건 + 검증실패 1건 -> success=2(MANUAL 포함), failure=1")
        fun mixedRows() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "관리자").apply {
                origin = EmployeeOrigin.MANUAL
            }
            val sapEmployee = Employee(employeeCode = "E001", name = "기존이름").apply {
                origin = EmployeeOrigin.SAP
            }
            every { employeeRepository.findByEmployeeCodeIn(any<List<String>>()) } returns listOf(manualEmployee, sapEmployee)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()
            stubSaveAllCapture()

            val result = service.upsert(
                listOf(
                    command(employeeCode = "E001", employeeName = "변경된"),
                    command(employeeCode = "ADMIN-001", employeeName = "관리자갱신"),
                    command(employeeCode = "E002", employeeName = "")
                )
            )

            // MANUAL(ADMIN-001) 도 이제 갱신 대상 → SAP 직원과 함께 success=2.
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("E002")
        }
    }
}
