import { useQuery } from '@tanstack/react-query';
import { fetchMonthlySalesBranches } from '@/api/salesBranch';
import { useAuthStore } from '@/stores/authStore';

/**
 * 월 매출(물류배부) 전용 지점 셀렉터 옵션.
 *
 * 다른 매출/실적 화면(여사원 일정 스코프, 조직 트리 전체)과 달리, 월 매출(물류배부) 화면은
 * 대시보드와 동일한 지점 기준(전사 권한자에게 고정 화이트리스트 34개)을 요구하므로
 * 전용 endpoint(`/api/v1/admin/sales/monthly/branches`)를 호출한다.
 *
 * 지점 목록은 권한 주체(사용자)별로 다르므로 대행 전환 시 캐시 분리를 위해 사용자 id 를 쿼리 키에 포함한다.
 */
export function useMonthlySalesBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'sales', 'monthly', 'branches', userId],
    queryFn: fetchMonthlySalesBranches,
  });
}
