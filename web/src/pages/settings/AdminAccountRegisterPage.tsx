import { useCallback } from 'react';
import { Button, Form, Modal, Space, Typography, notification } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { AxiosError } from 'axios';
import AdminAccountRegisterForm, {
  type AdminAccountRegisterFormValues,
} from './components/AdminAccountRegisterForm';
import { useRegisterAdminAccount } from '@/hooks/useRegisterAdminAccount';

const { Title } = Typography;

interface RawFormValues {
  employeeCodeBody: string;
  name: string;
  password: string;
  passwordConfirm: string;
  workEmail?: string;
  workPhone?: string;
  orgName?: string;
  costCenterCode?: string;
}

interface ApiError {
  success?: boolean;
  error?: { code: string; message: string };
}

const ERROR_FIELD_MAP: Record<string, keyof RawFormValues | null> = {
  INVALID_EMPLOYEE_CODE_FORMAT: 'employeeCodeBody',
  EMPLOYEE_CODE_DUPLICATED: 'employeeCodeBody',
  PASSWORD_CONFIRM_MISMATCH: 'passwordConfirm',
  PASSWORD_POLICY_VIOLATION: 'password',
};

const SERVER_ERROR_MESSAGE: Record<string, string> = {
  INVALID_EMPLOYEE_CODE_FORMAT: '사번 형식이 올바르지 않습니다',
  EMPLOYEE_CODE_DUPLICATED: '이미 사용 중인 사번입니다',
  PASSWORD_CONFIRM_MISMATCH: '비밀번호가 일치하지 않습니다',
  PASSWORD_POLICY_VIOLATION: '비밀번호 정책을 만족하지 않습니다',
};

export default function AdminAccountRegisterPage() {
  const navigate = useNavigate();
  const [form] = Form.useForm<RawFormValues>();
  const mutation = useRegisterAdminAccount();

  const handleCancel = useCallback(() => {
    const isDirty = form.isFieldsTouched();
    const goBack = () => navigate('/settings/permissions/employees');
    if (!isDirty) {
      goBack();
      return;
    }
    Modal.confirm({
      title: '입력 중인 내용이 있습니다',
      content: '페이지를 이탈하면 입력 내용이 사라집니다. 계속하시겠습니까?',
      okText: '이탈',
      okButtonProps: { danger: true },
      cancelText: '머무르기',
      onOk: goBack,
    });
  }, [form, navigate]);

  const handleSubmit = async (values: AdminAccountRegisterFormValues) => {
    try {
      await mutation.mutateAsync({
        employee_code: values.employeeCode,
        name: values.name,
        password: values.password,
        password_confirm: values.passwordConfirm,
        work_email: values.workEmail || null,
        work_phone: values.workPhone || null,
        org_name: values.orgName || null,
        cost_center_code: values.costCenterCode || null,
      });
      notification.success({
        message: '관리자 계정이 등록되었습니다',
      });
      navigate('/settings/permissions/employees');
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleApiError = (err: unknown) => {
    if (err instanceof AxiosError) {
      const status = err.response?.status;
      const data = err.response?.data as ApiError | undefined;
      const code = data?.error?.code;
      const message = data?.error?.message;

      if (status === 403) {
        notification.error({
          message: '권한 없음',
          description: '이 작업을 수행할 권한이 없습니다',
        });
        navigate(-1);
        return;
      }

      if (code && ERROR_FIELD_MAP[code]) {
        const field = ERROR_FIELD_MAP[code];
        if (field) {
          form.setFields([
            {
              name: field,
              errors: [SERVER_ERROR_MESSAGE[code] ?? message ?? '입력값을 확인해주세요'],
            },
          ]);
          return;
        }
      }

      if (status && status >= 500) {
        // 500번대는 axios 인터셉터가 글로벌 토스트 처리 — 추가 처리 불필요
        return;
      }

      notification.error({
        message: '관리자 등록 실패',
        description: message ?? '요청을 처리하지 못했습니다',
      });
      return;
    }

    notification.error({
      message: '관리자 등록 실패',
      description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다',
    });
  };

  return (
    <div style={{ padding: 16 }}>
      <Space align="center" style={{ marginBottom: 16 }}>
        <Button
          icon={<ArrowLeftOutlined />}
          type="text"
          onClick={() => navigate('/settings/permissions/employees')}
        >
          직원 권한 관리
        </Button>
        <Title level={4} style={{ margin: 0 }}>
          시스템 관리자 등록
        </Title>
      </Space>

      <div style={{ maxWidth: 600 }}>
        <AdminAccountRegisterForm
          form={form}
          isSubmitting={mutation.isPending}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
        />
      </div>
    </div>
  );
}
