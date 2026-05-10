package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.AccountCategoryMaster
import com.otoki.powersales.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.account.service.dto.AccountCategoryUpsertCommand
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

@ExtendWith(MockitoExtension::class)
@DisplayName("AccountCategoryUpsertService 테스트")
class AccountCategoryUpsertServiceTest {

    @Mock
    private lateinit var accountCategoryMasterRepository: AccountCategoryMasterRepository

    @InjectMocks
    private lateinit var service: AccountCategoryUpsertService

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 카테고리 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            whenever(accountCategoryMasterRepository.findByAccountCode("Z001")).thenReturn(null)

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "일반거래처")))

            val captor = argumentCaptor<List<AccountCategoryMaster>>()
            verify(accountCategoryMasterRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(1)
            assertThat(captor.firstValue[0].accountCode).isEqualTo("Z001")
            assertThat(captor.firstValue[0].name).isEqualTo("일반거래처")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 카테고리 갱신 - 동일 accountCode, name 만 갱신")
        fun upsert_updateExisting() {
            val existing = AccountCategoryMaster(accountCode = "Z001", name = "기존이름")
            whenever(accountCategoryMasterRepository.findByAccountCode("Z001")).thenReturn(existing)

            service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "새이름")))

            val captor = argumentCaptor<List<AccountCategoryMaster>>()
            verify(accountCategoryMasterRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
        }

        @Test
        @DisplayName("다중 행 - 모두 적재")
        fun upsert_multipleItems() {
            whenever(accountCategoryMasterRepository.findByAccountCode(any())).thenReturn(null)

            val result = service.upsert(
                listOf(
                    AccountCategoryUpsertCommand(accountCode = "Z001", name = "일반거래처"),
                    AccountCategoryUpsertCommand(accountCode = "Z002", name = "위탁거래처")
                )
            )

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("AccountCode 누락 - failures 기록, identifier null, 적재 스킵")
        fun upsert_missingAccountCode() {
            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = null, name = "일반거래처")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("AccountCode 필수")
            verify(accountCategoryMasterRepository, never()).saveAll(any<List<AccountCategoryMaster>>())
        }

        @Test
        @DisplayName("Name 누락 - failures 에 accountCode 와 함께 기록")
        fun upsert_missingName() {
            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("Z001")
            assertThat(result.failures.single().reason).contains("Name 필수")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            whenever(accountCategoryMasterRepository.findByAccountCode("Z001")).thenReturn(null)

            val result = service.upsert(
                listOf(
                    AccountCategoryUpsertCommand(accountCode = "Z001", name = "정상"),
                    AccountCategoryUpsertCommand(accountCode = "Z002", name = null)
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("Z002")
        }
    }
}
