import { useQuery } from '@tanstack/react-query';
import { fetchFemaleEmployeeFormMeta } from '@/api/employee';

/**
 * 여사원 상세 폼(수정 모달) 렌더링용 메타 로드 — "form 전용 API 분리" 표준 패턴.
 *
 * 재직상태 + 권한 + 전문행사조 Select 옵션을 서버 단일 출처로 로드해, 화면 하드코딩 상수를 대체한다.
 *
 * 목록 조건 로드([useFemaleEmployeeListMeta]) 와 두 가지가 다르다:
 * - 권한 의존 옵션(지점 셀렉터) 이 없어 전 사용자 동일 응답이라 쿼리 키에 userId 를 넣지 않는다.
 * - 상세 진입이 아니라 **모달을 여는 시점**에만 조회한다 (`enabled` 로 제어).
 *
 * @param enabled 모달이 열려 있을 때만 true — 상세 진입만으로는 호출하지 않는다.
 */
export function useFemaleEmployeeFormMeta(enabled: boolean) {
  return useQuery({
    queryKey: ['admin', 'female-employees', 'form-meta'],
    queryFn: fetchFemaleEmployeeFormMeta,
    enabled,
    staleTime: 10 * 60 * 1000,
  });
}
