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
 * Spec #640 P1-B: 동일명 활성 거래처 존재 검증 — `is_deleted IS NULL OR is_deleted = false` 동등.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository.existsActiveByName 테스트")
class AccountRepositoryExistsActiveByNameTest {

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
    @DisplayName("미삭제(false) row 존재 - true")
    fun activeFalse_returnsTrue() {
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-1", isDeleted = false)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByName("(신규) 강남점")).isTrue
    }

    @Test
    @DisplayName("is_deleted IS NULL row 존재 - true (Heroku Connect 잔재 호환)")
    fun nullDeleted_returnsTrue() {
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-2", isDeleted = null)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByName("(신규) 강남점")).isTrue
    }

    @Test
    @DisplayName("is_deleted = true 만 존재 - false (soft-delete row 무시)")
    fun softDeleted_returnsFalse() {
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-3", isDeleted = true)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByName("(신규) 강남점")).isFalse
    }

    @Test
    @DisplayName("동일명 row 0건 - false")
    fun noRow_returnsFalse() {
        assertThat(accountRepository.existsActiveByName("(신규) 강남점")).isFalse
    }

    @Test
    @DisplayName("이름 다름 - false (정확 일치 검증)")
    fun differentName_returnsFalse() {
        testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-4", isDeleted = false)
        )
        testEntityManager.clear()

        assertThat(accountRepository.existsActiveByName("(신규) 다른점")).isFalse
    }
}
