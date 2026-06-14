package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

/**
 * 제안 Repository Custom (Spec #830 P1-B §2.4).
 *
 * admin 권한 검색 — 7종 필터 + 페이징 + soft-delete 제외 + created_at DESC.
 *
 * `policyPredicate` 는 [com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator]
 * 가 산출한 SF 가시 범위 Predicate (owner / role hierarchy 합성 — `DKRetail__Proposal__c` 대상
 * sharing rule 은 SF 운영 0건). 검색 필터와 AND 합성.
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

    /**
     * 물류 클레임 보고서 조회 (Spec #844 — SF Report `OLS_dmK`/`new_report_6dy`/`OLS_NDx` 이식).
     * `suggestion` ⋈ employee ⋈ account ⋈ product. 페이지네이션 없이 전량 추출 (전사 — SF scope=organization).
     * 필터: category='물류 클레임'(LOGISTICS_CLAIM), claimDate ∈ [startDate, endDate], soft-delete 제외.
     *       SF WERK1_TX/WERK3_TX 의 'contains 빈값' 은 항상 참(no-op)이라 미구현.
     * 정렬: claimDate 내림차순.
     */
    fun findLogisticsClaimReport(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Suggestion>
}
