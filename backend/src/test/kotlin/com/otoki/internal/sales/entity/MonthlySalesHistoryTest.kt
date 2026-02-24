package com.otoki.internal.sales.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
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
            accountExternalKey = "ACC-001",
            accountBranchName = "서울지점",
            accountType = "일반",
            salesYear = "2026",
            salesMonth = "01",
            fmYear = 2026.0,
            fmMonth = 1.0,
            targetMonthResults = 1500.0,
            lastMonthResults = 1200.0,
            lastMonthTargetFormula = 1300.0,
            lastMonthTargetAchievedRatio = 92.3,
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
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isEqualTo("MSH-2026-01")
        assertThat(found.accountExternalKey).isEqualTo("ACC-001")
        assertThat(found.accountBranchName).isEqualTo("서울지점")
        assertThat(found.accountType).isEqualTo("일반")
        assertThat(found.salesYear).isEqualTo("2026")
        assertThat(found.salesMonth).isEqualTo("01")
        assertThat(found.fmYear).isEqualTo(2026.0)
        assertThat(found.fmMonth).isEqualTo(1.0)
        assertThat(found.targetMonthResults).isEqualTo(1500.0)
        assertThat(found.lastMonthResults).isEqualTo(1200.0)
        assertThat(found.lastMonthTargetFormula).isEqualTo(1300.0)
        assertThat(found.lastMonthTargetAchievedRatio).isEqualTo(92.3)
        assertThat(found.shipClosingAmount).isEqualTo(800.0)
        assertThat(found.abcClosingAmount1).isEqualTo(100.0)
        assertThat(found.abcClosingAmount2).isEqualTo(200.0)
        assertThat(found.abcClosingAmount3).isEqualTo(300.0)
        assertThat(found.ambientPurpose).isEqualTo(500.0)
        assertThat(found.fridgePurpose).isEqualTo(400.0)
    }

    @Test
    @DisplayName("MonthlySalesHistory FLOAT8 필드 - Double 값 저장 및 null 허용 확인")
    fun createMonthlySalesHistory_float8Fields() {
        // Given - Double 값 저장
        val historyWithValues = MonthlySalesHistory(
            targetMonthResults = 1500.0
        )

        // When
        val persisted = testEntityManager.persistAndFlush(historyWithValues)
        testEntityManager.clear()
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.targetMonthResults).isEqualTo(1500.0)

        // Given - null 저장
        val historyWithNull = MonthlySalesHistory(
            targetMonthResults = null
        )

        // When
        val persistedNull = testEntityManager.persistAndFlush(historyWithNull)
        testEntityManager.clear()
        val foundNull = testEntityManager.find(MonthlySalesHistory::class.java, persistedNull.id)

        // Then
        assertThat(foundNull).isNotNull
        assertThat(foundNull.targetMonthResults).isNull()
    }

    @Test
    @DisplayName("MonthlySalesHistory 기존 필드 제거 - V1 스키마에서 제거된 필드 미존재 확인")
    fun monthlySalesHistory_removedFieldsNotExist() {
        // Given - V1 스키마에서 제거된 필드 목록
        val removedFields = listOf("customerId", "yearMonth", "category", "targetAmount", "achievedAmount")
        val entityFields = MonthlySalesHistory::class.java.declaredFields.map { it.name }

        // Then - 제거된 필드가 엔티티에 존재하지 않는지 확인
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
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.name).isNull()
        assertThat(found.accountExternalKey).isNull()
        assertThat(found.accountBranchName).isNull()
        assertThat(found.accountType).isNull()
        assertThat(found.salesYear).isNull()
        assertThat(found.salesMonth).isNull()
        assertThat(found.fmYear).isNull()
        assertThat(found.fmMonth).isNull()
        assertThat(found.targetMonthResults).isNull()
        assertThat(found.lastMonthResults).isNull()
        assertThat(found.lastMonthTargetFormula).isNull()
        assertThat(found.lastMonthTargetAchievedRatio).isNull()
        assertThat(found.shipClosingAmount).isNull()
        assertThat(found.abcClosingAmount1).isNull()
        assertThat(found.abcClosingAmount2).isNull()
        assertThat(found.abcClosingAmount3).isNull()
        assertThat(found.ambientPurpose).isNull()
        assertThat(found.fridgePurpose).isNull()
        assertThat(found.sfid).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("MonthlySalesHistory 생성 - SF 공통 필드 매핑 확인")
    fun createMonthlySalesHistory_sfCommonFields() {
        // Given
        val now = LocalDateTime.now()
        val history = MonthlySalesHistory(
            name = "MSH-SF",
            sfid = "a0B5g000001XYZ",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(history)
        testEntityManager.clear()
        val found = testEntityManager.find(MonthlySalesHistory::class.java, persisted.id)

        // Then
        assertThat(found.sfid).isEqualTo("a0B5g000001XYZ")
        assertThat(found.isDeleted).isFalse()
        assertThat(found.createdDate).isEqualTo(now)
        assertThat(found.systemModStamp).isEqualTo(now)
        assertThat(found.hcLastOp).isEqualTo("SYNCED")
    }
}
