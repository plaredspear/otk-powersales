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
 * 재직상태 값에 매칭되는 Tag 색상을 반환한다.
 * 미정의 값(도메인 외)은 'default'를 반환하여 UI 깨짐을 방지한다.
 */
export function getEmploymentStatusColor(status: string | null | undefined): string {
  if (status == null) return 'default';
  return EMPLOYMENT_STATUS_COLORS[status] ?? 'default';
}
