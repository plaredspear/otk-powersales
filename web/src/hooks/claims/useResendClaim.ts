import { useMutation, useQueryClient } from '@tanstack/react-query';
import { resendClaim, type AdminClaimCreateResult } from '@/api/claims';

/**
 * Spec #829: SF 재전송 (claim.sfSendStatus == SEND_FAILED 일 때만 허용).
 */
export function useResendClaim() {
  const queryClient = useQueryClient();
  return useMutation<AdminClaimCreateResult, Error, number>({
    mutationFn: resendClaim,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'claims'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'claims', 'detail', data.claimId] });
    },
  });
}
