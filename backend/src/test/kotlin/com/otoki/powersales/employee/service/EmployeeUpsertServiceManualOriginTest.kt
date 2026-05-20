package com.otoki.powersales.employee.service

import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
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
 * Spec #579 — EmployeeUpsertService 가 origin=MANUAL 직원을 보호하는지 검증.
 *
 * 어댑터 ↔ 도메인 분리(#635 P2-B) 후 보호 로직은 도메인 서비스로 이전. 어댑터 측 audit 트리거 검증은
 * [com.otoki.powersales.sap.inbound.service.SapEmployeeMasterServiceTest] 의 `manualOriginProtected_extraAudit` 케이스 참조.
 */
@DisplayName("EmployeeUpsertService - MANUAL 보호 테스트")
class EmployeeUpsertServiceManualOriginTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk()
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxUnitFun = true)

    private val service = EmployeeUpsertService(
        employeeRepository,
        systemCodeMasterRepository,
        eventPublisher,
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
    @DisplayName("origin=MANUAL 직원 보호")
    inner class ManualProtection {

        @Test
        @DisplayName("MANUAL 직원 - 갱신 차단, save 호출 안 됨, 카운트 미반영")
        fun manualOriginNotUpdated() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "기존관리자").apply {
                origin = EmployeeOrigin.MANUAL
                appLoginActive = false
            }
            every { employeeRepository.findByEmployeeCodeIn(listOf("ADMIN-001")) } returns listOf(manualEmployee)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()

            val result = service.upsert(listOf(command(lockingFlag = "Y")))

            assertThat(manualEmployee.name).isEqualTo("기존관리자")
            assertThat(manualEmployee.appLoginActive).isFalse
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(result.failures).isEmpty()
            assertThat(result.protectedManualCodes).containsExactly("ADMIN-001")
            verify(exactly = 0) { employeeRepository.saveAll(any<List<Employee>>()) }
        }

        @Test
        @DisplayName("MANUAL 보호 발생 - protectedManualCodes 에 사번 1건 누적")
        fun manualOriginCodeRecorded() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "기존관리자").apply {
                origin = EmployeeOrigin.MANUAL
            }
            every { employeeRepository.findByEmployeeCodeIn(listOf("ADMIN-001")) } returns listOf(manualEmployee)
            every { systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")) } returns emptyList()

            val result = service.upsert(listOf(command()))

            assertThat(result.protectedManualCodes).containsExactly("ADMIN-001")
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
        @DisplayName("응답 형식 불변 - 정상 1건 + MANUAL 1건 + 검증실패 1건 -> success=1, failure=1, protected=1")
        fun responseShapeUnchanged() {
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
                    command(employeeCode = "ADMIN-001", employeeName = "공격이름"),
                    command(employeeCode = "E002", employeeName = "")
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("E002")
            assertThat(result.protectedManualCodes).containsExactly("ADMIN-001")
        }
    }
}
