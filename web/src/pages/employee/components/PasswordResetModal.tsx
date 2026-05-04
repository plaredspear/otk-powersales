import { Alert, Modal, Typography, notification } from 'antd';
import type { Employee } from '@/api/employee';
import { useResetPassword } from '@/hooks/employee/useEmployeeCredentialMutation';
import { mapCredentialErrorMessage } from './credentialErrorMapping';

const { Paragraph, Text } = Typography;

const TEMPORARY_PASSWORD = '1234';

interface PasswordResetModalProps {
  employee: Employee;
  open: boolean;
  onClose: () => void;
}

export default function PasswordResetModal({ employee, open, onClose }: PasswordResetModalProps) {
  const mutation = useResetPassword();

  const handleConfirm = async () => {
    try {
      await mutation.mutateAsync(employee.id);
      notification.success({
        message: '비밀번호가 초기화되었습니다.',
        description: `임시 비밀번호 '${TEMPORARY_PASSWORD}' 를 사원에게 전달해 주세요. 사원은 다음 로그인 시 비밀번호 변경을 요구받습니다.`,
        duration: 10,
      });
      onClose();
    } catch (err) {
      notification.error({
        message: '비밀번호 초기화 실패',
        description: mapCredentialErrorMessage(err),
      });
    }
  };

  return (
    <Modal
      title="비밀번호 초기화 확인"
      open={open}
      onOk={handleConfirm}
      onCancel={onClose}
      okText="초기화 실행"
      cancelText="취소"
      okButtonProps={{ danger: true }}
      confirmLoading={mutation.isPending}
      destroyOnClose
    >
      <Paragraph>다음 사원의 비밀번호를 임시 비밀번호로 초기화합니다.</Paragraph>
      <Paragraph>
        <Text strong>사번:</Text> {employee.employeeCode}
        <br />
        <Text strong>이름:</Text> {employee.name}
      </Paragraph>
      <Alert
        type="info"
        showIcon
        message="임시 비밀번호"
        description={
          <>
            <Text strong copyable style={{ fontSize: 20 }}>
              {TEMPORARY_PASSWORD}
            </Text>
            <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0 }}>
              사원 본인에게 별도 전달해 주세요.
            </Paragraph>
          </>
        }
        style={{ marginBottom: 12 }}
      />
      <Paragraph type="secondary">
        사원은 다음 로그인 시 비밀번호 변경 화면으로 자동 이동합니다.
      </Paragraph>
    </Modal>
  );
}
