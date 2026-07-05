import { useQuery } from '@tanstack/react-query';
import { fetchSalesBranches } from '@/api/salesBranch';
import { useAuthStore } from '@/stores/authStore';

/**
 * 매출/실적 계열 화면 공용 지점 셀렉터 옵션.
 *
 * `monthly_sales_history` READ 로 가드된 `/api/v1/admin/sales/branches` 를 호출한다. 매출 계열
 * 화면(월 매출·투입적합성·배치 적합성)의 게이팅 권한과 지점 셀렉터 API 가드를 정합시킨다.
 *
 * 지점 목록은 권한 주체(사용자)별로 다르므로 대행 전환 시 캐시 분리를 위해 사용자 id 를 쿼리 키에 포함한다.
 */
export function useSalesBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'sales', 'branches', userId],
    queryFn: fetchSalesBranches,
  });
}
