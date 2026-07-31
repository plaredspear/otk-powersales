package com.otoki.powersales.admin.tools.branchscope

/**
 * 투입현황 대시보드의 지점 스코프 산출 방식 — 개발자 도구 > 대시보드 > 지점 스코프 방식 토글.
 *
 * 통합 리졸버([com.otoki.powersales.admin.service.UnifiedBranchScopeResolver]) 전환의 영향을 운영에서
 * 직접 비교하기 위한 **한시적** 스위치다. 비교/검증이 끝나면 [UNIFIED] 로 고정하고 [LEGACY] 경로와
 * 본 토글을 함께 제거한다.
 *
 * 두 모드의 셀렉터 목록은 동일하다 (전사 34개 / 비전사 조직 트리) — 달라지는 것은 **판정과 확장**뿐:
 * - [UNIFIED]: 요청 ⊆ 셀렉터 목록 판정 → 통과 코드 확장. 셀렉터에 보이는 지점은 항상 조회된다.
 * - [LEGACY]: 요청 ⊆ `DataScope.branchCodes`(비전사 = 본인 코드 1건) 판정 → 통과 코드 확장.
 *   상위 조직 사용자는 셀렉터에 보이는 하위 지점을 골라도 매칭 0건이 된다.
 */
enum class BranchScopeMode {
    /** 통합 리졸버 (기본값) — 셀렉터 = 판정 화이트리스트. */
    UNIFIED,

    /** 전환 이전 동작 — `DashboardBranchResolver` + `DataScope` 판정. */
    LEGACY,
    ;

    companion object {
        val DEFAULT = UNIFIED

        fun fromNameOrNull(value: String?): BranchScopeMode? =
            entries.find { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}
