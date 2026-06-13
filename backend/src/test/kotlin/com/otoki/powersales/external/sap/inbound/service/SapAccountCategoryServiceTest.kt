package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.account.service.AccountCategoryUpsertService
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertResult
import com.otoki.powersales.external.sap.inbound.service.SapAccountCategoryService
import com.otoki.powersales.external.sap.inbound.dto.account.AccountCategoryRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapAccountCategoryService 어댑터 테스트")
class SapAccountCategoryServiceTest {

    private val accountCategoryUpsertService: AccountCategoryUpsertService = mockk()
    private val service = SapAccountCategoryService(accountCategoryUpsertService)

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=2, failure=0) → AccountMasterDetail")
        fun happy_domainResultMapped() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "일반거래처"),
                AccountCategoryRequestItem(accountCode = "Z002", name = "위탁거래처")
            )
            every { accountCategoryUpsertService.upsert(any()) } returns
                AccountCategoryUpsertResult(successCount = 2, failureCount = 0, failures = emptyList())

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 으로 1:1 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "정상"),
                AccountCategoryRequestItem(accountCode = "Z002", name = null)
            )
            every { accountCategoryUpsertService.upsert(any()) } returns
                AccountCategoryUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(AccountCategoryUpsertFailedRow(identifier = "Z002", reason = "Name 필수"))
                )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures).hasSize(1)
            assertThat(detail.failures.single().identifier).isEqualTo("Z002")
            assertThat(detail.failures.single().reason).isEqualTo("Name 필수")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            val items = listOf(
                AccountCategoryRequestItem(accountCode = "Z001", name = "일반"),
                AccountCategoryRequestItem(accountCode = "Z002", name = "위탁")
            )
            every { accountCategoryUpsertService.upsert(any()) } throws
                IllegalStateException("DB connection lost")

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("DB connection lost")
        }

        @Test
        @DisplayName("DTO 매핑: AccountCategoryRequestItem → AccountCategoryUpsertCommand 필드 매핑")
        fun dtoMapping_itemToCommand() {
            val items = listOf(AccountCategoryRequestItem(accountCode = "Z001", name = "일반거래처"))
            every { accountCategoryUpsertService.upsert(any()) } returns
                AccountCategoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            service.upsert(items)

            val captor = slot<List<AccountCategoryUpsertCommand>>()
            verify { accountCategoryUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.accountCode).isEqualTo("Z001")
            assertThat(command.name).isEqualTo("일반거래처")
        }
    }
}
