import { useEffect } from 'react';
import { Alert, DatePicker, Form, Input, Modal, notification } from 'antd';
import type { FormInstance } from 'antd';
import { isAxiosError } from 'axios';
import dayjs, { type Dayjs } from 'dayjs';
import { useCreateAgreementWord } from '@/hooks/agreementWord/useAgreementWordMutations';
import { isApiErrorBody } from '@/api/types';

interface FormValues {
  name: string;
  contents: string;
  afterActiveDate: Dayjs;
}

export interface AdminAgreementWordCreateModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * 관리자 웹 신규 약관 등록 Modal. (Spec #658 P2-W)
 *
 * Backend DTO `AdminAgreementWordCreateRequest` 와 클라이언트 검증 정합:
 * - `name` required + max 80
 * - `contents` required + max 8000
 * - `afterActiveDate` required + 미래 일자 (오늘 이전 비활성)
 * - `active` / `activeDate` 는 UI 미노출 (Backend Service 단 false / null 강제).
 */
export default function AdminAgreementWordCreateModal({
  open,
  onClose,
  onSuccess,
}: AdminAgreementWordCreateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const mutation = useCreateAgreementWord();

  useEffect(() => {
    if (!open) {
      form.resetFields();
      mutation.reset();
    }
    // mutation 인스턴스는 매 렌더에서 새로 생성되지 않으며, reset 만 호출
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const handleSubmit = async () => {
    let values: FormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }

    try {
      await mutation.mutateAsync({
        name: values.name.trim(),
        contents: values.contents,
        afterActiveDate: values.afterActiveDate.format('YYYY-MM-DD'),
      });
      notification.success({
        message: '약관이 등록되었습니다.',
        description: 'cycle batch 가 도래일자에 활성 토글합니다.',
      });
      onSuccess?.();
      onClose();
    } catch (err) {
      handleError(err, form);
    }
  };

  const isSubmitting = mutation.isPending;

  return (
    <Modal
      open={open}
      title="신규 약관 등록"
      okText="등록"
      cancelText="취소"
      okButtonProps={{ loading: isSubmitting }}
      cancelButtonProps={{ disabled: isSubmitting }}
      onOk={handleSubmit}
      onCancel={() => {
        if (!isSubmitting) onClose();
      }}
      maskClosable={!isSubmitting}
      closable={!isSubmitting}
      destroyOnHidden
      width={720}
    >
      <Form<FormValues> form={form} layout="vertical" requiredMark>
        <Form.Item
          label="약관 이름"
          name="name"
          required
          rules={[
            { required: true, message: '약관 이름은 필수입니다' },
            { max: 80, message: '80자 이내여야 합니다' },
          ]}
        >
          <Input placeholder="예: AGR-2026-001" maxLength={80} disabled={isSubmitting} />
        </Form.Item>

        <Form.Item
          label="다음 시행 일자"
          name="afterActiveDate"
          required
          rules={[{ required: true, message: '다음 시행 일자는 필수입니다' }]}
        >
          <DatePicker
            style={{ width: '100%' }}
            disabled={isSubmitting}
            disabledDate={(d) => !d || d.isSame(dayjs(), 'day') || d.isBefore(dayjs(), 'day')}
          />
        </Form.Item>

        <Form.Item
          label="약관 본문"
          name="contents"
          required
          rules={[
            { required: true, message: '약관 본문은 필수입니다' },
            { max: 8000, message: '8000자 이내여야 합니다' },
          ]}
        >
          <Input.TextArea rows={10} maxLength={8000} showCount disabled={isSubmitting} />
        </Form.Item>

        <Alert
          type="info"
          showIcon
          message="활성 상태 / 활성 일자는 본 화면에서 입력할 수 없습니다."
          description="등록된 약관은 cycle batch (#654) 가 다음 시행 일자에 자동으로 활성 토글합니다. 이 화면에서는 약관 텍스트와 시행 일자만 등록합니다."
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
      const message = body.error!.message;
      notification.error({ message: '약관 등록 실패', description: message });
      // Backend 가 필드별 메시지를 명확히 분리하지 않으므로 form 전체 메시지로 노출
      form.setFields([{ name: 'name', errors: [message] }]);
      return;
    }
    if (status === 401) {
      // axios interceptor 가 로그인 리다이렉트 처리
      return;
    }
    if (status === 403) {
      notification.error({ message: '약관 등록 실패', description: '등록 권한이 없습니다.' });
      return;
    }
    if (status && status >= 500) {
      notification.error({
        message: '약관 등록 실패',
        description: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
      });
      return;
    }
  }
  notification.error({
    message: '약관 등록 실패',
    description: err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
  });
}
