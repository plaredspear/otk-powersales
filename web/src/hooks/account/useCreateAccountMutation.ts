import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createAdminAccount, type AdminAccountCreateRequest } from '@/api/account';

/**
 * 관리자 웹 신규 거래처 등록 mutation. (Spec #640 P2-W)
 *
 * 성공 시 거래처 목록 쿼리(`['admin', 'accounts']`) 를 invalidate 하여
 * 등록 직후 목록에 반영되도록 한다.
 */
export function useCreateAccountMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AdminAccountCreateRequest) => createAdminAccount(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['admin', 'accounts'] });
    },
  });
}
