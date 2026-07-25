import { useQuery } from '@tanstack/react-query';
import { fetchFemaleEmployeeListMeta } from '@/api/employee';
import { useAuthStore } from '@/stores/authStore';

/**
 * 여사원 현황 목록 화면 조회 조건 로드 — "권한 기반 조건 로드" 표준 패턴 (행사마스터 정합).
 *
 * 지점 셀렉터(권한 의존) + 재직상태 + 근무형태1/3 + 전문행사조 + 텍스트 필터 + 기본값을 한 번에
 * 로드한다. 기존 useFemaleEmployeeBranches 호출 + 화면 하드코딩 옵션을 대체한다.
 *
 * 전용 endpoint(`/api/v1/admin/female-employees/meta`)는 화면의 게이팅 권한(`female_employee`)과
 * 동일하게 가드되므로, 조장 등 여사원 권한만 가진 직책도 접근 가능하다. 지점 목록 자체는 여사원
 * 일정/대시보드/전문행사조와 동일한 backend 화이트리스트(WomenScheduleBranchResolver)에서 나온다.
 *
 * 지점 옵션은 권한 주체별로 다르므로 사용자 id 를 쿼리 키에 포함해 대행 전환 시 캐시를 분리한다.
 */
export function useFemaleEmployeeListMeta() {
  const userId = useAuthStore((state) => state.user?.id);
  return useQuery({
    queryKey: ['admin', 'female-employees', 'list-meta', userId],
    queryFn: fetchFemaleEmployeeListMeta,
    staleTime: 10 * 60 * 1000,
  });
}
