package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface SalesProgressRateMasterRepositoryCustom {

    /**
     * SF 가시 범위 정책 적용 admin 거래처목표등록마스터 목록.
     *
     * `policyPredicate` 는 [com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator]
     * 가 산출한 `SalesProgressRateMaster__c` 가시 범위 Predicate (OWD Private → owner / role hierarchy /
     * sharing rule / legacy branch OR 합성). 검색 필터와 AND 합성.
     *
     * 거래처명/지점명/코드/유형 컬럼은 `account` lookup 의 값을 fetchJoin 으로 함께 적재 (SF Formula 동등).
     *
     * `branchCode` 는 `account.branchCode` 기준 지점 필터 — 가시 범위 Predicate 와 AND 합성되어
     * 권한 외 지점 요청 시 자연히 0건 반환된다 (IDOR 자연 차단).
     */
    fun searchForAdmin(
        policyPredicate: Predicate,
        keyword: String?,
        targetYear: String?,
        targetMonth: String?,
        branchCode: String?,
        pageable: Pageable
    ): Page<SalesProgressRateMaster>

    /** 상세용 단건 조회 — `account` fetchJoin. */
    fun findByIdWithRelations(id: Long): SalesProgressRateMaster?

    /**
     * 단건이 SF 가시 범위(`policyPredicate`) 안에 있는지 (soft-delete 제외).
     *
     * 목록과 동일한 가시 범위 Predicate 로 단건 가시성을 평가. `false` 면 호출 측에서 404 처리
     * (목록↔단건 일관성 — 목록에 안 보이는 레코드는 상세 조회 불가).
     */
    fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean
}
