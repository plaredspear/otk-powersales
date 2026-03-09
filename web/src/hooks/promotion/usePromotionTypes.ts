import { useQuery } from '@tanstack/react-query';
import { fetchPromotionTypes } from '@/api/promotionType';

export function usePromotionTypes() {
  return useQuery({
    queryKey: ['admin', 'promotion-types'],
    queryFn: fetchPromotionTypes,
  });
}
