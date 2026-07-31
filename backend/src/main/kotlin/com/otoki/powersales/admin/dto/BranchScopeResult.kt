package com.otoki.powersales.admin.dto

/**
 * [com.otoki.powersales.admin.service.UnifiedBranchScopeResolver.resolveScope] 결과.
 *
 * [EffectiveBranchResult] 와 달리 판정 통과 원본 코드와 조회 필터용 확장 코드를 **함께** 반환한다 —
 * 확장([com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander])을 리졸버가
 * 보장하므로 소비자가 확장을 누락할 수 없고, 라벨 표기(원본 코드 기준)도 그대로 지원한다.
 *
 * `All`(전건) variant 는 두지 않는다 — 통합 리졸버 정책에서 전사 권한자도 화이트리스트(34개)로
 * 제한되므로 "필터 없음" 상태가 존재하지 않는다. (전건 조회가 필요한 보고서 계열은 아직
 * [EffectiveBranchResult] 기반 — 통합 여부는 별도 논의.)
 */
sealed interface BranchScopeResult {

    /**
     * 조회 허용.
     *
     * @param grantedCodes 판정을 통과한 **확장 전 원본** 지점 코드 — 화면 라벨("OO 외 N개") 등 표기용.
     *   미선택 시 화이트리스트 전체, 선택 시 그 코드들.
     * @param queryCodes [grantedCodes] 의 BranchMapping 확장 집합 — 실제 `IN` 조회 필터용.
     *   조직 개편 전 이력 코드로 적재된 행까지 매칭한다.
     */
    data class Allowed(
        val grantedCodes: List<String>,
        val queryCodes: List<String>,
    ) : BranchScopeResult

    /** 조회 차단 — 권한 화이트리스트 밖 코드가 섞였거나, 권한 지점이 없는 사용자. */
    data object NoAccess : BranchScopeResult
}
