import { useQuery } from '@tanstack/react-query';
import { fetchPromotionFormMeta } from '@/api/promotion';

export function usePromotionFormMeta() {
  return useQuery({
    queryKey: ['admin', 'promotions', 'form-meta'],
    queryFn: fetchPromotionFormMeta,
    staleTime: 10 * 60 * 1000,
  });
}
