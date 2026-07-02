import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchSuggestions, type SuggestionListParams } from '@/api/suggestions';

export function useSuggestions(params: SuggestionListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'suggestions',
      params.startDate,
      params.endDate,
      params.category,
      params.employeeName,
      params.accountCode,
      params.actionStatus,
      params.productCode,
      params.page,
      params.size,
    ],
    queryFn: () => fetchSuggestions(params),
    // 페이지/조건 변경 시 직전 데이터를 유지해 테이블 깜빡임(빈 상태 노출)을 방지.
    placeholderData: keepPreviousData,
  });
}
