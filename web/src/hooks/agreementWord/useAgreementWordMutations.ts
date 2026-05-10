import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createAgreementWord } from '@/api/agreementWord';

/**
 * 약관 등록 mutation 훅. (Spec #658 P2-W)
 *
 * 성공 시 활성 약관 query 캐시 무효화 — 등록 직후에는 `active=false` 라 노출 변동은 없으나,
 * cycle batch (#654) 가 도래일자에 토글하면 후속 새로고침에서 갱신된 활성 약관이 반영된다.
 */
export function useCreateAgreementWord() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createAgreementWord,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'agreement-words', 'active'] });
    },
  });
}
