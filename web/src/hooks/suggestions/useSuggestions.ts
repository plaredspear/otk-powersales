import { useQuery } from '@tanstack/react-query';
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
  });
}
