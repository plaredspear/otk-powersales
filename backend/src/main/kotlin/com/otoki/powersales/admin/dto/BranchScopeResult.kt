package com.otoki.powersales.admin.dto

/**
 * [com.otoki.powersales.admin.service.UnifiedBranchScopeResolver.resolveScope] 결과.
 *
 * [EffectiveBranchResult] 와 달리 판정 통과 원본 코드와 조회 필터용 확장 코드를 **함께** 반환한다 —
 * 확장([com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander])을 리졸버가
 * 보장하므로 소비자가 확장을 누락할 수 없고, 라벨 표기(원본 코드 기준)도 그대로 지원한다.
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

    /**
     * 지점 필터 미적용(전건).
     *
     * 전사 권한자가 지점을 고르지 않았고, 그 화면이 전사 권한자를 34개 화이트리스트로 **제한하지 않는**
     * 경우([com.otoki.powersales.admin.service.BranchScopeProfile.restrictsAllBranches] = false) 에만 나온다.
     * 거래처 조회·매출진도율마스터처럼 전사 권한자가 종전부터 전건을 보던 화면의 범위를 좁히지 않기 위한 상태.
     */
    data object Unrestricted : BranchScopeResult

    /** 조회 차단 — 권한 화이트리스트 밖 코드가 섞였거나, 권한 지점이 없는 사용자. */
    data object NoAccess : BranchScopeResult

    /**
     * 목록 쿼리의 지점 필터 파라미터로 바로 쓸 수 있는 형태.
     * `null` = 필터 미적용(전건), 빈 목록 = 매칭 0건(차단), 그 외 = `IN` 대상 확장 코드.
     */
    fun queryCodesOrNull(): List<String>? = when (this) {
        is Allowed -> queryCodes
        is Unrestricted -> null
        is NoAccess -> emptyList()
    }

    /** 기존 [EffectiveBranchResult] 소비자(보고서 계열 서비스)를 위한 변환. */
    fun toEffectiveBranchResult(): EffectiveBranchResult = when (this) {
        is Allowed -> EffectiveBranchResult.Filtered(queryCodes)
        is Unrestricted -> EffectiveBranchResult.All
        is NoAccess -> EffectiveBranchResult.NoAccess
    }
}
