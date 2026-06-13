package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.service.dto.AccountCategoryUpsertCommand
import com.otoki.powersales.domain.foundation.account.service.AccountCategoryUpsertService
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AccountCategoryUpsertService 테스트")
class AccountCategoryUpsertServiceTest {

    private val accountCategoryMasterRepository: AccountCategoryMasterRepository = mockk()

    private val service = AccountCategoryUpsertService(
        accountCategoryMasterRepository,
    )

    private fun stubSaveAllCapture(): CapturingSlot<List<AccountCategoryMaster>> {
        val slot = slot<List<AccountCategoryMaster>>()
        every { accountCategoryMasterRepository.saveAll(capture(slot)) } answers { firstArg<List<AccountCategoryMaster>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 카테고리 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns null
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "일반거래처")))

            assertThat(savedSlot.captured).hasSize(1)
            assertThat(savedSlot.captured[0].accountCode).isEqualTo("Z001")
            assertThat(savedSlot.captured[0].name).isEqualTo("일반거래처")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 카테고리 갱신 - 동일 accountCode, name 만 갱신")
        fun upsert_updateExisting() {
            val existing = AccountCategoryMaster(accountCode = "Z001", name = "기존이름")
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns existing
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "새이름")))

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
        }

        @Test
        @DisplayName("다중 행 - 모두 적재")
        fun upsert_multipleItems() {
            every { accountCategoryMasterRepository.findByAccountCode(any()) } returns null
            stubSaveAllCapture()

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
            verify(exactly = 0) { accountCategoryMasterRepository.saveAll(any<List<AccountCategoryMaster>>()) }
        }

        @Test
        @DisplayName("Name 누락 - failures 에 accountCode 와 함께 기록")
        fun upsert_missingName() {
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns null
            every { accountCategoryMasterRepository.saveAll(any<List<AccountCategoryMaster>>()) } answers { firstArg<List<AccountCategoryMaster>>() }

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("Z001")
            assertThat(result.failures.single().reason).contains("Name 필수")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            every { accountCategoryMasterRepository.findByAccountCode(any()) } returns null
            stubSaveAllCapture()

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
