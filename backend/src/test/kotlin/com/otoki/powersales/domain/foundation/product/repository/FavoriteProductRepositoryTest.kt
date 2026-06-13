package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.foundation.product.entity.FavoriteProduct
import com.otoki.powersales.domain.foundation.product.entity.ProductFavoriteId
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
import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.domain.foundation.product.repository.FavoriteProductRepository

/**
 * FavoriteProduct V1 리매핑 테스트
 *
 * product_favorites 테이블 매핑, @IdClass 복합 키 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("FavoriteProductRepository 테스트 (V1 리매핑)")
class FavoriteProductRepositoryTest {

    @Autowired
    private lateinit var favoriteProductRepository: FavoriteProductRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        favoriteProductRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("save + findById - @IdClass 복합 키 매핑")
    inner class SaveAndFindByIdTests {

        @Test
        @DisplayName("복합 키로 저장 후 조회 - 정상 매핑")
        fun save_andFindByCompositeKey_success() {
            // Given
            val favorite = FavoriteProduct(
                employeeCode = "20030117",
                productCode = "30310009"
            )

            // When
            favoriteProductRepository.save(favorite)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then — JPA save 경로는 AuditingEntityListener 가 createdAt/updatedAt 을 자동 채운다.
            // 본 테스트의 관심사는 @IdClass 복합 키 매핑이다.
            val compositeKey = ProductFavoriteId("20030117", "30310009")
            val found = favoriteProductRepository.findById(compositeKey)
            assertThat(found).isPresent
            assertThat(found.get().employeeCode).isEqualTo("20030117")
            assertThat(found.get().productCode).isEqualTo("30310009")
            assertThat(found.get().createdAt).isNotNull()
            assertThat(found.get().updatedAt).isNotNull()
        }

        @Test
        @DisplayName("nullable 필드 - null 값 저장/조회 정상")
        fun save_withNullableFields_success() {
            // Given
            val favorite = FavoriteProduct(
                employeeCode = "20030117",
                productCode = "30310009"
            )

            // When
            favoriteProductRepository.save(favorite)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val compositeKey = ProductFavoriteId("20030117", "30310009")
            val found = favoriteProductRepository.findById(compositeKey)
            assertThat(found).isPresent
            assertThat(found.get().createdAt).isNotNull()
            assertThat(found.get().updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("복합 키 동작 검증")
    inner class CompositeKeyBehaviorTests {

        @Test
        @DisplayName("다른 복합 키 조합 - 각각 독립 저장/조회")
        fun differentCompositeKeys_independentStorage() {
            // Given
            val fav1 = FavoriteProduct(
                employeeCode = "EMP001",
                productCode = "PROD001"
            )
            val fav2 = FavoriteProduct(
                employeeCode = "EMP001",
                productCode = "PROD002"
            )
            val fav3 = FavoriteProduct(
                employeeCode = "EMP002",
                productCode = "PROD001"
            )

            // When
            favoriteProductRepository.saveAll(listOf(fav1, fav2, fav3))
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            assertThat(favoriteProductRepository.count()).isEqualTo(3)
            assertThat(favoriteProductRepository.findById(ProductFavoriteId("EMP001", "PROD001"))).isPresent
            assertThat(favoriteProductRepository.findById(ProductFavoriteId("EMP001", "PROD002"))).isPresent
            assertThat(favoriteProductRepository.findById(ProductFavoriteId("EMP002", "PROD001"))).isPresent
            assertThat(favoriteProductRepository.findById(ProductFavoriteId("EMP002", "PROD002"))).isNotPresent
        }

        @Test
        @DisplayName("ProductFavoriteId equals/hashCode - 동일 키 비교")
        fun productFavoriteId_equalsAndHashCode() {
            // Given
            val id1 = ProductFavoriteId("EMP001", "PROD001")
            val id2 = ProductFavoriteId("EMP001", "PROD001")
            val id3 = ProductFavoriteId("EMP001", "PROD002")

            // Then
            assertThat(id1).isEqualTo(id2)
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
            assertThat(id1).isNotEqualTo(id3)
        }
    }
}
