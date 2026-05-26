import { useQuery } from '@tanstack/react-query';
import { fetchPromotionPosProducts } from '@/api/promotionPosProduct';

export function usePromotionPosProducts(promotionId: number) {
  return useQuery({
    queryKey: ['admin', 'promotions', promotionId, 'pos-products'],
    queryFn: () => fetchPromotionPosProducts(promotionId),
    enabled: promotionId > 0,
  });
}
