import { useQuery } from '@tanstack/react-query';
import {
  getSapOutboundCatalog,
  getSapOutboundLogDetail,
  getSapOutboundLogs,
  getSapOutboundOutboxPending,
  type SapOutboundLogQuery,
} from '@/api/admin/sapIntegration';

const KEY_BASE = ['admin', 'sap-integration', 'outbound'] as const;

/** SAP 아웃바운드 카탈로그 조회 (코드 SoT — staleTime: Infinity). */
export function useSapOutboundCatalog() {
  return useQuery({
    queryKey: [...KEY_BASE, 'catalog'],
    queryFn: getSapOutboundCatalog,
    staleTime: Infinity,
  });
}

/** SAP 아웃바운드 호출 이력 페이지 조회. params 변경 시 자동 refetch. */
export function useSapOutboundLogs(query: SapOutboundLogQuery) {
  return useQuery({
    queryKey: [...KEY_BASE, 'logs', query],
    queryFn: () => getSapOutboundLogs(query),
    placeholderData: (previous) => previous,
  });
}

/** SAP 아웃바운드 호출 이력 단건 상세 — 모달 open 시점에만 호출. errorDetail 포함. */
export function useSapOutboundLogDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: [...KEY_BASE, 'log-detail', id],
    queryFn: () => getSapOutboundLogDetail(id),
    enabled: enabled && id > 0,
    staleTime: 5 * 60_000,
  });
}

/**
 * SAP 아웃박스 대기 큐 모니터링 — 수동 새로고침.
 */
export function useSapOutboundOutboxPending(page: number = 1, size: number = 20) {
  return useQuery({
    queryKey: [...KEY_BASE, 'outbox-pending', page, size],
    queryFn: () => getSapOutboundOutboxPending(page, size),
    placeholderData: (previous) => previous,
  });
}
