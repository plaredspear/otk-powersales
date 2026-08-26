import { useMutation, useQueryClient } from '@tanstack/react-query';
import { resetUserPassword, updateUserActiveStatus, updateUserProfile } from '@/api/user';

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

/**
 * web admin User 프로파일 수동 변경 mutation — 시스템 관리자 전용.
 *
 * 성공 시 목록/상세를 invalidate 한다. 대상자의 권한 캐시는 backend 가 변경 시점에 버리므로
 * 프론트에서 별도 처리할 것은 없다.
 */
export function useUpdateUserProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, profileId }: { id: number; profileId: number }) =>
      updateUserProfile(id, profileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}
