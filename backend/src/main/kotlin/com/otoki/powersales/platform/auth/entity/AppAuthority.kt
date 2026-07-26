package com.otoki.powersales.platform.auth.entity

/**
 * SF `DKRetail__Employee__c.DKRetail__AppAuthority__c` picklist value 상수.
 *
 * SF 원본 4종 picklist 정의 정합 (운영 실측 확인 — `_raw/DKRetail__Employee__c.json:1636-1664`).
 * `restrictedPicklist = false` 라 운영 UI 에서 신규 값이 추가될 수 있으나 현 시점 0건.
 * 신규 값 추가 시 본 파일 갱신.
 *
 * Employee.role 컬럼은 String 으로 본 picklist value 를 그대로 저장.
 * 6종 운영 분기 (시스템 관리자 / 영업지원실 / 영업부장 / 사업부장 / 본부장 / 영업사원) 는
 * SF AppAuthority 가 아니라 Profile.Name 으로 분기 — `WebUserPrincipal.profileName` 사용.
 */
object AppAuthority {
    const val WOMAN = "여사원"
    const val LEADER = "조장"
    const val BRANCH_MANAGER = "지점장"
    const val ACCOUNT_VIEW_ALL = "AccountViewAll"

    /**
     * 팀(여사원) 관리 권한 — 조장 + 지점장.
     *
     * 레거시 권한 분기는 전부 `eq '조장'` 정확 일치였으나, **지점장도 조장과 동일하게
     * 팀을 관리하도록 의도적으로 확장한 지점**이다 (레거시 이탈). 여사원 관리 API
     * ([com.otoki.powersales.domain.activity.schedule.service.LeaderScheduleService]) 와
     * 지점 단위 거래처 조회([com.otoki.powersales.platform.common.service.MyAccountService]) 가
     * 본 판정을 공유한다.
     *
     * **출근 / 안전점검 축에는 쓰지 않는다.** 지점장은 출근·근태 대상이 아니므로
     * `HomeService.attendanceApplicable` (= WOMAN|LEADER) 및 조장 팀 출근집계·팀 스케줄
     * 조회 분기는 [LEADER] 정확 일치를 유지한다. 팀장 1명을 특정하는 로직
     * (`AttendanceService.findTeamLeader`, `TeamMemberScheduleOwnerResolver`) 도 동일.
     */
    fun isTeamManager(role: String?): Boolean = role == LEADER || role == BRANCH_MANAGER
}
