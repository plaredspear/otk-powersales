import { keepPreviousData, useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProductExpirations,
  fetchProductExpirationSummary,
  createProductExpiration,
  updateProductExpiration,
  deleteProductExpiration,
  batchDeleteProductExpirations,
  type FetchProductExpirationsParams,
  type CreateProductExpirationRequest,
  type UpdateProductExpirationRequest,
} from '@/api/productExpiration';

const QUERY_KEY = ['admin', 'product-expiration'];

export function useProductExpirations(params: FetchProductExpirationsParams) {
  return useQuery({
    queryKey: [...QUERY_KEY, params],
    queryFn: () => fetchProductExpirations(params),
    // 페이지/필터 전환 중 직전 데이터 유지 — 테이블이 빈 상태로 깜빡이지 않게.
    placeholderData: keepPreviousData,
  });
}

export function useProductExpirationSummary() {
  return useQuery({
    queryKey: [...QUERY_KEY, 'summary'],
    queryFn: () => fetchProductExpirationSummary(),
  });
}

export function useCreateProductExpiration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateProductExpirationRequest) => createProductExpiration(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useUpdateProductExpiration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProductExpirationRequest }) =>
      updateProductExpiration(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useDeleteProductExpiration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteProductExpiration(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}

export function useBatchDeleteProductExpiration() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (ids: number[]) => batchDeleteProductExpirations(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
    },
  });
}
