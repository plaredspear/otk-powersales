import { useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Table, Tag, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { usePromotionTypes } from '@/hooks/promotion/usePromotionTypes';
import {
  useCreatePromotionType,
  useUpdatePromotionType,
  useDeletePromotionType,
} from '@/hooks/promotion/usePromotionTypeMutation';
import type { PromotionType } from '@/api/promotionType';

interface FormValues {
  name: string;
  displayOrder: number;
}

export default function PromotionTypesPage() {
  const { data: promotionTypes, isLoading } = usePromotionTypes();
  const createMutation = useCreatePromotionType();
  const updateMutation = useUpdatePromotionType();
  const deleteMutation = useDeletePromotionType();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<PromotionType | null>(null);
  const [form] = Form.useForm<FormValues>();

  const handleAdd = () => {
    setEditingItem(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: PromotionType) => {
    setEditingItem(record);
    form.setFieldsValue({ name: record.name, displayOrder: record.displayOrder });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const payload = { name: values.name, display_order: values.displayOrder };

      if (editingItem) {
        await updateMutation.mutateAsync({ id: editingItem.id, data: payload });
        message.success('행사유형이 수정되었습니다');
      } else {
        await createMutation.mutateAsync(payload);
        message.success('행사유형이 추가되었습니다');
      }
      setModalOpen(false);
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleDeactivate = async (id: number) => {
    try {
      await deleteMutation.mutateAsync(id);
      message.success('행사유형이 비활성화되었습니다');
      setModalOpen(false);
    } catch {
      message.error('행사유형 비활성화에 실패했습니다');
    }
  };

  const columns: ColumnsType<PromotionType> = [
    {
      title: '#',
      dataIndex: 'id',
      width: 60,
      align: 'center',
    },
    {
      title: '행사유형명',
      dataIndex: 'name',
      width: 200,
    },
    {
      title: '표시순서',
      dataIndex: 'displayOrder',
      width: 100,
      align: 'center',
    },
    {
      title: '상태',
      dataIndex: 'isActive',
      width: 100,
      align: 'center',
      render: (val: boolean) => (
        <Tag color={val ? 'green' : 'default'}>{val ? '활성' : '비활성'}</Tag>
      ),
    },
    {
      title: '관리',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => handleEdit(record)}>
          수정
        </Button>
      ),
    },
  ];

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          marginBottom: 16,
        }}
      >
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
          추가
        </Button>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={promotionTypes}
        loading={isLoading}
        pagination={false}
      />

      <Modal
        title={editingItem ? '행사유형 수정' : '행사유형 추가'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              {editingItem && (
                <Popconfirm
                  title="행사유형 비활성화"
                  description="이 행사유형을 비활성화하시겠습니까?"
                  onConfirm={() => handleDeactivate(editingItem.id)}
                  okText="확인"
                  cancelText="취소"
                >
                  <Button danger loading={deleteMutation.isPending}>
                    비활성화
                  </Button>
                </Popconfirm>
              )}
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <Button onClick={() => setModalOpen(false)}>취소</Button>
              <Button type="primary" onClick={handleSave} loading={isSaving}>
                저장
              </Button>
            </div>
          </div>
        }
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="name"
            label="행사유형명"
            rules={[
              { required: true, message: '행사유형명을 입력해주세요' },
              { max: 50, message: '50자 이하로 입력해주세요' },
            ]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item
            name="displayOrder"
            label="표시순서"
            rules={[
              { required: true, message: '표시순서를 입력해주세요' },
              { type: 'number', min: 1, message: '1 이상의 숫자를 입력해주세요' },
            ]}
          >
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
