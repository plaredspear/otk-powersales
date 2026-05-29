import { useState } from 'react';
import { Alert, App, Input, Modal, Space, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { useDeletePermissionSet } from '@/hooks/admin/usePermissionSetMutation';
import type { AxiosError } from 'axios';

const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  permissionSetId: number;
  permissionSetName: string;
  permissionSetLabel: string;
  assignedUserCount: number;
  onClose: () => void;
}

/**
 * Spec #837 — PS 삭제 확인 모달.
 *
 * 사용자가 PS 이름을 정확히 입력해야 [삭제] 활성화. assignment 일괄 회수 결과를 안내.
 * SF 출처 PS 는 본 모달에 진입하지 않음 — 호출처 (편집 페이지) 가 [삭제] 버튼 비활성화로 차단.
 */
export default function PermissionSetDeleteConfirmModal({
  open,
  permissionSetId,
  permissionSetName,
  permissionSetLabel,
  assignedUserCount,
  onClose,
}: Props) {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [confirmInput, setConfirmInput] = useState('');
  const del = useDeletePermissionSet();

  const handleClose = () => {
    setConfirmInput('');
    onClose();
  };

  const handleDelete = async () => {
    try {
      await del.mutateAsync(permissionSetId);
      message.success('PermissionSet 이 삭제되었습니다');
      handleClose();
      navigate('/admin/permissions/permission-sets');
    } catch (e) {
      const err = e as AxiosError<{ error?: { code?: string; message?: string } }>;
      const code = err.response?.data?.error?.code;
      if (code === 'SF_ORIGIN_DELETE_BLOCKED') {
        message.error('SF 출처 PermissionSet 은 삭제할 수 없습니다');
      } else {
        message.error(err.response?.data?.error?.message || 'PermissionSet 삭제에 실패했습니다');
      }
    }
  };

  return (
    <Modal
      open={open}
      title="⚠️ PermissionSet 삭제"
      okText="삭제"
      okType="danger"
      cancelText="취소"
      onCancel={handleClose}
      onOk={handleDelete}
      okButtonProps={{
        disabled: confirmInput !== permissionSetName,
        loading: del.isPending,
      }}
    >
      <Space direction="vertical" style={{ width: '100%' }}>
        <Paragraph>
          <Text>이 PermissionSet 을 삭제합니다.</Text>
        </Paragraph>
        <ul>
          <li>
            PS 이름: <Text code>{permissionSetName}</Text>
            {permissionSetLabel !== permissionSetName && (
              <Text type="secondary"> ({permissionSetLabel})</Text>
            )}
          </li>
          <li>부여된 사용자: {assignedUserCount}명</li>
        </ul>
        {assignedUserCount > 0 && (
          <Alert
            type="warning"
            showIcon
            message="부여된 사용자의 권한이 즉시 회수됩니다 (assignment 일괄 hard delete)"
          />
        )}
        <Paragraph style={{ marginTop: 12 }}>
          삭제하시려면 아래에 PS 이름 <Text code>{permissionSetName}</Text> 을 정확히 입력하세요.
        </Paragraph>
        <Input
          placeholder={permissionSetName}
          value={confirmInput}
          onChange={(e) => setConfirmInput(e.target.value)}
          onPressEnter={() => {
            if (confirmInput === permissionSetName && !del.isPending) handleDelete();
          }}
        />
      </Space>
    </Modal>
  );
}
