import { useQuery } from '@tanstack/react-query';
import { fetchElectronicSalesBranches } from '@/api/salesBranch';
import { useAuthStore } from '@/stores/authStore';

/**
 * 월 매출(전산실적) 전용 지점 셀렉터 옵션 — 조직 트리 스코프.
 *
 * 화면별 전용 endpoint(`/api/v1/admin/sales/electronic/branches`)로 분리해 향후 화면별 권한/스코프를
 * 독립적으로 조정할 수 있게 한다. 지점 목록은 권한 주체(사용자)별로 다르므로 대행 전환 시 캐시 분리를
 * 위해 사용자 id 를 쿼리 키에 포함한다.
 */
export function useElectronicSalesBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'sales', 'electronic', 'branches', userId],
    queryFn: fetchElectronicSalesBranches,
  });
}
