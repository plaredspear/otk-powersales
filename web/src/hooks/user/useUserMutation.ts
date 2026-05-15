import { useMutation, useQueryClient } from '@tanstack/react-query';
import { resetUserPassword, updateUserActiveStatus } from '@/api/user';

/**
 * web admin User 비밀번호 임시 리셋 mutation.
 *
 * onSuccess 시 `['admin', 'users']` prefix 의 모든 query (목록 + 상세) 를 invalidate.
 */
export function useResetUserPassword() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => resetUserPassword(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

/**
 * web admin User 활성/비활성 토글 mutation.
 *
 * 성공 시 목록/상세 모두 invalidate. 자기 자신 비활성화는 backend 단에서 차단된다.
 */
export function useUpdateUserActiveStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, isActive }: { id: number; isActive: boolean }) =>
      updateUserActiveStatus(id, isActive),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}
