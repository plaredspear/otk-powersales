import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useAccountLookupSearch } from './useAccountLookupSearch';
import * as accountApi from '@/api/account';

vi.mock('@/api/account', () => ({
  fetchAccountsForPromotionLookup: vi.fn(),
}));

const mockFetch = vi.mocked(accountApi.fetchAccountsForPromotionLookup);

/**
 * debounce / 요청 순번 / 빈 검색어 무시 등 공통 동작은 useLookupSearch 를 공유하므로
 * useProductLookupSearch 테스트가 커버한다. 여기서는 거래처 고유의 매핑만 검증한다.
 */
describe('useAccountLookupSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    mockFetch.mockReset();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('거래상태를 포함해 매핑한다 (선택 즉시 거래상태 표시용)', async () => {
    mockFetch.mockResolvedValue({
      content: [
        { id: 1, name: '이마트 성수점', externalKey: '1000', accountStatusName: '거래' },
        { id: 2, name: '이마트 왕십리점', externalKey: '1010', accountStatusName: '폐업' },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    } as Awaited<ReturnType<typeof accountApi.fetchAccountsForPromotionLookup>>);

    const { result } = renderHook(() => useAccountLookupSearch());

    act(() => result.current.onSearch('이마트'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current.items).toEqual([
      { id: 1, name: '이마트 성수점', externalKey: '1000', accountStatusName: '거래' },
      { id: 2, name: '이마트 왕십리점', externalKey: '1010', accountStatusName: '폐업' },
    ]);
    expect(result.current.total).toBe(2);
  });

  it('id/name 이 없는 row 는 결과에서 제외한다', async () => {
    mockFetch.mockResolvedValue({
      content: [
        { id: 1, name: '이마트 성수점', externalKey: '1000', accountStatusName: '거래' },
        { id: 2, name: null, externalKey: '1010', accountStatusName: '거래' },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    } as Awaited<ReturnType<typeof accountApi.fetchAccountsForPromotionLookup>>);

    const { result } = renderHook(() => useAccountLookupSearch());

    act(() => result.current.onSearch('이마트'));
    await act(async () => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0].id).toBe(1);
  });
});
