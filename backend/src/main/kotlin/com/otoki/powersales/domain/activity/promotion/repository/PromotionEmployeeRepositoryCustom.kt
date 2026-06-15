package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
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

    /**
     * 다수 행사마스터의 목표/실적금액 일괄 집계 — SF rollup 재현 (단건 상세도 1건 리스트로 재사용).
     * - 목표금액 = SUM(조원 `dailyTargetCount * basePrice`) = SF `DKRetail__TargetAmount__c` 동등.
     * - 실적금액 = SUM(조원 `primaryProductAmount + otherSalesAmount`) = SF `ActualAmount__c`(총실적 rollup) 동등.
     * entity 저장 컬럼 target_amount/actual_amount 및 Promotion 의 dk*Amount 동기화 스칼라는 stale 이므로 미사용.
     * 반환: promotionId -> Pair(목표금액 합, 실적금액 합). 조원이 없는 promotionId 는 맵에 미포함.
     */
    fun sumTargetActualAmountByPromotionIds(promotionIds: Collection<Long>): Map<Long, Pair<Long, Long>>

    fun findMinScheduleDateByPromotionIdAndEmployeeId(promotionId: Long, employeeId: Long): LocalDate?

    /**
     * 행사사원 목표 대비 실적 보고서 조회 (Spec #845 — SF Report `new_report_AtQ` 이식).
     * `promotion_employee` ⋈ promotion ⋈ promotion.account ⋈ promotion.primaryProduct ⋈ employee ⋈ teamMemberSchedule.
     * 전사 (SF scope=organization). 필터: scheduleDate ∈ [startDate, endDate], soft-delete 제외.
     * 정렬: 행사명(promotion.promotionNumber = SF Name) 오름차순 + scheduleDate 오름차순 (Summary 그룹 재현).
     */
    fun findTargetActualReport(startDate: LocalDate, endDate: LocalDate): List<PromotionEmployee>

    /**
     * 로그인 여사원의 특정 일자 담당 행사 일람 (홈 "행사매출 등록" → 일 매출 등록 진입화면용).
     * 레거시 Heroku `eventlistapi`(EmployeeCode + StartDate=EndDate=today) 동등.
     * `promotion_employee` ⋈ promotion ⋈ promotion.account (fetchJoin).
     * 필터: employeeId == me, scheduleDate == date, soft-delete 제외, promotion soft-delete 제외.
     * 정렬: promotion.promotionNumber 오름차순.
     */
    fun findMyAssignmentsByDate(employeeId: Long, date: LocalDate): List<PromotionEmployee>
}
