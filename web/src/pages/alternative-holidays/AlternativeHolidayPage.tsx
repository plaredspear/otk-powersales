import { useState } from 'react';
import {
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { Dayjs } from 'dayjs';
import { useAlternativeHolidays } from '@/hooks/alternative-holiday/useAlternativeHolidays';
import {
  useCreateAlternativeHoliday,
  useApproveAlternativeHoliday,
  useRejectAlternativeHoliday,
} from '@/hooks/alternative-holiday/useAlternativeHolidayMutation';
import type { AlternativeHolidayItem } from '@/api/alternativeHoliday';

const STATUS_OPTIONS = [
  { label: '전체', value: '' },
  { label: '신규', value: '신규' },
  { label: '승인', value: '승인' },
  { label: '반려', value: '반려' },
  { label: '조정', value: '조정' },
];

const STATUS_COLORS: Record<string, string> = {
  신규: 'blue',
  승인: 'green',
  반려: 'red',
  조정: 'orange',
};

interface FilterState {
  dateRange: [Dayjs, Dayjs];
  status: string;
  employeeId: string;
}

export default function AlternativeHolidayPage() {
  const [filters, setFilters] = useState<FilterState>({
    dateRange: [dayjs().startOf('month'), dayjs().endOf('month')],
    status: '',
    employeeId: '',
  });

  const { data, isLoading } = useAlternativeHolidays({
    startDate: filters.dateRange[0].format('YYYY-MM-DD'),
    endDate: filters.dateRange[1].format('YYYY-MM-DD'),
    status: filters.status || undefined,
    employeeId: filters.employeeId || undefined,
  });

  // Create modal
  const [createOpen, setCreateOpen] = useState(false);
  const [createForm] = Form.useForm();
  const createMutation = useCreateAlternativeHoliday();

  // Approve modal
  const [approveOpen, setApproveOpen] = useState(false);
  const [approveTarget, setApproveTarget] = useState<AlternativeHolidayItem | null>(null);
  const [approveForm] = Form.useForm();
  const approveMutation = useApproveAlternativeHoliday();

  // Reject modal
  const [rejectOpen, setRejectOpen] = useState(false);
  const [rejectTarget, setRejectTarget] = useState<AlternativeHolidayItem | null>(null);
  const [rejectForm] = Form.useForm();
  const rejectMutation = useRejectAlternativeHoliday();

  const handleCreate = async () => {
    try {
      const values = await createForm.validateFields();
      await createMutation.mutateAsync({
        employee_id: values.employeeId,
        actual_work_date: values.actualWorkDate.format('YYYY-MM-DD'),
        target_alt_holiday_date: values.targetAltHolidayDate.format('YYYY-MM-DD'),
      });
      message.success('대체휴무가 신청되었습니다');
      setCreateOpen(false);
      createForm.resetFields();
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleApproveOpen = (record: AlternativeHolidayItem) => {
    setApproveTarget(record);
    approveForm.setFieldsValue({
      confirmAltHolidayDate: dayjs(record.targetAltHolidayDate),
    });
    setApproveOpen(true);
  };

  const handleApprove = async () => {
    if (!approveTarget) return;
    try {
      const values = await approveForm.validateFields();
      const confirmDate = values.confirmAltHolidayDate?.format('YYYY-MM-DD') || null;
      await approveMutation.mutateAsync({
        id: approveTarget.id,
        data: { confirm_alt_holiday_date: confirmDate },
      });
      message.success('대체휴무가 승인되었습니다');
      setApproveOpen(false);
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleRejectOpen = (record: AlternativeHolidayItem) => {
    setRejectTarget(record);
    rejectForm.resetFields();
    setRejectOpen(true);
  };

  const handleReject = async () => {
    if (!rejectTarget) return;
    try {
      const values = await rejectForm.validateFields();
      await rejectMutation.mutateAsync({
        id: rejectTarget.id,
        data: { change_reason: values.changeReason },
      });
      message.success('대체휴무가 반려되었습니다');
      setRejectOpen(false);
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const canAction = (status: string) => status === '신규' || status === '조정';

  const columns: ColumnsType<AlternativeHolidayItem> = [
    { title: '사번', dataIndex: 'employeeId', width: 100 },
    { title: '사원명', dataIndex: 'employeeName', width: 80 },
    { title: '조직', dataIndex: 'orgName', width: 100 },
    {
      title: '대상일',
      dataIndex: 'actualWorkDate',
      width: 120,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD (dd)'),
    },
    {
      title: '신청일',
      dataIndex: 'targetAltHolidayDate',
      width: 120,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD (dd)'),
    },
    {
      title: '확정일',
      dataIndex: 'confirmAltHolidayDate',
      width: 120,
      render: (v: string | null) => (v ? dayjs(v).format('YYYY-MM-DD') : '-'),
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 80,
      align: 'center',
      render: (v: string) => <Tag color={STATUS_COLORS[v] || 'default'}>{v}</Tag>,
    },
    { title: '변경사유', dataIndex: 'changeReason', width: 150, ellipsis: true },
    {
      title: '액션',
      width: 140,
      align: 'center',
      render: (_, record) =>
        canAction(record.status) ? (
          <Space size="small">
            <Button type="link" size="small" onClick={() => handleApproveOpen(record)}>
              승인
            </Button>
            <Button type="link" size="small" danger onClick={() => handleRejectOpen(record)}>
              반려
            </Button>
          </Space>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      {/* Filter */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap', alignItems: 'center' }}>
        <DatePicker.RangePicker
          value={filters.dateRange}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setFilters((prev) => ({ ...prev, dateRange: [dates[0]!, dates[1]!] }));
            }
          }}
        />
        <Select
          style={{ width: 100 }}
          value={filters.status}
          options={STATUS_OPTIONS}
          onChange={(v) => setFilters((prev) => ({ ...prev, status: v }))}
        />
        <Input
          style={{ width: 160 }}
          placeholder="사번"
          value={filters.employeeId}
          onChange={(e) => setFilters((prev) => ({ ...prev, employeeId: e.target.value }))}
          allowClear
        />
        <Button type="primary" icon={<SearchOutlined />}>
          검색
        </Button>
        <div style={{ flex: 1 }} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.resetFields(); setCreateOpen(true); }}>
          대체휴무 신청
        </Button>
      </div>

      {/* Table */}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={isLoading}
        pagination={false}
        size="middle"
        scroll={{ x: 1100 }}
      />

      {/* Create Modal */}
      <Modal
        title="대체휴무 신청"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        onOk={handleCreate}
        confirmLoading={createMutation.isPending}
        okText="신청"
        cancelText="취소"
      >
        <Form form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="employeeId"
            label="사번"
            rules={[{ required: true, message: '사번을 입력하세요' }]}
          >
            <Input placeholder="사번 입력" maxLength={20} />
          </Form.Item>
          <Form.Item
            name="actualWorkDate"
            label="대상일 (실제 근무한 주말/공휴일)"
            rules={[{ required: true, message: '대상일을 선택하세요' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="targetAltHolidayDate"
            label="대체휴무일 (쉬고 싶은 평일)"
            rules={[{ required: true, message: '대체휴무일을 선택하세요' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Approve Modal */}
      <Modal
        title="대체휴무 승인"
        open={approveOpen}
        onCancel={() => setApproveOpen(false)}
        onOk={handleApprove}
        confirmLoading={approveMutation.isPending}
        okText="승인"
        cancelText="취소"
      >
        {approveTarget && (
          <div style={{ marginBottom: 16 }}>
            <p>
              <strong>사원:</strong> {approveTarget.employeeName} ({approveTarget.employeeId})
            </p>
            <p>
              <strong>대상일:</strong> {dayjs(approveTarget.actualWorkDate).format('YYYY-MM-DD (dd)')}
            </p>
            <p>
              <strong>신청일:</strong> {dayjs(approveTarget.targetAltHolidayDate).format('YYYY-MM-DD (dd)')}
            </p>
          </div>
        )}
        <Form form={approveForm} layout="vertical">
          <Form.Item name="confirmAltHolidayDate" label="확정일 (변경 가능)">
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Reject Modal */}
      <Modal
        title="대체휴무 반려"
        open={rejectOpen}
        onCancel={() => setRejectOpen(false)}
        onOk={handleReject}
        confirmLoading={rejectMutation.isPending}
        okText="반려"
        okButtonProps={{ danger: true }}
        cancelText="취소"
      >
        {rejectTarget && (
          <div style={{ marginBottom: 16 }}>
            <p>
              <strong>사원:</strong> {rejectTarget.employeeName} ({rejectTarget.employeeId})
            </p>
            <p>
              <strong>대상일:</strong> {dayjs(rejectTarget.actualWorkDate).format('YYYY-MM-DD (dd)')}
            </p>
            <p>
              <strong>신청일:</strong> {dayjs(rejectTarget.targetAltHolidayDate).format('YYYY-MM-DD (dd)')}
            </p>
          </div>
        )}
        <Form form={rejectForm} layout="vertical">
          <Form.Item
            name="changeReason"
            label="반려 사유"
            rules={[{ required: true, message: '반려 사유를 입력하세요' }]}
          >
            <Input.TextArea rows={3} maxLength={500} placeholder="반려 사유를 입력하세요" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
