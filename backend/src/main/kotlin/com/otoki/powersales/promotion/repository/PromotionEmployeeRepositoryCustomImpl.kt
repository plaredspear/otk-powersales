package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.entity.QPromotion.Companion.promotion
import com.otoki.powersales.promotion.entity.QPromotionEmployee.Companion.promotionEmployee
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.otoki.powersales.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
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
            // parentPolicyPredicate 의 owner/hierarchy path (promotion.ownerUser.*) 가 implicit
            // inner join 을 만들지 않도록 명시적 leftJoin. OR 합성이라 부모 ownerUser=null row 도
            // 다른 절로 통과해야 한다 (PromotionRepositoryCustomImpl 동일 패턴).
            .leftJoin(promotion.ownerUser)
            .where(parentPolicyPredicate)
            .fetch()
    }

    override fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee> {
        return queryFactory
            .selectFrom(promotionEmployee)
            .leftJoin(promotionEmployee.employee, employee).fetchJoin()
            // 전문행사조(투입당시) 값 = teamMemberSchedule.professionalPromotionTeam (SF ScheduleId__r.ProfessionalPromotionTeam__c 동등)
            .leftJoin(promotionEmployee.teamMemberSchedule, teamMemberSchedule).fetchJoin()
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
                // soft-delete 제외
                promotionEmployee.isDeleted.isNull.or(promotionEmployee.isDeleted.isFalse),
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
                promotionEmployee.isDeleted.isNull.or(promotionEmployee.isDeleted.isFalse),
                promotion.isDeleted.isFalse,
            )
            .orderBy(promotion.promotionNumber.asc())
            .fetch()
    }
}
