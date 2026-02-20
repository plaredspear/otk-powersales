package com.otoki.internal.repository

import com.otoki.internal.entity.ExpiryProduct
import com.otoki.internal.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * ExpiryProductRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ExpiryProductRepositoryTest {

    @Autowired
    private lateinit var expiryProductRepository: ExpiryProductRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private var testUserId: Long = 0

    @BeforeEach
    fun setUp() {
        expiryProductRepository.deleteAll()
        testEntityManager.clear()

        val user = User(
            employeeId = "20030117",
            password = "encodedPassword",
            name = "최금주",
            orgName = "부산1지점"
        )
        testUserId = testEntityManager.persistAndFlush(user).id
        testEntityManager.clear()
    }

    @Test
    @DisplayName("countByUserIdAndExpiryDateBetween - 범위 내 제품이 있으면 건수를 반환한다")
    fun countByUserIdAndExpiryDateBetween_withProducts() {
        // Given
        val today = LocalDate.now()
        val product1 = ExpiryProduct(
            userId = testUserId,
            productName = "진라면",
            storeName = "이마트 부산점",
            expiryDate = today.plusDays(3)
        )
        val product2 = ExpiryProduct(
            userId = testUserId,
            productName = "참치캔",
            storeName = "홈플러스 해운대점",
            expiryDate = today.plusDays(5)
        )
        val product3 = ExpiryProduct(
            userId = testUserId,
            productName = "케첩",
            storeName = "이마트 부산점",
            expiryDate = today.plusDays(7)
        )
        testEntityManager.persistAndFlush(product1)
        testEntityManager.persistAndFlush(product2)
        testEntityManager.persistAndFlush(product3)
        testEntityManager.clear()

        // When
        val count = expiryProductRepository.countByUserIdAndExpiryDateBetween(
            testUserId, today, today.plusDays(7)
        )

        // Then
        assertThat(count).isEqualTo(3L)
    }

    @Test
    @DisplayName("countByUserIdAndExpiryDateBetween - 범위 외 제품만 있으면 0을 반환한다")
    fun countByUserIdAndExpiryDateBetween_outOfRange() {
        // Given
        val today = LocalDate.now()
        val product = ExpiryProduct(
            userId = testUserId,
            productName = "진라면",
            storeName = "이마트 부산점",
            expiryDate = today.plusDays(30) // 30일 후 = 범위 밖
        )
        testEntityManager.persistAndFlush(product)
        testEntityManager.clear()

        // When
        val count = expiryProductRepository.countByUserIdAndExpiryDateBetween(
            testUserId, today, today.plusDays(7)
        )

        // Then
        assertThat(count).isEqualTo(0L)
    }

    @Test
    @DisplayName("countByUserIdAndExpiryDateBetween - 다른 사용자의 제품은 카운트되지 않는다")
    fun countByUserIdAndExpiryDateBetween_differentUser() {
        // Given
        val today = LocalDate.now()
        val otherUser = User(
            employeeId = "20030118",
            password = "encodedPassword",
            name = "김영희",
            orgName = "서울1지점"
        )
        val otherUserId = testEntityManager.persistAndFlush(otherUser).id

        val product = ExpiryProduct(
            userId = otherUserId,
            productName = "진라면",
            storeName = "이마트 서울점",
            expiryDate = today.plusDays(3)
        )
        testEntityManager.persistAndFlush(product)
        testEntityManager.clear()

        // When
        val count = expiryProductRepository.countByUserIdAndExpiryDateBetween(
            testUserId, today, today.plusDays(7)
        )

        // Then
        assertThat(count).isEqualTo(0L)
    }

    @Test
    @DisplayName("countByUserIdAndExpiryDateBetween - 제품이 없으면 0을 반환한다")
    fun countByUserIdAndExpiryDateBetween_noProducts() {
        // When
        val today = LocalDate.now()
        val count = expiryProductRepository.countByUserIdAndExpiryDateBetween(
            testUserId, today, today.plusDays(7)
        )

        // Then
        assertThat(count).isEqualTo(0L)
    }

    @Test
    @DisplayName("countByUserIdAndExpiryDateBetween - 경계값(오늘, 7일 후) 포함 확인")
    fun countByUserIdAndExpiryDateBetween_boundaryDates() {
        // Given
        val today = LocalDate.now()
        val productToday = ExpiryProduct(
            userId = testUserId,
            productName = "오늘만료",
            storeName = "이마트",
            expiryDate = today // 오늘 (포함)
        )
        val productEndDay = ExpiryProduct(
            userId = testUserId,
            productName = "7일후만료",
            storeName = "이마트",
            expiryDate = today.plusDays(7) // 7일 후 (포함)
        )
        val productOutside = ExpiryProduct(
            userId = testUserId,
            productName = "8일후만료",
            storeName = "이마트",
            expiryDate = today.plusDays(8) // 8일 후 (미포함)
        )
        testEntityManager.persistAndFlush(productToday)
        testEntityManager.persistAndFlush(productEndDay)
        testEntityManager.persistAndFlush(productOutside)
        testEntityManager.clear()

        // When
        val count = expiryProductRepository.countByUserIdAndExpiryDateBetween(
            testUserId, today, today.plusDays(7)
        )

        // Then
        assertThat(count).isEqualTo(2L) // 오늘 + 7일후 (Between은 양쪽 포함)
    }
}
