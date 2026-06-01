import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

/** 미인증 시 /login 으로 리다이렉트. 인증된 경우 하위 라우트 렌더. */
export default function AuthGuard() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  return <Outlet />;
}
