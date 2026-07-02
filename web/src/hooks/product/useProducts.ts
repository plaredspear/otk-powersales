import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
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
    // 재조회(페이지 이동/필터 변경) 중 이전 결과를 유지해 빈 화면 깜빡임 방지.
    placeholderData: keepPreviousData,
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
