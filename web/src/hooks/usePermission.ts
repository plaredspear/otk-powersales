import { useAuthStore } from '@/stores/authStore';

export type SfEntityOperation = 'READ' | 'CREATE' | 'EDIT' | 'DELETE';

export type SfSystemPermission =
  | 'VIEW_ALL_DATA'
  | 'MODIFY_ALL_DATA'
  | 'VIEW_ALL_USERS'
  | 'MANAGE_USERS'
  | 'API_ENABLED';

const OPERATION_SHORT_CODE: Record<SfEntityOperation, string> = {
  READ: 'R',
  CREATE: 'C',
  EDIT: 'E',
  DELETE: 'D',
};

export function entityPermissionKey(entity: string, operation: SfEntityOperation): string {
  return `${entity}:${OPERATION_SHORT_CODE[operation]}`;
}

export function systemPermissionKey(permission: SfSystemPermission): string {
  return `SYSTEM:${permission}`;
}

/**
 * SF Profile.Name 의 시스템 관리자 식별자.
 * Backend `SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME` 과 대칭.
 */
export const SYSTEM_ADMIN_PROFILE_NAME = '시스템 관리자';

/**
 * 영업지원2팀 조직코드 (costCenterCode = 4889, org_nm3="영업지원실" / org_nm4="영업지원2팀").
 * Backend `WomenScheduleBranchResolver.ALL_BRANCH_LOOKUP_COST_CENTER_CODE` 와 대칭 —
 * 백엔드에선 행사마스터 거래처 전사 조회 예외에, web 에선 진열스케줄마스터 메뉴/라우트 차단에 사용.
 * 조직 개편으로 코드가 바뀌면 본 상수만 변경.
 */
export const SALES_SUPPORT_TEAM2_COST_CENTER_CODE = '4889';

/**
 * Spec #802 — SF 권한 모델 기반 client 가드 hook.
 *
 * Backend `SfPermissionResolver` 가 평탄화한 string set 을 그대로 검사한다.
 * - entity × operation: `"<entity-table-name>:<R|C|E|D>"` (예: `"employee:R"`)
 * - system permission: `"SYSTEM:<name>"` (예: `"SYSTEM:MANAGE_USERS"`)
 *
 * ## 시스템 관리자 예외
 *
 * `profileName === "시스템 관리자"` 인 사용자는 모든 entity/system permission 검사를
 * 무조건 통과시킨다. Backend `WebAdminContextFilter` 의 API 가드 우회와 대칭 —
 * SF 표준 System Administrator Profile 은 `profile_flags` 가 미적재될 수 있어
 * `SfPermissionResolver` 가 `SYSTEM:MANAGE_USERS` 등 system permission 을 못 주입하는데,
 * 그 경우에도 UI 버튼/메뉴가 정상 노출되도록 한다.
 */
export function usePermission() {
  const permissions = useAuthStore((state) => state.user?.permissions ?? []);
  const isSystemAdmin = useAuthStore(
    (state) => state.user?.profileName === SYSTEM_ADMIN_PROFILE_NAME,
  );

  const hasEntityPermission = (entity: string, operation: SfEntityOperation): boolean =>
    isSystemAdmin || permissions.includes(entityPermissionKey(entity, operation));

  const hasSystemPermission = (permission: SfSystemPermission): boolean =>
    isSystemAdmin || permissions.includes(systemPermissionKey(permission));

  const hasAnyEntityPermission = (entity: string, operations: SfEntityOperation[]): boolean =>
    operations.some((op) => hasEntityPermission(entity, op));

  return { hasEntityPermission, hasSystemPermission, hasAnyEntityPermission };
}
