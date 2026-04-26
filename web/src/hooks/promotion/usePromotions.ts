import { useQuery } from '@tanstack/react-query';
import { fetchPromotions, type PromotionListParams } from '@/api/promotion';

export function usePromotions(params: PromotionListParams) {
  return useQuery({
    queryKey: [
      'admin',
      'promotions',
      params.keyword,
      params.promotionTypeId,
      params.category,
      params.startDate,
      params.endDate,
      params.page,
      params.size,
    ],
    queryFn: () => fetchPromotions(params),
  });
}
