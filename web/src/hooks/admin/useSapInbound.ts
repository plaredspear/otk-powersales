import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getSapInboundAuditDetail,
  getSapInboundAudits,
  getSapInboundCatalog,
  setSapInboundEnabled,
  type SapInboundAuditQuery,
  type SapInboundCatalogItem,
} from '@/api/admin/sapIntegration';

const KEY_BASE = ['admin', 'sap-integration', 'inbound'] as const;

/** SAP 인바운드 카탈로그 조회 (코드 SoT — staleTime: Infinity). */
export function useSapInboundCatalog() {
  return useQuery({
    queryKey: [...KEY_BASE, 'catalog'],
    queryFn: getSapInboundCatalog,
    staleTime: Infinity,
  });
}

/**
 * SAP 인바운드 endpoint 처리 활성/비활성 변경 mutation.
 * 성공 시 서버가 돌려준 최신 카탈로그로 캐시를 직접 갱신한다 (catalog 는 staleTime: Infinity).
 */
export function useSetSapInboundEnabled() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ endpointPath, enabled }: { endpointPath: string; enabled: boolean }) =>
      setSapInboundEnabled(endpointPath, enabled),
    onSuccess: (catalog: SapInboundCatalogItem[]) => {
      queryClient.setQueryData([...KEY_BASE, 'catalog'], catalog);
    },
  });
}

/** SAP 인바운드 audit 이력 페이지 조회. params 변경 시 자동 refetch. */
export function useSapInboundAudits(query: SapInboundAuditQuery) {
  return useQuery({
    queryKey: [...KEY_BASE, 'audits', query],
    queryFn: () => getSapInboundAudits(query),
    placeholderData: (previous) => previous,
  });
}

/** SAP 인바운드 audit 단건 상세 — 모달 open 시점에만 호출 (enabled flag). */
export function useSapInboundAuditDetail(id: number, enabled: boolean = true) {
  return useQuery({
    queryKey: [...KEY_BASE, 'audit-detail', id],
    queryFn: () => getSapInboundAuditDetail(id),
    enabled: enabled && id > 0,
    staleTime: 5 * 60_000,
  });
}
