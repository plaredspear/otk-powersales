import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createPromotionEmployee,
  updatePromotionEmployee,
  deletePromotionEmployee,
  type PromotionEmployeeFormData,
} from '@/api/promotionEmployee';

export function useCreatePromotionEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ promotionId, data }: { promotionId: number; data: PromotionEmployeeFormData }) =>
      createPromotionEmployee(promotionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

export function useUpdatePromotionEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PromotionEmployeeFormData }) =>
      updatePromotionEmployee(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

export function useDeletePromotionEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePromotionEmployee(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}
