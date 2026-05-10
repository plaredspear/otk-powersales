package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.employee.service.EmployeeUpsertService
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.employee.service.dto.EmployeeUpsertFailedRow
import com.otoki.powersales.employee.service.dto.EmployeeUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.employee.EmployeeMasterRequestItem
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapEmployeeMasterService 어댑터 테스트")
class SapEmployeeMasterServiceTest {

    @Mock
    private lateinit var employeeUpsertService: EmployeeUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapEmployeeMasterService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=2) → EmployeeMasterDetail + audit reason='success=2 failure=0'")
        fun happy_domainResultMappedAndAudit() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"),
                EmployeeMasterRequestItem(employeeCode = "100124", employeeName = "임꺽정")
            )
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(successCount = 2, failureCount = 0, failures = emptyList(), protectedManualCodes = emptyList())
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=2 failure=0")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑 (empCode 보존)")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "정상"),
                EmployeeMasterRequestItem(employeeCode = "100124", employeeName = null)
            )
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(EmployeeUpsertFailedRow("100124", "EmployeeName 필수")),
                    protectedManualCodes = emptyList()
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isEqualTo("100124")
            assertThat(detail.failures.single().reason).isEqualTo("EmployeeName 필수")
        }

        @Test
        @DisplayName("도메인 throw: 실패 audit (reason='success=0 failure=N') 후 예외 재전파")
        fun domainThrow_failureAuditAndRethrow() {
            val items = listOf(EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"))
            whenever(employeeUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=0 failure=1")
        }

        @Test
        @DisplayName("Spec #579 - protectedManualCodes 존재 시 MANUAL_ORIGIN_PROTECTED audit 추가 호출 (총 2회)")
        fun manualOriginProtected_extraAudit() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"),
                EmployeeMasterRequestItem(employeeCode = "M0001", employeeName = "수동등록")
            )
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    protectedManualCodes = listOf("M0001")
                )
            )

            service.upsert(items)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService, times(2)).record(auditCaptor.capture())
            val audits = auditCaptor.allValues
            assertThat(audits[0].eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(audits[1].eventType).isEqualTo(SapInboundAuditEventType.MANUAL_ORIGIN_PROTECTED)
            assertThat(audits[1].reason).isEqualTo("M0001")
        }

        @Test
        @DisplayName("protectedManualCodes 비어있을 시 MANUAL_ORIGIN_PROTECTED audit 미호출 (총 1회만)")
        fun noManualProtection_singleAudit() {
            val items = listOf(EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"))
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(successCount = 1, failureCount = 0, failures = emptyList(), protectedManualCodes = emptyList())
            )

            service.upsert(items)

            verify(auditService, times(1)).record(any())
        }

        @Test
        @DisplayName("DTO 매핑: EmployeeMasterRequestItem → EmployeeUpsertCommand 13개 필드")
        fun dtoMapping_itemToCommand() {
            val items = listOf(
                EmployeeMasterRequestItem(
                    employeeCode = "100123",
                    employeeName = "홍길동",
                    gender = "1",
                    homePhone = "02-0000-0000",
                    workPhone = "02-0000-0001",
                    workEmail = "work@otoki.com",
                    email = "personal@otoki.com",
                    startDate = "20200401",
                    endDate = "00000000",
                    status = "10",
                    birthdate = "19850315",
                    orgCode = "11110",
                    lockingFlag = "N"
                )
            )
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(successCount = 1, failureCount = 0, failures = emptyList(), protectedManualCodes = emptyList())
            )

            service.upsert(items)

            val captor = argumentCaptor<List<EmployeeUpsertCommand>>()
            verify(employeeUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.employeeCode).isEqualTo("100123")
            assertThat(command.employeeName).isEqualTo("홍길동")
            assertThat(command.gender).isEqualTo("1")
            assertThat(command.workEmail).isEqualTo("work@otoki.com")
            assertThat(command.startDate).isEqualTo("20200401")
            assertThat(command.status).isEqualTo("10")
            assertThat(command.lockingFlag).isEqualTo("N")
        }

        @Test
        @DisplayName("도메인 throw 시 saveAll 미호출 검증 — 도메인이 검증/적재 모두 책임")
        fun domainThrow_noSaveAllConcern() {
            // 어댑터는 도메인 호출만 책임. 도메인이 throw 하면 어댑터에서 saveAll 호출하지 않음 (도메인이 안 함)
            val items = listOf(EmployeeMasterRequestItem(employeeCode = null, employeeName = "X"))
            whenever(employeeUpsertService.upsert(any())).thenReturn(
                EmployeeUpsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(EmployeeUpsertFailedRow(null, "EmployeeCode 필수")),
                    protectedManualCodes = emptyList()
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(0)
            verify(auditService, times(1)).record(any())
        }
    }
}
