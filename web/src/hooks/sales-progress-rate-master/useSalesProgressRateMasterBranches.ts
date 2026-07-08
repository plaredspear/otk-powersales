import { useQuery } from '@tanstack/react-query';
import { getSalesProgressRateMasterBranches } from '@/api/salesProgressRateMaster';
import { useAuthStore } from '@/stores/authStore';

/**
 * 거래처목표등록마스터 화면 지점 셀렉터 옵션 — 권한 주체별 지점 화이트리스트.
 *
 * 지점 목록은 권한 주체별로 다르므로 queryKey 에 userId 를 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useSalesProgressRateMasterBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'sales-progress-rate-masters', 'branches', userId],
    queryFn: getSalesProgressRateMasterBranches,
  });
}
