package com.otoki.powersales.entity

import com.otoki.powersales.common.enums.EvaluationType
import com.otoki.powersales.common.entity.HqReview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.common.config.QueryDslConfig
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("HqReview Entity 매핑 테스트")
class HqReviewTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("HqReview 생성/조회 - branchCode 매핑 확인")
    fun createHqReview_basicFields() {
        // Given
        val hqReview = HqReview(
            branchCode = "B001",
            branchName = "서울지점",
            firstDayOfMonth = LocalDate.of(2026, 2, 1),
            evaluationType = EvaluationType.FIRST_DIVISION,
            abcTypeCode = "A",
            hrCode = "HR001"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.branchCode).isEqualTo("B001")
        assertThat(found.branchName).isEqualTo("서울지점")
        assertThat(found.firstDayOfMonth).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(found.evaluationType).isEqualTo(EvaluationType.FIRST_DIVISION)
        assertThat(found.abcTypeCode).isEqualTo("A")
        assertThat(found.hrCode).isEqualTo("HR001")
    }

    @Test
    @DisplayName("HqReview 생성 - nullable 필드 null 허용 확인")
    fun createHqReview_nullableFields() {
        // Given
        val hqReview = HqReview()

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.branchCode).isNull()
        assertThat(found.branchName).isNull()
        assertThat(found.firstDayOfMonth).isNull()
        assertThat(found.evaluationType).isNull()
        assertThat(found.abcTypeCode).isNull()
        assertThat(found.hrCode).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("HqReview 생성 - SF 공통 필드 매핑 확인")
    fun createHqReview_sfCommonFields() {
        // Given
        val hqReview = HqReview(
            branchCode = "B001",
            sfid = "a0B5g000001ABC",
            isDeleted = false
        )

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)!!

        // Then — JPA save 경로는 AuditingEntityListener 가 createdAt/updatedAt 을
        // 자동 채운다. 본 테스트의 관심사는 sfid/isDeleted 매핑이므로 timestamp 는
        // 비어있지 않은지만 확인한다 (Migration 경로의 timestamp 보존은 native INSERT 경로).
        assertThat(found).isNotNull
        assertThat(found.sfid).isEqualTo("a0B5g000001ABC")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("HqReview evaluationType Converter — DB 한국어 원본 저장 확인")
    fun createHqReview_evaluationTypeConverter() {
        // Given
        val hqReview = HqReview(evaluationType = EvaluationType.DISTRIBUTION_HQ)

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)!!

        // Then — EvaluationTypeConverter 가 한국어 displayName 으로 저장/역직렬화
        assertThat(found.evaluationType).isEqualTo(EvaluationType.DISTRIBUTION_HQ)
        assertThat(found.evaluationType?.displayName).isEqualTo("유통총괄실")
    }
}
