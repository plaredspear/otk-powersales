import { renderHook, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useListQueryParams } from './useListQueryParams';

const DEFAULTS = { status: '', costCenterCode: '', keyword: '' };

/** initialEntries 의 URL 로 시작하는 MemoryRouter wrapper. */
function makeWrapper(initialEntry = '/list') {
  return ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
  );
}

/** 훅 + 현재 location.search 를 함께 노출하는 테스트 훅. */
function useHarness() {
  const list = useListQueryParams({ defaultFilters: DEFAULTS });
  const location = useLocation();
  return { ...list, search: location.search };
}

describe('useListQueryParams', () => {
  it('파라미터가 없으면 page 0, 필터는 기본값', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list') });
    expect(result.current.page).toBe(0);
    expect(result.current.filters).toEqual(DEFAULTS);
  });

  it('URL 의 1-indexed page 를 0-indexed 로 복원', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?page=5') });
    expect(result.current.page).toBe(4);
  });

  it('URL 의 필터 값을 복원', () => {
    const { result } = renderHook(useHarness, {
      wrapper: makeWrapper('/list?page=3&status=재직&keyword=홍'),
    });
    expect(result.current.page).toBe(2);
    expect(result.current.filters.status).toBe('재직');
    expect(result.current.filters.keyword).toBe('홍');
    expect(result.current.filters.costCenterCode).toBe('');
  });

  it('setPage 는 URL 을 1-indexed 로 기록', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list') });
    act(() => result.current.setPage(4));
    expect(result.current.page).toBe(4);
    expect(result.current.search).toContain('page=5');
  });

  it('setPage(0) 은 page 파라미터를 URL 에서 제거', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?page=5') });
    act(() => result.current.setPage(0));
    expect(result.current.page).toBe(0);
    expect(result.current.search).not.toContain('page=');
  });

  it('setFilter 는 필터를 반영하고 page 를 0 으로 리셋', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?page=7') });
    expect(result.current.page).toBe(6);
    act(() => result.current.setFilter('status', '휴직'));
    expect(result.current.filters.status).toBe('휴직');
    expect(result.current.page).toBe(0);
    expect(result.current.search).toContain('status=');
    expect(result.current.search).not.toContain('page=');
  });

  it('setFilter 에 기본값(빈 문자열) 을 주면 URL 에서 제거', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?status=재직') });
    expect(result.current.filters.status).toBe('재직');
    act(() => result.current.setFilter('status', ''));
    expect(result.current.filters.status).toBe('');
    expect(result.current.search).not.toContain('status=');
  });

  it('setFilters 는 여러 필터를 동시 반영하고 page 를 0 으로 리셋', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?page=4') });
    act(() => result.current.setFilters({ status: '재직', keyword: '김' }));
    expect(result.current.filters.status).toBe('재직');
    expect(result.current.filters.keyword).toBe('김');
    expect(result.current.page).toBe(0);
  });

  it('필터를 바꿔도 기존 다른 필터는 유지', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?status=재직') });
    act(() => result.current.setFilter('keyword', '홍'));
    expect(result.current.filters.status).toBe('재직');
    expect(result.current.filters.keyword).toBe('홍');
  });

  it('비정상 page 값(0, 음수, 문자) 은 0 페이지로 처리', () => {
    const w0 = renderHook(useHarness, { wrapper: makeWrapper('/list?page=0') });
    expect(w0.result.current.page).toBe(0);
    const wNeg = renderHook(useHarness, { wrapper: makeWrapper('/list?page=-3') });
    expect(wNeg.result.current.page).toBe(0);
    const wStr = renderHook(useHarness, { wrapper: makeWrapper('/list?page=abc') });
    expect(wStr.result.current.page).toBe(0);
  });

  it('size 파라미터가 없으면 기본 20', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list') });
    expect(result.current.size).toBe(20);
  });

  it('URL 의 size 를 복원', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?size=50') });
    expect(result.current.size).toBe(50);
  });

  it('setSize 는 URL 에 size 를 기록하고 page 를 0 으로 리셋', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?page=4') });
    act(() => result.current.setSize(100));
    expect(result.current.size).toBe(100);
    expect(result.current.page).toBe(0);
    expect(result.current.search).toContain('size=100');
    expect(result.current.search).not.toContain('page=');
  });

  it('setSize 에 기본값(20) 을 주면 URL 에서 제거', () => {
    const { result } = renderHook(useHarness, { wrapper: makeWrapper('/list?size=50') });
    act(() => result.current.setSize(20));
    expect(result.current.size).toBe(20);
    expect(result.current.search).not.toContain('size=');
  });

  it('비정상 size 값(0, 음수, 문자) 은 기본 20 으로 처리', () => {
    const w0 = renderHook(useHarness, { wrapper: makeWrapper('/list?size=0') });
    expect(w0.result.current.size).toBe(20);
    const wNeg = renderHook(useHarness, { wrapper: makeWrapper('/list?size=-10') });
    expect(wNeg.result.current.size).toBe(20);
    const wStr = renderHook(useHarness, { wrapper: makeWrapper('/list?size=abc') });
    expect(wStr.result.current.size).toBe(20);
  });
});
