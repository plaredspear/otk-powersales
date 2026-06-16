import { useQuery } from '@tanstack/react-query';
import {
  getExternalApiLogDetail,
  getExternalApiLogKeys,
  getExternalApiLogs,
  type ExternalApiLogQuery,
} from '@/api/admin/externalApiLog';

const KEY_BASE = ['admin', 'external-api', 'logs'] as const;

/** 외부 API 호출 이력 페이지 조회. params 변경 시 자동 refetch. */
export function useExternalApiLogs(query: ExternalApiLogQuery) {
  return useQuery({
    queryKey: [...KEY_BASE, 'list', query],
    queryFn: () => getExternalApiLogs(query),
    placeholderData: (previous) => previous,
  });
}

/** 외부 API 호출 이력 단건 상세 — 모달 open 시점에만 호출. errorDetail 포함. */
export function useExternalApiLogDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: [...KEY_BASE, 'detail', id],
    queryFn: () => getExternalApiLogDetail(id),
    enabled: enabled && id > 0,
    staleTime: 5 * 60_000,
  });
}

/** 필터 셀렉터용 endpoint key 목록 (SoT — staleTime: Infinity). */
export function useExternalApiLogKeys() {
  return useQuery({
    queryKey: [...KEY_BASE, 'keys'],
    queryFn: getExternalApiLogKeys,
    staleTime: Infinity,
  });
}
