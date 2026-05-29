package com.otoki.powersales.suggestion.repository

import com.otoki.powersales.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.suggestion.entity.Suggestion
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 제안 Repository Custom (Spec #830 P1-B §2.4).
 *
 * admin 권한 검색 — 7종 필터 + 페이징 + soft-delete 제외 + created_at DESC.
 *
 * `policyPredicate` 는 [com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator]
 * 가 산출한 SF 가시 범위 Predicate (owner / role hierarchy / sharing rule(OLS) 합성). 검색 필터와 AND 합성.
 */
interface SuggestionRepositoryCustom {

    fun searchForAdmin(
        policyPredicate: Predicate,
        filter: AdminSuggestionFilter,
        pageable: Pageable
    ): Page<Suggestion>

    /**
     * 단건이 SF 가시 범위(`policyPredicate`) 안에 있는지 여부 (soft-delete 제외).
     *
     * SF OWD=Private 동등: 목록에 안 보이는 레코드는 상세도 접근 불가. `false` 면 호출 측에서 404 처리.
     */
    fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean
}
