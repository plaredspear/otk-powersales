import { useQuery } from '@tanstack/react-query';
import { fetchEmployeeInputCriteriaMasterFormMeta } from '@/api/employeeInputCriteriaMaster';

/**
 * 진열사원 투입기준 마스터 폼(등록/수정 모달) 메타 로드 — 행사마스터 usePromotionFormMeta 와 동일한 패턴.
 *
 * 도메인 목록 queryKey(`['admin','employee-input-criteria-masters']`) 와 분리된 독립 키를 써서
 * 등록/수정/확정/삭제 mutation 의 invalidate 가 정적 성격의 폼 메타까지 재조회하지 않도록 한다.
 */
export function useEmployeeInputCriteriaMasterFormMeta() {
  return useQuery({
    queryKey: ['admin', 'employee-input-criteria-masters', 'form-meta'],
    queryFn: fetchEmployeeInputCriteriaMasterFormMeta,
    staleTime: 10 * 60 * 1000,
  });
}
