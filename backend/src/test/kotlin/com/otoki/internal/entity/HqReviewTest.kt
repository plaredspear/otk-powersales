package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
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
import java.time.temporal.ChronoUnit

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
            evaluationType = "월간평가",
            abcTypeCode = "A",
            hrCode = "HR001"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.branchCode).isEqualTo("B001")
        assertThat(found.branchName).isEqualTo("서울지점")
        assertThat(found.firstDayOfMonth).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(found.evaluationType).isEqualTo("월간평가")
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
        val found = testEntityManager.find(HqReview::class.java, persisted.id)

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
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("HqReview 생성 - SF 공통 필드 매핑 확인")
    fun createHqReview_sfCommonFields() {
        // Given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val hqReview = HqReview(
            branchCode = "B001",
            sfid = "a0B5g000001ABC",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(hqReview)
        testEntityManager.clear()
        val found = testEntityManager.find(HqReview::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isEqualTo("a0B5g000001ABC")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdDate).isEqualTo(now)
        assertThat(found.systemModStamp).isEqualTo(now)
        assertThat(found.hcLastOp).isEqualTo("SYNCED")
        assertThat(found.hcErr).isNull()
    }
}
