import { useQuery } from '@tanstack/react-query';
import { fetchEmployeeInputCriteriaMasterListMeta } from '@/api/employeeInputCriteriaMaster';

/**
 * 진열사원 투입기준 마스터 목록 조회 조건 로드 — 행사마스터 usePromotionListMeta 와 동일한 패턴.
 *
 * 상태 필터 옵션과 기본값을 서버에서 단일 출처로 받는다.
 * 본 화면은 지점/권한 의존 조건이 없어 행사마스터와 달리 쿼리 키에 사용자 id 를 포함하지 않는다.
 */
export function useEmployeeInputCriteriaMasterListMeta() {
  return useQuery({
    queryKey: ['admin', 'employee-input-criteria-masters', 'list-meta'],
    queryFn: fetchEmployeeInputCriteriaMasterListMeta,
    staleTime: 10 * 60 * 1000,
  });
}
