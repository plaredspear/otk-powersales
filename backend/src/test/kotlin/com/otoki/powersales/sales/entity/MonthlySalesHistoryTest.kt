package com.otoki.powersales.sales.entity

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.common.config.QueryDslConfig

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("MonthlySalesHistory Entity 매핑 테스트")
class MonthlySalesHistoryTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("MonthlySalesHistory 테이블 매핑 - 기본 필드 매핑 확인")
    fun createMonthlySalesHistory_basicFields() {
        // Given
        val history = MonthlySalesHistory(
            name = "MSH-2026-01",
            salesYear = SalesYear.Y2026,
            salesMonth = SalesMonth.M01,
            lastMonthResults = BigDecimal("1200"),
            shipClosingAmount = 800.0,
            abcClosingAmount1 = 100.0,
            abcClosingAmount2 = 200.0,
            abcClosingAmount3 = 300.0,
            ambientPurpose = 500.0,
            fridgePurpose = 400.0
        )

        // When
        val persisted = testEntityManager.persistAndFlush(history)
        testEntityManager.clear()
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isEqualTo("MSH-2026-01")
        assertThat(found.salesYear).isEqualTo(SalesYear.Y2026)
        assertThat(found.salesMonth).isEqualTo(SalesMonth.M01)
        assertThat(found.lastMonthResults).isEqualByComparingTo(BigDecimal("1200"))
        assertThat(found.shipClosingAmount).isEqualTo(800.0)
        assertThat(found.abcClosingAmount1).isEqualTo(100.0)
        assertThat(found.abcClosingAmount2).isEqualTo(200.0)
        assertThat(found.abcClosingAmount3).isEqualTo(300.0)
        assertThat(found.ambientPurpose).isEqualTo(500.0)
        assertThat(found.fridgePurpose).isEqualTo(400.0)
    }

    @Test
    @DisplayName("MonthlySalesHistory 기존 필드 제거 - V1 스키마 / Spec #740 에서 제거된 필드 미존재 확인")
    fun monthlySalesHistory_removedFieldsNotExist() {
        // Spec #740 Track A: SF Formula 정합 제거 8건
        val removedFields = listOf(
            "customerId", "yearMonth", "category", "achievedAmount",
            "accountExternalKey", "accountBranchName", "accountType",
            "fmYear", "fmMonth", "targetMonthResults",
            "lastMonthTargetFormula", "lastMonthTargetAchievedRatio"
        )
        val entityFields = MonthlySalesHistory::class.java.declaredFields.map { it.name }

        removedFields.forEach { fieldName ->
            assertThat(entityFields).doesNotContain(fieldName)
        }
    }

    @Test
    @DisplayName("MonthlySalesHistory 생성 - nullable 필드 null 허용 확인")
    fun createMonthlySalesHistory_nullableFields() {
        // Given
        val history = MonthlySalesHistory()

        // When
        val persisted = testEntityManager.persistAndFlush(history)
        testEntityManager.clear()
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isNull()
        assertThat(found.salesYear).isNull()
        assertThat(found.salesMonth).isNull()
        assertThat(found.lastMonthResults).isNull()
        assertThat(found.shipClosingAmount).isNull()
        assertThat(found.abcClosingAmount1).isNull()
        assertThat(found.abcClosingAmount2).isNull()
        assertThat(found.abcClosingAmount3).isNull()
        assertThat(found.ambientPurpose).isNull()
        assertThat(found.fridgePurpose).isNull()
        assertThat(found.sfid).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("MonthlySalesHistory 생성 - SF 공통 필드 매핑 확인")
    fun createMonthlySalesHistory_sfCommonFields() {
        // Given
        val history = MonthlySalesHistory(
            name = "MSH-SF",
            sfid = "a0B5g000001XYZ",
            isDeleted = false
        )

        // When
        val persisted = testEntityManager.persistAndFlush(history)
        testEntityManager.clear()
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)!!

        // Then — JPA save 경로는 AuditingEntityListener 가 createdAt/updatedAt 을 자동 채운다.
        assertThat(found.sfid).isEqualTo("a0B5g000001XYZ")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }
}
