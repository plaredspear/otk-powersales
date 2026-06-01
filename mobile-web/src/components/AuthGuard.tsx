import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

const FORCE_PW_PATH = '/password/change';

/**
 * 인증 가드.
 * - 미인증 → /login
 * - 인증됐으나 강제 비밀번호 변경 필요(passwordChangeRequired) → /password/change 로 고정
 *   (레거시 resetPwd 강제 리다이렉트 대응)
 */
export default function AuthGuard() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const passwordChangeRequired = useAuthStore((s) => s.passwordChangeRequired);
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }
  if (passwordChangeRequired && location.pathname !== FORCE_PW_PATH) {
    return <Navigate to={FORCE_PW_PATH} replace />;
  }
  return <Outlet />;
}
