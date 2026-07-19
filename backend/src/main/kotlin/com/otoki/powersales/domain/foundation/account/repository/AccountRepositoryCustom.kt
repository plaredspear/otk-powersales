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
     * Service layer 가 [com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator.buildPredicate]
     * 결과를 [policyPredicate] 로 전달. Repository 는 Service 의존을 갖지 않음.
     *
     * @param policyPredicate SharingRulePolicyEvaluator 가 합성한 sharing policy Predicate
     *                        (우선순위 1-6 평가: viewAllData / Owner / Hierarchy / SharingRule / branchCodes / ControlledByParent)
     * @param keyword 거래처코드 (externalKey) / 거래처명 (name) 부분 일치 (lowercase 매칭)
     * @param abcType ABC 유형 정확 일치
     * @param accountType 거래처유형 정확 일치
     * @param accountStatusName 상태 정확 일치
     * @param applyPromotionFilter `DKRetail__Promotion__c.AccId__c.lookupFilter` (accountGroup ∈ {1000,1010}
     *                        + 폐업/distribution 조건) 적용 여부. SF 에서 이 조건은 Promotion 거래처 선택
     *                        Lookup 필드에만 존재하고 메인 거래처 탭 listView(AllAccounts=Everything)에는
     *                        미적용 — 따라서 lookup 진입점은 true, 메인 목록은 false.
     * @param excludeClosedAccount 폐업(`account_status_name = '폐업'`) 거래처를 distribution/abcType 면제
     *                        없이 완전 제외할지 여부. 진열사원스케줄 마스터 등록 거래처 lookup 전용 —
     *                        폐업 거래처는 등록 자체가 차단되므로 조회 후보에서도 일관되게 제외한다.
     *                        [applyPromotionFilter] 의 distribution 면제 노출보다 우선 (AND 합성).
     * @param coordinatesMissing true 면 Naver Geocode batch(#637) 진입 후보와 **동일 조건**을 AND 합성
     *                        ((latitude IS NULL OR longitude IS NULL) AND address1 IS NOT NULL AND
     *                        external_key IS NOT NULL AND account_status_name = '거래'). 거래처 화면에서
     *                        "좌표 미수신" 필터로 배치가 매번 스캔·재시도하는 거래처를 운영자가 직접 조회하기 위함.
     *                        [findCoordinatesMissingAccounts] 와 동일한 [coordinatesMissingPredicate] 를 재사용한다.
     * @param pageable 페이지네이션 + 정렬 (count query 정합 자동 처리)
     */
    fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountType: String?,
        accountStatusName: String?,
        applyPromotionFilter: Boolean,
        excludeClosedAccount: Boolean,
        coordinatesMissing: Boolean,
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

    /**
     * 활성(미삭제) 거래처의 유통형태 구성요소(거래처상태코드, 거래처타입) distinct 조합.
     * 라벨 조합 규칙은 [Account.distributionChannelLabel] companion 이 정본 — 호출 측에서 조합.
     */
    fun findDistinctDistributionChannelParts(): List<AccountLabelPartsRow>

    /**
     * 활성(미삭제) 거래처의 거래처유형 구성요소(ABC유형코드, ABC유형) distinct 조합.
     * 라벨 조합 규칙은 [Account.abcTypeLabel] companion 이 정본 — 호출 측에서 조합.
     */
    fun findDistinctAbcTypeParts(): List<AccountLabelPartsRow>

    /**
     * 활성(미삭제) 거래처의 (유통형태, 거래처유형) 동시출현(co-occurrence) distinct 4-튜플.
     *
     * 유통형태(거래처상태코드 + 거래처타입)와 거래처유형(ABC유형코드 + ABC유형)이 같은 Account row 에
     * 공존하므로, 4컬럼 distinct 로 "유통형태별로 어떤 거래처유형이 붙는지" 종속 매핑을 데이터에서 도출한다.
     * [findDistinctDistributionChannelParts] / [findDistinctAbcTypeParts] 는 축별 독립 distinct 라 교차
     * 정보가 소실되어 종속 매핑에 쓸 수 없으므로 본 메서드가 별도로 필요하다.
     * 라벨 조합은 호출 측에서 [Account.distributionChannelLabel] / [Account.abcTypeLabel] 로 수행.
     */
    fun findDistinctDistributionAbcPairs(): List<AccountDistributionAbcPairRow>

    /**
     * 행사마스터 거래처 고급 검색 필터 드롭다운용 distinct 값 조회.
     *
     * [predicate] 로 행사 lookup 게이팅(promotionLookupFilter + 폐업 제외 + 지점 스코프)을 전달해
     * 실제 검색 대상이 되는 거래처 집합의 값만 반환한다 — 선택지에 폐업 등 노출 불가 값이 뜨지 않게 한다.
     */
    fun findDistinctAccountTypes(predicate: Predicate): List<String>

    /** [findDistinctAccountTypes] 와 동일 게이팅으로 거래상태(accountStatusName) distinct 값 조회. */
    fun findDistinctAccountStatusNames(predicate: Predicate): List<String>

    /**
     * 외부 조회(RDP inbound) 전량 스냅샷 — PK keyset 페이지 1건 분량.
     *
     * MFEIS 스냅샷과 달리 **entity 전 컬럼**을 노출하므로 projection 축소 없이 entity 를 그대로 반환한다.
     *
     * 관계 FK(ownerUser/createdBy/lastModifiedBy/parent)는 **entity 가 아니라 [AccountSnapshotRow] 의
     * 별도 필드로 함께 select** 한다. `entity.parent?.id` 로 읽어도 (식별자 접근은 프록시를 초기화하지
     * 않으므로) 추가 쿼리는 없지만, FK 를 쿼리에서 직접 가져오면 관계 필드에 의존하지 않아 그 성질이
     * 깨질 여지 자체가 없다. 회귀 감시는 `RdpSnapshotKeysetRepositoryTest` 의 **쿼리 1회** 단언이 담당한다.
     *
     * @param cursor 직전 페이지 마지막 id. null 이면 처음부터
     * @param limit  조회 상한 (hasNext 판정을 위해 호출 측이 pageSize + 1 을 넘긴다)
     */
    fun findSnapshotByKeyset(cursor: Long?, limit: Int): List<AccountSnapshotRow>
}

/**
 * 거래처 전량 스냅샷 1건 — entity + 관계 FK id.
 *
 * 관계 FK 를 entity 의 LAZY 필드에 의존하지 않고 쿼리에서 함께 뽑아 오기 위한 묶음
 * (근거는 [AccountRepositoryCustom.findSnapshotByKeyset] KDoc).
 */
data class AccountSnapshotRow(
    val account: Account,
    val ownerUserId: Long?,
    val createdById: Long?,
    val lastModifiedById: Long?,
    val parentId: Long?,
)

/** 거래처 라벨 구성요소 distinct 1건 — (코드, 명칭) 쌍. */
data class AccountLabelPartsRow(
    val code: String?,
    val name: String?,
)

/** 유통형태 ↔ 거래처유형 동시출현 distinct 1건 — 유통형태(코드/명칭) + 거래처유형(코드/명칭). */
data class AccountDistributionAbcPairRow(
    val accountStatusCode: String?,
    val accountType: String?,
    val abcTypeCode: String?,
    val abcType: String?,
)
