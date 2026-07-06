import { Modal, Form, Select, notification, Alert } from 'antd';
import type { EmployeeDetail } from '@/api/employee';
import { useUpdateEmployeeRole } from '@/hooks/employee/useEmployee';
import { APP_AUTHORITY_OPTIONS, type AppAuthority } from '@/constants/userRole';

interface EmployeeRoleModalProps {
  employee: EmployeeDetail;
  open: boolean;
  onClose: () => void;
}

interface FormValues {
  role: AppAuthority;
}

const ROLE_SELECT_OPTIONS = APP_AUTHORITY_OPTIONS.map((opt) => ({
  value: opt.value,
  label: opt.label,
}));

/**
 * 사원 권한(role) 전용 수정 모달.
 *
 * 일반 수정([EmployeeEditModal]) 은 origin=SAP 사원을 차단하지만, 권한 필드는 SAP 인입이
 * 갱신하지 않아 경합하지 않으므로 origin 과 무관하게 변경할 수 있다. AccountViewAll(전체
 * 거래처 조회 권한) 은 SAP 발령으로 산출되지 않아, 이 경로가 부여하는 유일한 수단이다.
 */
export default function EmployeeRoleModal({ employee, open, onClose }: EmployeeRoleModalProps) {
  const [form] = Form.useForm<FormValues>();
  const mutation = useUpdateEmployeeRole();

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      await mutation.mutateAsync({ employeeId: employee.id, role: values.role });
      notification.success({ message: '사원 권한이 수정되었습니다' });
      onClose();
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        // validation 실패 — antd 가 자동으로 표시
        return;
      }
      notification.error({
        message: '사원 권한 수정 실패',
        description: err instanceof Error ? err.message : '알 수 없는 오류',
      });
    }
  };

  return (
    <Modal
      title={`권한 변경 — ${employee.name} (${employee.employeeCode})`}
      open={open}
      onOk={handleSubmit}
      onCancel={onClose}
      okText="저장"
      cancelText="취소"
      width={480}
      confirmLoading={mutation.isPending}
      destroyOnHidden
    >
      <Alert
        type="info"
        showIcon
        message="권한은 SAP 원천 사원도 변경할 수 있습니다"
        description="권한(App권한) 은 SAP 인입이 갱신하지 않는 항목이라 여기서 변경해도 SAP 인입과 충돌하지 않습니다. 'AccountViewAll(영업부장)' 은 전체 거래처 조회 권한으로, SAP 발령으로는 부여되지 않아 이 화면에서만 지정할 수 있습니다."
        style={{ marginBottom: 16 }}
      />
      <Form
        form={form}
        layout="vertical"
        initialValues={{ role: employee.role ?? undefined }}
      >
        <Form.Item
          name="role"
          label="권한"
          rules={[{ required: true, message: '권한을 선택해 주세요' }]}
        >
          <Select options={ROLE_SELECT_OPTIONS} placeholder="선택" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
