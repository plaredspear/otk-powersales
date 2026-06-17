import { useQuery } from '@tanstack/react-query';
import { fetchDashboardBranches } from '@/api/adminDashboard';
import { useAuthStore } from '@/stores/authStore';

/**
 * 대시보드 지점 셀렉터 옵션.
 *
 * 여사원일정의 useTeamScheduleBranches 와 동일 산출이나, 권한 가드 없는 대시보드 전용
 * endpoint (`/api/v1/admin/dashboard/branches`) 를 호출한다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useDashboardBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'dashboard', 'branches', userId],
    queryFn: fetchDashboardBranches,
  });
}
