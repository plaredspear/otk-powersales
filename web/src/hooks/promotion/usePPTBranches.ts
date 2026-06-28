import { useQuery } from '@tanstack/react-query';
import { getPPTBranches } from '@/api/pptMaster';
import { useAuthStore } from '@/stores/authStore';

/**
 * 전문행사조 화면 지점 셀렉터 옵션 (마스터/이력/확정인원 공용).
 *
 * 여사원 일정의 useTeamScheduleBranches 와 동일 산출이나 전문행사조 전용 endpoint
 * (`/api/v1/admin/ppt-masters/branches`) 를 호출한다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function usePPTBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'ppt-masters', 'branches', userId],
    queryFn: getPPTBranches,
  });
}
