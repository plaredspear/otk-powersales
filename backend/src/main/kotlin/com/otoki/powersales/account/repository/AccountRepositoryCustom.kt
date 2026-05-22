package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
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
     * @param pageable 페이지네이션 + 정렬 (count query 정합 자동 처리)
     */
    fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountStatusName: String?,
        pageable: Pageable,
    ): Page<Account>

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
    fun findActiveById(id: Int): Account?

    /**
     * 동일 [name] + 활성(미삭제) + 자기 자신 ([id]) 제외 거래처 존재 여부.
     *
     * UPDATE 흐름에서 자기 자신은 중복 검증에서 제외 (`Trigger.oldMap` 비교 동등 효과).
     */
    fun existsActiveByNameAndIdNot(name: String, id: Int): Boolean
}
