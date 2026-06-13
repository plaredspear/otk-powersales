package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface AccountRepositoryCustom {

    /**
     * SF Sharing Rule 정책 + 검색 필터 + 페이지네이션을 합성한 가시 Account 일람 (spec #787).
     *
     * 본 spec 의 단일화 정책 — `searchForAdmin` (legacy branchCodes 필터) + 1차
     * `findAllAccessibleByPolicy(Predicate): List` (sharing policy 만) 의 2 메서드를 본 메서드로 통합.
     *
     * Service layer 가 [com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator.buildPredicate]
     * 결과를 [policyPredicate] 로 전달. Repository 는 Service 의존을 갖지 않음.
     *
     * @param policyPredicate SharingRulePolicyEvaluator 가 합성한 sharing policy Predicate
     *                        (우선순위 1-6 평가: viewAllData / Owner / Hierarchy / SharingRule / branchCodes / ControlledByParent)
     * @param keyword 거래처코드 (externalKey) / 거래처명 (name) 부분 일치 (lowercase 매칭)
     * @param abcType ABC 유형 정확 일치
     * @param accountStatusName 상태 정확 일치
     * @param applyPromotionFilter `DKRetail__Promotion__c.AccId__c.lookupFilter` (accountGroup ∈ {1000,1010}
     *                        + 폐업/distribution 조건) 적용 여부. SF 에서 이 조건은 Promotion 거래처 선택
     *                        Lookup 필드에만 존재하고 메인 거래처 탭 listView(AllAccounts=Everything)에는
     *                        미적용 — 따라서 lookup 진입점은 true, 메인 목록은 false.
     * @param pageable 페이지네이션 + 정렬 (count query 정합 자동 처리)
     */
    fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountStatusName: String?,
        applyPromotionFilter: Boolean,
        pageable: Pageable,
    ): Page<Account>

    /**
     * SF Sharing Rule 정책 적용 단건 거래처 상세 조회.
     *
     * [findAllAccessibleByPolicy] 의 단건 버전 — sharing policy Predicate + soft-delete 제외 +
     * `account.id = [id]` 합성으로 조회한다. 가시 범위 밖 거래처를 요청하면 매칭 0건으로 `null` 반환
     * (SF sharing rule 의 "권한 없는 레코드는 존재하지 않음" 동등 — 호출 측에서 404 변환).
     *
     * @param policyPredicate SharingRulePolicyEvaluator 가 합성한 sharing policy Predicate
     * @param id 조회 대상 Account.id
     */
    fun findAccessibleByPolicyAndId(policyPredicate: Predicate, id: Long): Account?

    /**
     * 좌표 미수신 거래처 조회 — Naver Geocode batch (#637) 진입 조건.
     *
     * 조건:
     * - latitude IS NULL OR longitude IS NULL
     * - address1 IS NOT NULL
     * - external_key IS NOT NULL
     * - account_status_name = '거래'
     * - LIMIT [limit]
     *
     * 레거시 SOQL (`Batch_AccountLatLong.cls#start`) 동등.
     */
    fun findCoordinatesMissingAccounts(limit: Int): List<Account>

    /**
     * 동일 [name] + 활성(미삭제) 거래처 존재 여부.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     */
    fun existsActiveByName(name: String): Boolean

    /**
     * 활성(미삭제) 거래처 단건 조회.
     *
     * `is_deleted` 가 nullable Boolean 이므로 `IS NULL` 과 `= false` 두 케이스 모두 활성으로 간주.
     */
    fun findActiveById(id: Long): Account?

    /**
     * 동일 [name] + 활성(미삭제) + 자기 자신 ([id]) 제외 거래처 존재 여부.
     *
     * UPDATE 흐름에서 자기 자신은 중복 검증에서 제외 (`Trigger.oldMap` 비교 동등 효과).
     */
    fun existsActiveByNameAndIdNot(name: String, id: Long): Boolean

    /**
     * SF `UplExcelBtnSchduleMasterController.checkResult` (L174) 정합 —
     * `Account WHERE BranchCode__c IN :newOrgValues AND ExternalKey__c IN :accCodes`.
     * BranchCodeExpander 확장 결과로 조장 지점 (이력 합집합) 필터 + 외부키 필터 동시 적용.
     */
    fun findByBranchCodeInAndExternalKeyIn(
        branchCodes: Collection<String>,
        externalKeys: Collection<String>
    ): List<Account>
}
