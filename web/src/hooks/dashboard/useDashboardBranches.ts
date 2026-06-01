import { useQuery } from '@tanstack/react-query';
import { fetchDashboardBranches } from '@/api/adminDashboard';

/**
 * 대시보드 지점 셀렉터 옵션.
 *
 * 여사원일정의 useTeamScheduleBranches 와 동일 산출이나, 권한 가드 없는 대시보드 전용
 * endpoint (`/api/v1/admin/dashboard/branches`) 를 호출한다.
 */
export function useDashboardBranches() {
  return useQuery({
    queryKey: ['admin', 'dashboard', 'branches'],
    queryFn: fetchDashboardBranches,
  });
}
