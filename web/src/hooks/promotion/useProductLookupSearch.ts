import { useCallback } from 'react';
import { fetchProductsForPromotionLookup, type Product } from '@/api/product';
import { useLookupSearch, type LookupSearchResult } from '@/hooks/common/useLookupSearch';

/** 드롭다운 옵션 표시에 필요한 최소 필드 — 검색 결과와 상세 응답 양쪽을 수용한다. */
export interface ProductLookupOption {
  id: number;
  name: string | null;
  productCode: string | null;
  productStatus: string | null;
}

/**
 * 행사마스터 대표제품 lookup 검색 — 등록/수정 폼과 상세 인라인 편집이 공유한다.
 *
 * 조회/매핑만 지정하고 debounce·요청 순번·키워드 보관은 [useLookupSearch] 에 위임한다.
 *
 * @param size 한 번에 가져올 건수 (폼 20 / 상세 인라인 편집 10)
 */
export function useProductLookupSearch(
  { size = 20 }: { size?: number } = {},
): LookupSearchResult<ProductLookupOption> {
  const toItem = useCallback(
    (p: Product): ProductLookupOption | null =>
      p.id != null && p.name != null
        ? {
            id: p.id,
            name: p.name,
            productCode: p.productCode,
            productStatus: p.productStatus,
          }
        : null,
    [],
  );

  return useLookupSearch({ fetchPage: fetchProductsForPromotionLookup, toItem, size });
}
