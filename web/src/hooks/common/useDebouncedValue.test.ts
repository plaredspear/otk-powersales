import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useDebouncedValue } from './useDebouncedValue';

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns initial value immediately', () => {
    const { result } = renderHook(() => useDebouncedValue('진'));

    expect(result.current).toBe('진');
  });

  it('does not update before delay elapses', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: '진' },
    });

    rerender({ v: '진라' });
    act(() => {
      vi.advanceTimersByTime(299);
    });

    expect(result.current).toBe('진');
  });

  it('updates after delay elapses', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: '진' },
    });

    rerender({ v: '진라면' });
    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current).toBe('진라면');
  });

  it('emits only the final value for rapid consecutive changes', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: '' },
    });

    // 키 입력을 빠르게 이어서 — 각 입력 사이 100ms
    for (const keyword of ['진', '진라', '진라면', '진라면_']) {
      rerender({ v: keyword });
      act(() => {
        vi.advanceTimersByTime(100);
      });
    }

    // 아직 마지막 입력으로부터 300ms 미경과 → 초기값 유지
    expect(result.current).toBe('');

    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current).toBe('진라면_');
  });

  it('updates synchronously when delay is 0', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 0), {
      initialProps: { v: '진' },
    });

    rerender({ v: '진라면' });

    expect(result.current).toBe('진라면');
  });

  it('supports non-string values', () => {
    const { result, rerender } = renderHook(({ v }) => useDebouncedValue(v, 300), {
      initialProps: { v: 1 },
    });

    rerender({ v: 42 });
    act(() => {
      vi.advanceTimersByTime(300);
    });

    expect(result.current).toBe(42);
  });
});
