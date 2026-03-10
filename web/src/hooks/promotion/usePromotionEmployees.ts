import { useQuery } from '@tanstack/react-query';
import { fetchPromotionEmployees } from '@/api/promotionEmployee';

export function usePromotionEmployees(promotionId: number) {
  return useQuery({
    queryKey: ['admin', 'promotions', promotionId, 'employees'],
    queryFn: () => fetchPromotionEmployees(promotionId),
    enabled: promotionId > 0,
  });
}
