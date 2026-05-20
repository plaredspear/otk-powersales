package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AccountDeleteService 테스트 (Spec #642 P1-B)")
class AccountDeleteServiceTest {

    private val accountRepository: AccountRepository = mockk()

    private val service = AccountDeleteService(
        accountRepository,
    )

    private fun nativeAccount(id: Int = 1234, isDeleted: Boolean? = null): Account = Account(
        id = id,
        name = "(신규) 강남점",
        externalKey = null,
        accountGroup = "9999",
        isDeleted = isDeleted
    )

    @Test
    @DisplayName("T1 정상 삭제 - external_key IS NULL + is_deleted IS NULL (활성)")
    fun delete_success_isDeletedNull() {
        val account = nativeAccount(id = 1234, isDeleted = null)
        every { accountRepository.findActiveById(1234) } returns account

        service.delete(1234)

        assertThat(account.isDeleted).isTrue
    }

    @Test
    @DisplayName("T2 정상 삭제 - external_key IS NULL + is_deleted = false (활성)")
    fun delete_success_isDeletedFalse() {
        val account = nativeAccount(id = 1234, isDeleted = false)
        every { accountRepository.findActiveById(1234) } returns account

        service.delete(1234)

        assertThat(account.isDeleted).isTrue
    }

    @Test
    @DisplayName("T3 차단 - external_key 존재 (SAP 동기 거래처) → ACCOUNT_DELETE_BLOCKED_SAP_SYNCED")
    fun delete_blocked_sapSynced() {
        val account = Account(
            id = 1234,
            name = "GS25 역삼점",
            externalKey = "SAP-ABC123",
            accountGroup = "1010",
            isDeleted = false
        )
        every { accountRepository.findActiveById(1234) } returns account

        assertThatThrownBy { service.delete(1234) }
            .isInstanceOf(AccountDeleteBlockedSapSyncedException::class.java)
            .hasMessage("거래처 코드가 있는 거래처는 삭제할 수 없습니다.")

        assertThat(account.isDeleted).isFalse
    }

    @Test
    @DisplayName("T4 차단 - id row 부재 → ACCOUNT_NOT_FOUND")
    fun delete_notFound_noRow() {
        every { accountRepository.findActiveById(9999) } returns null

        assertThatThrownBy { service.delete(9999) }
            .isInstanceOf(AccountNotFoundException::class.java)
            .hasMessage("거래처를 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("T5 차단 - 이미 is_deleted = true (멱등 보장) → ACCOUNT_NOT_FOUND")
    fun delete_notFound_alreadyDeleted() {
        every { accountRepository.findActiveById(1234) } returns null

        assertThatThrownBy { service.delete(1234) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }
}
