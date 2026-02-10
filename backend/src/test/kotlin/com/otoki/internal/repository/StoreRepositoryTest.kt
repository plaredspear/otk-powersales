package com.otoki.internal.repository

import com.otoki.internal.entity.Store
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
import java.time.LocalDateTime

/**
 * StoreRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class StoreRepositoryTest {

    @Autowired
    private lateinit var storeRepository: StoreRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        storeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("기본 CRUD 테스트")
    inner class BasicCrudTests {

        @Test
        @DisplayName("Store 저장 및 ID로 조회")
        fun saveAndFindById() {
            // Given
            val store = createStore(
                storeCode = "ST-00101",
                storeName = "이마트 부산점",
                address = "부산시 해운대구",
                representativeName = "홍길동",
                phoneNumber = "051-1234-5678"
            )
            val saved = testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result = storeRepository.findById(saved.id)

            // Then
            assertThat(result).isPresent
            assertThat(result.get().storeCode).isEqualTo("ST-00101")
            assertThat(result.get().storeName).isEqualTo("이마트 부산점")
            assertThat(result.get().address).isEqualTo("부산시 해운대구")
            assertThat(result.get().representativeName).isEqualTo("홍길동")
            assertThat(result.get().phoneNumber).isEqualTo("051-1234-5678")
        }
    }

    @Nested
    @DisplayName("findByStoreCode 테스트")
    inner class FindByStoreCodeTests {

        @Test
        @DisplayName("거래처 코드로 조회 성공")
        fun findByStoreCode_success() {
            // Given
            val store = createStore(
                storeCode = "ST-00101",
                storeName = "이마트 부산점"
            )
            testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByStoreCode("ST-00101")

            // Then
            assertThat(result).isNotNull
            assertThat(result!!.storeCode).isEqualTo("ST-00101")
            assertThat(result.storeName).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("존재하지 않는 거래처 코드 조회 시 null 반환")
        fun findByStoreCode_notFound_returnsNull() {
            // Given
            val store = createStore(storeCode = "ST-00101")
            testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByStoreCode("ST-99999")

            // Then
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("거래처 코드는 유니크하므로 단일 결과만 반환")
        fun findByStoreCode_returnsUniqueResult() {
            // Given
            val store = createStore(
                storeCode = "ST-00101",
                storeName = "이마트 부산점"
            )
            testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result1 = storeRepository.findByStoreCode("ST-00101")
            val result2 = storeRepository.findByStoreCode("ST-00101")

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
            val store1 = createStore(storeCode = "ST-00101", storeName = "이마트 부산점")
            val store2 = createStore(storeCode = "ST-00102", storeName = "홈플러스 서면점")
            val store3 = createStore(storeCode = "ST-00103", storeName = "롯데마트 해운대점")
            val saved1 = testEntityManager.persistAndFlush(store1)
            val saved2 = testEntityManager.persistAndFlush(store2)
            val saved3 = testEntityManager.persistAndFlush(store3)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByIdIn(listOf(saved1.id, saved2.id))

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.storeName }).containsExactlyInAnyOrder(
                "이마트 부산점",
                "홈플러스 서면점"
            )
            assertThat(result.map { it.id }).containsExactlyInAnyOrder(saved1.id, saved2.id)
        }

        @Test
        @DisplayName("빈 ID 목록으로 조회 시 빈 리스트 반환")
        fun findByIdIn_emptyList_returnsEmpty() {
            // Given
            val store = createStore(storeCode = "ST-00101")
            testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByIdIn(emptyList())

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 ID는 결과에서 제외")
        fun findByIdIn_nonExistingIds_excluded() {
            // Given
            val store1 = createStore(storeCode = "ST-00101", storeName = "이마트 부산점")
            val store2 = createStore(storeCode = "ST-00102", storeName = "홈플러스 서면점")
            val saved1 = testEntityManager.persistAndFlush(store1)
            val saved2 = testEntityManager.persistAndFlush(store2)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByIdIn(listOf(saved1.id, saved2.id, 999L, 888L))

            // Then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.id }).containsExactlyInAnyOrder(saved1.id, saved2.id)
        }

        @Test
        @DisplayName("단일 ID로 조회")
        fun findByIdIn_singleId() {
            // Given
            val store = createStore(storeCode = "ST-00101", storeName = "이마트 부산점")
            val saved = testEntityManager.persistAndFlush(store)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByIdIn(listOf(saved.id))

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].storeName).isEqualTo("이마트 부산점")
        }

        @Test
        @DisplayName("모든 거래처 조회")
        fun findByIdIn_allStores() {
            // Given
            val store1 = createStore(storeCode = "ST-00101", storeName = "이마트 부산점")
            val store2 = createStore(storeCode = "ST-00102", storeName = "홈플러스 서면점")
            val store3 = createStore(storeCode = "ST-00103", storeName = "롯데마트 해운대점")
            val saved1 = testEntityManager.persistAndFlush(store1)
            val saved2 = testEntityManager.persistAndFlush(store2)
            val saved3 = testEntityManager.persistAndFlush(store3)
            testEntityManager.clear()

            // When
            val result = storeRepository.findByIdIn(listOf(saved1.id, saved2.id, saved3.id))

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.storeName }).containsExactlyInAnyOrder(
                "이마트 부산점",
                "홈플러스 서면점",
                "롯데마트 해운대점"
            )
        }
    }

    // ========== Helpers ==========

    private fun createStore(
        storeCode: String = "ST-00001",
        storeName: String = "테스트 거래처",
        address: String? = "부산시 테스트구",
        representativeName: String? = "테스트 대표",
        phoneNumber: String? = "010-1234-5678"
    ): Store {
        return Store(
            storeCode = storeCode,
            storeName = storeName,
            address = address,
            representativeName = representativeName,
            phoneNumber = phoneNumber,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
