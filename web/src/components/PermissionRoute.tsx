import { Outlet } from 'react-router-dom';
import { usePermission, type SfEntityOperation, type SfSystemPermission } from '@/hooks/usePermission';
import ForbiddenResult from '@/components/ForbiddenResult';

/**
 * Spec #802 — SF 권한 모델 라우트 가드.
 *
 * - entity + operation: 특정 entity 의 CRUD 권한 검사
 * - systemPermission: SF 시스템 권한 검사 (VIEW_ALL_DATA / MANAGE_USERS 등)
 *
 * 둘 다 지정 시 둘 중 하나라도 충족하면 통과.
 * 둘 다 미지정 시 통과 (가드 미사용).
 */
interface PermissionRouteProps {
  entity?: string;
  operation?: SfEntityOperation;
  systemPermission?: SfSystemPermission;
}

export default function PermissionRoute({ entity, operation, systemPermission }: PermissionRouteProps) {
  const { hasEntityPermission, hasSystemPermission } = usePermission();

  const requiresEntity = !!(entity && operation);
  const requiresSystem = !!systemPermission;
  const allows =
    (!requiresEntity && !requiresSystem) ||
    (requiresEntity && hasEntityPermission(entity!, operation!)) ||
    (requiresSystem && hasSystemPermission(systemPermission!));

  if (!allows) {
    return <ForbiddenResult />;
  }

  return <Outlet />;
}
