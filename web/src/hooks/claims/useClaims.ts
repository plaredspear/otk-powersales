import { useQuery } from '@tanstack/react-query';
import { fetchClaims, type ClaimListParams } from '@/api/claims';

export function useClaims(params: ClaimListParams) {
  return useQuery({
    queryKey: ['admin', 'claims', params.startDate, params.endDate, params.status, params.employeeName, params.storeName, params.page, params.size],
    queryFn: () => fetchClaims(params),
  });
}
