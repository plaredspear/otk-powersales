package com.otoki.powersales.admin.service

/**
 * 지점 스코프 화면 계열 — 통합 리졸버([UnifiedBranchScopeResolver]) 가 화면군마다 달랐던 두 가지 축을
 * 구분하기 위한 값이다. 통합 이전 각 화면이 쓰던 리졸버(= [BranchScopeGateway] 의 LEGACY 재현 대상) 와
 * 1:1 로 대응한다.
 *
 * ## 통합해도 화면군마다 유지해야 하는 축
 * 1. **전사 권한자 범위** ([restrictsAllBranches]) — 34개 화이트리스트로 제한할지, 종전처럼 전건을 볼지.
 *    이 축을 일괄로 맞추면 종전 전건 화면(거래처 조회·매출진도율마스터)에서 34개 밖 지점의 데이터가
 *    통째로 사라진다(= 조회 누락). 그래서 화면의 현행 정책을 그대로 보존한다.
 * 2. **전사 권한자 셀렉터 목록** ([allBranchesSelectorWhitelisted]) — 34개 고정인지, 조직 전건 목록인지.
 *
 * ## 통합으로 모든 계열이 공유하게 되는 규칙
 * - **비전사 사용자**: 셀렉터 = 판정 = 본인 costCenterCode 의 **조직 트리**
 *   ([com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver]).
 *   종전에는 셀렉터만 트리이고 판정은 본인 코드 1건(DataScope / ReportBranchScopeService) 이라,
 *   상위 조직(영업부·팀) 계정이 셀렉터에 보이는 하위 지점을 고르면 0건이 되는 불일치가 있었다.
 * - **판정 후 확장**: 판정을 통과한 원본 코드만 `BranchMapping` 으로 넓혀 조회 필터로 쓴다.
 */
enum class BranchScopeProfile(
    /** 전사 권한자를 34개 화이트리스트로 제한하는 화면인지(미선택 조회 범위 포함). */
    internal val restrictsAllBranches: Boolean,
    /** 전사 권한자 셀렉터가 34개 고정인지(false 면 조직 전건 목록). */
    internal val allBranchesSelectorWhitelisted: Boolean,
) {
    /**
     * 투입현황 대시보드 — 지점 **다중 선택**. 전사 34개 제한(운영 요구).
     * LEGACY: `DashboardBranchResolver.effectiveBranchCodes` + `DataScope`.
     */
    DASHBOARD(restrictsAllBranches = true, allBranchesSelectorWhitelisted = true),

    /**
     * 마스터 목록 계열 — 진열스케줄마스터·행사마스터·진열사원 배치 적합성. 전사 34개 제한.
     * LEGACY: `WhitelistBranchScopeResolver`.
     */
    MASTER_LIST(restrictsAllBranches = true, allBranchesSelectorWhitelisted = true),

    /**
     * 보고서 계열 — 기간별 클레임·물류클레임·여사원 배치점검·행사 목표대비실적.
     * LEGACY: `ReportBranchScopeService` (셀렉터는 34개인데 미선택 조회는 전건이라 두 축이 어긋나 있었다).
     * 통합 후에는 셀렉터와 같은 34개로 조회 범위도 맞춘다.
     */
    REPORT(restrictsAllBranches = true, allBranchesSelectorWhitelisted = true),

    /**
     * 매출/실적 계열 — 전산실적·POS매출·월매출(물류배부)·월별 투입적합성.
     * 셀렉터는 34개지만 지점 선택이 **필수**라 미선택 전건 케이스가 없다(제한 여부 무의미 → false 유지).
     * LEGACY: 셀렉터 `DashboardBranchResolver` + 조회 `DataScope` 교집합(확장 없음).
     */
    SALES(restrictsAllBranches = false, allBranchesSelectorWhitelisted = true),

    /**
     * 조직 전건 계열 — 거래처 조회·매출진도율마스터.
     * 전사 권한자는 셀렉터·조회 모두 종전대로 전건(34개로 좁히면 34개 밖 거래처가 사라진다).
     * LEGACY: 셀렉터 `WomenScheduleBranchResolver` + 조회 `DataScope`(선택값만 확장).
     */
    ORG_WIDE(restrictsAllBranches = false, allBranchesSelectorWhitelisted = false),
}
