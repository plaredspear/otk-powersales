import { useState } from 'react';
import { Button, DatePicker, Form, Input, Modal, Popconfirm, Select, Table, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useHolidayMasters } from '@/hooks/holiday/useHolidayMasters';
import {
  useCreateHolidayMaster,
  useUpdateHolidayMaster,
  useDeleteHolidayMaster,
} from '@/hooks/holiday/useHolidayMasterMutation';
import type { HolidayMaster } from '@/api/holidayMaster';

const HOLIDAY_TYPES = ['법정공휴일', '대체공휴일', '임시공휴일'];

interface FormValues {
  holidayDate: dayjs.Dayjs;
  name: string;
  type: string;
}

export default function HolidayMasterListPage() {
  const currentYear = dayjs().year();
  const [year, setYear] = useState(currentYear);
  const { data: holidays, isLoading } = useHolidayMasters(year);
  const createMutation = useCreateHolidayMaster();
  const updateMutation = useUpdateHolidayMaster();
  const deleteMutation = useDeleteHolidayMaster();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<HolidayMaster | null>(null);
  const [form] = Form.useForm<FormValues>();

  const handleAdd = () => {
    setEditingItem(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: HolidayMaster) => {
    setEditingItem(record);
    form.setFieldsValue({
      holidayDate: dayjs(record.holidayDate),
      name: record.name,
      type: record.type,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const payload = {
        holidayDate: values.holidayDate.format('YYYY-MM-DD'),
        name: values.name,
        type: values.type,
      };

      if (editingItem) {
        await updateMutation.mutateAsync({ id: editingItem.id, data: payload });
        message.success('공휴일이 수정되었습니다');
      } else {
        await createMutation.mutateAsync(payload);
        message.success('공휴일이 등록되었습니다');
      }
      setModalOpen(false);
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDelete = async (record: HolidayMaster) => {
    try {
      await deleteMutation.mutateAsync(record.id);
      message.success('공휴일이 삭제되었습니다');
    } catch {
      message.error('공휴일 삭제에 실패했습니다');
    }
  };

  const columns: ColumnsType<HolidayMaster> = [
    {
      title: '날짜',
      dataIndex: 'holidayDate',
      width: 140,
    },
    {
      title: '공휴일명',
      dataIndex: 'name',
      width: 200,
    },
    {
      title: '유형',
      dataIndex: 'type',
      width: 120,
    },
    {
      title: '관리',
      width: 140,
      align: 'center',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 4, justifyContent: 'center' }}>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            수정
          </Button>
          <Popconfirm
            title="공휴일 삭제"
            description={`'${record.name}' 공휴일을 삭제하시겠습니까?`}
            onConfirm={() => handleDelete(record)}
            okText="확인"
            cancelText="취소"
          >
            <Button type="link" size="small" danger>
              삭제
            </Button>
          </Popconfirm>
        </div>
      ),
    },
  ];

  const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - 1 + i);

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          marginBottom: 16,
        }}
      >
        <Select
          value={year}
          onChange={setYear}
          style={{ width: 120 }}
          options={yearOptions.map((y) => ({ value: y, label: `${y}년` }))}
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          공휴일 등록
        </Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={holidays}
        loading={isLoading}
        pagination={false}
      />

      <Modal
        title={editingItem ? '공휴일 수정' : '공휴일 등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
            <Button onClick={() => setModalOpen(false)}>취소</Button>
            <Button type="primary" onClick={handleSave} loading={isSaving}>
              저장
            </Button>
          </div>
        }
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="holidayDate"
            label="날짜"
            rules={[{ required: true, message: '날짜를 선택해주세요' }]}
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="name"
            label="공휴일명"
            rules={[
              { required: true, message: '공휴일명을 입력해주세요' },
              { max: 50, message: '50자 이하로 입력해주세요' },
            ]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item
            name="type"
            label="유형"
            rules={[{ required: true, message: '유형을 선택해주세요' }]}
          >
            <Select
              options={HOLIDAY_TYPES.map((t) => ({ value: t, label: t }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
