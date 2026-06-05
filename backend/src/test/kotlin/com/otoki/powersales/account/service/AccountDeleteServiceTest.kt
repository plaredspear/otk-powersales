package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.account.exception.AccountDeleteNotOwnerException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.user.entity.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AccountDeleteService 테스트 (Spec #642 P1-B + SF owner 가드 복원)")
class AccountDeleteServiceTest {

    private val accountRepository: AccountRepository = mockk()

    private val service = AccountDeleteService(
        accountRepository,
    )

    private val ownerUserId = 100L

    private fun user(id: Long): User = User(id = id, username = "user$id", employeeCode = "E$id", password = "")

    private fun nativeAccount(
        id: Long = 1234L,
        isDeleted: Boolean? = null,
        owner: User? = user(ownerUserId)
    ): Account = Account(
        id = id,
        name = "(신규) 강남점",
        externalKey = null,
        accountGroup = "9999",
        isDeleted = isDeleted,
        ownerUser = owner
    )

    @Test
    @DisplayName("T1 정상 삭제 - external_key IS NULL + owner == 요청자 + is_deleted IS NULL")
    fun delete_success_isDeletedNull() {
        val account = nativeAccount(id = 1234, isDeleted = null)
        every { accountRepository.findActiveById(1234) } returns account

        service.delete(1234, ownerUserId)

        assertThat(account.isDeleted).isTrue
    }

    @Test
    @DisplayName("T2 정상 삭제 - external_key IS NULL + owner == 요청자 + is_deleted = false")
    fun delete_success_isDeletedFalse() {
        val account = nativeAccount(id = 1234, isDeleted = false)
        every { accountRepository.findActiveById(1234) } returns account

        service.delete(1234, ownerUserId)

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
            isDeleted = false,
            ownerUser = user(ownerUserId)
        )
        every { accountRepository.findActiveById(1234) } returns account

        // ExternalKey 가드가 owner 가드보다 우선 (SF if/else if 순서 동등)
        assertThatThrownBy { service.delete(1234, ownerUserId) }
            .isInstanceOf(AccountDeleteBlockedSapSyncedException::class.java)
            .hasMessage("거래처 코드가 있는 거래처는 삭제할 수 없습니다.")

        assertThat(account.isDeleted).isFalse
    }

    @Test
    @DisplayName("T4 차단 - id row 부재 → ACCOUNT_NOT_FOUND")
    fun delete_notFound_noRow() {
        every { accountRepository.findActiveById(9999) } returns null

        assertThatThrownBy { service.delete(9999, ownerUserId) }
            .isInstanceOf(AccountNotFoundException::class.java)
            .hasMessage("거래처를 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("T5 차단 - 이미 is_deleted = true (멱등 보장) → ACCOUNT_NOT_FOUND")
    fun delete_notFound_alreadyDeleted() {
        every { accountRepository.findActiveById(1234) } returns null

        assertThatThrownBy { service.delete(1234, ownerUserId) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    @DisplayName("T6 차단 - owner != 요청자 (타인 소유 거래처) → ACCOUNT_DELETE_NOT_OWNER (SF OwnerId 가드 동등)")
    fun delete_blocked_notOwner() {
        val account = nativeAccount(id = 1234, isDeleted = false, owner = user(ownerUserId))
        every { accountRepository.findActiveById(1234) } returns account

        // 요청자(999) != owner(100) → 차단. SF 'OwnerId != UserInfo.getUserId()' 동등.
        assertThatThrownBy { service.delete(1234, 999L) }
            .isInstanceOf(AccountDeleteNotOwnerException::class.java)
            .hasMessage("자신의 신규 거래처만 삭제가 가능합니다.")

        assertThat(account.isDeleted).isFalse
    }

    @Test
    @DisplayName("T7 차단 - owner 가 NULL 인 거래처 → ACCOUNT_DELETE_NOT_OWNER (어떤 요청자와도 불일치)")
    fun delete_blocked_ownerNull() {
        val account = nativeAccount(id = 1234, isDeleted = false, owner = null)
        every { accountRepository.findActiveById(1234) } returns account

        assertThatThrownBy { service.delete(1234, ownerUserId) }
            .isInstanceOf(AccountDeleteNotOwnerException::class.java)

        assertThat(account.isDeleted).isFalse
    }
}
