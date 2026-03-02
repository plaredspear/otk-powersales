package com.otoki.internal.repository

import com.otoki.internal.entity.EmployeeLoginHistory
import com.otoki.internal.entity.EmployeeLoginHistoryId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.internal.common.config.QueryDslConfig
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeLoginHistoryRepositoryTest {

    @Autowired
    private lateinit var employeeLoginHistoryRepository: EmployeeLoginHistoryRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeLoginHistoryRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("복합 키 CRUD 테스트")
    inner class CompositeKeyCrudTests {

        @Test
        @DisplayName("EmployeeLoginHistory 저장 및 복합 키로 조회 - empCode/instDate 저장 후 재조회 -> 일치")
        fun saveAndFindByCompositeKey() {
            // Given
            val now = LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val history = EmployeeLoginHistory(empCode = "E001", instDate = now)
            testEntityManager.persistAndFlush(history)
            testEntityManager.clear()

            // When
            val id = EmployeeLoginHistoryId(empCode = "E001", instDate = now)
            val result = employeeLoginHistoryRepository.findById(id)

            // Then
            assertThat(result).isPresent
            assertThat(result.get().empCode).isEqualTo("E001")
            assertThat(result.get().instDate).isEqualTo(now)
        }

        @Test
        @DisplayName("미존재 복합 키 조회 시 Optional.empty() 반환")
        fun findById_notFound_returnsEmpty() {
            // Given
            val now = LocalDateTime.of(2026, 2, 24, 10, 0, 0)
            val history = EmployeeLoginHistory(empCode = "E001", instDate = now)
            testEntityManager.persistAndFlush(history)
            testEntityManager.clear()

            // When
            val nonExistId = EmployeeLoginHistoryId(
                empCode = "NONEXIST",
                instDate = LocalDateTime.of(2099, 1, 1, 0, 0, 0)
            )
            val result = employeeLoginHistoryRepository.findById(nonExistId)

            // Then
            assertThat(result).isEmpty
        }
    }
}
