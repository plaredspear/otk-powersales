import { useCallback, useRef } from 'react';

export function useThrottleClick<T extends unknown[]>(
  callback: (...args: T) => void,
  interval = 500,
): (...args: T) => void {
  const lastCalledRef = useRef(0);
  const callbackRef = useRef(callback);
  callbackRef.current = callback;

  return useCallback(
    (...args: T) => {
      if (interval <= 0) {
        callbackRef.current(...args);
        return;
      }
      const now = Date.now();
      if (now - lastCalledRef.current >= interval) {
        lastCalledRef.current = now;
        callbackRef.current(...args);
      }
    },
    [interval],
  );
}
