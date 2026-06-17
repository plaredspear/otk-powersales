import { useQuery } from '@tanstack/react-query';
import { fetchTeamScheduleBranches } from '@/api/team-schedule';
import { useAuthStore } from '@/stores/authStore';

export function useTeamScheduleBranches() {
  // 지점 목록은 권한 주체(사용자)별로 다르다. 대행 시작/종료로 주체가 바뀌면
  // 캐시 엔트리도 분리되도록 사용자 id 를 쿼리 키에 포함한다 (queryClient.clear() 누락 경로 대비).
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'team-schedule', 'branches', userId],
    queryFn: fetchTeamScheduleBranches,
  });
}
