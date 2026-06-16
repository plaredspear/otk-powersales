package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.org.employee.entity.Employee
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 행사사원 조회의 soft-delete(IsDeleted) 제외 회귀 테스트 — SF 정합.
 *
 * SF 는 행사사원 조회 시 표준 SOQL 기본 동작으로 IsDeleted=true row 를 항상 제외한다.
 * 신규 환경의 promotion_employee.is_deleted 는 nullable(SF migration row 정합)이며,
 * 삭제 row(=true)는 제외하되 NULL(미삭제)은 통과해야 한다. 상세 화면 목록 / 복제·cascade /
 * 중복 체크 등 promotionId 기준 조회 전반이 동일하게 삭제 row 를 거르는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("PromotionEmployeeRepository soft-delete 제외 테스트")
class PromotionEmployeeRepositorySoftDeleteTest {

    @Autowired
    private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private var promotionNumberSeq = 0

    @BeforeEach
    fun setUp() {
        promotionEmployeeRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistPromotion(): Promotion {
        val p = Promotion(
            promotionNumber = "P-${promotionNumberSeq++}",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            costCenterCode = "5815",
            isDeleted = false,
        )
        return testEntityManager.persistAndFlush(p)
    }

    private fun persistPromotionEmployee(
        promotionId: Long,
        employeeId: Long? = null,
        isDeleted: Boolean? = null,
        promoCloseByTm: Boolean = false,
    ): PromotionEmployee {
        val pe = PromotionEmployee(
            promotionId = promotionId,
            employeeId = employeeId,
            scheduleDate = LocalDate.of(2026, 6, 12),
            promoCloseByTm = promoCloseByTm,
            isDeleted = isDeleted,
        )
        return testEntityManager.persistAndFlush(pe)
    }

    @Test
    @DisplayName("findByPromotionId — is_deleted=true 는 제외, NULL/false 는 포함")
    fun findByPromotionId_excludesSoftDeleted() {
        val promotion = persistPromotion()
        persistPromotionEmployee(promotion.id, isDeleted = null)   // 미삭제 (NULL)
        persistPromotionEmployee(promotion.id, isDeleted = false)  // 미삭제
        persistPromotionEmployee(promotion.id, isDeleted = true)   // 삭제 — 제외 대상

        val result = promotionEmployeeRepository.findByPromotionId(promotion.id)

        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.isDeleted != true }
    }

    @Test
    @DisplayName("findWithEmployeeByPromotionId — 상세 화면 목록도 삭제 row 제외")
    fun findWithEmployeeByPromotionId_excludesSoftDeleted() {
        val promotion = persistPromotion()
        persistPromotionEmployee(promotion.id, isDeleted = false)
        persistPromotionEmployee(promotion.id, isDeleted = true)

        val result = promotionEmployeeRepository.findWithEmployeeByPromotionId(promotion.id)

        assertThat(result).hasSize(1)
        assertThat(result[0].isDeleted).isNotEqualTo(true)
    }

    @Test
    @DisplayName("existsByPromotionIdAndEmployeeId — 삭제된 행사사원은 중복으로 보지 않는다")
    fun existsByPromotionIdAndEmployeeId_excludesSoftDeleted() {
        val promotion = persistPromotion()
        val employee = testEntityManager.persistAndFlush(
            Employee(employeeCode = "EMP100", name = "삭제대상사원")
        )
        persistPromotionEmployee(promotion.id, employeeId = employee.id, isDeleted = true)

        // 삭제된 row 만 있으므로 미존재로 판정되어야 함 (재등록 허용)
        val exists = promotionEmployeeRepository.existsByPromotionIdAndEmployeeId(promotion.id, employee.id)

        assertThat(exists).isFalse()
    }

    @Test
    @DisplayName("existsByPromotionIdAndPromoCloseByTmTrue — 삭제된 마감 행사사원은 마감 보호에서 제외")
    fun existsByPromotionIdAndPromoCloseByTmTrue_excludesSoftDeleted() {
        val promotion = persistPromotion()
        persistPromotionEmployee(promotion.id, promoCloseByTm = true, isDeleted = true)

        val exists = promotionEmployeeRepository.existsByPromotionIdAndPromoCloseByTmTrue(promotion.id)

        assertThat(exists).isFalse()
    }
}
