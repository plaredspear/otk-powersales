package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * AccountViewAll 대리출근 화면의 "지점 선택" 옵션 결정 — 대리출근 전용.
 *
 * AccountViewAll(전체 거래처 조회 권한)은 특정 지점에 매이지 않는 전사 성격이라,
 * 대리출근 대상 여사원을 고르기 전에 **지점을 먼저 선택**해야 한다. 이때 노출할 지점 목록은
 * 대시보드 지점 드롭다운과 동일한 [DashboardBranchResolver.DASHBOARD_ALL_BRANCHES] 34개
 * (Retail사업부 32개 지점 + 영업지원2팀 + CVS전략팀) 를 그대로 사용한다.
 *
 * 대시보드 리졸버([DashboardBranchResolver])를 직접 위임하지 않고 목록 상수만 참조하는 이유:
 * - 대시보드 리졸버는 `WebUserPrincipal.profileName` 기반 전사 판정이라 AccountViewAll(Profile 상
 *   "5.영업사원")을 전 지점으로 인정하지 않는다. 여기서는 **role = AccountViewAll 자체가 전사 조건**
 *   이므로 principal profile 과 무관하게 34개를 노출한다.
 * - 대시보드는 웹(`WebUserPrincipal`) 전용이나 대리출근은 모바일(`UserPrincipal`)이라 principal 축이 다르다.
 *
 * [assertBranchAllowed] 로 임의 branchCode 조회(IDOR)를 차단한다.
 */
@Component
class ProxyAttendanceBranchResolver {

    /** 대리출근 지점 선택 옵션 — 대시보드 34개 화이트리스트 그대로. */
    fun resolveBranches(): List<BranchResponse> = DashboardBranchResolver.DASHBOARD_ALL_BRANCHES

    /** 요청 branchCode 가 대리출근 허용 지점(34개)에 포함되는지 여부. 미포함이면 false. */
    fun isBranchAllowed(branchCode: String): Boolean =
        branchCode in DashboardBranchResolver.WHITELIST_CODES
}
