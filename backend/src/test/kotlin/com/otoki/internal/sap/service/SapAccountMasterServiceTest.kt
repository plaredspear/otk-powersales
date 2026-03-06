package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapAccountMasterRequest.ReqItem
import com.otoki.internal.sap.entity.AccountCategoryMaster
import com.otoki.internal.sap.repository.AccountCategoryMasterRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapAccountMasterService 테스트")
class SapAccountMasterServiceTest {

    @Mock
    private lateinit var accountCategoryMasterRepository: AccountCategoryMasterRepository

    @InjectMocks
    private lateinit var sapAccountMasterService: SapAccountMasterService

    @Nested
    @DisplayName("sync - 신규 거래처분류 등록")
    inner class NewAccountCategoryTests {

        @Test
        @DisplayName("정상 등록 - DB에 없는 account_code -> Insert")
        fun sync_newAccountCategory_creates() {
            val items = listOf(ReqItem(accountCode = "610000", name = "매출"))
            whenever(accountCategoryMasterRepository.findByAccountCode("610000")).thenReturn(null)
            whenever(accountCategoryMasterRepository.save(any<AccountCategoryMaster>()))
                .thenAnswer { it.getArgument<AccountCategoryMaster>(0) }

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<AccountCategoryMaster>()
            verify(accountCategoryMasterRepository).save(captor.capture())
            assertThat(captor.firstValue.accountCode).isEqualTo("610000")
            assertThat(captor.firstValue.name).isEqualTo("매출")
        }
    }

    @Nested
    @DisplayName("sync - 기존 거래처분류 업데이트")
    inner class ExistingAccountCategoryTests {

        @Test
        @DisplayName("기존 업데이트 - name 변경")
        fun sync_existingAccountCategory_updates() {
            val existing = AccountCategoryMaster(id = 1, accountCode = "610000", name = "매출")
            val items = listOf(ReqItem(accountCode = "610000", name = "매출(수정)"))
            whenever(accountCategoryMasterRepository.findByAccountCode("610000")).thenReturn(existing)
            whenever(accountCategoryMasterRepository.save(any<AccountCategoryMaster>()))
                .thenAnswer { it.getArgument<AccountCategoryMaster>(0) }

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.name).isEqualTo("매출(수정)")
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("account_code 누락 - 해당 레코드 실패")
        fun sync_missingAccountCode_fails() {
            val items = listOf(ReqItem(accountCode = null, name = "매출"))

            val result = sapAccountMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("account_code")
        }

        @Test
        @DisplayName("name 누락 - 해당 레코드 실패")
        fun sync_missingName_fails() {
            val items = listOf(ReqItem(accountCode = "610000", name = null))

            val result = sapAccountMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("name")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                ReqItem(accountCode = "610000", name = "매출"),
                ReqItem(accountCode = null, name = "실패"),
                ReqItem(accountCode = "620000", name = "매입")
            )
            whenever(accountCategoryMasterRepository.findByAccountCode("610000")).thenReturn(null)
            whenever(accountCategoryMasterRepository.findByAccountCode("620000")).thenReturn(null)
            whenever(accountCategoryMasterRepository.save(any<AccountCategoryMaster>()))
                .thenAnswer { it.getArgument<AccountCategoryMaster>(0) }

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
        }
    }
}
