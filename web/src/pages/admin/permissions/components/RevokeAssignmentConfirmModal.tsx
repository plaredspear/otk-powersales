import { Alert, App, Modal, Typography } from 'antd';
import { useRevokeAssignment } from '@/hooks/admin/useAdminPermission';
import type { AxiosError } from 'axios';

const { Paragraph } = Typography;

interface Props {
  open: boolean;
  assignmentId: number;
  permissionSetLabel: string;
  userLabel: string;
  onClose: () => void;
}

/**
 * Spec #804 — Assignment 회수 확인 모달.
 *
 * 회수 후 사용자는 다음 로그인 시점부터 권한이 회수됨 (active session 미반영) 안내.
 */
export default function RevokeAssignmentConfirmModal({
  open,
  assignmentId,
  permissionSetLabel,
  userLabel,
  onClose,
}: Props) {
  const { message } = App.useApp();
  const revoke = useRevokeAssignment();

  const handleConfirm = async () => {
    try {
      await revoke.mutateAsync(assignmentId);
      message.success('회수되었습니다');
      onClose();
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      const errorCode = err.response?.data?.error?.code;
      if (errorCode === 'CANNOT_REVOKE_SELF') {
        message.error('자기 자신의 MANAGE_USERS 권한은 회수할 수 없습니다');
      } else if (errorCode === 'LAST_ADMIN_GUARD') {
        message.error('회수 후 MANAGE_USERS 권한 보유 사용자가 없습니다. 최소 1명 admin 필요');
      } else {
        message.error(err.response?.data?.error?.message || '회수에 실패했습니다');
      }
    }
  };

  return (
    <Modal
      open={open}
      title="권한 회수 확인"
      okText="회수"
      okType="danger"
      cancelText="취소"
      onOk={handleConfirm}
      confirmLoading={revoke.isPending}
      onCancel={onClose}
    >
      <Paragraph>
        PermissionSet <b>{permissionSetLabel}</b> 부여를 사용자 <b>{userLabel}</b> 에게서 회수합니다.
      </Paragraph>
      <Alert
        type="info"
        showIcon
        message="사용자는 다음 로그인 시점부터 본 권한이 회수됩니다 (현재 active session 은 즉시 반영되지 않습니다)."
      />
    </Modal>
  );
}
