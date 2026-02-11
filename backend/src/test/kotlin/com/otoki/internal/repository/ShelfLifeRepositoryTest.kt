package com.otoki.internal.repository

import com.otoki.internal.entity.*
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
import java.time.LocalDate

/**
 * ShelfLifeRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ShelfLifeRepositoryTest {

    @Autowired
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testUser: User
    private lateinit var testStore: Store
    private lateinit var testProduct: Product
    private lateinit var otherUser: User
    private lateinit var otherStore: Store

    @BeforeEach
    fun setUp() {
        shelfLifeRepository.deleteAll()
        testEntityManager.clear()

        testUser = testEntityManager.persistAndFlush(
            User(
                employeeId = "20030117",
                password = "encodedPassword",
                name = "최금주",
                department = "영업1팀",
                branchName = "부산1지점",
                role = UserRole.USER
            )
        )

        otherUser = testEntityManager.persistAndFlush(
            User(
                employeeId = "20030118",
                password = "encodedPassword",
                name = "김영희",
                department = "영업2팀",
                branchName = "서울1지점",
                role = UserRole.USER
            )
        )

        testStore = testEntityManager.persistAndFlush(
            Store(
                storeCode = "1025",
                storeName = "그린유통D"
            )
        )

        otherStore = testEntityManager.persistAndFlush(
            Store(
                storeCode = "2030",
                storeName = "광양식자재도매센터(주)"
            )
        )

        testProduct = testEntityManager.persistAndFlush(
            Product(
                productId = "30310009",
                productName = "고등어김치&무조림(캔)280G",
                productCode = "30310009",
                barcode = "8801045570001",
                storageType = "상온"
            )
        )

        testEntityManager.clear()
    }

    // ========== findByUserIdAndExpiryDateBetween ==========

    @Nested
    @DisplayName("findByUserIdAndExpiryDateBetween - 사용자별 기간 조회")
    inner class FindByUserIdAndExpiryDateBetweenTests {

        @Test
        @DisplayName("기간 내 데이터 조회 - 해당 기간 데이터 반환")
        fun findByUserIdAndExpiryDateBetween_success() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today.plusDays(2)
            )

            // When
            val result = shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                testUser.id, today, today.plusDays(7)
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].productName).isEqualTo("고등어김치&무조림(캔)280G")
        }

        @Test
        @DisplayName("기간 외 데이터 - 범위 밖 데이터는 조회되지 않음")
        fun findByUserIdAndExpiryDateBetween_outOfRange() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(30), alertDate = today.plusDays(29)
            )

            // When
            val result = shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                testUser.id, today, today.plusDays(7)
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("다른 사용자 데이터 - 타인 데이터는 조회되지 않음")
        fun findByUserIdAndExpiryDateBetween_differentUser() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = otherUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today.plusDays(2)
            )

            // When
            val result = shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                testUser.id, today, today.plusDays(7)
            )

            // Then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("expiryDate 오름차순 정렬 확인")
        fun findByUserIdAndExpiryDateBetween_orderedByExpiryDate() {
            // Given
            val today = LocalDate.now()
            val product2 = testEntityManager.persistAndFlush(
                Product(
                    productId = "11110015",
                    productName = "카레케찹280G",
                    productCode = "11110015",
                    barcode = "8801045570002",
                    storageType = "상온"
                )
            )
            testEntityManager.clear()

            persistShelfLife(
                user = testUser, store = testStore, product = product2,
                expiryDate = today.plusDays(5), alertDate = today.plusDays(4)
            )
            persistShelfLife(
                user = testUser, store = otherStore, product = testProduct,
                expiryDate = today.plusDays(2), alertDate = today.plusDays(1)
            )

            // When
            val result = shelfLifeRepository.findByUserIdAndExpiryDateBetween(
                testUser.id, today, today.plusDays(7)
            )

            // Then
            assertThat(result).hasSize(2)
            assertThat(result[0].expiryDate).isBefore(result[1].expiryDate)
        }
    }

    // ========== findByUserIdAndStoreIdAndExpiryDateBetween ==========

    @Nested
    @DisplayName("findByUserIdAndStoreIdAndExpiryDateBetween - 사용자+거래처별 기간 조회")
    inner class FindByUserIdAndStoreIdAndExpiryDateBetweenTests {

        @Test
        @DisplayName("특정 거래처 데이터만 조회")
        fun findByUserIdAndStoreIdAndExpiryDateBetween_storeFilter() {
            // Given
            val today = LocalDate.now()
            val product2 = testEntityManager.persistAndFlush(
                Product(
                    productId = "11110015",
                    productName = "카레케찹280G",
                    productCode = "11110015",
                    barcode = "8801045570002",
                    storageType = "상온"
                )
            )
            testEntityManager.clear()

            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today.plusDays(2)
            )
            persistShelfLife(
                user = testUser, store = otherStore, product = product2,
                expiryDate = today.plusDays(4), alertDate = today.plusDays(3)
            )

            // When
            val result = shelfLifeRepository.findByUserIdAndStoreIdAndExpiryDateBetween(
                testUser.id, testStore.id, today, today.plusDays(7)
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].storeName).isEqualTo("그린유통D")
        }
    }

    // ========== existsByUserIdAndStoreIdAndProductId ==========

    @Nested
    @DisplayName("existsByUserIdAndStoreIdAndProductId - 중복 확인")
    inner class ExistsByUserIdAndStoreIdAndProductIdTests {

        @Test
        @DisplayName("존재하는 조합 -> true")
        fun existsByUserIdAndStoreIdAndProductId_exists() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today.plusDays(2)
            )

            // When
            val result = shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(
                testUser.id, testStore.id, testProduct.id
            )

            // Then
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 조합 -> false")
        fun existsByUserIdAndStoreIdAndProductId_notExists() {
            // When
            val result = shelfLifeRepository.existsByUserIdAndStoreIdAndProductId(
                testUser.id, testStore.id, testProduct.id
            )

            // Then
            assertThat(result).isFalse()
        }
    }

    // ========== findByAlertDateAndAlertSentFalse ==========

    @Nested
    @DisplayName("findByAlertDateAndAlertSentFalse - 알림 발송 대상 조회")
    inner class FindByAlertDateAndAlertSentFalseTests {

        @Test
        @DisplayName("오늘 알림 대상 - alertDate=오늘, alertSent=false -> 조회됨")
        fun findByAlertDateAndAlertSentFalse_success() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today, alertSent = false
            )

            // When
            val result = shelfLifeRepository.findByAlertDateAndAlertSentFalse(today)

            // Then
            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("이미 발송 완료 - alertSent=true -> 조회되지 않음")
        fun findByAlertDateAndAlertSentFalse_alreadySent() {
            // Given
            val today = LocalDate.now()
            persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today, alertSent = true
            )

            // When
            val result = shelfLifeRepository.findByAlertDateAndAlertSentFalse(today)

            // Then
            assertThat(result).isEmpty()
        }
    }

    // ========== findByIdInAndUserId ==========

    @Nested
    @DisplayName("findByIdInAndUserId - 사용자 소유 항목 일괄 조회")
    inner class FindByIdInAndUserIdTests {

        @Test
        @DisplayName("본인 소유 항목만 조회")
        fun findByIdInAndUserId_ownerOnly() {
            // Given
            val today = LocalDate.now()
            val product2 = testEntityManager.persistAndFlush(
                Product(
                    productId = "11110015",
                    productName = "카레케찹280G",
                    productCode = "11110015",
                    barcode = "8801045570002",
                    storageType = "상온"
                )
            )
            testEntityManager.clear()

            val sl1 = persistShelfLife(
                user = testUser, store = testStore, product = testProduct,
                expiryDate = today.plusDays(3), alertDate = today.plusDays(2)
            )
            val sl2 = persistShelfLife(
                user = otherUser, store = testStore, product = product2,
                expiryDate = today.plusDays(4), alertDate = today.plusDays(3)
            )

            // When
            val result = shelfLifeRepository.findByIdInAndUserId(
                listOf(sl1.id, sl2.id), testUser.id
            )

            // Then
            assertThat(result).hasSize(1)
            assertThat(result[0].user.id).isEqualTo(testUser.id)
        }
    }

    // ========== save ==========

    @Nested
    @DisplayName("save - 유통기한 저장")
    inner class SaveTests {

        @Test
        @DisplayName("신규 저장 성공")
        fun save_success() {
            // Given
            val today = LocalDate.now()
            val shelfLife = ShelfLife(
                user = testUser,
                store = testStore,
                product = testProduct,
                productCode = testProduct.productCode,
                productName = testProduct.productName,
                storeName = testStore.storeName,
                expiryDate = today.plusDays(10),
                alertDate = today.plusDays(9),
                description = "테스트 설명"
            )

            // When
            val saved = shelfLifeRepository.save(shelfLife)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = shelfLifeRepository.findById(saved.id)
            assertThat(found).isPresent
            assertThat(found.get().productCode).isEqualTo("30310009")
            assertThat(found.get().description).isEqualTo("테스트 설명")
        }
    }

    // ========== Helper ==========

    private fun persistShelfLife(
        user: User,
        store: Store,
        product: Product,
        expiryDate: LocalDate,
        alertDate: LocalDate,
        alertSent: Boolean = false,
        description: String? = null
    ): ShelfLife {
        val shelfLife = ShelfLife(
            user = user,
            store = store,
            product = product,
            productCode = product.productCode,
            productName = product.productName,
            storeName = store.storeName,
            expiryDate = expiryDate,
            alertDate = alertDate,
            description = description,
            alertSent = alertSent
        )
        val persisted = testEntityManager.persistAndFlush(shelfLife)
        testEntityManager.clear()
        return persisted
    }
}
