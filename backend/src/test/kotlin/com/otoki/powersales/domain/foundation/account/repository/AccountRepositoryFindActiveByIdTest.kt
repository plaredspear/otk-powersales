package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
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
 * Spec #642 P1-B: 활성 거래처 단건 조회 — `is_deleted IS NULL OR is_deleted = false` 동등.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository.findActiveById 테스트")
class AccountRepositoryFindActiveByIdTest {

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
    @DisplayName("R1 is_deleted IS NULL row - hit (Heroku Connect 잔재 호환)")
    fun nullDeleted_returnsAccount() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-N1", isDeleted = null)
        )
        testEntityManager.clear()

        val result = accountRepository.findActiveById(saved.id)
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("R2 is_deleted = false row - hit")
    fun deletedFalse_returnsAccount() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-N2", isDeleted = false)
        )
        testEntityManager.clear()

        val result = accountRepository.findActiveById(saved.id)
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("R3 is_deleted = true row - empty (soft-deleted 무시)")
    fun deletedTrue_returnsNull() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-N3", isDeleted = true)
        )
        testEntityManager.clear()

        val result = accountRepository.findActiveById(saved.id)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("R4 row 부재 - empty")
    fun noRow_returnsNull() {
        val result = accountRepository.findActiveById(9999)
        assertThat(result).isNull()
    }
}
