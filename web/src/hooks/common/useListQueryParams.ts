import { useCallback, useMemo, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * 목록 조회 페이지의 페이지네이션 + 필터 상태를 URL query string 에 보관하는 공통 훅.
 *
 * 목록 페이지가 상세로 이동하며 unmount 되어도 page/필터가 URL 에 남아,
 * 뒤로가기 / 목록 재진입 / 새로고침 / 링크 공유 시 직전 조건이 그대로 복원된다.
 * (기존 `useState(page)` 방식은 unmount 시 상태가 소실되어 항상 1페이지로 리셋됨)
 *
 * - page 는 0-indexed (백엔드 페이지 인덱스와 동일). URL 에는 사용자 친화적으로 1-indexed 로 노출.
 * - size(페이지 크기) 도 URL 에 보관하되, 기본값(defaultPageSize) 과 같으면 생략.
 * - 기본값과 동일한 값은 URL 에서 생략하여 URL 을 깔끔하게 유지.
 * - 필터(setFilter) / size(setSize) 변경 시 page 를 0 으로 자동 리셋.
 */
export interface UseListQueryParamsOptions<F extends Record<string, string | undefined>> {
  /** 필터 키별 기본값. 기본값과 같은 값은 URL 에 기록하지 않는다. */
  defaultFilters: F;
  /** URL query 의 page 파라미터 키 (기본 'page'). */
  pageParamKey?: string;
  /** URL query 의 size 파라미터 키 (기본 'size'). */
  sizeParamKey?: string;
  /** 페이지 크기 기본값 (기본 20). 기본값과 같으면 URL 에 기록하지 않는다. */
  defaultPageSize?: number;
}

export interface UseListQueryParamsResult<F extends Record<string, string | undefined>> {
  /** 0-indexed 현재 페이지. */
  page: number;
  /** 0-indexed 페이지로 이동. */
  setPage: (page: number) => void;
  /** 현재 페이지 크기. */
  size: number;
  /** 페이지 크기 변경. page 를 0 으로 리셋한다. */
  setSize: (size: number) => void;
  /** 현재 필터 값 (defaultFilters 와 병합된 결과). */
  filters: F;
  /** 단일 필터 변경. page 를 0 으로 리셋한다. */
  setFilter: <K extends keyof F>(key: K, value: F[K]) => void;
  /** 여러 필터 동시 변경. page 를 0 으로 리셋한다. */
  setFilters: (next: Partial<F>) => void;
}

export function useListQueryParams<F extends Record<string, string | undefined>>(
  options: UseListQueryParamsOptions<F>,
): UseListQueryParamsResult<F> {
  const { pageParamKey = 'page', sizeParamKey = 'size', defaultPageSize = 20 } = options;
  const [searchParams, setSearchParams] = useSearchParams();
  // 호출부 리터럴은 매 렌더 새 참조 → 첫 렌더 값을 고정해 의존성/동작을 안정화.
  const defaultFiltersRef = useRef(options.defaultFilters);
  const defaultFilters = defaultFiltersRef.current;

  const page = useMemo(() => {
    const raw = searchParams.get(pageParamKey);
    const parsed = raw != null ? Number.parseInt(raw, 10) : NaN;
    // URL 은 1-indexed → 내부 0-indexed 로 변환. 비정상 값은 0 페이지.
    return Number.isFinite(parsed) && parsed > 0 ? parsed - 1 : 0;
  }, [searchParams, pageParamKey]);

  const size = useMemo(() => {
    const raw = searchParams.get(sizeParamKey);
    const parsed = raw != null ? Number.parseInt(raw, 10) : NaN;
    // 비정상 값은 기본 크기.
    return Number.isFinite(parsed) && parsed > 0 ? parsed : defaultPageSize;
  }, [searchParams, sizeParamKey, defaultPageSize]);

  const filters = useMemo(() => {
    const result = { ...defaultFilters };
    for (const key of Object.keys(defaultFilters) as Array<keyof F>) {
      const raw = searchParams.get(key as string);
      if (raw != null) {
        result[key] = raw as F[keyof F];
      }
    }
    return result;
  }, [searchParams, defaultFilters]);

  /** 현재 URL 파라미터를 기준으로 patch 를 적용한 새 URLSearchParams 생성 (기본값/빈값은 제거). */
  const buildNext = useCallback(
    (prev: URLSearchParams, patch: { page?: number; size?: number; filters?: Partial<F> }) => {
      const next = new URLSearchParams(prev);

      if (patch.page !== undefined) {
        if (patch.page > 0) {
          next.set(pageParamKey, String(patch.page + 1));
        } else {
          next.delete(pageParamKey);
        }
      }

      if (patch.size !== undefined) {
        if (patch.size !== defaultPageSize) {
          next.set(sizeParamKey, String(patch.size));
        } else {
          next.delete(sizeParamKey);
        }
      }

      if (patch.filters) {
        for (const [key, value] of Object.entries(patch.filters)) {
          const isDefault = value === undefined || value === '' || value === defaultFilters[key as keyof F];
          if (isDefault) {
            next.delete(key);
          } else {
            next.set(key, value as string);
          }
        }
      }

      return next;
    },
    [pageParamKey, sizeParamKey, defaultPageSize, defaultFilters],
  );

  const setPage = useCallback(
    (nextPage: number) => {
      setSearchParams((prev) => buildNext(prev, { page: nextPage }), { replace: true });
    },
    [setSearchParams, buildNext],
  );

  const setSize = useCallback(
    (nextSize: number) => {
      setSearchParams((prev) => buildNext(prev, { page: 0, size: nextSize }), { replace: true });
    },
    [setSearchParams, buildNext],
  );

  const setFilter = useCallback(
    <K extends keyof F>(key: K, value: F[K]) => {
      const patch = { [key]: value } as unknown as Partial<F>;
      setSearchParams((prev) => buildNext(prev, { page: 0, filters: patch }), { replace: true });
    },
    [setSearchParams, buildNext],
  );

  const setFilters = useCallback(
    (nextFilters: Partial<F>) => {
      setSearchParams((prev) => buildNext(prev, { page: 0, filters: nextFilters }), { replace: true });
    },
    [setSearchParams, buildNext],
  );

  return { page, setPage, size, setSize, filters, setFilter, setFilters };
}
