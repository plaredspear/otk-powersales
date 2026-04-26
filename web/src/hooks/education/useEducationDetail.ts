import { useQuery } from '@tanstack/react-query';
import { fetchEducationDetail } from '@/api/education';

export function useEducationDetail(id: string) {
  return useQuery({
    queryKey: ['admin', 'education', id],
    queryFn: () => fetchEducationDetail(id),
    enabled: !!id,
  });
}
