import { useMutation, useQueryClient } from '@tanstack/react-query';
import { registerAdminAccount } from '@/api/admin/registerAdminAccount';
import type { AdminAccountRegisterRequest } from '@/api/admin/types';

/**
 * 시스템 관리자 수동 등록 mutation 훅 (Spec #579).
 *
 * 성공 시 직원 목록 쿼리(`['admin', 'employees']`)를 invalidate 하여
 * 등록 직후 직원 목록에 반영되도록 한다.
 */
export function useRegisterAdminAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AdminAccountRegisterRequest) =>
      registerAdminAccount(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'employees'] });
    },
  });
}
