/**
 * 재직상태 상수 모듈
 *
 * 재직상태 문자열은 화면 계보에 따라 세 종류의 값 도메인이 공존한다. 하나의 색상 맵으로
 * 통합하기 위해 아래 합집합을 단일 출처로 둔다.
 *
 * - backend `EmploymentStatus.code` : 재직 / 휴직 / 퇴직
 * - backend `DismissalPolicy.DISPLAY_STATUS` : 퇴직(면직)
 *   여사원 현황 상세에서 서버가 발령명 '면직' 사원에게 부여하는 파생 표시값. 퇴직과 동일 취급.
 * - 여사원 일정(TeamScheduleMember) API : 퇴사
 *   같은 의미지만 표기가 '퇴직' 이 아닌 '퇴사' 로 내려온다.
 * - SF `ValidConditionData__c` formula : 퇴직예정
 *   진열스케줄마스터 / 전문행사조 등 SF 정합 화면에서만 나타나는 4분류 중 하나.
 */

/** Ant Design Tag 색상 매핑 (3개 값 도메인의 합집합) */
export const EMPLOYMENT_STATUS_COLORS: Record<string, string> = {
  재직: 'green',
  휴직: 'orange',
  퇴직: 'red',
  '퇴직(면직)': 'red',
  퇴사: 'red',
  퇴직예정: 'volcano',
};

/**
 * 「재직상태」 조회 필터 값 (재직 / 휴직 / 퇴직) — backend `EmploymentStatus.code` 3값.
 *
 * 여사원 현황은 같은 3값을 서버 `/female-employees/meta` 로 받지만(단일 출처), 그 endpoint 는
 * `female_employee` 권한 가드라 전문행사조만 볼 수 있는 사용자가 호출할 수 없다. 전문행사조 마스터
 * 화면은 이 상수를 쓴다 — 같은 화면의 전문행사조 유형 필터도 프론트 상수(`pptTeamType`) 를 쓰는 것과 동일 방식.
 * SF `DKRetail__Status__c` picklist 가 늘어나면 여기와 backend `EMPLOYEE_STATUS_OPTIONS` 를 함께 갱신한다.
 *
 * '퇴직(면직)'/'퇴직예정' 은 표시 전용 파생값이라 조회 선택지에 넣지 않는다 — 면직자는 '퇴직' 조회에
 * 포함된다 (backend `DismissalPolicy`).
 */
export const EMPLOYMENT_STATUS_FILTER_VALUES = ['재직', '휴직', '퇴직'] as const;

/** Ant Design Select 용 「재직상태」 필터 옵션. '전체'(미필터) 선택지는 화면에서 앞에 붙인다. */
export const EMPLOYMENT_STATUS_FILTER_OPTIONS = EMPLOYMENT_STATUS_FILTER_VALUES.map((v) => ({
  value: v,
  label: v,
}));

/**
 * 재직상태 값에 매칭되는 Tag 색상을 반환한다.
 * 미정의 값(도메인 외)은 'default'를 반환하여 UI 깨짐을 방지한다.
 */
export function getEmploymentStatusColor(status: string | null | undefined): string {
  if (status == null) return 'default';
  return EMPLOYMENT_STATUS_COLORS[status] ?? 'default';
}
