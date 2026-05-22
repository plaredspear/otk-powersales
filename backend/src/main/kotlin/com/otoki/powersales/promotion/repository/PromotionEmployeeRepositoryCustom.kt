package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.querydsl.core.types.Predicate
import java.time.LocalDate

interface PromotionEmployeeRepositoryCustom {

    /**
     * SF Sharing Rule 정책이 합성된 가시 PromotionEmployee 일람 (spec #782 P4-B — ControlledByParent).
     *
     * PromotionEmployee 의 sharingModel = ControlledByParent (parent = Promotion). Service layer 가
     * 부모 Promotion entity 기준 [SharingRulePolicyEvaluator.buildPredicate] 호출 결과를 [parentPolicyPredicate] 로 전달.
     * Repository 는 `join(promotionEmployee.promotion, promotion).where(parentPolicyPredicate)` 합성.
     */
    fun findAllAccessibleByParentPolicy(parentPolicyPredicate: Predicate): List<PromotionEmployee>

    fun findWithEmployeeByPromotionId(promotionId: Long): List<PromotionEmployee>

    fun findMinScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun findMaxScheduleDateByPromotionId(promotionId: Long): LocalDate?

    fun sumTargetAmountByPromotionId(promotionId: Long): Long

    fun sumActualAmountByPromotionId(promotionId: Long): Long

    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate?
}
