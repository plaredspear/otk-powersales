import { useQuery } from '@tanstack/react-query';
import { fetchSalesProgressRateMaster } from '@/api/salesProgressRateMaster';

export function useSalesProgressRateMaster(id: number) {
  return useQuery({
    queryKey: ['admin', 'sales-progress-rate-master', id],
    queryFn: () => fetchSalesProgressRateMaster(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}
