import { keepPreviousData, useQuery } from '@tanstack/react-query';
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
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
  });
}
