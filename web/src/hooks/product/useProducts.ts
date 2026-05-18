import { useMutation, useQuery } from '@tanstack/react-query';
import {
  fetchProducts,
  fetchProductCategories,
  fetchProductDetail,
  searchInventory,
  type FetchProductsParams,
  type InventorySearchRequest,
} from '@/api/product';

export function useProducts(params: FetchProductsParams) {
  return useQuery({
    queryKey: ['admin', 'products', params.keyword, params.category1, params.category2, params.category3, params.productStatus, params.page, params.size],
    queryFn: () => fetchProducts(params),
  });
}

export function useProductCategories() {
  return useQuery({
    queryKey: ['admin', 'products', 'categories'],
    queryFn: fetchProductCategories,
    staleTime: 30 * 60 * 1000, // 30분
  });
}

export function useProductDetail(productCode: string | undefined) {
  return useQuery({
    queryKey: ['admin', 'products', 'detail', productCode],
    queryFn: () => fetchProductDetail(productCode!),
    enabled: !!productCode,
  });
}

export function useInventorySearch() {
  return useMutation({
    mutationFn: (request: InventorySearchRequest) => searchInventory(request),
  });
}
