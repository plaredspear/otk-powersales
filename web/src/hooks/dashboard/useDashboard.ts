import { useQuery } from '@tanstack/react-query';
import { fetchDashboard, type FetchDashboardParams } from '@/api/dashboard';

export function useDashboard(params?: FetchDashboardParams) {
  return useQuery({
    queryKey: ['admin', 'dashboard', params?.yearMonth, params?.branchCode],
    queryFn: () => fetchDashboard(params),
    staleTime: 5 * 60 * 1000,
  });
}
