/**
 * UserRole 정의 — 백엔드 enum.name 과 동일.
 *
 * Spec #573: 백엔드 `appAuthority`(한글) 컬럼이 `role`(enum.name 영문) 으로 변경되면서
 * 클라이언트도 enum.name 기반으로 통일한다. 화면 표시 시에는 `ROLE_LABELS` 매핑 또는
 * 백엔드 응답의 `role_label` 필드를 사용한다.
 */
export type UserRole =
  | 'WOMAN'
  | 'LEADER'
  | 'BRANCH_MANAGER'
  | 'SALES_MANAGER'
  | 'BUSINESS_MANAGER'
  | 'HEADQUARTERS_MANAGER'
  | 'SALES_SUPPORT'
  | 'SYSTEM_ADMIN'
  | 'UNKNOWN';

/** enum.name → 한글 표시명 */
export const ROLE_LABELS: Record<UserRole, string> = {
  WOMAN: '여사원',
  LEADER: '조장',
  BRANCH_MANAGER: '지점장',
  SALES_MANAGER: '영업부장',
  BUSINESS_MANAGER: '사업부장',
  HEADQUARTERS_MANAGER: '영업본부장',
  SALES_SUPPORT: '영업지원실',
  SYSTEM_ADMIN: '시스템관리자',
  UNKNOWN: '(미인지)',
};

export interface RoleOption {
  value: UserRole;
  label: string;
}

/** Web Admin 로그인 허용 7개 역할 (WOMAN 제외) */
export const ROLE_OPTIONS_FOR_ADMIN_LOGIN: RoleOption[] = [
  { value: 'LEADER', label: ROLE_LABELS.LEADER },
  { value: 'BRANCH_MANAGER', label: ROLE_LABELS.BRANCH_MANAGER },
  { value: 'SALES_MANAGER', label: ROLE_LABELS.SALES_MANAGER },
  { value: 'BUSINESS_MANAGER', label: ROLE_LABELS.BUSINESS_MANAGER },
  { value: 'HEADQUARTERS_MANAGER', label: ROLE_LABELS.HEADQUARTERS_MANAGER },
  { value: 'SALES_SUPPORT', label: ROLE_LABELS.SALES_SUPPORT },
  { value: 'SYSTEM_ADMIN', label: ROLE_LABELS.SYSTEM_ADMIN },
];

/** 사원 검색 필터 dropdown 노출 옵션 */
export const ROLE_OPTIONS_FOR_FILTER: RoleOption[] = [
  { value: 'WOMAN', label: ROLE_LABELS.WOMAN },
  ...ROLE_OPTIONS_FOR_ADMIN_LOGIN,
];

/** enum.name → 한글 표시명 변환 헬퍼 (fallback: enum.name) */
export function roleLabel(role: string | null | undefined): string {
  if (!role) return '';
  return ROLE_LABELS[role as UserRole] ?? role;
}
