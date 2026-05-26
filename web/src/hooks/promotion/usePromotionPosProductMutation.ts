import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createPromotionPosProduct,
  deletePromotionPosProduct,
  updatePromotionPosProduct,
  type PromotionPosProductRequest,
} from '@/api/promotionPosProduct';

function invalidatePosProductList(queryClient: ReturnType<typeof useQueryClient>, promotionId: number) {
  queryClient.invalidateQueries({
    queryKey: ['admin', 'promotions', promotionId, 'pos-products'],
  });
}

export function useCreatePromotionPosProduct(promotionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PromotionPosProductRequest) =>
      createPromotionPosProduct(promotionId, data),
    onSuccess: () => invalidatePosProductList(queryClient, promotionId),
  });
}

export function useUpdatePromotionPosProduct(promotionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PromotionPosProductRequest }) =>
      updatePromotionPosProduct(id, data),
    onSuccess: () => invalidatePosProductList(queryClient, promotionId),
  });
}

export function useDeletePromotionPosProduct(promotionId: number) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePromotionPosProduct(id),
    onSuccess: () => invalidatePosProductList(queryClient, promotionId),
  });
}
