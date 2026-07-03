import { useQuery } from '@tanstack/react-query';
import { fetchMonthlyIntegrationDetail } from '@/api/monthlyIntegration';

export function useMonthlyIntegrationDetail(id: number | null) {
  return useQuery({
    queryKey: ['admin', 'schedules', 'monthly-integration', 'detail', id],
    queryFn: () => fetchMonthlyIntegrationDetail(id!),
    enabled: id != null,
  });
}
