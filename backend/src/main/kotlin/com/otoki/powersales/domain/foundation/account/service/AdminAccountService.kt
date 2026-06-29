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
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.Expressions
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
     * @param excludeClosedAccount 폐업 거래처 완전 제외 여부. 진열사원스케줄 마스터 등록 거래처 lookup
     *              진입점만 true — 폐업 거래처는 등록 자체가 차단되므로 조회 후보에서도 제외한다.
     * @param applyMyBranchScope SF Account "내 지점 거래처"(`myBranchAccount` listView, `myAccount__c=1`)
     *              동등 — owner 무관 조직코드 매칭으로 가시성을 평가할지 여부. true 면 sharing policy
     *              (owner/hierarchy) 를 [myBranchScopePredicate] 로 대체한다. 행사마스터 거래처 lookup
     *              진입점 전용 — SF 레거시 listView 필터(`$User.HR_Code__c == DivisionCode__c OR
     *              SalesDeptCode__c OR BranchCode__c`)가 owner 가 아닌 조직코드 매칭이라, sharing policy
     *              (owner.user_role_id 계층) 로 평가하면 본인 지점 거래처가 owner 불일치로 전부 누락되는
     *              GAP 을 막는다.
     */
    fun getAccounts(
        scope: DataScope,
        keyword: String?,
        abcType: String?,
        branchCode: String?,
        accountStatusName: String?,
        page: Int,
        size: Int,
        applyPromotionFilter: Boolean = true,
        excludeClosedAccount: Boolean = false,
        applyMyBranchScope: Boolean = false
    ): AccountListResponse {
        val visibilityPredicate = if (applyMyBranchScope) {
            myBranchScopePredicate(scope)
        } else {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "Account",
                entityPath = account,
            )
        }

        // branchCode request param 은 별도 추가 필터로 AND 합성 — BooleanBuilder 합성.
        // sharing policy 가 가시성 평가 → branchCode 가 가시 범위 외면 매칭 0건 (자연 처리).
        val composedPolicyPredicate = BooleanBuilder().and(visibilityPredicate).also {
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
            excludeClosedAccount = excludeClosedAccount,
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
     * SF Account `myBranchAccount` listView 의 `myAccount__c = 1` 동등 가시성 predicate.
     *
     * SF formula (`myAccount__c.field-meta.xml`):
     * ```
     * IF($User.HR_Code__c == DivisionCode__c, TRUE,
     *   IF($User.HR_Code__c == SalesDeptCode__c, TRUE,
     *     IF($User.HR_Code__c == BranchCode__c, TRUE, FALSE)))
     * ```
     * 즉 로그인 사용자의 HR_Code (= Employee.costCenterCode = [DataScope.branchCodes]) 가 거래처의
     * 조직코드 3종 (사업부 `division_code` / 영업부 `sales_dept_code` / 지점 `branch_code`) 중 하나라도
     * 일치하면 노출 — owner 무관. [scope.branchCodes] 가 단일 코드라도 `in` 으로 합성해 다중 코드 권한자
     * (영업지원 등 비-전사 다중지점) 도 동일 식으로 커버한다.
     *
     * 전사 권한자 ([scope.isAllBranches] = 시스템 관리자 / 본부장·사업부장·영업부장 / 영업지원) 는
     * 무조건 전체 가시 — SF 에서 상위 조직코드 (OrgCodeLevel3 등) 가 광범위 매칭되는 동작의 단순화.
     * branchCodes 가 비어있으면 (코드 미보유) 매칭 0건.
     */
    private fun myBranchScopePredicate(scope: DataScope): Predicate {
        if (scope.isAllBranches) {
            return Expressions.asBoolean(true).isTrue
        }
        if (scope.branchCodes.isEmpty()) {
            return Expressions.asBoolean(false).isTrue
        }
        val codes = scope.branchCodes
        return account.divisionCode.`in`(codes)
            .or(account.salesDeptCode.`in`(codes))
            .or(account.branchCode.`in`(codes))
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
