import { useEffect, useState } from 'react';
import { Alert, Form, Input, message, Modal, Select } from 'antd';
import type { TeamMember, TeamScheduleAccount } from '@/api/team-schedule';
import { useCreateTeamSchedule } from '@/hooks/team-schedule/useTeamScheduleMutation';

interface ScheduleCreateModalProps {
  open: boolean;
  onClose: () => void;
  date: string;
  members: TeamMember[];
  accounts: TeamScheduleAccount[];
}

const WORKING_TYPE_OPTIONS = [
  { value: '근무', label: '근무' },
  { value: '연차', label: '연차' },
  { value: '대휴', label: '대휴' },
];

const CATEGORY1_OPTIONS = [
  { value: '진열', label: '진열' },
  { value: '행사', label: '행사' },
];

const CATEGORY2_OPTIONS = [
  { value: '전담', label: '전담' },
  { value: '진열겸임', label: '진열겸임' },
];

const CATEGORY3_OPTIONS = [
  { value: '고정', label: '고정' },
  { value: '격고', label: '격고' },
  { value: '순회', label: '순회' },
];

export function ScheduleCreateModal({
  open,
  onClose,
  date,
  members,
  accounts,
}: ScheduleCreateModalProps) {
  const [form] = Form.useForm();
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateTeamSchedule();

  const workingType = Form.useWatch('workingType', form);
  const isWork = workingType === '근무';

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({ workingDate: date });
      setError(null);
    }
  }, [open, date, form]);

  const memberOptions = members.map((m) => ({
    value: m.employeeId,
    label: `${m.name}(${m.empCode})`,
  }));

  const accountOptions = accounts.map((a) => ({
    value: a.accountSfid,
    label: a.name,
  }));

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setError(null);

      await createMutation.mutateAsync({
        employee_id: values.employeeId,
        working_date: values.workingDate,
        working_type: values.workingType,
        ...(isWork && {
          working_category1: values.workingCategory1,
          working_category2: values.workingCategory2,
          working_category3: values.workingCategory3,
          account_sfid: values.accountSfid,
        }),
      });

      message.success('일정이 등록되었습니다');
      onClose();
    } catch (err: unknown) {
      const errMsg =
        err instanceof Error ? err.message : '일정 등록에 실패했습니다';
      setError(errMsg);
    }
  };

  return (
    <Modal
      title="일정 등록"
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      okText="등록"
      cancelText="취소"
      confirmLoading={createMutation.isPending}
      destroyOnClose
    >
      {error && (
        <Alert
          type="error"
          message={error}
          closable
          onClose={() => setError(null)}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form form={form} layout="vertical">
        <Form.Item label="근무일자" name="workingDate">
          <Input readOnly />
        </Form.Item>

        <Form.Item
          label="여사원"
          name="employeeId"
          rules={[{ required: true, message: '여사원을 선택해주세요' }]}
        >
          <Select
            placeholder="여사원 선택"
            options={memberOptions}
            showSearch
            optionFilterProp="label"
          />
        </Form.Item>

        <Form.Item
          label="근무형태"
          name="workingType"
          rules={[{ required: true, message: '근무형태를 선택해주세요' }]}
        >
          <Select placeholder="근무형태 선택" options={WORKING_TYPE_OPTIONS} />
        </Form.Item>

        {isWork && (
          <>
            <Form.Item
              label="근무유형1"
              name="workingCategory1"
              rules={[{ required: true, message: '근무유형1을 선택해주세요' }]}
            >
              <Select placeholder="근무유형1 선택" options={CATEGORY1_OPTIONS} />
            </Form.Item>

            <Form.Item label="근무유형2" name="workingCategory2">
              <Select placeholder="근무유형2 선택" options={CATEGORY2_OPTIONS} allowClear />
            </Form.Item>

            <Form.Item label="근무유형3" name="workingCategory3">
              <Select placeholder="근무유형3 선택" options={CATEGORY3_OPTIONS} allowClear />
            </Form.Item>

            <Form.Item label="거래처" name="accountSfid">
              <Select
                placeholder="거래처 선택"
                options={accountOptions}
                showSearch
                optionFilterProp="label"
                allowClear
              />
            </Form.Item>
          </>
        )}
      </Form>
    </Modal>
  );
}
