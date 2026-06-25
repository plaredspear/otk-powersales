import { useMutation } from '@tanstack/react-query';
import { createClaim, type AdminClaimCreateInput, type AdminClaimCreateResult } from '@/api/claims';

/**
 * Spec #829: Web admin 클레임 등록.
 *
 * 등록 응답 `status` 는 SF_PENDING(전송대기) — SF 송신은 backend 가 커밋 후 비동기로 처리한다.
 * caller (페이지) 는 "등록되었습니다" toast 만 띄운다. mutation 자체는 5xx 또는 422 에서만 onError 진입.
 */
export function useCreateClaim() {
  return useMutation<AdminClaimCreateResult, Error, AdminClaimCreateInput>({
    mutationFn: createClaim,
  });
}
