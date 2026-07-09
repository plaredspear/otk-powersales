package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.response.AccountDetailResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListItem
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountLookupFilterOptions
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
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
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val branchCodeExpander: BranchCodeExpander,
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
     * @param myBranchScopePrincipal 비-null 이면 SF 행사마스터(PPTMaster) 거래처 lookup 정합 —
     *              sharing policy(owner/hierarchy) 대신 [myBranchScopePredicate] (지점 화이트리스트 →
     *              `branch_code IN`) 로 가시성을 평가한다. 행사마스터 거래처 lookup 진입점 전용.
     *              SF 레거시(`UplExcelBtnPPTMasterController` → `CurrentUserBranchNameList` →
     *              `Account.BranchCode__c IN (조직트리 지점코드)`)는 owner 가 아닌 지점코드 IN 매칭이라,
     *              sharing policy(owner.user_role_id 계층) 로 평가하면 본인 지점 거래처가 owner 불일치로
     *              전부 누락되는 GAP 을 막는다. resolver 가 principal 의 지점 화이트리스트를 산출하므로
     *              [scope] 가 아닌 principal 이 필요.
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
        myBranchScopePrincipal: WebUserPrincipal? = null,
        accountType: String? = null,
        coordinatesMissing: Boolean = false
    ): AccountListResponse {
        val visibilityPredicate = if (myBranchScopePrincipal != null) {
            myBranchScopePredicate(myBranchScopePrincipal)
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
            accountType = accountType,
            accountStatusName = accountStatusName,
            applyPromotionFilter = applyPromotionFilter,
            excludeClosedAccount = excludeClosedAccount,
            coordinatesMissing = coordinatesMissing,
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
     * SF 행사마스터(PPTMaster) 거래처 lookup 의 지점 가시성 동등 predicate.
     *
     * SF 레거시 경로 (`UplExcelBtnPPTMaster` 엑셀 업로드 → 거래처 인라인 lookup):
     * - `UplExcelBtnPPTMasterController.js` doInit → `CurrentUserBranchNameList.getBranchNames()` 가
     *   사용자 `CostCenterCode__c`(hrCode) 로 `Org__c` 조직 트리를 펼쳐 휘하 지점/영업부 코드 집합 산출
     *   (`OrgNameLevel3 IN ('Retail/제1/CVS사업부')` 화이트리스트 + 전사는 UserRole `%영업지원%`/`영업본부`
     *   기준 제한 사업부). 전사라도 전체가 아니라 특정 사업부 Org 로 제한.
     * - `whereStr = "BranchCode__c IN (<코드집합>) AND AccountGroup__c IN ('1010','1000')"`
     * - `LookupController` `with sharing` 동적 SOQL → Account OWD=Private sharing 교집합.
     *
     * 신규 정합: [WomenScheduleBranchResolver.resolveBranches] 가 SF `CurrentUserBranchNameList` 정합으로
     * 산출한 지점 화이트리스트 (SYSTEM_ADMIN=전체 / 전사=제한 사업부 / 그 외=본인 조직트리) 를
     * [BranchCodeExpander] (SF `Util.getIncludedBranchCode` 정합) 로 이력 코드 확장한 뒤
     * `account.branchCode IN (확장집합)` 단일 필드 매칭. 여사원일정 거래처 조회
     * ([AdminTeamScheduleService.getAccounts]) 와 동일 계열. `AccountGroup__c IN (1000,1010)` 행사필터는
     * [getAccounts] 의 `applyPromotionFilter` 가 별도 적용.
     *
     * 화이트리스트가 비면 매칭 0건 (지점 미보유 사용자).
     */
    private fun myBranchScopePredicate(principal: WebUserPrincipal): Predicate {
        val allowedBranchCodes = womenScheduleBranchResolver.resolveBranches(principal)
            .mapNotNull { it.branchCode }
            .toSet()
        if (allowedBranchCodes.isEmpty()) {
            return Expressions.asBoolean(false).isTrue
        }
        val expanded = branchCodeExpander.expand(allowedBranchCodes)
        return account.branchCode.`in`(expanded)
    }

    /**
     * 행사마스터 거래처 고급 검색 필터 드롭다운 옵션 — 거래처유형/거래상태 distinct 값.
     *
     * 지점 스코프([myBranchScopePredicate]) + 행사 lookup 게이팅(promotionLookupFilter + 폐업 제외)을
     * 적용한 실제 검색 대상 집합의 값만 반환한다 (선택지에 폐업 등 노출 불가 값이 뜨지 않게 함).
     */
    fun getPromotionLookupFilterOptions(principal: WebUserPrincipal): AccountLookupFilterOptions {
        val predicate = myBranchScopePredicate(principal)
        return AccountLookupFilterOptions(
            accountTypes = accountRepository.findDistinctAccountTypes(predicate),
            accountStatusNames = accountRepository.findDistinctAccountStatusNames(predicate),
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
