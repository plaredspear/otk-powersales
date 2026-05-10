import { Modal, Typography, notification } from 'antd';
import type { Employee } from '@/api/employee';
import { useResetDevice } from '@/hooks/employee/useEmployeeCredentialMutation';
import { mapCredentialErrorMessage } from './credentialErrorMapping';

const { Paragraph, Text } = Typography;

interface DeviceResetModalProps {
  employee: Employee;
  open: boolean;
  onClose: () => void;
}

export default function DeviceResetModal({ employee, open, onClose }: DeviceResetModalProps) {
  const mutation = useResetDevice();

  const handleConfirm = async () => {
    try {
      await mutation.mutateAsync(employee.id);
      notification.success({
        message: '단말이 초기화되었습니다.',
        description: '사원이 다음 로그인 시 새 단말로 자동 등록됩니다.',
      });
      onClose();
    } catch (err) {
      notification.error({
        message: '단말 초기화 실패',
        description: mapCredentialErrorMessage(err),
      });
    }
  };

  return (
    <Modal
      title="단말 초기화 확인"
      open={open}
      onOk={handleConfirm}
      onCancel={onClose}
      okText="초기화 실행"
      cancelText="취소"
      okButtonProps={{ danger: true }}
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      <Paragraph>다음 사원의 단말 바인딩을 초기화합니다.</Paragraph>
      <Paragraph>
        <Text strong>사번:</Text> {employee.employeeCode}
        <br />
        <Text strong>이름:</Text> {employee.name}
      </Paragraph>
      <Paragraph type="secondary">
        처리 후 사원이 모바일 앱에 다시 로그인하면 새 단말로 자동 등록됩니다.
      </Paragraph>
    </Modal>
  );
}
