import { useQuery } from '@tanstack/react-query';
import { fetchScheduleListMeta } from '@/api/schedule';
import { useAuthStore } from '@/stores/authStore';

/**
 * 진열스케줄마스터 목록 화면 조회 조건 로드 — "권한 기반 조건 로드" 표준 패턴.
 *
 * 지점 셀렉터(권한 의존) + 근무유형3 + 확정상태 + 텍스트/날짜 필터 + 기본값을 한 번에 로드한다.
 * 기존 useScheduleBranches + web 하드코딩(근무유형3/확정상태 options)을 대체한다.
 *
 * 지점 옵션은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useScheduleListMeta() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'schedule', 'list-meta', userId],
    queryFn: fetchScheduleListMeta,
    staleTime: 10 * 60 * 1000,
  });
}
