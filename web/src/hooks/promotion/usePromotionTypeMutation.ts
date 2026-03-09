import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createPromotionType,
  updatePromotionType,
  deletePromotionType,
  type PromotionTypeRequest,
} from '@/api/promotionType';

export function useCreatePromotionType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PromotionTypeRequest) => createPromotionType(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotion-types'] });
    },
  });
}

export function useUpdatePromotionType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PromotionTypeRequest }) =>
      updatePromotionType(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotion-types'] });
    },
  });
}

export function useDeletePromotionType() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePromotionType(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotion-types'] });
    },
  });
}
