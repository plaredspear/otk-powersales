import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { notification } from 'antd';
import {
  startImpersonation,
  stopImpersonation,
  type ImpersonationStartRequest,
} from '@/api/impersonation';
import { useAuthStore } from '@/stores/authStore';

/**
 * 대행 로그인 시작/종료 훅 (Spec #851).
 *
 * 성공/실패 부수효과를 한 곳에 모은다:
 * - 성공: 토큰 + 사용자 전체 교체(authStore.applyAuth) → query 캐시 초기화(권한 주체 변경) → 홈 이동.
 *   대행 시작/종료는 권한 주체가 통째로 바뀌므로 invalidate 가 아닌 clear 를 쓴다 (logout 과 동일 의미).
 * - 실패: 도메인 메시지 notification (전역 인터셉터 알림과 별개의 도메인 안내).
 */
export function useImpersonation() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const applyAuth = useAuthStore((state) => state.applyAuth);

  const startMutation = useMutation({
    mutationFn: (request: ImpersonationStartRequest) => startImpersonation(request),
    onSuccess: (data) => {
      applyAuth(
        { accessToken: data.accessToken, refreshToken: data.refreshToken },
        data.user,
        {
          byUserId: data.impersonation.impersonatedByUserId,
          byName: data.impersonation.impersonatedByName ?? '',
          targetUserId: data.impersonation.targetUserId,
          targetName: data.impersonation.targetName ?? '',
          startedAt: data.impersonation.startedAt,
        },
      );
      queryClient.clear();
      navigate('/', { replace: true });
    },
    onError: (err) => {
      notification.error({
        message: '대행 로그인 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    },
  });

  const stopMutation = useMutation({
    mutationFn: () => stopImpersonation(),
    onSuccess: (data) => {
      applyAuth(
        { accessToken: data.accessToken, refreshToken: data.refreshToken },
        data.user,
        null,
      );
      queryClient.clear();
      navigate('/', { replace: true });
    },
    onError: (err) => {
      notification.error({
        message: '대행 종료 실패',
        description: (err as Error)?.message ?? '잠시 후 다시 시도해 주세요.',
      });
    },
  });

  return { startMutation, stopMutation };
}
