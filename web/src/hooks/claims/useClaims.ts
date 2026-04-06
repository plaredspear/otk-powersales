import { useQuery } from '@tanstack/react-query';
import { fetchClaims, type ClaimListParams } from '@/api/claims';

export function useClaims(params: ClaimListParams) {
  return useQuery({
    queryKey: ['admin', 'claims', params.start_date, params.end_date, params.status, params.employee_name, params.store_name, params.page, params.size],
    queryFn: () => fetchClaims(params),
  });
}
