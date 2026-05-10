package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.service.SystemCodeMasterUpsertService
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertFailedRow
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequestItem
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
@DisplayName("SapSystemCodeMasterService 어댑터 테스트")
class SapSystemCodeMasterServiceTest {

    @Mock
    private lateinit var systemCodeMasterUpsertService: SystemCodeMasterUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapSystemCodeMasterService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 → ProductMasterDetail + audit reason='success=1 failure=0'")
        fun happy_domainResultMappedAndAudit() {
            whenever(systemCodeMasterUpsertService.upsert(any())).thenReturn(
                SystemCodeMasterUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        detailCodeName = "재직"
                    )
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=1 failure=0")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            whenever(systemCodeMasterUpsertService.upsert(any())).thenReturn(
                SystemCodeMasterUpsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(SystemCodeMasterUpsertFailedRow(null, "GroupCode 필수"))
                )
            )

            val detail = service.upsert(
                listOf(SystemCodeMasterRequestItem(companyCode = "1000", groupCode = null, detailCode = "10"))
            )

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("GroupCode 필수")
        }

        @Test
        @DisplayName("도메인 throw: 실패 audit 후 예외 재전파")
        fun domainThrow_failureAuditAndRethrow() {
            whenever(systemCodeMasterUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy {
                service.upsert(
                    listOf(SystemCodeMasterRequestItem(companyCode = "1000", groupCode = "H10010", detailCode = "10"))
                )
            }.isInstanceOf(IllegalStateException::class.java)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=0 failure=1")
        }

        @Test
        @DisplayName("DTO 매핑: SystemCodeMasterRequestItem → SystemCodeMasterUpsertCommand")
        fun dtoMapping_itemToCommand() {
            whenever(systemCodeMasterUpsertService.upsert(any())).thenReturn(
                SystemCodeMasterUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )
            val items = listOf(
                SystemCodeMasterRequestItem(
                    companyCode = "1000",
                    groupCode = "H10010",
                    detailCode = "10",
                    groupCodeName = "직원 상태",
                    detailCodeName = "재직",
                    seq = "1"
                )
            )

            service.upsert(items)

            val captor = argumentCaptor<List<SystemCodeMasterUpsertCommand>>()
            verify(systemCodeMasterUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.companyCode).isEqualTo("1000")
            assertThat(command.groupCode).isEqualTo("H10010")
            assertThat(command.detailCode).isEqualTo("10")
            assertThat(command.groupCodeName).isEqualTo("직원 상태")
            assertThat(command.detailCodeName).isEqualTo("재직")
            assertThat(command.seq).isEqualTo("1")
        }
    }
}
