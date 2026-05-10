package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.account.service.AccountUpsertService
import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.account.service.dto.AccountUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountUpsertResult
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequestItem
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

/**
 * Spec #639: REQUEST_ACCEPTED audit 검증은 SapInboundAuditAspectTest 가 책임.
 * 본 테스트는 어댑터의 도메인 호출 / DTO 매핑 / 응답 매핑만 검증.
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("SapClientMasterService 어댑터 테스트")
class SapClientMasterServiceTest {

    @Mock
    private lateinit var accountUpsertService: AccountUpsertService

    @InjectMocks
    private lateinit var service: SapClientMasterService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=2, failure=0) → AccountMasterDetail")
        fun happy_domainResultMapped() {
            val items = listOf(
                ClientMasterRequestItem(sapAccountCode = "A", name = "거래처A"),
                ClientMasterRequestItem(sapAccountCode = "B", name = "거래처B")
            )
            whenever(accountUpsertService.upsert(any())).thenReturn(
                AccountUpsertResult(successCount = 2, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 으로 1:1 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                ClientMasterRequestItem(sapAccountCode = "A", name = "정상"),
                ClientMasterRequestItem(sapAccountCode = "B", name = null)
            )
            whenever(accountUpsertService.upsert(any())).thenReturn(
                AccountUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AccountUpsertFailedRow(identifier = "B", reason = "Name 필수"))
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures).hasSize(1)
            assertThat(detail.failures.single().identifier).isEqualTo("B")
            assertThat(detail.failures.single().reason).isEqualTo("Name 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            val items = listOf(
                ClientMasterRequestItem(sapAccountCode = "A", name = "거래처A"),
                ClientMasterRequestItem(sapAccountCode = "B", name = "거래처B")
            )
            whenever(accountUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("DB connection lost")
        }

        @Test
        @DisplayName("DTO 매핑: ClientMasterRequestItem.SAPAccountCode → AccountUpsertCommand.externalKey 등 도메인 호출 인자 검증")
        fun dtoMapping_itemToCommand() {
            val items = listOf(
                ClientMasterRequestItem(
                    sapAccountCode = "1032619",
                    name = "(주)홍길동상회",
                    employeeCode = "100123",
                    branchCode = "1111",
                    branchName = "서울지점",
                    salesDeptCode = "1110",
                    divisionCode = "1100",
                    consignmentAcc = "Y",
                    werk1 = "1100",
                    accountType = "GENERAL",
                    phone = "02-0000-0000"
                )
            )
            whenever(accountUpsertService.upsert(any())).thenReturn(
                AccountUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            service.upsert(items)

            val captor = argumentCaptor<List<AccountUpsertCommand>>()
            verify(accountUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.externalKey).isEqualTo("1032619")
            assertThat(command.name).isEqualTo("(주)홍길동상회")
            assertThat(command.employeeCode).isEqualTo("100123")
            assertThat(command.branchCode).isEqualTo("1111")
            assertThat(command.branchName).isEqualTo("서울지점")
            assertThat(command.salesDeptCode).isEqualTo("1110")
            assertThat(command.divisionCode).isEqualTo("1100")
            assertThat(command.consignmentAcc).isEqualTo("Y")
            assertThat(command.werk1).isEqualTo("1100")
            assertThat(command.accountType).isEqualTo("GENERAL")
            assertThat(command.phone).isEqualTo("02-0000-0000")
        }
    }
}
