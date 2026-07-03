import { useQuery } from '@tanstack/react-query';
import { fetchReportBranches } from '@/api/femaleEmployeeSafetyCheckReport';
import { useAuthStore } from '@/stores/authStore';

/**
 * 여사원 보고서 화면(안전점검·환산인원) 지점 셀렉터 옵션 Query 훅.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 * 드롭다운 메타 데이터라 staleTime 10분 적용.
 */
export function useReportBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'female-employees', 'report-branches', userId],
    queryFn: fetchReportBranches,
    staleTime: 10 * 60 * 1000,
  });
}
