import { useQuery } from '@tanstack/react-query';
import { fetchSuggestionDetail } from '@/api/suggestions';

export function useSuggestionDetail(id: number) {
  return useQuery({
    queryKey: ['admin', 'suggestions', id],
    queryFn: () => fetchSuggestionDetail(id),
    enabled: id > 0,
  });
}
