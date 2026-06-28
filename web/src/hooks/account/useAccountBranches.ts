import { useQuery } from '@tanstack/react-query';
import { getAccountBranches } from '@/api/account';
import { useAuthStore } from '@/stores/authStore';

/**
 * 거래처 화면 지점 셀렉터 옵션 — 권한 주체별 지점 화이트리스트.
 *
 * 지점 목록은 권한 주체별로 다르므로 queryKey 에 userId 를 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useAccountBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'accounts', 'branches', userId],
    queryFn: getAccountBranches,
  });
}
