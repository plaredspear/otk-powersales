package com.otoki.powersales.entity

import com.otoki.powersales.common.config.QueryDslConfig
import com.otoki.powersales.common.entity.BranchReview
import com.otoki.powersales.common.entity.StaffReview
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("BranchReview Entity 매핑 테스트 (Spec #735)")
class BranchReviewTest {

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    @Test
    @DisplayName("BranchReview 생성/조회 - 기본 필드 매핑 확인")
    fun createBranchReview_basicFields() {
        // Given
        val branchReview = BranchReview(
            sfid = "a0h2x000001ABCD",
            name = "2026-01 지점평가",
            branchName = "서울지점",
            costCenterCode = "CC100",
            firstDayOfMonth = LocalDate.of(2026, 1, 1),
            confirmed = true,
        )

        // When
        val persisted = testEntityManager.persistAndFlush(branchReview)
        testEntityManager.clear()
        val found = testEntityManager.find(BranchReview::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isEqualTo("a0h2x000001ABCD")
        assertThat(found.name).isEqualTo("2026-01 지점평가")
        assertThat(found.branchName).isEqualTo("서울지점")
        assertThat(found.costCenterCode).isEqualTo("CC100")
        assertThat(found.firstDayOfMonth).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(found.confirmed).isTrue()
    }

    @Test
    @DisplayName("BranchReview 판촉 부문 - 합계 / 평균 필드 매핑")
    fun createBranchReview_salesPromotionFields() {
        // Given
        val branchReview = BranchReview(
            employeeEvaluationNumber = 12.0,
            sumAttendance = 95.0,
            sumTotalScore = 850.5,
            attendanceAverage = 7.9,
            sumTotalScoreAverage = 70.8,
        )

        // When
        val persisted = testEntityManager.persistAndFlush(branchReview)
        testEntityManager.clear()
        val found = testEntityManager.find(BranchReview::class.java, persisted.id)!!

        // Then
        assertThat(found.employeeEvaluationNumber).isEqualTo(12.0)
        assertThat(found.sumAttendance).isEqualTo(95.0)
        assertThat(found.sumTotalScore).isEqualTo(850.5)
        assertThat(found.attendanceAverage).isEqualTo(7.9)
        assertThat(found.sumTotalScoreAverage).isEqualTo(70.8)
    }

    @Test
    @DisplayName("BranchReview 레이디 부문 - 합계 / 평균 필드 매핑")
    fun createBranchReview_ladyFields() {
        // Given
        val branchReview = BranchReview(
            employeeEvaluationNumberLady = 8.0,
            sumAttendanceLady = 60.0,
            sumTotalScoreLady = 480.0,
            attendanceAverageLady = 7.5,
            sumTotalScoreAverageLady = 60.0,
        )

        // When
        val persisted = testEntityManager.persistAndFlush(branchReview)
        testEntityManager.clear()
        val found = testEntityManager.find(BranchReview::class.java, persisted.id)!!

        // Then
        assertThat(found.employeeEvaluationNumberLady).isEqualTo(8.0)
        assertThat(found.sumAttendanceLady).isEqualTo(60.0)
        assertThat(found.sumTotalScoreLady).isEqualTo(480.0)
        assertThat(found.attendanceAverageLady).isEqualTo(7.5)
        assertThat(found.sumTotalScoreAverageLady).isEqualTo(60.0)
    }

    @Test
    @DisplayName("BranchReview Group A — IsDeleted + sfid 버퍼 매핑")
    fun createBranchReview_groupAFields() {
        // Given
        val branchReview = BranchReview(
            isDeleted = false,
            ownerSfid = "005000000000001",
            createdBySfid = "005000000000002",
            lastModifiedBySfid = "005000000000003",
        )

        // When
        val persisted = testEntityManager.persistAndFlush(branchReview)
        testEntityManager.clear()
        val found = testEntityManager.find(BranchReview::class.java, persisted.id)!!

        // Then
        assertThat(found.isDeleted).isFalse()
        assertThat(found.ownerSfid).isEqualTo("005000000000001")
        assertThat(found.createdBySfid).isEqualTo("005000000000002")
        assertThat(found.lastModifiedBySfid).isEqualTo("005000000000003")
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("BranchReview 빈 객체 생성 - 모든 필드 null 허용")
    fun createBranchReview_nullableFields() {
        // Given
        val branchReview = BranchReview()

        // When
        val persisted = testEntityManager.persistAndFlush(branchReview)
        testEntityManager.clear()
        val found = testEntityManager.find(BranchReview::class.java, persisted.id)!!

        // Then
        assertThat(found).isNotNull
        assertThat(found.sfid).isNull()
        assertThat(found.name).isNull()
        assertThat(found.branchName).isNull()
        assertThat(found.isDeleted).isNull()
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("StaffReview ↔ BranchReview FK 양방향 — branch_review_id 매핑")
    fun staffReview_branchReviewRelation() {
        // Given
        val branchReview = testEntityManager.persistAndFlush(
            BranchReview(sfid = "a0h2x000001REL01", name = "2026-01 지점평가")
        )
        val staffReview = StaffReview(
            branchReviewSfid = branchReview.sfid,
            branchReview = branchReview,
        )

        // When
        val persisted = testEntityManager.persistAndFlush(staffReview)
        testEntityManager.clear()
        val found = testEntityManager.find(StaffReview::class.java, persisted.id)!!

        // Then
        assertThat(found.branchReviewSfid).isEqualTo("a0h2x000001REL01")
        assertThat(found.branchReview).isNotNull
        assertThat(found.branchReview!!.id).isEqualTo(branchReview.id)
        assertThat(found.branchReview!!.name).isEqualTo("2026-01 지점평가")
    }
}
