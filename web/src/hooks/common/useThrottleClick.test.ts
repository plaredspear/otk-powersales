import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { useThrottleClick } from './useThrottleClick';

describe('useThrottleClick', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('executes callback on first click', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback));

    act(() => {
      result.current();
    });

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it('blocks rapid consecutive clicks within interval', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback, 500));

    act(() => {
      result.current();
    });

    // 100ms later - should be blocked
    vi.advanceTimersByTime(100);
    act(() => {
      result.current();
    });

    // 100ms later - should be blocked
    vi.advanceTimersByTime(100);
    act(() => {
      result.current();
    });

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it('allows click after interval has elapsed', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback, 500));

    act(() => {
      result.current();
    });
    expect(callback).toHaveBeenCalledTimes(1);

    // Wait 600ms - should allow next click
    vi.advanceTimersByTime(600);
    act(() => {
      result.current();
    });
    expect(callback).toHaveBeenCalledTimes(2);
  });

  it('always calls latest callback reference', () => {
    const callback1 = vi.fn();
    const callback2 = vi.fn();

    const { result, rerender } = renderHook(
      ({ cb }) => useThrottleClick(cb, 500),
      { initialProps: { cb: callback1 } },
    );

    // Update callback
    rerender({ cb: callback2 });

    // Wait past interval
    vi.advanceTimersByTime(600);

    act(() => {
      result.current();
    });

    expect(callback1).not.toHaveBeenCalled();
    expect(callback2).toHaveBeenCalledTimes(1);
  });

  it('passes arguments through to callback', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback));

    act(() => {
      result.current(42, 'hello');
    });

    expect(callback).toHaveBeenCalledWith(42, 'hello');
  });

  it('executes immediately when interval is 0', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback, 0));

    act(() => {
      result.current();
      result.current();
      result.current();
    });

    expect(callback).toHaveBeenCalledTimes(3);
  });

  it('executes immediately when interval is negative', () => {
    const callback = vi.fn();
    const { result } = renderHook(() => useThrottleClick(callback, -100));

    act(() => {
      result.current();
      result.current();
    });

    expect(callback).toHaveBeenCalledTimes(2);
  });
});
