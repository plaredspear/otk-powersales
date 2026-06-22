package com.otoki.powersales.domain.activity.suggestion.repository

import com.otoki.powersales.domain.activity.suggestion.dto.admin.AdminSuggestionFilter
import com.otoki.powersales.domain.activity.suggestion.entity.Suggestion
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionCategory
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.time.LocalDateTime

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
     * 본인/원가센터 단위 제안·물류클레임 목록 조회 (모바일 — 레거시 `LogisticsClaimSearch` 동등).
     *
     * 스코프: [scopeOrgCostCenterCode] 가 non-null 이면 해당 원가센터 전체(조장·지점장 물류클레임),
     *        null 이면 [employeeId] 본인분(여사원 / 정규 제안)으로 분기한다.
     * 선택 필터(전부 nullable, null 이면 미적용):
     * - [category] : 분류(물류클레임 전용 진입 = LOGISTICS_CLAIM)
     * - [accountId] : 거래처 (레거시 SAPAccountCode 정확일치 대응)
     * - [createdFrom] / [createdToExclusive] : `created_at` 범위. 레거시는 `CreatedDate` 기준 +
     *   종료일 익일 미만 경계이므로 [createdToExclusive] 에 (종료일+1일 00:00) 을 전달한다.
     * 정렬: created_at DESC, soft-delete 제외.
     */
    fun searchMine(
        employeeId: Long,
        scopeOrgCostCenterCode: String?,
        category: SuggestionCategory?,
        accountId: Long?,
        createdFrom: LocalDateTime?,
        createdToExclusive: LocalDateTime?,
        pageable: Pageable
    ): Page<Suggestion>

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
