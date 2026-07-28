import { useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  fetchProductLookup,
  type ElectronicSalesProductAdvancedItem,
  type ElectronicSalesProductLookupItem,
} from '@/api/electronicSalesDashboard';

/** 드롭다운 옵션 1건 — `item` 은 선택 확정 시 메타 복원에 쓴다. */
interface ProductOption {
  value: number;
  label: string;
  item: ElectronicSalesProductLookupItem;
}

/**
 * [useSalesProductSelector] 의 공개 계약.
 *
 * `SalesProductSelector` 가 이 인터페이스만 받도록 해 훅 내부 구현과 분리한다 —
 * 훅에 내부용 필드를 추가해도 컴포넌트 계약이 따라 넓어지지 않는다.
 */
export interface SalesProductSelectorState {
  /** 조회 요청에 넣을 제품 id 목록 (미선택이면 빈 배열 = 전체). */
  productIds: number[];
  selectedProducts: ElectronicSalesProductLookupItem[];
  /** 드롭다운 Select 에 전달할 값/핸들러/옵션. */
  dropdown: {
    value: number[];
    options: ProductOption[];
    onChange: (ids: number[]) => void;
    onSearch: (value: string) => void;
    loading: boolean;
    /** 디바운스 확정된 검색어 — 고급 검색 모달에 초기 키워드로 넘긴다. */
    keyword: string;
  };
  /** 고급 검색 모달 제어. */
  advanced: {
    open: boolean;
    setOpen: (open: boolean) => void;
    onSelect: (picked: { id: number; item?: ElectronicSalesProductAdvancedItem }[]) => void;
  };
}

/**
 * POS 매출 / 월 매출(전산실적) 조회 조건의 제품 선택 상태.
 *
 * 두 화면이 같은 endpoint · 같은 타입 · 같은 UX 를 쓰므로 검색/디바운스/옵션 조립/선택 병합을
 * 한곳에 모은다. 화면은 반환값을 `SalesProductSelector` 에 그대로 넘기면 된다.
 *
 * 진입점이 둘이다:
 *  - 드롭다운 빠른 검색: 키워드 부분일치 상위 50건. 타이핑 300ms 디바운스.
 *  - 고급 검색 모달: 분류/상태 필터 + 페이징으로 전체 결과 열람 (50건 cap 회피).
 *
 * 두 진입점의 제품 id 축은 동일하다 — 드롭다운의 `productId` 와 고급 검색의 `id` 는 모두
 * 메인 DB `product` PK 다 (backend `ElectronicSalesProductLookupItem.productId` /
 * `ProductListItem.id`). 조회 요청에는 이 id 가 `productIds` 로 전달된다.
 */
export function useSalesProductSelector(): SalesProductSelectorState {
  const [selectedProducts, setSelectedProducts] = useState<ElectronicSalesProductLookupItem[]>([]);
  const [advancedOpen, setAdvancedOpen] = useState<boolean>(false);

  // 제품 검색 — 입력 300ms 디바운스 후 제품명/제품코드/바코드 부분일치 조회 (최대 50건).
  const [keyword, setKeyword] = useState('');
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const handleSearch = (value: string) => {
    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => setKeyword(value.trim()), 300);
  };

  const lookupQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'product-lookup', keyword],
    queryFn: () => fetchProductLookup(keyword),
    enabled: keyword.length > 0,
    staleTime: 60 * 1000,
  });

  // 선택된 제품 + 현재 검색 결과를 병합해 옵션 구성 — 선택 항목의 라벨이 검색어 변경 후에도 유지.
  const options = (() => {
    const byId = new Map<number, ElectronicSalesProductLookupItem>();
    selectedProducts.forEach((p) => byId.set(p.productId, p));
    (lookupQuery.data ?? []).forEach((p) => {
      if (!byId.has(p.productId)) byId.set(p.productId, p);
    });
    return [...byId.values()].map((p) => ({
      value: p.productId,
      label: `${p.name ?? '-'} (${p.productCode ?? '-'} / ${p.barcode})`,
      item: p,
    }));
  })();

  const handleChange = (ids: number[]) => {
    const pool = new Map<number, ElectronicSalesProductLookupItem>();
    options.forEach((o) => pool.set(o.value, o.item));
    setSelectedProducts(
      ids.map((id) => pool.get(id)).filter((p): p is ElectronicSalesProductLookupItem => p != null),
    );
  };

  /**
   * 고급 검색 확정 — 모달이 오픈 시 선택을 복원하므로 확정 결과가 곧 최종 선택이다(대체).
   *
   * 모달이 메타를 확보하지 못한 항목(다른 조건으로 넘어가 결과에 다시 안 나온 경우)은 `item`
   * 이 없으므로, 이미 가진 메타를 우선 사용한다. 빈 배열이면 제품 조건 해제(전체 조회) 다.
   */
  const handleAdvancedSelect = (
    picked: { id: number; item?: ElectronicSalesProductAdvancedItem }[],
  ) => {
    const prevById = new Map(selectedProducts.map((p) => [p.productId, p]));
    setSelectedProducts(
      picked.map(({ id, item }) => {
        const prev = prevById.get(id);
        if (prev) return prev;
        return {
          productId: id,
          name: item?.name ?? null,
          productCode: item?.productCode ?? null,
          // 고급 검색도 대표 바코드를 함께 내려주므로 드롭다운과 라벨이 동일해진다.
          barcode: item?.barcode ?? '',
        };
      }),
    );
  };

  return {
    productIds: selectedProducts.map((p) => p.productId),
    selectedProducts,
    dropdown: {
      value: selectedProducts.map((p) => p.productId),
      options,
      onChange: handleChange,
      onSearch: handleSearch,
      loading: lookupQuery.isFetching,
      keyword,
    },
    advanced: {
      open: advancedOpen,
      setOpen: setAdvancedOpen,
      onSelect: handleAdvancedSelect,
    },
  };
}
