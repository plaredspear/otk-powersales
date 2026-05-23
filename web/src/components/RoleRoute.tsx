import { Outlet } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import ForbiddenResult from '@/components/ForbiddenResult';

interface RoleRouteProps {
  /**
   * 허용 Profile.name 집합 — backend `WebUserSummary.user.profileName` 매칭.
   *
   * spec #807: SF DKRetail__AppAuthority__c picklist 부재 직위
   * (시스템 관리자 / 영업부장 / 본부장 등) 가드는 Profile.name 으로 분기.
   */
  allowedProfileNames: string[];
}

/**
 * 사용자 `profileName` 기반 라우터 가드.
 *
 * Backend `WebUserSummary.user.profileName` 값이 `allowedProfileNames` 에 포함되지 않으면
 * ForbiddenResult 페이지를 렌더링한다. permission 배열 매칭이 아닌 단일 Profile 매칭이
 * 필요한 경우(예: 시스템 관리자만 접근 가능한 화면) 사용한다.
 */
export default function RoleRoute({ allowedProfileNames }: RoleRouteProps) {
  const profileName = useAuthStore((state) => state.user?.profileName ?? null);

  if (!profileName || !allowedProfileNames.includes(profileName)) {
    return <ForbiddenResult />;
  }

  return <Outlet />;
}
