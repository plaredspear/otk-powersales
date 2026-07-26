import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useProductLookupSearch } from './useProductLookupSearch';
import * as productApi from '@/api/product';

vi.mock('@/api/product', () => ({
  fetchProductsForPromotionLookup: vi.fn(),
}));

const mockFetch = vi.mocked(productApi.fetchProductsForPromotionLookup);

function pageOf(names: string[], totalElements = names.length) {
  return {
    content: names.map((name, i) => ({
      id: i + 1,
      name,
      productCode: `1000000${i}`,
      productStatus: '판매중',
    })),
    page: 0,
    size: 20,
    totalElements,
    totalPages: 1,
  } as Awaited<ReturnType<typeof productApi.fetchProductsForPromotionLookup>>;
}

describe('useProductLookupSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('2자 미만이면 조회하지 않는다', async () => {
    const { result } = renderHook(() => useProductLookupSearch());

    act(() => result.current.onSearch('진'));
    await act(async () => {
      vi.advanceTimersByTime(500);
    });

    expect(mockFetch).not.toHaveBeenCalled();
    expect(result.current.total).toBeNull();
  });

  it('입력이 멈춘 뒤 1회만 조회한다 (debounce)', async () => {
    mockFetch.mockResolvedValue(pageOf(['진라면_매운맛'], 45));
    const { result } = renderHook(() => useProductLookupSearch());

    // 키 입력을 빠르게 이어서 — 각 입력 사이 100ms
    for (const kw of ['진라', '진라면', '진라면_']) {
      act(() => result.current.onSearch(kw));
      act(() => {
        vi.advanceTimersByTime(100);
      });
    }
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    expect(mockFetch).toHaveBeenCalledTimes(1);
    expect(mockFetch).toHaveBeenCalledWith({ keyword: '진라면_', size: 20 });
    expect(result.current.total).toBe(45);
    expect(result.current.items).toHaveLength(1);
  });

  it('빈 검색어는 보관 키워드를 덮어쓰지 않는다 (AntD blur 초기화 무시)', async () => {
    mockFetch.mockResolvedValue(pageOf(['진라면_매운맛']));
    const { result } = renderHook(() => useProductLookupSearch());

    act(() => result.current.onSearch('진라면'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current.keyword).toBe('진라면');

    // "고급 검색" 버튼 클릭 → blur → AntD 가 onSearch('') 호출
    act(() => result.current.onSearch(''));

    expect(result.current.keyword).toBe('진라면');
  });

  it('clearKeyword 는 보관 키워드를 비운다 (사용자가 x 로 지운 경우)', async () => {
    mockFetch.mockResolvedValue(pageOf(['진라면_매운맛']));
    const { result } = renderHook(() => useProductLookupSearch());

    act(() => result.current.onSearch('진라면'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    act(() => result.current.clearKeyword());

    expect(result.current.keyword).toBe('');
  });

  it('selectItem 은 옵션을 1건으로 좁히고 키워드를 비운다', async () => {
    mockFetch.mockResolvedValue(pageOf(['진라면_매운맛', '진라면_순한맛']));
    const { result } = renderHook(() => useProductLookupSearch());

    act(() => result.current.onSearch('진라면'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });
    expect(result.current.items).toHaveLength(2);

    act(() =>
      result.current.selectItem({
        id: 99,
        name: '진라면_매운맛 120G',
        productCode: '18010009',
        productStatus: '단종',
      }),
    );

    expect(result.current.items).toEqual([
      { id: 99, name: '진라면_매운맛 120G', productCode: '18010009', productStatus: '단종' },
    ]);
    expect(result.current.keyword).toBe('');
  });

  it('size 옵션이 요청에 반영된다 (상세 인라인 편집은 10건)', async () => {
    mockFetch.mockResolvedValue(pageOf(['진라면_매운맛']));
    const { result } = renderHook(() => useProductLookupSearch({ size: 10 }));

    act(() => result.current.onSearch('진라면'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    expect(mockFetch).toHaveBeenCalledWith({ keyword: '진라면', size: 10 });
  });

  it('조회 실패 시 결과를 비운다', async () => {
    mockFetch.mockRejectedValue(new Error('network'));
    const { result } = renderHook(() => useProductLookupSearch());

    act(() => result.current.onSearch('진라면'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current.items).toEqual([]);
    expect(result.current.total).toBeNull();
    expect(result.current.searching).toBe(false);
  });
});
