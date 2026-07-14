import { Outlet } from 'react-router-dom';
import { usePermission, type SfEntityOperation, type SfSystemPermission } from '@/hooks/usePermission';
import { useAuthStore } from '@/stores/authStore';
import ForbiddenResult from '@/components/ForbiddenResult';

/**
 * Spec #802 — SF 권한 모델 라우트 가드.
 *
 * - entity + operation: 특정 entity 의 CRUD 권한 검사
 * - systemPermission: SF 시스템 권한 검사 (VIEW_ALL_DATA / MANAGE_USERS 등)
 *
 * 둘 다 지정 시 둘 중 하나라도 충족하면 통과.
 * 둘 다 미지정 시 통과 (가드 미사용).
 *
 * - deniedCostCenterCodes: deny-list. 위 권한을 충족하더라도 user.costCenterCode 가
 *   이 집합에 포함되면 차단(ForbiddenResult). 특정 조직/팀에게만 화면 접근을 막는 용도
 *   (menuConfig `deniedForCostCenterCodes` 메뉴 숨김과 대칭 — URL 직접 진입도 차단).
 */
interface PermissionRouteProps {
  entity?: string;
  operation?: SfEntityOperation;
  systemPermission?: SfSystemPermission;
  deniedCostCenterCodes?: string[];
}

export default function PermissionRoute({
  entity,
  operation,
  systemPermission,
  deniedCostCenterCodes,
}: PermissionRouteProps) {
  const { hasEntityPermission, hasSystemPermission } = usePermission();
  const costCenterCode = useAuthStore((state) => state.user?.costCenterCode ?? null);

  const requiresEntity = !!(entity && operation);
  const requiresSystem = !!systemPermission;
  const allows =
    (!requiresEntity && !requiresSystem) ||
    (requiresEntity && hasEntityPermission(entity!, operation!)) ||
    (requiresSystem && hasSystemPermission(systemPermission!));

  const denied =
    !!deniedCostCenterCodes && !!costCenterCode && deniedCostCenterCodes.includes(costCenterCode);

  if (!allows || denied) {
    return <ForbiddenResult />;
  }

  return <Outlet />;
}
