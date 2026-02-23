package com.otoki.internal.repository

import com.otoki.internal.entity.Account
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles

/**
 * AccountRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    inner class BasicCrudTests {

        @Test
        @DisplayName("Account 저장 및 ID로 조회")
        fun saveAndFindById() {
            // Given
            val account = createAccount(
                name = "이마트 부산점",
                phone = "051-1234-5678",
                address1 = "부산시 해운대구",
                representative = "홍길동",
                externalKey = "EXT-00101"
            )
            val saved = testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result = accountRepository.findById(saved.id)

            // Then
            assertThat(result).isPresent
            assertThat(result.get().name).isEqualTo("이마트 부산점")
            assertThat(result.get().phone).isEqualTo("051-1234-5678")
            assertThat(result.get().address1).isEqualTo("부산시 해운대구")
            assertThat(result.get().representative).isEqualTo("홍길동")
            assertThat(result.get().externalKey).isEqualTo("EXT-00101")
        }
    }

    @Nested
    @DisplayName("findByExternalKey 테스트")
    inner class FindByExternalKeyTests {

        @Test
        @DisplayName("외부키로 조회 성공")
        fun findByExternalKey_success() {
            // Given
            val account = createAccount(
                externalKey = "EXT-00101",
                name = "이마트 부산점"
            )
            testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByExternalKey("EXT-00101")

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.externalKey).isEqualTo("EXT-00101")
            assertThat(result.name).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("존재하지 않는 외부키 조회 시 null 반환")
        fun findByExternalKey_notFound_returnsNull() {
            // Given
            val account = createAccount(externalKey = "EXT-00101")
            testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByExternalKey("EXT-99999")

            // Then
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("외부키는 유니크하므로 단일 결과만 반환")
        fun findByExternalKey_returnsUniqueResult() {
            // Given
            val account = createAccount(
                externalKey = "EXT-00101",
                name = "이마트 부산점"
            )
            testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result1 = accountRepository.findByExternalKey("EXT-00101")
            val result2 = accountRepository.findByExternalKey("EXT-00101")

            // Then
            assertThat(result1).isNotNull
            assertThat(result2).isNotNull
            assertThat(result1!!.id).isEqualTo(result2!!.id)
        }
    }

    @Nested
    @DisplayName("findByIdIn 테스트")
    inner class FindByIdInTests {

        @Test
        @DisplayName("ID 목록으로 일괄 조회 성공")
        fun findByIdIn_success() {
            // Given
            val account1 = createAccount(externalKey = "EXT-00101", name = "이마트 부산점")
            val account2 = createAccount(externalKey = "EXT-00102", name = "홈플러스 서면점")
            val account3 = createAccount(externalKey = "EXT-00103", name = "롯데마트 해운대점")
            val saved1 = testEntityManager.persistAndFlush(account1)
            val saved2 = testEntityManager.persistAndFlush(account2)
            val saved3 = testEntityManager.persistAndFlush(account3)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByIdIn(listOf(saved1.id, saved2.id))

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.name }).containsExactlyInAnyOrder(
                "이마트 부산점",
                "홈플러스 서면점"
            )
            assertThat(result.map { it.id }).containsExactlyInAnyOrder(saved1.id, saved2.id)
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회 시 빈 리스트 반환")
        fun findByIdIn_emptyList_returnsEmpty() {
            // Given
            val account = createAccount(externalKey = "EXT-00101")
            testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByIdIn(emptyList())

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 ID는 결과에서 제외")
        fun findByIdIn_nonExistingIds_excluded() {
            // Given
            val account1 = createAccount(externalKey = "EXT-00101", name = "이마트 부산점")
            val account2 = createAccount(externalKey = "EXT-00102", name = "홈플러스 서면점")
            val saved1 = testEntityManager.persistAndFlush(account1)
            val saved2 = testEntityManager.persistAndFlush(account2)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByIdIn(listOf(saved1.id, saved2.id, 999L, 888L))

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactlyInAnyOrder(saved1.id, saved2.id)
        }

        @Test
        @DisplayName("단일 ID로 조회")
        fun findByIdIn_singleId() {
            // Given
            val account = createAccount(externalKey = "EXT-00101", name = "이마트 부산점")
            val saved = testEntityManager.persistAndFlush(account)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByIdIn(listOf(saved.id))

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].name).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("모든 거래처 조회")
        fun findByIdIn_allAccounts() {
            // Given
            val account1 = createAccount(externalKey = "EXT-00101", name = "이마트 부산점")
            val account2 = createAccount(externalKey = "EXT-00102", name = "홈플러스 서면점")
            val account3 = createAccount(externalKey = "EXT-00103", name = "롯데마트 해운대점")
            val saved1 = testEntityManager.persistAndFlush(account1)
            val saved2 = testEntityManager.persistAndFlush(account2)
            val saved3 = testEntityManager.persistAndFlush(account3)
            testEntityManager.clear()

            // When
            val result = accountRepository.findByIdIn(listOf(saved1.id, saved2.id, saved3.id))

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.name }).containsExactlyInAnyOrder(
                "이마트 부산점",
                "홈플러스 서면점",
                "롯데마트 해운대점"
            )
        }
    }

    // ========== Helpers ==========

    private fun createAccount(
        name: String? = "테스트 거래처",
        externalKey: String? = "EXT-00001",
        address1: String? = "부산시 테스트구",
        representative: String? = "테스트 대표",
        phone: String? = "010-1234-5678"
    ): Account {
        return Account(
            name = name,
            externalKey = externalKey,
            address1 = address1,
            representative = representative,
            phone = phone
        )
    }
}
