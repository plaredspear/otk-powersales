import { useQuery } from '@tanstack/react-query';
import { fetchClaimDetail } from '@/api/claims';

export function useClaimDetail(claimId: number) {
  return useQuery({
    queryKey: ['admin', 'claims', claimId],
    queryFn: () => fetchClaimDetail(claimId),
    enabled: claimId > 0,
  });
}
