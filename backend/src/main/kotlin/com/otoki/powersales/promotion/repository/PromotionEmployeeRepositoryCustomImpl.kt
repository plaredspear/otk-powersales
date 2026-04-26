package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.sap.entity.QEmployee.Companion.employee
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class PromotionEmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PromotionEmployeeRepositoryCustom {

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
