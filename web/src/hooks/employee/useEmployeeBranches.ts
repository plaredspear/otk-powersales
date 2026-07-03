import { useQuery } from '@tanstack/react-query';
import { getEmployeeBranches } from '@/api/employee';

/**
 * 사원 목록 화면 지점 셀렉터 옵션 — 전 지점(전사) 목록.
 *
 * 사원 목록은 전사 조회이므로 옵션도 전사 지점이다 (권한 주체와 무관 → queryKey 에 userId 불포함).
 */
export function useEmployeeBranches() {
  return useQuery({
    queryKey: ['admin', 'employees', 'branches'],
    queryFn: getEmployeeBranches,
  });
}
