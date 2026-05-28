import type { SfEntityOperation, SfSystemPermission } from '@/hooks/usePermission';

/**
 * 권한 라벨 단일 source.
 *
 * - `ENTITY_LABELS`: entity table name → 한글 라벨
 * - `OPERATION_LABELS`: SfEntityOperation → 한글 라벨
 * - `SYSTEM_PERMISSION_LABELS`: SfSystemPermission → 한글 라벨
 *
 * `formatEntityLabel`, `formatOperationLabel`, `formatSystemPermissionLabel` 은 매핑 미존재
 * 시 raw key 를 fallback 으로 반환하므로, 누락된 항목이 있어도 즉시 깨지지 않는다.
 *
 * menuConfig.tsx 가 참조하는 모든 entity / systemPermission 을 빠짐없이 포함하는 것을
 * 원칙으로 한다. 신규 메뉴 추가 시 본 파일에도 동시 등록 권장.
 */

export const ENTITY_LABELS: Record<string, string> = {
  account: '거래처',
  agreement_word: '동의 약관',
  attend_info: '근무기간',
  attendance_log: '근무 등록',
  claim: '클레임',
  employee: '사원',
  employee_input_criteria_master: '진열사원 투입기준',
  monthly_sales_history: '월매출',
  notice_post: '공지사항',
  permission_set: '권한 세트',
  product: '제품',
  profile: '프로파일',
  promotion: '행사',
  suggestion: '물류 클레임',
  team_member_schedule: '여사원 일정',
  upload_file: '업로드 파일',
  user: '사용자',
  user_role: '역할 (조직 계층)',
};

export const OPERATION_LABELS: Record<SfEntityOperation, string> = {
  READ: '조회',
  CREATE: '생성',
  EDIT: '수정',
  DELETE: '삭제',
};

export const SYSTEM_PERMISSION_LABELS: Record<SfSystemPermission, string> = {
  VIEW_ALL_DATA: '전체 데이터 조회',
  MODIFY_ALL_DATA: '전체 데이터 수정',
  VIEW_ALL_USERS: '전체 사용자 조회',
  MANAGE_USERS: '사용자 관리',
  API_ENABLED: 'API 활성',
};

/** 매핑 부재 시 raw entity key 그대로 반환. */
export function formatEntityLabel(entity: string): string {
  return ENTITY_LABELS[entity] ?? entity;
}

export function formatOperationLabel(operation: SfEntityOperation): string {
  return OPERATION_LABELS[operation] ?? operation;
}

export function formatSystemPermissionLabel(permission: SfSystemPermission): string {
  return SYSTEM_PERMISSION_LABELS[permission] ?? permission;
}
