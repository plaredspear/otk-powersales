import { Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import ForbiddenResult from '@/components/ForbiddenResult';
import type { UserRole } from '@/constants/userRole';

interface RoleRouteProps {
  allowedRoles: UserRole[];
}

/**
 * 사용자 `role` 기반 라우터 가드 (Spec #579).
 *
 * `Employee.role` 값이 `allowedRoles` 에 포함되지 않으면 ForbiddenResult 페이지를 렌더링한다.
 * permission 배열 매칭이 아닌 단일 role 매칭이 필요한 경우(예: 시스템관리자만 접근 가능한
 * 화면) 사용한다. permission 기반 가드는 `PermissionRoute` 사용.
 */
export default function RoleRoute({ allowedRoles }: RoleRouteProps) {
  const role = useAuthStore((state) => state.user?.role ?? null);

  if (!role || !allowedRoles.includes(role)) {
    return <ForbiddenResult />;
  }

  return <Outlet />;
}
