package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.domain.org.employee.service.EmployeeUpsertService
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertFailedRow
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertResult
import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.inbound.dto.employee.EmployeeMasterRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapEmployeeMasterService 어댑터 테스트")
class SapEmployeeMasterServiceTest {

    private val employeeUpsertService: EmployeeUpsertService = mockk()
    private val auditService: SapInboundAuditService = mockk()
    private val service = SapEmployeeMasterService(employeeUpsertService, auditService)

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=2) → EmployeeMasterDetail, MANUAL_ORIGIN_PROTECTED 미호출 (audit 0회)")
        fun happy_domainResultMapped() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"),
                EmployeeMasterRequestItem(employeeCode = "100124", employeeName = "임꺽정")
            )
            every { employeeUpsertService.upsert(any()) } returns
                EmployeeUpsertResult(successCount = 2, failureCount = 0, failures = emptyList(), protectedManualCodes = emptyList())

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)
            verify(exactly = 0) { auditService.record(any()) }
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑 (empCode 보존)")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "정상"),
                EmployeeMasterRequestItem(employeeCode = "100124", employeeName = null)
            )
            every { employeeUpsertService.upsert(any()) } returns
                EmployeeUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(EmployeeUpsertFailedRow("100124", "EmployeeName 필수")),
                    protectedManualCodes = emptyList()
                )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().empCode).isEqualTo("100124")
            assertThat(detail.failures.single().reason).isEqualTo("EmployeeName 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            val items = listOf(EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"))
            every { employeeUpsertService.upsert(any()) } throws
                IllegalStateException("DB connection lost")

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        @DisplayName("Spec #579 - protectedManualCodes 존재 시 MANUAL_ORIGIN_PROTECTED audit 호출 (1회)")
        fun manualOriginProtected_extraAudit() {
            val items = listOf(
                EmployeeMasterRequestItem(employeeCode = "100123", employeeName = "홍길동"),
                EmployeeMasterRequestItem(employeeCode = "M0001", employeeName = "수동등록")
            )
            every { employeeUpsertService.upsert(any()) } returns
                EmployeeUpsertResult(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    protectedManualCodes = listOf("M0001")
                )
            every { auditService.record(any()) } answers { firstArg<SapInboundAudit>() }

            service.upsert(items)

            val auditCaptor = slot<SapInboundAudit>()
            verify(exactly = 1) { auditService.record(capture(auditCaptor)) }
            val audit = auditCaptor.captured
            assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.MANUAL_ORIGIN_PROTECTED)
            assertThat(audit.reason).isEqualTo("M0001")
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
            every { employeeUpsertService.upsert(any()) } returns
                EmployeeUpsertResult(successCount = 1, failureCount = 0, failures = emptyList(), protectedManualCodes = emptyList())

            service.upsert(items)

            val captor = slot<List<EmployeeUpsertCommand>>()
            verify { employeeUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.employeeCode).isEqualTo("100123")
            assertThat(command.employeeName).isEqualTo("홍길동")
            assertThat(command.gender).isEqualTo("1")
            assertThat(command.workEmail).isEqualTo("work@otoki.com")
            assertThat(command.startDate).isEqualTo("20200401")
            assertThat(command.status).isEqualTo("10")
            assertThat(command.lockingFlag).isEqualTo("N")
        }
    }
}
