import { useQuery } from '@tanstack/react-query';
import { fetchEducations, type EducationListParams } from '@/api/education';

export function useEducationPosts(params: EducationListParams) {
  return useQuery({
    queryKey: ['admin', 'education', params.category, params.search, params.page, params.size],
    queryFn: () => fetchEducations(params),
  });
}
