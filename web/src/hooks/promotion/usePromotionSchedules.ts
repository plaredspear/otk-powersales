import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  bulkDeletePromotionSchedules,
  bulkUpdatePromotionSchedules,
  fetchPromotionSchedules,
  type FetchPromotionSchedulesParams,
  type PromotionScheduleBulkDeleteRequest,
  type PromotionScheduleBulkUpdateRequest,
} from '@/api/promotionSchedule';

export function usePromotionSchedules(
  promotionId: number,
  params: FetchPromotionSchedulesParams = {},
) {
  return useQuery({
    queryKey: ['admin', 'promotions', promotionId, 'schedules', params.startDate, params.endDate],
    queryFn: () => fetchPromotionSchedules(promotionId, params),
    enabled: promotionId > 0,
  });
}

export function useBulkUpdatePromotionSchedules() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      promotionId,
      data,
    }: {
      promotionId: number;
      data: PromotionScheduleBulkUpdateRequest;
    }) => bulkUpdatePromotionSchedules(promotionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

export function useBulkDeletePromotionSchedules() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      promotionId,
      data,
    }: {
      promotionId: number;
      data: PromotionScheduleBulkDeleteRequest;
    }) => bulkDeletePromotionSchedules(promotionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}
