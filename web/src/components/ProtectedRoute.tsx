import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';

export default function ProtectedRoute() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const passwordChangeRequired = useAuthStore((s) => s.passwordChangeRequired);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // 임시 비밀번호(운영자 리셋) 상태면 변경 화면으로 강제 이동 — 변경 완료 전까지 다른 페이지 차단.
  // backend WebPasswordChangeRequiredFilter 가 API 레벨에서도 동일하게 차단한다 (이중 방어).
  if (passwordChangeRequired) {
    return <Navigate to="/change-password" replace />;
  }

  return <Outlet />;
}
