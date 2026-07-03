import { useQuery } from '@tanstack/react-query';
import { fetchClaimReportBranches } from '@/api/claimPeriodReport';
import { useAuthStore } from '@/stores/authStore';

/**
 * 기간별 클레임 보고서 지점 셀렉터 옵션 Query 훅.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 * 드롭다운 메타 데이터라 staleTime 10분 적용.
 */
export function useClaimReportBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'claims', 'period-report', 'branches', userId],
    queryFn: fetchClaimReportBranches,
    staleTime: 10 * 60 * 1000,
  });
}
