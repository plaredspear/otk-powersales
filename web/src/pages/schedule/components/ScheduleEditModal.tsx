import { useEffect, useState } from 'react';
import { Alert, Button, Form, Input, message, Modal, Select, Space } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import { AxiosError } from 'axios';
import type { TeamSchedule, TeamScheduleAccount } from '@/api/team-schedule';
import { useUpdateTeamSchedule, useDeleteTeamSchedule } from '@/hooks/team-schedule/useTeamScheduleMutation';

interface ScheduleEditModalProps {
  open: boolean;
  onClose: () => void;
  schedule: TeamSchedule | null;
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

export function ScheduleEditModal({
  open,
  onClose,
  schedule,
  accounts,
}: ScheduleEditModalProps) {
  const [form] = Form.useForm();
  const [error, setError] = useState<string | null>(null);
  const updateMutation = useUpdateTeamSchedule();
  const deleteMutation = useDeleteTeamSchedule();

  const workingType = Form.useWatch('workingType', form);
  const isWork = workingType === '근무';

  useEffect(() => {
    if (open && schedule) {
      form.setFieldsValue({
        workingDate: schedule.workingDate,
        employeeName: `${schedule.employeeName}(${schedule.empCode})`,
        workingType: schedule.workingType,
        workingCategory1: schedule.workingCategory1,
        workingCategory2: schedule.workingCategory2,
        workingCategory3: schedule.workingCategory3,
        accountSfid: schedule.accountSfid,
      });
      setError(null);
    }
  }, [open, schedule, form]);

  const accountOptions = accounts.map((a) => ({
    value: a.accountSfid,
    label: a.name,
  }));

  const extractErrorMessage = (err: unknown): string => {
    if (err instanceof AxiosError && err.response?.status === 403) {
      return '지점장님은 스케줄 편집 권한이 없습니다';
    }
    if (err instanceof Error) return err.message;
    return '일정 수정에 실패했습니다';
  };

  const handleUpdate = async () => {
    if (!schedule) return;
    try {
      const values = await form.validateFields();
      setError(null);

      await updateMutation.mutateAsync({
        id: schedule.id,
        data: {
          working_date: values.workingDate,
          working_type: values.workingType,
          ...(isWork && {
            working_category1: values.workingCategory1,
            working_category2: values.workingCategory2,
            working_category3: values.workingCategory3,
            account_sfid: values.accountSfid,
          }),
        },
      });

      message.success('일정이 수정되었습니다');
      onClose();
    } catch (err: unknown) {
      setError(extractErrorMessage(err));
    }
  };

  const handleDelete = () => {
    if (!schedule) return;
    Modal.confirm({
      title: '일정 삭제',
      icon: <ExclamationCircleOutlined />,
      content: '이 일정을 삭제하시겠습니까?',
      okText: '삭제',
      okType: 'danger',
      cancelText: '취소',
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(schedule.id);
          message.success('일정이 삭제되었습니다');
          onClose();
        } catch (err: unknown) {
          setError(extractErrorMessage(err));
        }
      },
    });
  };

  return (
    <Modal
      title="일정 수정"
      open={open}
      onCancel={onClose}
      destroyOnClose
      footer={
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <Button danger onClick={handleDelete} loading={deleteMutation.isPending}>
            삭제
          </Button>
          <Space>
            <Button onClick={onClose}>취소</Button>
            <Button type="primary" onClick={handleUpdate} loading={updateMutation.isPending}>
              수정
            </Button>
          </Space>
        </div>
      }
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

        <Form.Item label="여사원" name="employeeName">
          <Input readOnly />
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
