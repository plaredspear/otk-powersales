package com.otoki.powersales.repository

import com.otoki.powersales.platform.common.entity.LoginHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.platform.common.repository.LoginHistoryRepository

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class LoginHistoryRepositoryTest {

    @Autowired
    private lateinit var loginHistoryRepository: LoginHistoryRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        loginHistoryRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("surrogate PK CRUD 테스트")
    inner class SurrogatePkCrudTests {

        @Test
        @DisplayName("LoginHistory 저장 및 surrogate PK로 조회 - 저장 후 ID로 재조회 -> 일치")
        fun saveAndFindById() {
            // Given
            val now = java.time.LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val history = LoginHistory(empCode = "E001", instDate = now)
            val saved = testEntityManager.persistAndFlush(history)
            testEntityManager.clear()

            // When
            val result = loginHistoryRepository.findById(saved.id)

            // Then
            assertThat(result).isPresent
            assertThat(result.get().empCode).isEqualTo("E001")
            assertThat(result.get().instDate).isEqualTo(now)
        }

        @Test
        @DisplayName("미존재 ID 조회 시 Optional.empty() 반환")
        fun findById_notFound_returnsEmpty() {
            // When
            val result = loginHistoryRepository.findById(9999L)

            // Then
            assertThat(result).isEmpty
        }

        @Test
        @DisplayName("동일 사번 다중 로그인 이력 저장 - PK 충돌 없이 각각 별도 행으로 저장")
        fun saveMultipleForSameEmployee() {
            // Given
            val now = java.time.LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val history1 = LoginHistory(empCode = "E001", instDate = now)
            val history2 = LoginHistory(empCode = "E001", instDate = now)
            testEntityManager.persistAndFlush(history1)
            testEntityManager.persistAndFlush(history2)
            testEntityManager.clear()

            // When
            val result = loginHistoryRepository.findAll()

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }.distinct()).hasSize(2)
        }
    }
}
