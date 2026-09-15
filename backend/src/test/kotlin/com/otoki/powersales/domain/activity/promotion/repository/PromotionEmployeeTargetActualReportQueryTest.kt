package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import java.math.BigDecimal
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
 * 목표 대비 실적 보고서의 조회 2분할 회귀 테스트.
 *
 * 소계/합계/차트/전체 행 수는 전량 기준이어야 하고 상세 행만 상한까지 내려가야 한다. 이전에는 전량을
 * 메모리로 올려 `sumOf` 로 집계했으나, 집계를 DB `GROUP BY` 쿼리로 분리하고 상세에 SQL `LIMIT` 을 걸었다.
 * 따라서 검증의 핵심은 **집계 쿼리 결과 == 상세 전량을 in-memory 로 합산한 값** 이라는 등가성이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("PromotionEmployeeRepository 목표 대비 실적 보고서 쿼리 테스트")
class PromotionEmployeeTargetActualReportQueryTest {

    @Autowired
    private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val periodStart: LocalDate = LocalDate.of(2026, 6, 1)
    private val periodEnd: LocalDate = LocalDate.of(2026, 6, 30)

    @BeforeEach
    fun setUp() {
        promotionEmployeeRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistPromotion(promotionNumber: String): Promotion =
        testEntityManager.persistAndFlush(
            Promotion(
                promotionNumber = promotionNumber,
                startDate = periodStart,
                endDate = periodEnd,
                costCenterCode = "5815",
                isDeleted = false,
            )
        )

    private fun persistEmployee(employeeCode: String, costCenterCode: String?): Employee =
        testEntityManager.persistAndFlush(
            Employee(employeeCode = employeeCode, name = "행사사원$employeeCode").apply {
                this.costCenterCode = costCenterCode
            }
        )

    private fun persistRow(
        promotionId: Long,
        employeeId: Long?,
        scheduleDate: LocalDate = LocalDate.of(2026, 6, 12),
        targetCount: BigDecimal? = null,
        basePrice: BigDecimal? = null,
        primaryQty: BigDecimal? = null,
        primaryAmount: BigDecimal? = null,
        otherQty: BigDecimal? = null,
        otherAmount: BigDecimal? = null,
        isDeleted: Boolean? = false,
    ): PromotionEmployee =
        testEntityManager.persistAndFlush(
            PromotionEmployee(
                promotionId = promotionId,
                employeeId = employeeId,
                scheduleDate = scheduleDate,
                isDeleted = isDeleted,
                dailyTargetCount = targetCount,
                basePrice = basePrice,
                primarySalesQuantity = primaryQty,
                primaryProductAmount = primaryAmount,
                otherSalesQuantity = otherQty,
                otherSalesAmount = otherAmount,
            )
        )

    /** 상세 전량을 in-memory 합산 — 집계 쿼리가 재현해야 할 기준값 (이전 구현의 계산식 그대로). */
    private fun expectedSubtotals(
        records: List<PromotionTargetActualReportRecord>,
    ): Map<String?, List<BigDecimal>> =
        records.groupBy { it.promotionName }.mapValues { (_, recs) ->
            listOf(
                recs.sumOf { it.targetAmount ?: BigDecimal.ZERO },
                recs.sumOf { it.actualAmount ?: BigDecimal.ZERO },
                recs.sumOf { it.primarySalesQuantity ?: BigDecimal.ZERO },
                recs.sumOf { it.primaryProductAmount ?: BigDecimal.ZERO },
                recs.sumOf { it.otherSalesQuantity ?: BigDecimal.ZERO },
                recs.sumOf { it.otherSalesAmount ?: BigDecimal.ZERO },
            )
        }

    @Test
    @DisplayName("집계 쿼리 결과 = 상세 전량 in-memory 합산 (NULL 은 0 취급)")
    fun subtotalsMatchInMemorySumOfAllDetails() {
        val employee = persistEmployee("E001", "5815")
        val a = persistPromotion("P-A")
        val b = persistPromotion("P-B")
        // A행사 — 값이 채워진 행 + 일부 컬럼이 NULL 인 행 + 전 컬럼 NULL 인 행
        persistRow(a.id, employee.id, targetCount = BigDecimal(10), basePrice = BigDecimal(100), primaryQty = BigDecimal(2), primaryAmount = BigDecimal(300), otherQty = BigDecimal(1), otherAmount = BigDecimal(70))
        persistRow(a.id, employee.id, targetCount = BigDecimal(5), basePrice = null, primaryQty = null, primaryAmount = BigDecimal(50), otherQty = BigDecimal(3), otherAmount = null)
        persistRow(a.id, employee.id)
        // B행사
        persistRow(b.id, employee.id, targetCount = BigDecimal(7), basePrice = BigDecimal(10), primaryQty = BigDecimal(4), primaryAmount = BigDecimal(40), otherQty = BigDecimal(2), otherAmount = BigDecimal(20))

        val details = promotionEmployeeRepository.findTargetActualReport(periodStart, periodEnd, emptyList(), null)
        val subtotals = promotionEmployeeRepository.findTargetActualReportSubtotals(periodStart, periodEnd, emptyList())

        val expected = expectedSubtotals(details)
        assertThat(subtotals).hasSize(2)
        // 그룹 순서는 상세 조회와 같은 promotionNumber 오름차순
        assertThat(subtotals.map { it.promotionName }).containsExactly("P-A", "P-B")
        subtotals.forEach { subtotal ->
            val exp = expected.getValue(subtotal.promotionName)
            assertThat(subtotal.targetAmount).isEqualByComparingTo(exp[0])
            assertThat(subtotal.actualAmount).isEqualByComparingTo(exp[1])
            assertThat(subtotal.primaryQuantity).isEqualByComparingTo(exp[2])
            assertThat(subtotal.primaryAmount).isEqualByComparingTo(exp[3])
            assertThat(subtotal.otherQuantity).isEqualByComparingTo(exp[4])
            assertThat(subtotal.otherAmount).isEqualByComparingTo(exp[5])
        }
        assertThat(subtotals.sumOf { it.rowCount }).isEqualTo(details.size.toLong())
    }

    @Test
    @DisplayName("상세 limit 은 SQL LIMIT 으로 적용되고 집계는 그 영향을 받지 않는다")
    fun detailLimitDoesNotAffectSubtotals() {
        val employee = persistEmployee("E002", "5815")
        val promotion = persistPromotion("P-A")
        repeat(5) {
            persistRow(promotion.id, employee.id, targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN)
        }

        val limited = promotionEmployeeRepository.findTargetActualReport(periodStart, periodEnd, emptyList(), 2)
        val all = promotionEmployeeRepository.findTargetActualReport(periodStart, periodEnd, emptyList(), null)
        val subtotals = promotionEmployeeRepository.findTargetActualReportSubtotals(periodStart, periodEnd, emptyList())

        assertThat(limited).hasSize(2)
        assertThat(all).hasSize(5)
        assertThat(subtotals).hasSize(1)
        assertThat(subtotals[0].rowCount).isEqualTo(5)
        assertThat(subtotals[0].targetAmount).isEqualByComparingTo(BigDecimal(50))
    }

    @Test
    @DisplayName("지점 스코프(employee.costCenterCode)와 soft-delete 제외가 두 쿼리에 동일 적용된다")
    fun sameFilterOnBothQueries() {
        val inScope = persistEmployee("E010", "5815")
        val outOfScope = persistEmployee("E011", "9999")
        val promotion = persistPromotion("P-A")
        persistRow(promotion.id, inScope.id, targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN)
        persistRow(promotion.id, outOfScope.id, targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN)
        // 삭제 행 — 양쪽 모두에서 제외되어야 한다
        persistRow(promotion.id, inScope.id, targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN, isDeleted = true)

        val details = promotionEmployeeRepository.findTargetActualReport(periodStart, periodEnd, listOf("5815"), null)
        val subtotals = promotionEmployeeRepository.findTargetActualReportSubtotals(periodStart, periodEnd, listOf("5815"))

        assertThat(details).hasSize(1)
        assertThat(subtotals).hasSize(1)
        assertThat(subtotals[0].rowCount).isEqualTo(1)
        assertThat(subtotals[0].targetAmount).isEqualByComparingTo(BigDecimal.TEN)
    }

    @Test
    @DisplayName("기간 밖 행은 두 쿼리 모두에서 제외된다")
    fun outOfPeriodExcluded() {
        val employee = persistEmployee("E020", "5815")
        val promotion = persistPromotion("P-A")
        persistRow(promotion.id, employee.id, scheduleDate = periodStart.minusDays(1), targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN)
        persistRow(promotion.id, employee.id, scheduleDate = periodEnd, targetCount = BigDecimal.ONE, basePrice = BigDecimal.TEN)

        val details = promotionEmployeeRepository.findTargetActualReport(periodStart, periodEnd, emptyList(), null)
        val subtotals = promotionEmployeeRepository.findTargetActualReportSubtotals(periodStart, periodEnd, emptyList())

        assertThat(details).hasSize(1)
        assertThat(subtotals[0].rowCount).isEqualTo(1)
    }
}
