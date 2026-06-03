import { useQuery } from '@tanstack/react-query';
import { fetchSavedSearches } from '@/api/savedSearch';

export function useSavedSearches(resourceKey: string) {
  return useQuery({
    queryKey: ['admin', 'saved-searches', resourceKey],
    queryFn: () => fetchSavedSearches(resourceKey),
  });
}
