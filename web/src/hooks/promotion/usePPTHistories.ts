import { useQuery } from '@tanstack/react-query';
import { getPPTHistories, type PPTHistorySearchParams } from '@/api/pptMaster';

const QUERY_KEY = ['admin', 'ppt-histories'];

export function usePPTHistories(params: PPTHistorySearchParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: () => getPPTHistories(params),
  });
}
