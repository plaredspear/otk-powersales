package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.enums.PromotionType
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PromotionRepositoryCustom {

    fun findByIdWithRelations(id: Long): Promotion?

    /**
     * SF 가시 범위 정책 적용 admin 행사 목록.
     *
     * `policyPredicate` 는 [com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator]
     * 가 산출한 `DKRetail__Promotion__c` 가시 범위 Predicate (OWD Private → owner / role hierarchy /
     * sharing rule / legacy branch OR 합성). 검색 필터와 AND 합성.
     *
     * `ownerOnly` 가 true 면 가시 범위 안에서 다시 `ownerUser.id = currentUserId` 로 좁힌다
     * (SF 웹 ListView 의 filterScope=Mine 대응). `currentUserId` 가 null 이면 매칭 0건이 되도록 한다.
     *
     * `accountName` 은 거래처명/거래처코드(`Account.externalKey`) OR like 검색 (진열스케줄마스터 정합),
     * `accountNumber` 는 거래처번호(`Account.accountNumber`, SF `AccountNumber`) like 검색,
     * `category1` 은 제품유형(화면 "제품유형" 컬럼 = 대표제품 `Product.storeConditionText` 파생값) like 검색,
     * `primaryProduct` 는 대표제품명(`Product.name`)/제품코드(`Product.productCode`) OR like 검색. 모두 AND 합성.
     */
    fun searchForAdmin(
        policyPredicate: Predicate,
        keyword: String?,
        promotionType: PromotionType?,
        startDate: String?,
        endDate: String?,
        accountName: String?,
        accountNumber: String?,
        category1: String?,
        primaryProduct: String?,
        ownerOnly: Boolean,
        currentUserId: Long?,
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
        accountId: Long?,
        pageable: Pageable
    ): Page<Promotion>
}
