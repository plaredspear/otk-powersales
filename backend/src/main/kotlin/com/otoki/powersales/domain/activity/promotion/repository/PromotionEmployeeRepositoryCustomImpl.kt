package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.entity.QPromotion.Companion.promotion
import com.otoki.powersales.domain.activity.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.domain.activity.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.math.BigDecimal
import java.time.LocalDate

class PromotionEmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : PromotionEmployeeRepositoryCustom {

    // 행사사원 soft-delete(IsDeleted) 제외 — SF 정합 (모든 조회에서 IsDeleted=true row 자동 제외).
    // is_deleted 는 nullable(SF migration row 정합)이라 NULL 도 미삭제로 통과시킨다.
    private val notDeleted: Predicate =
        promotionEmployee.isDeleted.isNull.or(promotionEmployee.isDeleted.isFalse)

    override fun findAllAccessibleByParentPolicy(parentPolicyPredicate: Predicate): List<PromotionEmployee> {
        // ControlledByParent — 자식 PromotionEmployee 의 가시성은 부모 Promotion 의 정책 결과 흡수.
        // Service layer 가 SharingRulePolicyEvaluator.buildPredicate(scope, "DKRetail__Promotion__c", QPromotion) 호출.
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            // parentPolicyPredicate 의 owner/hierarchy path (promotion.ownerUser.*) 가 implicit
            // inner join 을 만들지 않도록 명시적 leftJoin. OR 합성이라 부모 ownerUser=null row 도
            // 다른 절로 통과해야 한다 (PromotionRepositoryCustomImpl 동일 패턴).
            .leftJoin(promotion.ownerUser)
            .where(parentPolicyPredicate, notDeleted)
            .fetch()
    }

    override fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .leftJoin(promotionEmployee.employee, employee).fetchJoin()
            // 전문행사조(투입당시) 값 = teamMemberSchedule.professionalPromotionTeam (SF ScheduleId__r.ProfessionalPromotionTeam__c 동등)
            .leftJoin(promotionEmployee.teamMemberSchedule, teamMemberSchedule).fetchJoin()
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .orderBy(promotionEmployee.scheduleDate.asc())
            .fetch()
    }

    override fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .fetchOne()
    }

    override fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.max())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId), notDeleted)
            .fetchOne()
    }

    // 조원 목표금액 파생식 SUM = SUM(COALESCE(daily_target_count,0) * COALESCE(base_price,0)).
    private val dailyTargetAmountSum: NumberExpression<BigDecimal> =
        promotionEmployee.dailyTargetCount.coalesce(BigDecimal.ZERO)
            .multiply(promotionEmployee.basePrice.coalesce(BigDecimal.ZERO))
            .sumAggregate()

    // 조원 실적금액(총 실적) 파생식 SUM = SUM(COALESCE(primary_product_amount,0) + COALESCE(other_sales_amount,0)).
    private val dailyActualAmountSum: NumberExpression<BigDecimal> =
        promotionEmployee.primaryProductAmount.coalesce(BigDecimal.ZERO)
            .add(promotionEmployee.otherSalesAmount.coalesce(BigDecimal.ZERO))
            .sumAggregate()

    override fun sumTargetActualAmountByPromotionIds(promotionIds: Collection<Long>): Map<Long, Pair<Long, Long>> {
        if (promotionIds.isEmpty()) return emptyMap()
        return queryFactory
            .select(
                promotionEmployee.promotionId,
                dailyTargetAmountSum,
                dailyActualAmountSum,
            )
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.`in`(promotionIds), notDeleted)
            .groupBy(promotionEmployee.promotionId)
            .fetch()
            .associate { tuple ->
                val pid = tuple.get(promotionEmployee.promotionId)!!
                val target = tuple.get(dailyTargetAmountSum)?.toLong() ?: 0L
                val actual = tuple.get(dailyActualAmountSum)?.toLong() ?: 0L
                pid to (target to actual)
            }
    }

    override fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(
                promotionEmployee.promotionId.eq(promotionId),
                promotionEmployee.employeeId.eq(employeeId),
                notDeleted
            )
            .fetchOne()
    }

    override fun findTargetActualReport(startDate: LocalDate, endDate: LocalDate): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion).fetchJoin()
            .leftJoin(promotion.account, account).fetchJoin()
            .leftJoin(promotion.primaryProduct, product).fetchJoin()
            .leftJoin(promotionEmployee.employee, employee).fetchJoin()
            // isWorkReport / commuteDate 컬럼은 TeamMemberSchedule 소유 (PromotionEmployee→schedule 조인)
            .leftJoin(promotionEmployee.teamMemberSchedule, teamMemberSchedule).fetchJoin()
            .where(
                promotionEmployee.scheduleDate.between(startDate, endDate),
                notDeleted, // soft-delete 제외
                // 전사 — SF scope=organization (영업지원실용)
            )
            // Summary 그룹 재현 — 행사명(promotion.Name = promotionNumber) 그룹 + 그룹 내 일자 오름차순
            .orderBy(promotion.promotionNumber.asc(), promotionEmployee.scheduleDate.asc())
            .fetch()
    }

    override fun findMyAssignmentsByDate(employeeId: Long, date: LocalDate): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion).fetchJoin()
            .leftJoin(promotion.account, account).fetchJoin()
            .where(
                promotionEmployee.employeeId.eq(employeeId),
                promotionEmployee.scheduleDate.eq(date),
                notDeleted,
                promotion.isDeleted.isFalse,
            )
            .orderBy(promotion.promotionNumber.asc())
            .fetch()
    }
}
