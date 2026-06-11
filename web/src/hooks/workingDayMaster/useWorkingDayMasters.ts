import { useQuery } from '@tanstack/react-query';
import { fetchWorkingDayMasters } from '@/api/workingDayMaster';

/**
 * 영업일관리마스터 조회 훅. (`/admin/working-day-masters`)
 *
 * 지정 연-월의 영업일 달력을 조회한다(조회 전용). 운영이 관리하는 SF 마스터라 자주 바뀌지 않아 길게 캐시한다.
 */
export function useWorkingDayMasters(year: number, month: number) {
  return useQuery({
    queryKey: ['admin', 'working-day-masters', year, month] as const,
    queryFn: () => fetchWorkingDayMasters(year, month),
    staleTime: 5 * 60 * 1000,
  });
}
