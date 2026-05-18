import { useState } from 'react';
import { Alert, DatePicker, Form, Input, Modal, Radio, Select, message } from 'antd';
import type { Dayjs } from 'dayjs';
import { useCreateAttendInfo } from '@/hooks/attend-info/useAttendInfo';
import { ATTEND_TYPE_OPTIONS, type AttendInfoStatus } from '@/api/attendInfo';

interface CreateFormValues {
  employeeCode: string;
  attendType: string;
  startDate: Dayjs;
  endDate: Dayjs;
  status: AttendInfoStatus;
  reason: string;
}

interface AttendInfoCreateModalProps {
  open: boolean;
  onClose: () => void;
}

export default function AttendInfoCreateModal({ open, onClose }: AttendInfoCreateModalProps) {
  const [form] = Form.useForm<CreateFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const createMutation = useCreateAttendInfo();

  const submit = async () => {
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const result = await createMutation.mutateAsync({
        employeeCode: values.employeeCode.trim(),
        attendType: values.attendType,
        startDate: values.startDate.format('YYYYMMDD'),
        endDate: values.endDate.format('YYYYMMDD'),
        status: values.status,
        reason: values.reason.trim(),
      });
      const summary = result.conversionSummary;
      const parts: string[] = [`근태정보 등록 완료 (id=${result.id})`];
      if (summary) {
        if (summary.converted_schedule_count > 0) {
          parts.push(`연차 일정 ${summary.converted_schedule_count}건 자동 생성`);
        }
        if (summary.deleted_schedule_count > 0) {
          parts.push(`기존 연차 일정 ${summary.deleted_schedule_count}건 삭제`);
        }
        if (summary.skipped_job_filter > 0) {
          parts.push(`직무 미일치로 ${summary.skipped_job_filter}건 skip`);
        }
      }
      message.success(parts.join(' · '));
      form.resetFields();
      onClose();
    } catch (e) {
      if (e instanceof Error) {
        message.error(e.message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="근태정보 신규 등록 — SAP 미적재 / 오류 시 보정 입력용"
      open={open}
      onOk={submit}
      onCancel={onClose}
      confirmLoading={submitting}
      okText="등록"
      cancelText="취소"
      width={520}
    >
      <Alert
        type="info"
        showIcon
        message="보정 입력 사유는 audit 로그에 기록됩니다. SAP 마스터 보정 필요 시 SAP 팀에 재요청 안내 의무."
        style={{ marginBottom: 16 }}
      />
      <Form form={form} layout="vertical" preserve={false}>
        <Form.Item
          label="사원번호"
          name="employeeCode"
          rules={[{ required: true, message: '사원번호는 필수입니다' }]}
        >
          <Input placeholder="예: 20120253" />
        </Form.Item>
        <Form.Item
          label="근태유형"
          name="attendType"
          rules={[{ required: true, message: '근태유형은 필수입니다' }]}
        >
          <Select
            options={ATTEND_TYPE_OPTIONS.map((o) => ({ value: o.value, label: `${o.value} · ${o.label}` }))}
          />
        </Form.Item>
        <Form.Item
          label="시작일"
          name="startDate"
          rules={[{ required: true, message: '시작일은 필수입니다' }]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          label="종료일"
          name="endDate"
          dependencies={['startDate']}
          rules={[
            { required: true, message: '종료일은 필수입니다' },
            ({ getFieldValue }) => ({
              validator(_, value: Dayjs) {
                const start = getFieldValue('startDate') as Dayjs | undefined;
                if (!value || !start) return Promise.resolve();
                if (value.isBefore(start, 'day')) {
                  return Promise.reject(new Error('종료일은 시작일 이후여야 합니다'));
                }
                return Promise.resolve();
              },
            }),
          ]}
        >
          <DatePicker style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item
          label="상태"
          name="status"
          rules={[{ required: true, message: '상태는 필수입니다' }]}
          initialValue="N"
        >
          <Radio.Group>
            <Radio value="N">N (등록)</Radio>
            <Radio value="Y">Y (취소)</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item
          label="보정 입력 사유"
          name="reason"
          rules={[
            { required: true, message: '사유는 필수입니다' },
            { min: 5, message: '사유는 최소 5자 이상이어야 합니다' },
          ]}
        >
          <Input.TextArea rows={3} placeholder="예: SAP 적재 누락 보정 등록" />
        </Form.Item>
      </Form>
    </Modal>
  );
}
