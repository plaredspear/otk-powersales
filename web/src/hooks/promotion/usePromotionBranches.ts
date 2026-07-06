import { useQuery } from '@tanstack/react-query';
import { fetchPromotionBranches } from '@/api/promotion';
import { useAuthStore } from '@/stores/authStore';

/**
 * 행사마스터 목록 화면 지점 셀렉터 옵션.
 *
 * 목표 대비 실적 보고서 지점 셀렉터와 동일 산출(권한별 지점 화이트리스트)이나
 * 행사마스터 목록 전용 endpoint(`/api/v1/admin/promotions/branches`)를 호출한다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function usePromotionBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'promotions', 'branches', userId],
    queryFn: fetchPromotionBranches,
  });
}
