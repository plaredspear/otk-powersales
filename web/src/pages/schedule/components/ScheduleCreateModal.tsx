import { useEffect, useState } from 'react';
import { Modal, Form, DatePicker, Select, message, Alert } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import EmployeeSelect from './EmployeeSelect';
import AccountSelect from './AccountSelect';
import { useScheduleCreate, useScheduleUpdate } from '@/hooks/schedule/useScheduleCreate';
import type { ScheduleCreateRequest, ScheduleListItem } from '@/api/schedule';

const WORK_TYPE3_OPTIONS = [
  { label: '고정', value: '고정' },
  { label: '격고', value: '격고' },
  { label: '순회', value: '순회' },
];

const WORK_TYPE4_OPTIONS = [
  { label: '상온', value: '상온' },
  { label: '냉동/냉장', value: '냉동/냉장' },
];

const WORK_TYPE5_OPTIONS = [
  { label: '상시', value: '상시' },
  { label: '임시', value: '임시' },
];

export interface ScheduleCreateModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
  /** edit 모드일 때 초기값 + scheduleId. 없으면 create 모드. */
  editTarget?: ScheduleListItem | null;
}

interface FormValues {
  employeeCode?: string;
  accountCode?: string;
  typeOfWork3?: string;
  typeOfWork4?: string;
  typeOfWork5?: string;
  dateRange?: [Dayjs, Dayjs | null];
}

/**
 * UC-02 / UC-03 — 진열사원 스케줄 단건 신규 등록 / 편집 Modal.
 *
 * 레거시 SF 「New」 / 「Edit」 버튼 → 표준 레코드 입력 폼 동등.
 * 검증/자동채움은 backend `POST /api/v1/admin/schedule` (신규) 또는 `PUT /{id}` (편집) 에서 일괄 처리되어
 * 본 컴포넌트는 입력 받아 호출 + 결과 토스트만 담당.
 *
 * 편집 모드에서 confirmed=true 인 레코드를 ADMIN_GRADE 외 사용자가 종료일 외 필드를 변경 시도하면
 * backend 가 `SCHEDULE_EDIT_BLOCKED_AFTER_CONFIRM` (409) 로 차단 — UC-05 정합.
 */
export default function ScheduleCreateModal({ open, onClose, onSuccess, editTarget }: ScheduleCreateModalProps) {
  const [form] = Form.useForm<FormValues>();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const createMutation = useScheduleCreate();
  const updateMutation = useScheduleUpdate();

  const isEdit = editTarget != null;
  const mutation = isEdit ? updateMutation : createMutation;

  useEffect(() => {
    if (!open) return;
    setErrorMessage(null);
    if (isEdit && editTarget) {
      form.setFieldsValue({
        employeeCode: editTarget.employeeCode,
        accountCode: editTarget.accountCode ?? undefined,
        typeOfWork3: editTarget.typeOfWork3 ?? undefined,
        typeOfWork4: undefined, // ScheduleListItem 에 없음 — 사용자 재선택
        typeOfWork5: editTarget.typeOfWork5 ?? undefined,
        dateRange: editTarget.startDate
          ? [dayjs(editTarget.startDate), editTarget.endDate ? dayjs(editTarget.endDate) : null]
          : undefined,
      });
    } else {
      form.resetFields();
    }
  }, [open, isEdit, editTarget, form]);

  const handleOk = async () => {
    setErrorMessage(null);
    try {
      const values = await form.validateFields();
      const [startDayjs, endDayjs] = values.dateRange ?? [];
      if (!startDayjs) {
        setErrorMessage('시작일은 필수입니다');
        return;
      }
      const payload: ScheduleCreateRequest = {
        employeeCode: values.employeeCode!,
        accountCode: values.accountCode!,
        typeOfWork3: values.typeOfWork3!,
        typeOfWork4: values.typeOfWork4!,
        typeOfWork5: values.typeOfWork5!,
        startDate: startDayjs.format('YYYY-MM-DD'),
        endDate: endDayjs ? endDayjs.format('YYYY-MM-DD') : null,
      };
      if (isEdit && editTarget) {
        const result = await updateMutation.mutateAsync({ id: editTarget.id, payload });
        message.success(`스케줄이 수정되었습니다 (사원: ${result.employeeName} / 거래처: ${result.accountName ?? '-'})`);
      } else {
        const result = await createMutation.mutateAsync(payload);
        message.success(`스케줄이 등록되었습니다 (사원: ${result.employeeName} / 거래처: ${result.accountName ?? '-'})`);
      }
      onSuccess();
      onClose();
    } catch (err) {
      if (err && typeof err === 'object' && 'errorFields' in err) {
        // Form validation error — antd 가 자체 표시
        return;
      }
      const msg = err instanceof Error ? err.message : '스케줄 저장에 실패했습니다';
      setErrorMessage(msg);
    }
  };

  return (
    <Modal
      title={isEdit ? '진열사원 스케줄 편집' : '진열사원 스케줄 신규 등록'}
      open={open}
      onCancel={onClose}
      onOk={handleOk}
      okText="저장"
      cancelText="취소"
      confirmLoading={mutation.isPending}
      width={560}
    >
      <Form form={form} layout="vertical" preserve={false}>
        {errorMessage && (
          <Alert type="error" message={errorMessage} style={{ marginBottom: 16 }} closable onClose={() => setErrorMessage(null)} />
        )}
        {isEdit && editTarget?.confirmed && (
          <Alert
            type="warning"
            message="확정된 스케줄입니다. 시스템 관리자 / 영업지원이 아닌 경우 종료일 외 필드 변경 시 저장이 차단됩니다."
            style={{ marginBottom: 16 }}
          />
        )}
        <Form.Item
          name="employeeCode"
          label="사원"
          rules={[{ required: true, message: '사원은 필수입니다' }]}
        >
          <EmployeeSelect
            value={form.getFieldValue('employeeCode')}
            onChange={(code) => form.setFieldValue('employeeCode', code)}
          />
        </Form.Item>

        <Form.Item
          name="accountCode"
          label="거래처"
          rules={[{ required: true, message: '거래처는 필수입니다' }]}
        >
          <AccountSelect
            value={form.getFieldValue('accountCode')}
            onChange={(code) => form.setFieldValue('accountCode', code)}
          />
        </Form.Item>

        <Form.Item
          name="typeOfWork3"
          label="근무형태3"
          rules={[{ required: true, message: '근무형태3은 필수입니다' }]}
        >
          <Select placeholder="고정 / 격고 / 순회" options={WORK_TYPE3_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="typeOfWork4"
          label="근무형태4"
          rules={[{ required: true, message: '근무형태4는 필수입니다' }]}
        >
          <Select placeholder="상온 / 냉동·냉장" options={WORK_TYPE4_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="typeOfWork5"
          label="근무형태5"
          rules={[{ required: true, message: '근무형태5는 필수입니다' }]}
        >
          <Select placeholder="상시 / 임시" options={WORK_TYPE5_OPTIONS} />
        </Form.Item>

        <Form.Item
          name="dateRange"
          label="기간 (시작일 필수 / 종료일 선택)"
          rules={[{ required: true, message: '기간은 필수입니다' }]}
        >
          <DatePicker.RangePicker allowEmpty={[false, true]} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}
