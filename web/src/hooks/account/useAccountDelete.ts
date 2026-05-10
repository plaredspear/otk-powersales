import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteAdminAccount } from '@/api/account';

/**
 * 관리자 웹 거래처 삭제 mutation. (Spec #642 P2-W)
 *
 * 성공 시 거래처 목록 쿼리(`['admin', 'accounts']`) 를 invalidate 하여
 * 삭제 직후 목록에서 제외되도록 한다. notification / 에러 매핑은 호출 측 (AccountDeleteAction)
 * 에서 처리 — `onError` 의 다른 관리자 선삭제(404) 회복 invalidate 도 호출 측 책임.
 */
export function useAccountDelete() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteAdminAccount(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] });
    },
  });
}
