package com.otoki.powersales.repository

import com.otoki.powersales.platform.common.entity.EmployeeAdmin
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
import com.otoki.powersales.platform.common.repository.EmployeeAdminRepository

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class EmployeeAdminRepositoryTest {

    @Autowired
    private lateinit var employeeAdminRepository: EmployeeAdminRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        employeeAdminRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    inner class BasicCrudTests {

        @Test
        @DisplayName("EmployeeAdmin 저장 및 조회 - empCode='A001' 저장 후 재조회 -> 일치")
        fun saveAndFindById() {
            // Given
            val admin = EmployeeAdmin(empCode = "A001")
            testEntityManager.persistAndFlush(admin)
            testEntityManager.clear()

            // When
            val result = employeeAdminRepository.findById("A001")

            // Then
            assertThat(result).isPresent
            assertThat(result.get().empCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("존재하지 않는 empCode 조회 시 empty 반환")
        fun findById_notFound_returnsEmpty() {
            // When
            val result = employeeAdminRepository.findById("NONEXIST")

            // Then
            assertThat(result).isEmpty
        }
    }
}
