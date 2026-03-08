import { useQuery } from '@tanstack/react-query';
import { fetchProducts, fetchProductCategories, type FetchProductsParams } from '@/api/product';

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
