import { Outlet } from 'react-router-dom';
import { usePermission } from '@/hooks/usePermission';
import ForbiddenResult from '@/components/ForbiddenResult';

interface PermissionRouteProps {
  requiredPermission?: string;
}

export default function PermissionRoute({ requiredPermission }: PermissionRouteProps) {
  const { hasPermission } = usePermission();

  if (requiredPermission && !hasPermission(requiredPermission)) {
    return <ForbiddenResult />;
  }

  return <Outlet />;
}
