package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequestItem
import org.assertj.core.api.Assertions.assertThat
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

/**
 * Spec #579 — SAP 인바운드 직원 마스터 upsert 가 origin=MANUAL 직원을 보호하는지 검증.
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("SapEmployeeMasterService - MANUAL 보호 테스트")
class SapEmployeeMasterServiceManualOriginTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapEmployeeMasterService

    private fun item(
        employeeCode: String = "ADMIN-001",
        employeeName: String = "신규이름",
        lockingFlag: String? = null
    ) = EmployeeMasterRequestItem(
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
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("ADMIN-001")))
                .thenReturn(listOf(manualEmployee))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(lockingFlag = "Y")))

            assertThat(manualEmployee.name).isEqualTo("기존관리자")
            assertThat(manualEmployee.appLoginActive).isFalse
            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()
            verify(employeeRepository, never()).saveAll(any<List<Employee>>())
        }

        @Test
        @DisplayName("MANUAL 보호 발생 - audit MANUAL_ORIGIN_PROTECTED 1건 + 보호 사번 reason 포함")
        fun manualOriginAuditRecorded() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "기존관리자").apply {
                origin = EmployeeOrigin.MANUAL
            }
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("ADMIN-001")))
                .thenReturn(listOf(manualEmployee))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(item()))

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService, org.mockito.Mockito.atLeastOnce()).record(captor.capture())
            val protectedAudit = captor.allValues
                .single { it.eventType == SapInboundAuditEventType.MANUAL_ORIGIN_PROTECTED }
            assertThat(protectedAudit.receivedCount).isEqualTo(1)
            assertThat(protectedAudit.reason).isEqualTo("ADMIN-001")
        }

        @Test
        @DisplayName("SAP 직원 정상 갱신 - origin=SAP 행은 기존 동작 유지")
        fun sapOriginUpdated() {
            val sapEmployee = Employee(employeeCode = "E001", name = "기존이름").apply {
                origin = EmployeeOrigin.SAP
            }
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("E001"))).thenReturn(listOf(sapEmployee))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(item(employeeCode = "E001", employeeName = "변경된이름"))
            )

            assertThat(sapEmployee.name).isEqualTo("변경된이름")
            assertThat(detail.successCount).isEqualTo(1)
            verify(employeeRepository).saveAll(any<List<Employee>>())
        }

        @Test
        @DisplayName("신규 INSERT - 매칭되는 기존 행 없음 -> origin=SAP default 유지")
        fun newInsertHasSapOrigin() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("E999"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(item(employeeCode = "E999")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.origin).isEqualTo(EmployeeOrigin.SAP)
        }

        @Test
        @DisplayName("응답 형식 불변 - 정상 1건 + MANUAL 1건 + 검증실패 1건 -> success=1, failure=1")
        fun responseShapeUnchanged() {
            val manualEmployee = Employee(employeeCode = "ADMIN-001", name = "관리자").apply {
                origin = EmployeeOrigin.MANUAL
            }
            val sapEmployee = Employee(employeeCode = "E001", name = "기존이름").apply {
                origin = EmployeeOrigin.SAP
            }
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>()))
                .thenReturn(listOf(manualEmployee, sapEmployee))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(
                    item(employeeCode = "E001", employeeName = "변경된"),
                    item(employeeCode = "ADMIN-001", employeeName = "공격이름"),
                    item(employeeCode = "E002", employeeName = "")
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isEqualTo("E002")
        }
    }
}
