package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.response.AccountDetailResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListItem
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListResponse
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.querydsl.core.BooleanBuilder
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminAccountService(
    private val accountRepository: AccountRepository,
    private val policyEvaluator: SharingRulePolicyEvaluator,
) {

    /**
     * SF Sharing Rule 정책 적용 거래처 목록 조회 (spec #787 — 단일화).
     *
     * 본 메서드는 [SharingRulePolicyEvaluator.buildPredicate] 결과 Predicate 를
     * 검색 필터 (keyword / abcType / branchCode / accountStatusName) + 페이지네이션과 합성한 후
     * [com.otoki.powersales.domain.foundation.account.repository.AccountRepositoryCustom.findAllAccessibleByPolicy] 호출.
     *
     * 우선순위 1-6 평가 (evaluator):
     * - Profile.viewAllData / PermissionSet.viewAllRecords[Account] → no-filter
     * - Owner / UserRole Hierarchy / SharingRule (Account 56 rule) / Legacy branchCodes OR 합성
     *
     * `branchCode` request param 은 sharing policy 와 AND 합성 — 사용자가 권한 외 branchCode 요청 시
     * sharing policy 매칭 0건 → empty page 자연 반환.
     *
     * @param scope 호출자(controller) 에서 산출/주입한 현재 사용자의 DataScope.
     *              service 가 holder/ambient context 에 의존하지 않도록 explicit parameter 로 받는다.
     * @param applyPromotionFilter `AccId__c.lookupFilter` (accountGroup ∈ {1000,1010} + 폐업/distribution)
     *              적용 여부. 행사/클레임/제품 거래처 lookup 진입점은 true (SF Lookup 필드 정합),
     *              메인 거래처 목록(`GET /api/v1/admin/accounts`)은 false (SF AllAccounts listView=Everything,
     *              추가 필터 없음 — lookupFilter 를 메인 목록에 적용하면 과소노출 GAP).
     */
    fun getAccounts(
        scope: DataScope,
        keyword: String?,
        abcType: String?,
        branchCode: String?,
        accountStatusName: String?,
        page: Int,
        size: Int,
        applyPromotionFilter: Boolean = true
    ): AccountListResponse {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "Account",
            entityPath = account,
        )

        // branchCode request param 은 별도 추가 필터로 AND 합성 — BooleanBuilder 합성.
        // sharing policy 가 가시성 평가 → branchCode 가 가시 범위 외면 매칭 0건 (자연 처리).
        val composedPolicyPredicate = BooleanBuilder().and(policyPredicate).also {
            if (!branchCode.isNullOrBlank()) {
                it.and(account.branchCode.eq(branchCode))
            }
        }

        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())

        val accountPage = accountRepository.findAllAccessibleByPolicy(
            policyPredicate = composedPolicyPredicate,
            keyword = keyword,
            abcType = abcType,
            accountStatusName = accountStatusName,
            applyPromotionFilter = applyPromotionFilter,
            pageable = pageable,
        )

        return AccountListResponse(
            content = accountPage.content.map { AccountListItem.Companion.from(it) },
            page = page,
            size = size,
            totalElements = accountPage.totalElements,
            totalPages = accountPage.totalPages
        )
    }

    /**
     * SF Sharing Rule 정책 적용 단건 거래처 상세 조회.
     *
     * 목록(`getAccounts`)과 동일하게 [SharingRulePolicyEvaluator.buildPredicate] 결과를 적용해
     * 가시 범위 안의 거래처만 조회한다. 가시 범위 밖 id 를 요청하면 매칭 0건 → [AccountNotFoundException]
     * (SF sharing rule 의 "권한 없는 레코드는 존재하지 않음" 동등 — 존재 여부 정보 노출 방지).
     *
     * @param scope 호출자(controller)에서 산출/주입한 현재 사용자의 DataScope.
     * @param id path variable Account.id
     * @throws AccountNotFoundException 부재 / soft-delete / 가시 범위 밖
     */
    fun getAccountDetail(scope: DataScope, id: Long): AccountDetailResponse {
        val policyPredicate = policyEvaluator.buildPredicate(
            scope = scope,
            sObjectName = "Account",
            entityPath = account,
        )

        val found = accountRepository.findAccessibleByPolicyAndId(policyPredicate, id)
            ?: throw AccountNotFoundException(id)

        return AccountDetailResponse.Companion.from(found)
    }
}
