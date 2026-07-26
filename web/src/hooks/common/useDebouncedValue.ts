import { useEffect, useRef, useState } from 'react';

/**
 * 값 변경을 지정 지연만큼 미뤄 반환한다 — 검색어를 `queryKey` 에 넣는 lookup 에서
 * 키 입력마다 쿼리가 발생하는 것을 막는다 (web-conventions.md § Search Debounce).
 *
 * 직접 `fetch` 하는 경우와 달리 `useQuery` 는 TanStack Query 가 낡은 응답을 처리하므로
 * 요청 순번(seq) 검증이 불필요하다.
 *
 * @param value 원본 값 (입력 중인 검색어)
 * @param delay 지연 시간 (ms). 기본 300ms
 */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (delay <= 0) {
      setDebounced(value);
      return;
    }
    timerRef.current = setTimeout(() => setDebounced(value), delay);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [value, delay]);

  return debounced;
}
