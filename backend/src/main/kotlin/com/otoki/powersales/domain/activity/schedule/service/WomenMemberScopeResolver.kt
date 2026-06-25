package com.otoki.powersales.domain.activity.schedule.service

/**
 * 여사원 목록 조회의 cost center 스코프 결정 — SF `TeamMemberListController.fetchTeamMembers` 정합.
 *
 * 여사원 일정관리(AdminTeamScheduleService)와 근무기간 조회(AdminAttendInfoService)가 동일한
 * 권한 스코프 분기를 공유하기 위한 단일 출처. branchCode 무관, 권한(사번/소속) 기준 일괄 조회:
 * - 특수 사번 4명 → 인천 cost center 광역 매핑 6개
 * - 그 외 → 본인 cost_center_code 단일
 */
object WomenMemberScopeResolver {
    /** SF `TeamMemberListController.fetchTeamMembers` 특수 사번 하드코딩 분기 — 인천 cost center 광역 매핑 */
    private val SF_SPECIAL_EMPLOYEE_CODES = setOf("19951029", "20001013", "20060052", "20050308")

    /** SF `TeamMemberListController.fetchTeamMembers:18` 특수 사번 4명 매핑 cost center 6개 */
    private val SF_SPECIAL_EMPLOYEE_COST_CENTERS = listOf("3233", "3234", "3235", "3236", "5691", "5694")

    /**
     * 권한 스코프 cost center 목록을 결정한다.
     *
     * @return 조회 대상 cost center 목록. costCenterCode 가 비어 있고 특수 사번도 아니면 빈 목록
     *         (= 조회 대상 없음 → 호출부에서 빈 결과 반환).
     */
    fun resolveCostCenterCodes(employeeCode: String?, costCenterCode: String?): List<String> = when {
        employeeCode in SF_SPECIAL_EMPLOYEE_CODES -> SF_SPECIAL_EMPLOYEE_COST_CENTERS
        costCenterCode.isNullOrBlank() -> emptyList()
        else -> listOf(costCenterCode)
    }
}
