package com.otoki.internal.promotion.repository

import com.otoki.internal.promotion.entity.QPromotionEmployee.promotionEmployee
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class PromotionEmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PromotionEmployeeRepositoryCustom {

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
            .select(promotionEmployee.targetAmount.sum().coalesce(0L))
            .from(promotionEmployee)
            .where(promotionEmployee.promotionId.eq(promotionId))
            .fetchOne() ?: 0L
    }

    override fun sumActualAmountByPromotionId(promotionId: Long): Long {
        return queryFactory
            .select(promotionEmployee.actualAmount.sum().coalesce(0L))
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
