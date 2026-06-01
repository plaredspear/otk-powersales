import { Alert, Button } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { useImpersonation } from '@/hooks/useImpersonation';

/**
 * 대행 로그인 중 전역 배너 (Spec #851).
 *
 * 대행 중일 때만 렌더링. 대행 대상 + 관리자 표시 + "관리자로 복귀" 버튼.
 * 실패 알림은 useImpersonation 훅 onError 가 담당.
 */
export default function ImpersonationBanner() {
  const impersonation = useAuthStore((state) => state.impersonation);
  const { stopMutation } = useImpersonation();

  if (!impersonation) {
    return null;
  }

  const handleStop = () => {
    stopMutation.mutate();
  };

  return (
    <Alert
      type="warning"
      banner
      showIcon
      message={
        <span>
          현재 <strong>{impersonation.targetName || `사용자 #${impersonation.targetUserId}`}</strong> 계정으로
          대행 중입니다 (관리자: {impersonation.byName || `#${impersonation.byUserId}`})
        </span>
      }
      action={
        <Button size="small" danger loading={stopMutation.isPending} onClick={handleStop}>
          관리자로 복귀
        </Button>
      }
    />
  );
}
