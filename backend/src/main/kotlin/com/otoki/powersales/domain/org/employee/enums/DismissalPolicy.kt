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
 * 여사원 현황 목록/엑셀([com.otoki.powersales.admin.controller.AdminFemaleEmployeeController]) 에만
 * 적용한다. 전체 사원 관리·lookup 은 SAP 원본 상태를 그대로 보여주고, 대시보드 기본현황 모수
 * ([FemaleStaffHeadcountFilter] — `status <> 퇴직`) 도 손대지 않는다. 즉 면직자가 재직으로 남아 있으면
 * 대시보드 인원수에는 계속 계상되며, 이는 사용자가 인지한 상태의 의도적 범위 제한이다.
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
