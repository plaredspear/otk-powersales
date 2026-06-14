package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * Spec #643 P1-B: 자기 자신 제외 동일명 활성 거래처 존재 검증 — UPDATE 시 자기 자신은 카운트에서 제외.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository.existsActiveByNameAndIdNot 테스트")
class AccountRepositoryExistsActiveByNameAndIdNotTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("R1 동일 name + 미삭제(false) + 다른 id - true")
    fun differentId_active_returnsTrue() {
        val self = testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-SELF", isDeleted = false)
        )
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-OTHER", isDeleted = false)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByNameAndIdNot("(신규) 자기자신", self.id)).isTrue
    }

    @Test
    @DisplayName("R2 동일 name + 자기 자신 (id 동일) 만 존재 - false")
    fun selfOnly_returnsFalse() {
        val self = testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-SELF", isDeleted = false)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByNameAndIdNot("(신규) 자기자신", self.id)).isFalse
    }

    @Test
    @DisplayName("R3 동일 name + is_deleted=true row - false (soft-delete row 무시)")
    fun softDeleted_returnsFalse() {
        val self = testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-SELF", isDeleted = false)
        )
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-OTHER", isDeleted = true)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByNameAndIdNot("(신규) 자기자신", self.id)).isFalse
    }

    @Test
    @DisplayName("R4 동일 name + is_deleted IS NULL row - true (NULL = 활성 간주, Heroku Connect 잔재 호환)")
    fun nullDeleted_returnsTrue() {
        val self = testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-SELF", isDeleted = false)
        )
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-OTHER", isDeleted = null)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByNameAndIdNot("(신규) 자기자신", self.id)).isTrue
    }

    @Test
    @DisplayName("R5 name 다름 - false")
    fun differentName_returnsFalse() {
        val self = testEntityManager.persistAndFlush(
            Account(name = "(신규) 자기자신", externalKey = "EXT-SELF", isDeleted = false)
        )
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 다른이름", externalKey = "EXT-OTHER", isDeleted = false)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByNameAndIdNot("(신규) 자기자신", self.id)).isFalse
    }
}
