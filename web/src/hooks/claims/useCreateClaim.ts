import { useMutation } from '@tanstack/react-query';
import { createClaim, type AdminClaimCreateInput, type AdminClaimCreateResult } from '@/api/claims';

/**
 * Spec #829: Web admin 클레임 등록 (dual-write).
 *
 * 응답의 `status` 가 SENT / SEND_FAILED 둘 다 backend.claim 적재 성공을 의미하므로
 * caller (페이지) 는 toast 표시만 분기. mutation 자체는 5xx 또는 422 에서만 onError 진입.
 */
export function useCreateClaim() {
  return useMutation<AdminClaimCreateResult, Error, AdminClaimCreateInput>({
    mutationFn: createClaim,
  });
}
