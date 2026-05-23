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
 * Spec #802 — SF 권한 모델 기반 client 가드 hook.
 *
 * Backend `SfPermissionResolver` 가 평탄화한 string set 을 그대로 검사한다.
 * - entity × operation: `"<entity-table-name>:<R|C|E|D>"` (예: `"employee:R"`)
 * - system permission: `"SYSTEM:<name>"` (예: `"SYSTEM:MANAGE_USERS"`)
 */
export function usePermission() {
  const permissions = useAuthStore((state) => state.user?.permissions ?? []);

  const hasEntityPermission = (entity: string, operation: SfEntityOperation): boolean =>
    permissions.includes(entityPermissionKey(entity, operation));

  const hasSystemPermission = (permission: SfSystemPermission): boolean =>
    permissions.includes(systemPermissionKey(permission));

  const hasAnyEntityPermission = (entity: string, operations: SfEntityOperation[]): boolean =>
    operations.some((op) => hasEntityPermission(entity, op));

  return { hasEntityPermission, hasSystemPermission, hasAnyEntityPermission };
}
