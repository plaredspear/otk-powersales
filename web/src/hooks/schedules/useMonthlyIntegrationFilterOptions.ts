import { useQuery } from '@tanstack/react-query';
import { fetchMonthlyIntegrationFilterOptions } from '@/api/monthlyIntegration';

/**
 * 통합일정 조회조건 드롭다운 옵션(유통형태/거래처유형 + 종속 매핑) 조회.
 * 값 도메인이 Account 전체 기반이라 자주 바뀌지 않으므로 staleTime 을 길게 둔다.
 */
export function useMonthlyIntegrationFilterOptions() {
  return useQuery({
    queryKey: ['admin', 'schedules', 'monthly-integration', 'filter-options'],
    queryFn: fetchMonthlyIntegrationFilterOptions,
    staleTime: 1000 * 60 * 30,
  });
}
