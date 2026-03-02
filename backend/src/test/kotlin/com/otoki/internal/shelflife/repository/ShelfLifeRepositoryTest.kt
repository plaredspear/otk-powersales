package com.otoki.internal.shelflife.repository

import com.otoki.internal.shelflife.entity.ShelfLife
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ShelfLife V1 리매핑 테스트
 *
 * expirationdate__mng 테이블 매핑, seq(Int) PK, raw String 컬럼 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("ShelfLifeRepository 테스트 (V1 리매핑)")
class ShelfLifeRepositoryTest {

    @Autowired
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @BeforeEach
    fun setUp() {
        shelfLifeRepository.deleteAll()
        testEntityManager.clear()
    }

    @Nested
    @DisplayName("save + findById - seq(Int) PK 매핑")
    inner class SaveAndFindByIdTests {

        @Test
        @DisplayName("저장 후 seq PK로 조회 - 정상 매핑")
        fun save_andFindBySeq_success() {
            // Given
            val shelfLife = ShelfLife(
                accountId = "ACC001",
                accountCode = "1025",
                employeeId = "20030117",
                productId = "PROD001",
                productCode = "30310009",
                expirationDate = LocalDate.of(2026, 6, 1),
                alarmDate = LocalDate.of(2026, 5, 25),
                description = "테스트 설명",
                instDt = LocalDateTime.of(2026, 2, 23, 10, 0),
                updtDt = LocalDateTime.of(2026, 2, 23, 10, 0)
            )

            // When
            val saved = shelfLifeRepository.save(shelfLife)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = shelfLifeRepository.findById(saved.seq)
            assertThat(found).isPresent
            assertThat(found.get().seq).isEqualTo(saved.seq)
            assertThat(found.get().accountId).isEqualTo("ACC001")
            assertThat(found.get().accountCode).isEqualTo("1025")
            assertThat(found.get().employeeId).isEqualTo("20030117")
            assertThat(found.get().productId).isEqualTo("PROD001")
            assertThat(found.get().productCode).isEqualTo("30310009")
            assertThat(found.get().expirationDate).isEqualTo(LocalDate.of(2026, 6, 1))
            assertThat(found.get().alarmDate).isEqualTo(LocalDate.of(2026, 5, 25))
            assertThat(found.get().description).isEqualTo("테스트 설명")
        }

        @Test
        @DisplayName("nullable 필드 - null 값 저장/조회 정상")
        fun save_withNullableFields_success() {
            // Given
            val shelfLife = ShelfLife(
                employeeId = "20030117",
                productCode = "30310009"
            )

            // When
            val saved = shelfLifeRepository.save(shelfLife)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = shelfLifeRepository.findById(saved.seq)
            assertThat(found).isPresent
            assertThat(found.get().accountId).isNull()
            assertThat(found.get().expirationDate).isNull()
            assertThat(found.get().alarmDate).isNull()
            assertThat(found.get().description).isNull()
            assertThat(found.get().instDt).isNull()
            assertThat(found.get().updtDt).isNull()
        }
    }

    @Nested
    @DisplayName("ManyToOne 제거 검증 - raw String 필드")
    inner class RawStringFieldTests {

        @Test
        @DisplayName("FK 없는 raw String 필드 - 임의 값 저장 가능")
        fun rawStringFields_noFkConstraint() {
            // Given - FK 제약 없이 임의의 문자열 저장 가능
            val shelfLife = ShelfLife(
                accountId = "NON_EXISTENT_ACCOUNT",
                employeeId = "NON_EXISTENT_EMP",
                productId = "NON_EXISTENT_PRODUCT",
                productCode = "ARBITRARY_CODE"
            )

            // When
            val saved = shelfLifeRepository.save(shelfLife)
            testEntityManager.flush()
            testEntityManager.clear()

            // Then
            val found = shelfLifeRepository.findById(saved.seq)
            assertThat(found).isPresent
            assertThat(found.get().accountId).isEqualTo("NON_EXISTENT_ACCOUNT")
            assertThat(found.get().employeeId).isEqualTo("NON_EXISTENT_EMP")
            assertThat(found.get().productId).isEqualTo("NON_EXISTENT_PRODUCT")
        }
    }
}
