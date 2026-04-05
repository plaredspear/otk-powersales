import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
