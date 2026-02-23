package com.otoki.internal.entity

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
@DisplayName("StaffReview Entity 매핑 테스트")
class StaffReviewTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("StaffReview 생성/조회 - employeeTotalScore 매핑 확인")
    fun createStaffReview_basicFields() {
        // Given
        val staffReview = StaffReview(
            name = "2026-01 영업사원 평가",
            employeeId = "a0B5g000001XYZ",
            employeeName = "홍길동",
            employeeNumber = "EMP001",
            branch = "서울지점",
            costCenterCode = "CC100",
            employeeTotalScore = 85.5
        )

        // When
        val persisted = testEntityManager.persistAndFlush(staffReview)
        testEntityManager.clear()
        val found = testEntityManager.find(StaffReview::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.employeeTotalScore).isEqualTo(85.5)
        assertThat(found.name).isEqualTo("2026-01 영업사원 평가")
        assertThat(found.employeeId).isEqualTo("a0B5g000001XYZ")
        assertThat(found.employeeName).isEqualTo("홍길동")
        assertThat(found.employeeNumber).isEqualTo("EMP001")
        assertThat(found.branch).isEqualTo("서울지점")
        assertThat(found.costCenterCode).isEqualTo("CC100")
    }

    @Test
    @DisplayName("StaffReview branchReviews - sfid 형식 문자열 저장 확인")
    fun createStaffReview_branchReviews() {
        // Given
        val staffReview = StaffReview(
            branchReviews = "a1B000000001234"
        )

        // When
        val persisted = testEntityManager.persistAndFlush(staffReview)
        testEntityManager.clear()
        val found = testEntityManager.find(StaffReview::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.branchReviews).isEqualTo("a1B000000001234")
    }

    @Test
    @DisplayName("StaffReview 생성 - nullable 필드 null 허용 확인")
    fun createStaffReview_nullableFields() {
        // Given
        val staffReview = StaffReview()

        // When
        val persisted = testEntityManager.persistAndFlush(staffReview)
        testEntityManager.clear()
        val found = testEntityManager.find(StaffReview::class.java, persisted.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.employeeId).isNull()
        assertThat(found.employeeName).isNull()
        assertThat(found.employeeNumber).isNull()
        assertThat(found.branch).isNull()
        assertThat(found.branchReviews).isNull()
        assertThat(found.costCenterCode).isNull()
        assertThat(found.employeeTotalScore).isNull()
        assertThat(found.attendance).isNull()
        assertThat(found.instructionsDefault).isNull()
        assertThat(found.priorityEventItemManage).isNull()
        assertThat(found.displayManageEventGoals).isNull()
        assertThat(found.businessPartnerTies).isNull()
        assertThat(found.clothesSatellite).isNull()
        assertThat(found.productManageCallment).isNull()
        assertThat(found.educationalEvaluation).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdDate).isNull()
        assertThat(found.systemModStamp).isNull()
        assertThat(found.hcLastOp).isNull()
        assertThat(found.hcErr).isNull()
    }

    @Test
    @DisplayName("StaffReview 생성 - SF 공통 필드 매핑 확인")
    fun createStaffReview_sfCommonFields() {
        // Given
        val now = LocalDateTime.now()
        val staffReview = StaffReview(
            sfid = "a0B5g000001ABC",
            isDeleted = false,
            createdDate = now,
            systemModStamp = now,
            hcLastOp = "SYNCED",
            hcErr = null
        )

        // When
        val persisted = testEntityManager.persistAndFlush(staffReview)
        testEntityManager.clear()
        val found = testEntityManager.find(StaffReview::class.java, persisted.id)

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
