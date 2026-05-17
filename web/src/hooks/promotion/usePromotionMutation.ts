import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  createPromotion,
  updatePromotion,
  deletePromotion,
  clonePromotion,
  cloneWithChildren,
  type PromotionFormData,
} from '@/api/promotion';

export function useCreatePromotion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PromotionFormData) => createPromotion(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

export function useUpdatePromotion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: PromotionFormData }) => updatePromotion(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

export function useDeletePromotion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deletePromotion(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

/**
 * 행사마스터 복제 (폼 방식) Mutation 훅 — UC-11.
 *
 * 성공 시 목록 캐시 무효화. 신규 promotion id 는 호출부에서 navigate 에 사용.
 */
export function useClonePromotion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ sourceId, data }: { sourceId: number; data: PromotionFormData }) =>
      clonePromotion(sourceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}

/**
 * 행사마스터 자식 포함 복제 Mutation 훅 — UC-12.
 *
 * 성공 시 목록 캐시 무효화. 신규 promotion id 는 호출부에서 navigate 에 사용.
 */
export function useCloneWithChildren() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (sourceId: number) => cloneWithChildren(sourceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'promotions'] });
    },
  });
}
