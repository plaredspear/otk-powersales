import { useEffect, useMemo, useState } from 'react';
import { Alert, Form, Input, Modal, notification } from 'antd';
import type { FormInstance } from 'antd';
import { isAxiosError } from 'axios';
import { useCreateAccountMutation } from '@/hooks/account/useCreateAccountMutation';
import { isApiErrorBody } from '@/api/types';
import type { Employee } from '@/api/employee';
import EmployeeSelect from './components/EmployeeSelect';

/**
 * 관리자 웹 신규 거래처 등록 모달. (Spec #640 P2-W)
 *
 * Backend `AccountNamePrefix.ALLOWED` 와 동일하게 유지. 운영 변경 시 Backend Constants 와
 * 동시 갱신 (`account/policy/AccountNamePrefix.kt`).
 */
const ACCOUNT_NAME_PREFIXES = ['(신규)', '(기타)'] as const;

const PREFIX_DISPLAY = ACCOUNT_NAME_PREFIXES.join(' / ');
const PREFIX_GUIDE = `※ ${PREFIX_DISPLAY} 중 1개를 거래처명에 포함해 주세요.`;

interface FormValues {
  name: string;
}

export interface AdminAccountCreateModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

function nameContainsPrefix(name: string): boolean {
  const trimmed = name.trim();
  return ACCOUNT_NAME_PREFIXES.some((prefix) => trimmed.includes(prefix));
}

export default function AdminAccountCreateModal({ open, onClose, onSuccess }: AdminAccountCreateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const [employeeCode, setEmployeeCode] = useState<string | undefined>();
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | undefined>();
  const [employeeError, setEmployeeError] = useState<string | undefined>();
  const mutation = useCreateAccountMutation();

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setEmployeeCode(undefined);
      setSelectedEmployee(undefined);
      setEmployeeError(undefined);
      mutation.reset();
    }
    // mutation 인스턴스는 매 렌더에서 새로 생성되지 않으며, reset 만 호출
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const submitDisabled = !employeeCode;

  const previewBranchCode = selectedEmployee?.costCenterCode ?? '-';
  const previewBranchName = selectedEmployee?.orgName ?? '-';

  const handleSubmit = async () => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    if (!employeeCode) {
      setEmployeeError('담당 영업사원을 선택해 주세요.');
      return;
    }
    setEmployeeError(undefined);

    try {
      const created = await mutation.mutateAsync({
        name: values.name.trim(),
        employeeCode,
      });
      notification.success({
        message: '거래처가 등록되었습니다.',
        description: created.name,
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      handleError(err, form);
    }
  };

  const isSubmitting = mutation.isPending;

  const employeeFeedback = useMemo(() => {
    if (employeeError) return employeeError;
    if (!selectedEmployee) return undefined;
    return undefined;
  }, [employeeError, selectedEmployee]);

  return (
    <Modal
      open={open}
      title="신규 거래처 등록"
      okText="등록"
      cancelText="취소"
      okButtonProps={{ disabled: submitDisabled, loading: isSubmitting }}
      cancelButtonProps={{ disabled: isSubmitting }}
      onOk={handleSubmit}
      onCancel={() => {
        if (!isSubmitting) onClose();
      }}
      maskClosable={!isSubmitting}
      closable={!isSubmitting}
      destroyOnHidden
    >
      <Form<FormValues>
        form={form}
        layout="vertical"
        requiredMark
        initialValues={{ name: '' }}
      >
        <Form.Item
          label="거래처명"
          name="name"
          required
          extra={PREFIX_GUIDE}
          rules={[
            { required: true, message: '거래처명을 입력해 주세요.' },
            { max: 255, message: '거래처명은 255자 이하여야 합니다.' },
            {
              validator: (_rule, value: string | undefined) => {
                if (!value || value.trim().length === 0) return Promise.resolve();
                if (!nameContainsPrefix(value)) {
                  return Promise.reject(new Error(`거래처명에 ${PREFIX_DISPLAY} 중 1개를 포함해 주세요.`));
                }
                return Promise.resolve();
              },
            },
          ]}
        >
          <Input placeholder="(신규) 강남점" maxLength={255} disabled={isSubmitting} />
        </Form.Item>

        <Form.Item
          label="담당 영업사원"
          required
          validateStatus={employeeError ? 'error' : undefined}
          help={employeeFeedback}
          extra="※ 선택 시 지점 코드 / 지점명이 자동 채움됩니다."
        >
          <EmployeeSelect
            value={employeeCode}
            disabled={isSubmitting}
            onChange={(code, emp) => {
              setEmployeeCode(code);
              setSelectedEmployee(emp);
              if (code) setEmployeeError(undefined);
            }}
          />
        </Form.Item>

        <Alert
          type="info"
          showIcon
          message="자동 채움 미리보기"
          description={
            <div style={{ lineHeight: 1.8 }}>
              <div>지점 코드 : {previewBranchCode}</div>
              <div>지점명 : {previewBranchName}</div>
              <div>거래처 그룹 : 9999 (영업사원 직접 등록)</div>
            </div>
          }
        />
      </Form>
    </Modal>
  );
}

function handleError(err: unknown, form: FormInstance<FormValues>): void {
  if (isAxiosError(err)) {
    const status = err.response?.status;
    const body = err.response?.data;
    if (isApiErrorBody(body)) {
      const code = body.error!.code;
      const message = body.error!.message;
      switch (code) {
        case 'ACCOUNT_NAME_DUPLICATE':
          notification.error({ message: '거래처 등록 실패', description: message });
          form.setFields([{ name: 'name', errors: [message] }]);
          return;
        case 'ACCOUNT_NAME_PREFIX_REQUIRED':
        case 'ACCOUNT_NAME_BLANK':
          form.setFields([{ name: 'name', errors: [message] }]);
          notification.error({ message: '거래처 등록 실패', description: message });
          return;
        case 'EMPLOYEE_CODE_BLANK':
        case 'EMPLOYEE_NOT_FOUND':
          notification.error({ message: '거래처 등록 실패', description: message });
          return;
        default:
          notification.error({ message: '거래처 등록 실패', description: message });
          return;
      }
    }
    if (status === 401) {
      // axios interceptor 가 로그인 리다이렉트 처리 — 추가 안내 생략
      return;
    }
    if (status === 403) {
      notification.error({ message: '거래처 등록 실패', description: '등록 권한이 없습니다.' });
      return;
    }
    if (status && status >= 500) {
      notification.error({ message: '거래처 등록 실패', description: '등록 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.' });
      return;
    }
  }
  notification.error({
    message: '거래처 등록 실패',
    description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
  });
}
