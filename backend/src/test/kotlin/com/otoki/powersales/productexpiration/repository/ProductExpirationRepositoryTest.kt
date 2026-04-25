package com.otoki.powersales.productexpiration.repository

import com.otoki.powersales.productexpiration.entity.ProductExpiration
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ProductExpiration V1 리매핑 테스트
 *
 * expirationdate__mng 테이블 매핑, seq(Int) PK, raw String 컬럼 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("ProductExpirationRepository 테스트 (V1 리매핑)")
class ProductExpirationRepositoryTest {

    @Autowired
    private lateinit var productExpirationRepository: ProductExpirationRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        productExpirationRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("save + findById - seq(Int) PK 매핑")
    inner class SaveAndFindByIdTests {

        @Test
        @DisplayName("저장 후 seq PK로 조회 - 정상 매핑")
        fun save_andFindBySeq_success() {
            // Given
            val productExpiration = ProductExpiration(
                accountName = "ACC001",
                accountCode = "1025",
                employeeId = 1L,
                productName = "PROD001",
                productCode = "30310009",
                expirationDate = LocalDate.of(2026, 6, 1),
                alarmDate = LocalDate.of(2026, 5, 25),
                description = "테스트 설명"
            ).apply {
                createdAt = LocalDateTime.of(2026, 2, 23, 10, 0)
                updatedAt = LocalDateTime.of(2026, 2, 23, 10, 0)
            }

            // When
            val saved = productExpirationRepository.save(productExpiration)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = productExpirationRepository.findById(saved.productExpirationId)
            assertThat(found).isPresent
            assertThat(found.get().productExpirationId).isEqualTo(saved.productExpirationId)
            assertThat(found.get().accountName).isEqualTo("ACC001")
            assertThat(found.get().accountCode).isEqualTo("1025")
            assertThat(found.get().employeeId).isEqualTo(1L)
            assertThat(found.get().productName).isEqualTo("PROD001")
            assertThat(found.get().productCode).isEqualTo("30310009")
            assertThat(found.get().expirationDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(found.get().alarmDate).isEqualTo(LocalDate.of(2026, 5, 25))
            assertThat(found.get().description).isEqualTo("테스트 설명")
        }

        @Test
        @DisplayName("nullable 필드 - null 값 저장/조회 정상")
        fun save_withNullableFields_success() {
            // Given
            val productExpiration = ProductExpiration(
                employeeId = 1L,
                productCode = "30310009"
            )

            // When
            val saved = productExpirationRepository.save(productExpiration)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = productExpirationRepository.findById(saved.productExpirationId)
            assertThat(found).isPresent
            assertThat(found.get().accountName).isNull()
            assertThat(found.get().expirationDate).isNull()
            assertThat(found.get().alarmDate).isNull()
            assertThat(found.get().description).isNull()
            assertThat(found.get().createdAt).isNotNull()
            assertThat(found.get().updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("countByEmployeeIdAndAlarmDate - 사원별 알람일 기준 건수 조회")
    inner class CountByEmployeeIdAndAlarmDateTests {

        @Test
        @DisplayName("알람 대상 건수 조회 - 해당 사원/오늘 3건 -> 3 반환")
        fun count_matchingRecords() {
            // Given
            val today = LocalDate.now()
            val employeeId = 100L

            repeat(3) {
                testEntityManager.persistAndFlush(
                    ProductExpiration(employeeId = employeeId, alarmDate = today, productCode = "P00$it")
                )
            }
            testEntityManager.clear()

            // When
            val count = productExpirationRepository.countByEmployeeIdAndAlarmDate(employeeId, today)

            // Then
            assertThat(count).isEqualTo(3L)
        }

        @Test
        @DisplayName("알람 대상 0건 - 해당 사원/오늘 0건 -> 0 반환")
        fun count_noMatchingRecords() {
            // Given
            val today = LocalDate.now()

            // When
            val count = productExpirationRepository.countByEmployeeIdAndAlarmDate(100L, today)

            // Then
            assertThat(count).isEqualTo(0L)
        }

        @Test
        @DisplayName("다른 날짜 건 제외 - 어제 알람만 존재 -> 오늘 조회 시 0")
        fun count_excludesDifferentDate() {
            // Given
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            val employeeId = 100L

            testEntityManager.persistAndFlush(
                ProductExpiration(employeeId = employeeId, alarmDate = yesterday, productCode = "P001")
            )
            testEntityManager.clear()

            // When
            val count = productExpirationRepository.countByEmployeeIdAndAlarmDate(employeeId, today)

            // Then
            assertThat(count).isEqualTo(0L)
        }

        @Test
        @DisplayName("다른 사원 건 제외 - 다른 employeeId의 건 -> 본인만 카운트")
        fun count_excludesDifferentEmployee() {
            // Given
            val today = LocalDate.now()

            testEntityManager.persistAndFlush(
                ProductExpiration(employeeId = 100L, alarmDate = today, productCode = "P001")
            )
            testEntityManager.persistAndFlush(
                ProductExpiration(employeeId = 200L, alarmDate = today, productCode = "P002")
            )
            testEntityManager.clear()

            // When
            val count = productExpirationRepository.countByEmployeeIdAndAlarmDate(100L, today)

            // Then
            assertThat(count).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("ManyToOne 제거 검증 - raw String 필드")
    inner class RawStringFieldTests {

        @Test
        @DisplayName("FK 없는 raw String 필드 - 임의 값 저장 가능")
        fun rawStringFields_noFkConstraint() {
            // Given - FK 제약 없이 임의의 문자열 저장 가능
            val productExpiration = ProductExpiration(
                accountName = "NON_EXISTENT_ACCOUNT",
                employeeId = 99999L,
                productName = "NON_EXISTENT_PRODUCT",
                productCode = "ARBITRARY_CODE"
            )

            // When
            val saved = productExpirationRepository.save(productExpiration)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = productExpirationRepository.findById(saved.productExpirationId)
            assertThat(found).isPresent
            assertThat(found.get().accountName).isEqualTo("NON_EXISTENT_ACCOUNT")
            assertThat(found.get().employeeId).isEqualTo(99999L)
            assertThat(found.get().productName).isEqualTo("NON_EXISTENT_PRODUCT")
        }
    }
}
