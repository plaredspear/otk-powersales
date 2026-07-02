import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getPPTHistories, type PPTHistorySearchParams } from '@/api/pptMaster';

const QUERY_KEY = ['admin', 'ppt-histories'];

export function usePPTHistories(params: PPTHistorySearchParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: () => getPPTHistories(params),
    // 페이지/조건 변경 시 직전 데이터를 유지해 테이블 깜빡임(빈 상태 노출)을 방지.
    placeholderData: keepPreviousData,
  });
}
