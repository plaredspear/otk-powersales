import { useQuery } from '@tanstack/react-query';
import { getPPTBranches } from '@/api/pptMaster';
import { useAuthStore } from '@/stores/authStore';

/**
 * 여사원 현황 화면 지점 셀렉터 옵션.
 *
 * 여사원 현황 / 전문행사조 / 여사원 일정은 backend 의 동일한 지점 화이트리스트
 * (WomenScheduleBranchResolver) 를 공유하므로, 전문행사조 전용 endpoint
 * (`/api/v1/admin/ppt-masters/branches`) 를 그대로 재사용한다. 캐시 키를
 * usePPTBranches 와 동일하게 두어 두 화면이 결과를 공유하도록 한다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useFemaleEmployeeBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'ppt-masters', 'branches', userId],
    queryFn: getPPTBranches,
  });
}
