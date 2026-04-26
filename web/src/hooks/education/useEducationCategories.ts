import { useQuery } from '@tanstack/react-query';
import { fetchEducationCategories } from '@/api/education';

export function useEducationCategories() {
  return useQuery({
    queryKey: ['admin', 'education', 'categories'],
    queryFn: fetchEducationCategories,
    staleTime: 10 * 60 * 1000,
  });
}
