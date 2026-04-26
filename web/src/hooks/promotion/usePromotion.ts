import { useQuery } from '@tanstack/react-query';
import { fetchPromotion } from '@/api/promotion';

export function usePromotion(id: number) {
  return useQuery({
    queryKey: ['admin', 'promotions', id],
    queryFn: () => fetchPromotion(id),
    enabled: id > 0,
  });
}
