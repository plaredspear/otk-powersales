import { useQuery } from '@tanstack/react-query';
import {
  fetchDailySalesHistories,
  type FetchDailySalesHistoriesParams,
} from '@/api/dailySalesHistory';

/**
 * ORORA 일매출 조회 훅. (`/settings/orora-daily-sales`)
 *
 * 거래처 + 매출월이 모두 확정된 뒤에만 조회한다 (params 가 null 이면 미조회 — 조회 전 안내 상태).
 * 적재 배치가 하루 1회 갱신하는 데이터라 재조회 빈도가 낮아 staleTime 을 길게 둔다.
 */
export function useDailySalesHistories(params: FetchDailySalesHistoriesParams | null) {
  return useQuery({
    queryKey: ['admin', 'daily-sales-histories', params?.accountCode, params?.salesMonth] as const,
    queryFn: () => fetchDailySalesHistories(params!),
    enabled: params != null,
    staleTime: 5 * 60 * 1000,
  });
}
