package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.service.dto.AccountCategoryUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("AccountCategoryUpsertService 테스트")
class AccountCategoryUpsertServiceTest {

    private val accountCategoryMasterRepository: AccountCategoryMasterRepository = mockk()

    // 행 단위 트랜잭션 빈 — 테스트에서는 실제 빈을 mock repository 로 구성 (REQUIRES_NEW 는 단위 테스트 무관).
    private val rowUpsertService = AccountCategoryRowUpsertService(accountCategoryMasterRepository)

    private val service = AccountCategoryUpsertService(rowUpsertService)

    private fun stubSaveAndFlushCapture(): CapturingSlot<AccountCategoryMaster> {
        val slot = slot<AccountCategoryMaster>()
        every { accountCategoryMasterRepository.saveAndFlush(capture(slot)) } answers { firstArg<AccountCategoryMaster>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 카테고리 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "일반거래처")))

            assertThat(savedSlot.captured.accountCode).isEqualTo("Z001")
            assertThat(savedSlot.captured.name).isEqualTo("일반거래처")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 카테고리 갱신 - 동일 accountCode, name 만 갱신")
        fun upsert_updateExisting() {
            val existing = AccountCategoryMaster(accountCode = "Z001", name = "기존이름")
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns existing
            val savedSlot = stubSaveAndFlushCapture()

            service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = "새이름")))

            val saved = savedSlot.captured
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
        }

        @Test
        @DisplayName("다중 행 - 모두 적재")
        fun upsert_multipleItems() {
            every { accountCategoryMasterRepository.findByAccountCode(any()) } returns null
            stubSaveAndFlushCapture()

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
    @DisplayName("upsert - 레거시 정합 (필수 검증 제거 — AccountCode/Name nillable raw 적재)")
    inner class UpsertLegacyAlignment {

        @Test
        @DisplayName("Name 누락 - 검증 없이 name=null 로 적재 (SF nillable=true 정합)")
        fun upsert_missingName_rawStored() {
            every { accountCategoryMasterRepository.findByAccountCode("Z001") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = "Z001", name = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.accountCode).isEqualTo("Z001")
            assertThat(savedSlot.captured.name).isNull()
        }

        @Test
        @DisplayName("AccountCode 누락 - 검증 없이 accountCode=null 로 적재 (SF required=false 정합)")
        fun upsert_missingAccountCode_rawStored() {
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(AccountCategoryUpsertCommand(accountCode = null, name = "일반거래처")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.accountCode).isNull()
            assertThat(savedSlot.captured.name).isEqualTo("일반거래처")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path (UNIQUE 충돌 행 격리)")
    inner class UpsertError {

        @Test
        @DisplayName("account_code UNIQUE 충돌 - 그 행만 failures, 트랜잭션 전체 롤백 안 함")
        fun upsert_uniqueViolation_isolatedAsFailure() {
            every { accountCategoryMasterRepository.findByAccountCode(any()) } returns null
            // Z001 정상, Z002 는 UNIQUE 충돌 (saveAndFlush 시 DataIntegrityViolationException)
            every { accountCategoryMasterRepository.saveAndFlush(match { it.accountCode == "Z001" }) } answers { firstArg() }
            every { accountCategoryMasterRepository.saveAndFlush(match { it.accountCode == "Z002" }) } throws
                DataIntegrityViolationException("duplicate key value violates unique constraint")

            val result = service.upsert(
                listOf(
                    AccountCategoryUpsertCommand(accountCode = "Z001", name = "정상"),
                    AccountCategoryUpsertCommand(accountCode = "Z002", name = "중복")
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("Z002")
            assertThat(result.failures.single().reason).contains("적재 실패")
        }
    }
}
