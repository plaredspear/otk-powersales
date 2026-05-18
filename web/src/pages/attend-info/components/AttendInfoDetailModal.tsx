import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  List,
  Modal,
  Radio,
  Select,
  Space,
  Spin,
  Tag,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { ATTEND_TYPE_OPTIONS, type AttendInfoStatus } from '@/api/attendInfo';
import { useAttendInfoDetail, useUpdateAttendInfo } from '@/hooks/attend-info/useAttendInfo';

interface AttendInfoDetailModalProps {
  attendInfoId: number;
  canWrite: boolean;
  onClose: () => void;
}

interface UpdateFormValues {
  attendType?: string;
  startDate?: Dayjs;
  endDate?: Dayjs;
  status?: AttendInfoStatus;
  reason?: string;
}

function formatYyyyMmDd(value: string | null | undefined): string {
  if (!value || value.length !== 8) return value ?? '-';
  return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
}

export default function AttendInfoDetailModal({
  attendInfoId,
  canWrite,
  onClose,
}: AttendInfoDetailModalProps) {
  const { data, isLoading } = useAttendInfoDetail(attendInfoId);
  const [editMode, setEditMode] = useState(false);
  const [form] = Form.useForm<UpdateFormValues>();
  const updateMutation = useUpdateAttendInfo();

  useEffect(() => {
    if (data && editMode) {
      form.setFieldsValue({
        attendType: data.attendType ?? undefined,
        startDate: data.startDate ? dayjs(data.startDate, 'YYYYMMDD') : undefined,
        endDate: data.endDate ? dayjs(data.endDate, 'YYYYMMDD') : undefined,
        status: data.status ?? undefined,
        reason: '',
      });
    }
  }, [data, editMode, form]);

  const submitUpdate = async () => {
    if (!data) return;
    try {
      const values = await form.validateFields();
      const result = await updateMutation.mutateAsync({
        id: data.id,
        data: {
          attendType: values.attendType,
          startDate: values.startDate?.format('YYYYMMDD'),
          endDate: values.endDate?.format('YYYYMMDD'),
          status: values.status,
          reason: values.reason?.trim() || undefined,
        },
      });
      const summary = result.conversionSummary;
      const parts: string[] = ['근태정보 수정 완료'];
      if (summary) {
        if (summary.deleted_schedule_count > 0) {
          parts.push(`기존 연차 일정 ${summary.deleted_schedule_count}건 삭제`);
        }
        if (summary.converted_schedule_count > 0) {
          parts.push(`신규 연차 일정 ${summary.converted_schedule_count}건 생성`);
        }
        if (summary.skipped_job_filter > 0) {
          parts.push(`직무 미일치로 ${summary.skipped_job_filter}건 skip`);
        }
      }
      message.success(parts.join(' · '));
      setEditMode(false);
    } catch (e) {
      if (e instanceof Error) message.error(e.message);
    }
  };

  return (
    <Modal
      title={`근태정보 상세 (id=${attendInfoId})`}
      open
      onCancel={() => {
        setEditMode(false);
        onClose();
      }}
      footer={
        editMode ? (
          <Space>
            <Button onClick={() => setEditMode(false)}>취소</Button>
            <Button type="primary" loading={updateMutation.isPending} onClick={submitUpdate}>
              저장
            </Button>
          </Space>
        ) : (
          <Space>
            <Button onClick={onClose}>닫기</Button>
            {canWrite && (
              <Button type="primary" onClick={() => setEditMode(true)}>
                수정
              </Button>
            )}
          </Space>
        )
      }
      width={640}
    >
      {isLoading || !data ? (
        <div style={{ textAlign: 'center', padding: 32 }}>
          <Spin />
        </div>
      ) : editMode ? (
        <Form form={form} layout="vertical">
          <Alert
            type="warning"
            showIcon
            message="수정 시 기존 연차 일정이 자동 삭제되고 새 일정이 생성됩니다. 사원 변경은 불가합니다 (새 row 등록 권장)."
            style={{ marginBottom: 16 }}
          />
          <Form.Item label="사원">
            <Input value={`${data.employeeName ?? ''} (${data.employeeCode})`} disabled />
          </Form.Item>
          <Form.Item label="근태유형" name="attendType">
            <Select
              options={ATTEND_TYPE_OPTIONS.map((o) => ({
                value: o.value,
                label: `${o.value} · ${o.label}`,
              }))}
            />
          </Form.Item>
          <Form.Item label="시작일" name="startDate">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            label="종료일"
            name="endDate"
            dependencies={['startDate']}
            rules={[
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
          <Form.Item label="상태" name="status">
            <Radio.Group>
              <Radio value="N">N (등록)</Radio>
              <Radio value="Y">Y (취소)</Radio>
            </Radio.Group>
          </Form.Item>
          <Form.Item
            label="수정 사유"
            name="reason"
            rules={[{ min: 5, message: '사유는 최소 5자 이상이어야 합니다' }]}
          >
            <Input.TextArea rows={3} placeholder="예: 기간 변경 보정" />
          </Form.Item>
        </Form>
      ) : (
        <>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="근태정보번호">{data.name ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="상태">
              {data.status === 'N' ? (
                <Tag color="blue">N (등록)</Tag>
              ) : data.status === 'Y' ? (
                <Tag>Y (취소)</Tag>
              ) : (
                '-'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="사원번호">{data.employeeCode}</Descriptions.Item>
            <Descriptions.Item label="사원명">
              <Space>
                {data.employeeName ?? '-'}
                {data.employeeJobCode && <Tag color="geekblue">{data.employeeJobCode}</Tag>}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="근태유형" span={2}>
              {data.attendTypeName ?? '-'} ({data.attendType ?? '-'})
            </Descriptions.Item>
            <Descriptions.Item label="시작일">{formatYyyyMmDd(data.startDate)}</Descriptions.Item>
            <Descriptions.Item label="종료일">{formatYyyyMmDd(data.endDate)}</Descriptions.Item>
            <Descriptions.Item label="등록자">{data.createdByName ?? 'SAP'}</Descriptions.Item>
            <Descriptions.Item label="최종 수정자">{data.lastModifiedByName ?? '-'}</Descriptions.Item>
          </Descriptions>
          <div style={{ marginTop: 24 }}>
            <h4>연결 연차 일정 (총 {data.linkedScheduleCount}건 — 최대 5건 미리보기)</h4>
            {data.linkedSchedules.length === 0 ? (
              <Alert type="info" message="연결된 연차 일정이 없습니다" />
            ) : (
              <List
                size="small"
                bordered
                dataSource={data.linkedSchedules}
                renderItem={(item) => (
                  <List.Item>
                    <Space>
                      <Tag color="green">{item.workingType}</Tag>
                      <span>{item.workingDate}</span>
                      <span style={{ color: '#888' }}>id={item.id}</span>
                    </Space>
                  </List.Item>
                )}
              />
            )}
          </div>
        </>
      )}
    </Modal>
  );
}
