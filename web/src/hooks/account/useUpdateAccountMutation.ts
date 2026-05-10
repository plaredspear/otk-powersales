import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateAdminAccount, type AdminAccountUpdateRequest } from '@/api/account';

/**
 * 관리자 웹 거래처 수정 mutation. (Spec #643 P2-W)
 *
 * 성공 시 거래처 목록 쿼리(`['admin', 'accounts']`) 를 invalidate 하여
 * 수정 직후 목록에 반영되도록 한다.
 */
export function useUpdateAccountMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AdminAccountUpdateRequest }) =>
      updateAdminAccount(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] });
    },
  });
}
