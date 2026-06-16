import { useEffect, useMemo, useState } from 'react';
import { Alert, Button, DatePicker, Form, Input, message, Modal, Select, Space, Typography } from 'antd';
import { ExclamationCircleOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { AxiosError } from 'axios';
import dayjs from 'dayjs';
import type { TeamSchedule, TeamScheduleAccount } from '@/api/team-schedule';
import { useUpdateTeamSchedule, useDeleteTeamSchedule } from '@/hooks/team-schedule/useTeamScheduleMutation';

interface ScheduleEditModalProps {
  open: boolean;
  onClose: () => void;
  schedule: TeamSchedule | null;
  accounts: TeamScheduleAccount[];
  readOnly?: boolean;
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
  readOnly = false,
}: ScheduleEditModalProps) {
  const [form] = Form.useForm();
  const [error, setError] = useState<string | null>(null);
  const updateMutation = useUpdateTeamSchedule();
  const deleteMutation = useDeleteTeamSchedule();

  const workingType = Form.useWatch('workingType', form);
  const isWork = workingType === '근무';

  const isPastNonEvent = useMemo(() => {
    if (!schedule) return false;
    const isPast = dayjs(schedule.workingDate).isBefore(dayjs(), 'day');
    const isEvent = schedule.workingCategory1 === '행사';
    return isPast && !isEvent;
  }, [schedule]);

  useEffect(() => {
    if (open && schedule) {
      form.setFieldsValue({
        workingDate: dayjs(schedule.workingDate),
        employeeName: `${schedule.employeeName}(${schedule.employeeCode})`,
        workingType: schedule.workingType,
        workingCategory1: schedule.workingCategory1,
        workingCategory2: schedule.workingCategory2,
        workingCategory3: schedule.workingCategory3,
        accountId: schedule.accountId,
      });
      setError(null);
    }
  }, [open, schedule, form]);

  const accountOptions = accounts.map((a) => ({
    value: a.accountId,
    label: a.name,
  }));

  const extractErrorMessage = (err: unknown): string => {
    if (err instanceof AxiosError) {
      if (err.response?.status === 400 && err.response?.data?.code === 'PAST_DATE_CHANGE_NOT_ALLOWED') {
        return '과거 근무일자의 날짜는 변경할 수 없습니다';
      }
      if (err.response?.status === 403) {
        return '지점장님은 스케줄 편집 권한이 없습니다';
      }
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
          workingDate: values.workingDate.format('YYYY-MM-DD'),
          workingType: values.workingType,
          ...(isWork && {
            workingCategory1: values.workingCategory1,
            workingCategory2: values.workingCategory2,
            workingCategory3: values.workingCategory3,
            accountId: values.accountId,
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
      title={readOnly ? '일정 상세' : '일정 수정'}
      open={open}
      onCancel={onClose}
      // 일정 목록 모달(DayScheduleListModal, 기본 z-index 1050) 위에서 열리므로
      // 더 높은 z-index 로 상세 모달이 항상 앞에 표시되도록 한다.
      zIndex={1100}
      destroyOnHidden
      footer={
        readOnly ? (
          <Button onClick={onClose}>닫기</Button>
        ) : (
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
        )
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

      <Form form={form} layout="vertical" disabled={readOnly}>
        <Form.Item label="근무일자" name="workingDate">
          <DatePicker
            format="YYYY-MM-DD"
            disabled={isPastNonEvent || readOnly}
            allowClear={false}
            style={{ width: '100%' }}
          />
        </Form.Item>
        {isPastNonEvent && !readOnly && (
          <Typography.Text type="secondary" style={{ display: 'block', marginTop: -16, marginBottom: 16 }}>
            <InfoCircleOutlined /> 과거 근무일자의 날짜는 변경할 수 없습니다
          </Typography.Text>
        )}

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

            <Form.Item label="거래처" name="accountId">
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
