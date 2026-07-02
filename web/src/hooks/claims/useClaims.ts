import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { fetchClaims, type ClaimListParams } from '@/api/claims';

export function useClaims(params: ClaimListParams) {
  return useQuery({
    queryKey: ['admin', 'claims', params.startDate, params.endDate, params.status, params.employeeName, params.storeName, params.page, params.size],
    queryFn: () => fetchClaims(params),
    // 페이지/조건 변경 시 직전 데이터를 유지해 테이블 깜빡임(빈 상태 노출)을 방지.
    placeholderData: keepPreviousData,
  });
}
