package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.account.service.AccountCategoryUpsertService
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.account.AccountCategoryRequestItem
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAccountCategoryService 어댑터 테스트")
class SapAccountCategoryServiceTest {

    @Mock
    private lateinit var accountCategoryUpsertService: AccountCategoryUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapAccountCategoryService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=2, failure=0) → AccountMasterDetail + audit reason='success=2 failure=0'")
        fun happy_domainResultMappedAndAudit() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "일반거래처"),
                AccountCategoryRequestItem(accountCode = "Z002", name = "위탁거래처")
            )
            whenever(accountCategoryUpsertService.upsert(any())).thenReturn(
                AccountCategoryUpsertResult(successCount = 2, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            val audit = auditCaptor.firstValue
            assertThat(audit.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(audit.receivedCount).isEqualTo(2)
            assertThat(audit.reason).isEqualTo("success=2 failure=0")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 으로 1:1 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "정상"),
                AccountCategoryRequestItem(accountCode = "Z002", name = null)
            )
            whenever(accountCategoryUpsertService.upsert(any())).thenReturn(
                AccountCategoryUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AccountCategoryUpsertFailedRow(identifier = "Z002", reason = "Name 필수"))
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures).hasSize(1)
            assertThat(detail.failures.single().identifier).isEqualTo("Z002")
            assertThat(detail.failures.single().reason).isEqualTo("Name 필수")

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=1 failure=1")
        }

        @Test
        @DisplayName("도메인 throw: 실패 audit (reason='success=0 failure=<received>') 후 예외 재전파")
        fun domainThrow_failureAuditAndRethrow() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "일반"),
                AccountCategoryRequestItem(accountCode = "Z002", name = "위탁")
            )
            whenever(accountCategoryUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("DB connection lost")

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            val audit = auditCaptor.firstValue
            assertThat(audit.receivedCount).isEqualTo(2)
            assertThat(audit.reason).isEqualTo("success=0 failure=2")
        }

        @Test
        @DisplayName("DTO 매핑: AccountCategoryRequestItem → AccountCategoryUpsertCommand 필드 매핑")
        fun dtoMapping_itemToCommand() {
            val items = listOf(AccountCategoryRequestItem(accountCode = "Z001", name = "일반거래처"))
            whenever(accountCategoryUpsertService.upsert(any())).thenReturn(
                AccountCategoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            service.upsert(items)

            val captor = argumentCaptor<List<AccountCategoryUpsertCommand>>()
            verify(accountCategoryUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.accountCode).isEqualTo("Z001")
            assertThat(command.name).isEqualTo("일반거래처")
        }
    }
}
