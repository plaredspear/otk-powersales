import { useQuery } from '@tanstack/react-query';
import { fetchActiveAgreementWord } from '@/api/agreementWord';

/**
 * 활성 약관 단일 조회 query 훅. (Spec #658 P2-W)
 *
 * `/admin/agreement-words` 화면 진입 시 미리보기 카드용. 응답이 `null` 이면 활성 약관 부재 안내.
 */
export function useActiveAgreementWord() {
  return useQuery({
    queryKey: ['admin', 'agreement-words', 'active'],
    queryFn: fetchActiveAgreementWord,
  });
}
