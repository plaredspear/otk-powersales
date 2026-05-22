package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.QPromotion.Companion.promotion
import com.otoki.powersales.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class PromotionEmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : PromotionEmployeeRepositoryCustom {

    override fun findAllAccessibleByParentPolicy(parentPolicyPredicate: Predicate): List<PromotionEmployee> {
        // ControlledByParent — 자식 PromotionEmployee 의 가시성은 부모 Promotion 의 정책 결과 흡수.
        // Service layer 가 SharingRulePolicyEvaluator.buildPredicate(scope, "DKRetail__Promotion__c", QPromotion) 호출.
        return queryFactory
            .selectFrom(promotionEmployee)
            .join(promotionEmployee.promotion, promotion)
            .where(parentPolicyPredicate)
            .fetch()
    }

    override fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .leftJoin(promotionEmployee.employee, employee).fetchJoin()
            .where(promotionEmployee.promotionId.eq(promotionId))
            .orderBy(promotionEmployee.scheduleDate.asc())
            .fetch()
    }

    override fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId))
            .fetchOne()
    }

    override fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.max())
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId))
            .fetchOne()
    }

    override fun sumTargetAmountByPromotionId(promotionId: Long): Long {
        return queryFactory
            .select(promotionEmployee.targetAmount.sumAggregate().coalesce(0L))
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId))
            .fetchOne() ?: 0L
    }

    override fun sumActualAmountByPromotionId(promotionId: Long): Long {
        return queryFactory
            .select(promotionEmployee.actualAmount.sumAggregate().coalesce(0L))
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId))
            .fetchOne() ?: 0L
    }

    override fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate? {
        return queryFactory
            .select(promotionEmployee.scheduleDate.min())
            .from(promotionEmployee)
            .where(
                promotionEmployee.promotionId.eq(promotionId),
                promotionEmployee.employeeId.eq(employeeId)
            )
            .fetchOne()
    }
}
