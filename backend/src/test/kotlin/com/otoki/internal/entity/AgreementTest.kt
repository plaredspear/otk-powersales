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
@DisplayName("Agreement Entity 매핑 테스트")
class AgreementTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("Agreement 생성 - 기본 필드 매핑 확인")
    fun createAgreement_basicFields() {
        // Given
        val agreement = Agreement(
            name = "개인정보 수집 동의",
            contents = "개인정보 수집 및 이용에 동의합니다.",
            active = true,
            activeDate = LocalDate.of(2026, 1, 1),
            afterActiveDate = LocalDate.of(2026, 12, 31)
        )

        // When
        val persisted = testEntityManager.persistAndFlush(agreement)
        testEntityManager.clear()
        val found = testEntityManager.find(Agreement::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isEqualTo("개인정보 수집 동의")
        assertThat(found.contents).isEqualTo("개인정보 수집 및 이용에 동의합니다.")
        assertThat(found.active).isTrue()
        assertThat(found.activeDate).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(found.afterActiveDate).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    @DisplayName("Agreement 생성 - nullable 필드 null 허용 확인")
    fun createAgreement_nullableFields() {
        // Given
        val agreement = Agreement(
            name = "약관명"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(agreement)
        testEntityManager.clear()
        val found = testEntityManager.find(Agreement::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isEqualTo("약관명")
        assertThat(found.contents).isNull()
        assertThat(found.active).isNull()
        assertThat(found.activeDate).isNull()
        assertThat(found.afterActiveDate).isNull()
        assertThat(found.sfid).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("Agreement 생성 - SF 공통 필드 매핑 확인")
    fun createAgreement_sfCommonFields() {
        // Given
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
        val agreement = Agreement(
            name = "GPS 동의",
            active = true,
            sfid = "a0B5g000001ABC",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(agreement)
        testEntityManager.clear()
        val found = testEntityManager.find(Agreement::class.java, persisted.id)

        // Then
        assertThat(found.sfid).isEqualTo("a0B5g000001ABC")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdDate).isEqualTo(now)
        assertThat(found.systemModStamp).isEqualTo(now)
        assertThat(found.hcLastOp).isEqualTo("SYNCED")
    }
}
