package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.enums.PromotionType
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PromotionRepositoryCustom {

    fun findByIdWithRelations(id: Long): Promotion?

    /**
     * SF 가시 범위 정책 적용 admin 행사 목록.
     *
     * `policyPredicate` 는 [com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator]
     * 가 산출한 `DKRetail__Promotion__c` 가시 범위 Predicate (OWD Private → owner / role hierarchy /
     * sharing rule / legacy branch OR 합성). 검색 필터와 AND 합성.
     */
    fun searchForAdmin(
        policyPredicate: Predicate,
        keyword: String?,
        promotionType: PromotionType?,
        startDate: String?,
        endDate: String?,
        pageable: Pageable
    ): Page<Promotion>

    /**
     * 단건이 SF 가시 범위(`policyPredicate`) 안에 있는지 (soft-delete 제외).
     *
     * 목록과 동일한 가시 범위 Predicate 로 단건 가시성을 평가. `false` 면 호출 측에서 403 처리
     * (목록↔단건 일관성 — 목록에 안 보이는 행사는 상세/수정/삭제 불가).
     */
    fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean

    /**
     * 모바일 행사 조회: 여사원은 배정된 행사만, 그 외는 같은 지점 행사 전체
     */
    fun searchForMobile(
        employeeId: Long?,
        costCenterCode: String?,
        isWoman: Boolean,
        keyword: String?,
        startDate: String?,
        endDate: String?,
        pageable: Pageable
    ): Page<Promotion>
}
