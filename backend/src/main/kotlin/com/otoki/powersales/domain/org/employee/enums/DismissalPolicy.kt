package com.otoki.powersales.domain.org.employee.enums

/**
 * 발령명(`ord_detail_node`) "면직" 을 퇴직과 동일하게 취급하는 정책 — 여사원 현황 화면 전용.
 *
 * ## 배경
 * 면직은 SAP 발령상 고용 종료지만, 운영 데이터에는 면직 발령을 받고도
 * [com.otoki.powersales.domain.org.employee.entity.Employee.status] 가 아직 '재직' 으로 남은 사원이 있다
 * (SAP 인사 마스터의 상태 갱신이 발령보다 늦거나 누락된 경우). 그 결과 여사원 현황에서
 * 퇴직으로 조회하면 면직자가 빠지고, 재직으로 조회하면 이미 나간 사원이 섞여 나온다.
 *
 * 이를 원본 데이터 수정 없이 **조회/표시 시점에** 보정한다 (2026-07-29 사용자 결정):
 * - 조회: 퇴직 필터에 면직 포함, 재직·휴직 필터에서 면직 제외
 *   ([com.otoki.powersales.domain.org.employee.repository.EmployeeRepositoryCustom.findEmployees]
 *   의 `treatDismissalAsResigned`)
 * - 표시: 상태 컬럼을 [DISPLAY_STATUS] 로 노출 — 퇴직 처리이되 사유가 면직임을 함께 드러낸다
 *   (원본 `status` 를 '퇴직' 으로 덮어쓴 것처럼 보이지 않도록 괄호 표기).
 *
 * ## 적용 범위
 * - 여사원 현황 목록/엑셀 ([com.otoki.powersales.admin.controller.AdminFemaleEmployeeController]):
 *   조회 필터 + 상태 표시
 * - 투입현황 대시보드 기본현황 모수
 *   ([com.otoki.powersales.domain.org.employee.repository.EmployeeRepositoryCustom.findDashboardBasicStatsProjection]):
 *   면직자를 모수에서 제외 — 두 화면의 재직 인원이 어긋나지 않도록 함께 적용한다
 *   ([FemaleStaffHeadcountFilter] 참조).
 * - 진열스케줄마스터 「재직상태」 조회 필터
 *   ([com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepositoryCustomImpl]
 *   의 `buildEmploymentStatusCondition`): 여사원 현황과 동일하게 퇴직 조회에 면직 포함,
 *   재직·휴직 조회에서 면직 제외. **필터만** 적용하며 「재직상태」 컬럼 표시값은 SF formula
 *   `ValidConditionData__c` 정합을 위해 원본 계산값(재직/휴직/퇴직·퇴직예정+날짜)을 유지한다.
 *
 * 전체 사원 관리·lookup 은 적용 대상이 아니다 — SAP 원본 상태를 그대로 노출한다.
 */
object DismissalPolicy {

    /** 면직 발령명 — [com.otoki.powersales.domain.org.employee.entity.Employee.ordDetailNode] 원본 값. */
    const val ORD_DETAIL_NODE = "면직"

    /** 면직자의 상태 표시값 — 퇴직으로 취급하되 사유(면직)를 괄호로 병기. */
    const val DISPLAY_STATUS = "퇴직(면직)"

    /** 발령명이 면직인지 여부. */
    fun isDismissed(ordDetailNode: String?): Boolean = ordDetailNode == ORD_DETAIL_NODE

    /**
     * 상태 표시값 — 면직이면 [DISPLAY_STATUS], 아니면 원본 상태 그대로.
     * 원본이 이미 '퇴직' 인 면직자도 [DISPLAY_STATUS] 로 통일해 화면상 판정 기준을 하나로 유지한다.
     */
    fun displayStatus(status: String?, ordDetailNode: String?): String? =
        if (isDismissed(ordDetailNode)) DISPLAY_STATUS else status
}
