import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import {
  fetchProducts,
  fetchProductCategories,
  fetchProductDetail,
  searchInventory,
  type FetchProductsParams,
  type InventorySearchRequest,
} from '@/api/product';

/**
 * 관리자 제품 목록 Query 훅.
 *
 * queryKey 에 params 객체 전체를 넣어 필터 누락으로 재조회가 빠지는 것을 막는다
 * (필터가 8개로 늘어 개별 나열은 누락 위험이 크다).
 */
export function useProducts(params: FetchProductsParams) {
  return useQuery({
    queryKey: ['admin', 'products', params],
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
