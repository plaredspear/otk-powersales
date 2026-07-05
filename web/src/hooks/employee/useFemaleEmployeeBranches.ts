import { useQuery } from '@tanstack/react-query';
import { fetchFemaleEmployeeBranches } from '@/api/employee';
import { useAuthStore } from '@/stores/authStore';

/**
 * 여사원 현황 화면 지점 셀렉터 옵션.
 *
 * 여사원 현황 전용 endpoint(`/api/v1/admin/female-employees/branches`)를 호출한다. 이 endpoint 는
 * 화면의 게이팅 권한(`female_employee`)과 동일하게 가드되므로, 조장 등 여사원 권한만 가진 직책도
 * 접근 가능하다. 지점 목록 자체는 여사원 일정/대시보드/전문행사조와 동일한 backend 화이트리스트
 * (WomenScheduleBranchResolver)에서 나온다.
 *
 * 지점 목록은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useFemaleEmployeeBranches() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'female-employees', 'branches', userId],
    queryFn: fetchFemaleEmployeeBranches,
  });
}
