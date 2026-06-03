import { useQuery } from '@tanstack/react-query';
import {
  fetchSalesProgressRateMasters,
  type SalesProgressRateMasterListParams,
} from '@/api/salesProgressRateMaster';

export function useSalesProgressRateMasters(params: SalesProgressRateMasterListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'sales-progress-rate-masters',
      params.keyword,
      params.targetYear,
      params.targetMonth,
      params.page,
      params.size,
    ],
    queryFn: () => fetchSalesProgressRateMasters(params),
  });
}
