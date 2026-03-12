import { useContext, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { usePromotion } from '@/hooks/promotion/usePromotion';
import { useDeletePromotion } from '@/hooks/promotion/usePromotionMutation';
import { usePromotionEmployees } from '@/hooks/promotion/usePromotionEmployees';
import {
  useCreatePromotionEmployee,
  useUpdatePromotionEmployee,
  useDeletePromotionEmployee,
} from '@/hooks/promotion/usePromotionEmployeeMutation';
import type { PromotionEmployee } from '@/api/promotionEmployee';
import { BreadcrumbContext } from '@/contexts/BreadcrumbContext';

const { Title } = Typography;

const CATEGORY_TAG: Record<string, string> = {
  라면: 'red',
  냉장: 'blue',
  냉동: 'cyan',
  만두: 'orange',
};

const PROMOTION_TYPE_TAG: Record<string, string> = {
  시식: 'blue',
  시음: 'cyan',
  판촉: 'green',
  증정: 'gold',
};

const WORK_STATUS_OPTIONS = [
  { label: '근무', value: '근무' },
  { label: '연차', value: '연차' },
  { label: '대휴', value: '대휴' },
];

const WORK_TYPE3_OPTIONS = [
  { label: '고정', value: '고정' },
  { label: '격고', value: '격고' },
  { label: '순회', value: '순회' },
];

export default function PromotionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const promotionId = Number(id);

  const { data: promotion, isLoading, error } = usePromotion(promotionId);
  const deleteMutation = useDeletePromotion();
  const { setDynamicTitle } = useContext(BreadcrumbContext);

  // --- 행사조원 ---
  const { data: employees, isLoading: employeesLoading } = usePromotionEmployees(promotionId);
  const createPeMutation = useCreatePromotionEmployee();
  const updatePeMutation = useUpdatePromotionEmployee();
  const deletePeMutation = useDeletePromotionEmployee();

  const [modalOpen, setModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState<PromotionEmployee | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    setDynamicTitle(promotion?.promotionNumber ?? null);
    return () => setDynamicTitle(null);
  }, [promotion?.promotionNumber, setDynamicTitle]);

  const handleDelete = () => {
    Modal.confirm({
      title: '행사마스터 삭제',
      content: '이 행사마스터를 삭제하시겠습니까?',
      okText: '확인',
      cancelText: '취소',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteMutation.mutateAsync(promotionId);
          message.success('행사마스터가 삭제되었습니다');
          navigate('/promotions');
        } catch {
          message.error('행사마스터 삭제에 실패했습니다');
        }
      },
    });
  };

  // --- 행사조원 핸들러 ---

  const openCreateModal = () => {
    setEditingEmployee(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEditModal = (record: PromotionEmployee) => {
    setEditingEmployee(record);
    form.setFieldsValue({
      employee_sfid: record.employeeSfid,
      schedule_date: dayjs(record.scheduleDate),
      work_status: record.workStatus,
      work_type1: record.workType1,
      work_type3: record.workType3,
      work_type4: record.workType4,
      professional_promotion_team: record.professionalPromotionTeam,
      base_price: record.basePrice,
      daily_target_count: record.dailyTargetCount,
    });
    setModalOpen(true);
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      const formData = {
        employee_sfid: values.employee_sfid,
        schedule_date: values.schedule_date.format('YYYY-MM-DD'),
        work_status: values.work_status,
        work_type1: values.work_type1,
        work_type3: values.work_type3,
        work_type4: values.work_type4 || null,
        professional_promotion_team: values.professional_promotion_team || null,
        base_price: values.base_price ?? null,
        daily_target_count: values.daily_target_count ?? null,
      };

      if (editingEmployee) {
        await updatePeMutation.mutateAsync({ id: editingEmployee.id, data: formData });
        message.success('조원이 수정되었습니다');
      } else {
        await createPeMutation.mutateAsync({ promotionId, data: formData });
        message.success('조원이 추가되었습니다');
      }
      setModalOpen(false);
    } catch (err) {
      if (err instanceof Error) {
        message.error(err.message);
      }
    }
  };

  const handleDeleteEmployee = async (peId: number) => {
    try {
      await deletePeMutation.mutateAsync(peId);
      message.success('조원이 삭제되었습니다');
    } catch {
      message.error('조원 삭제에 실패했습니다');
    }
  };

  const employeeColumns: ColumnsType<PromotionEmployee> = [
    {
      title: '사원명',
      dataIndex: 'employeeName',
      width: 100,
      render: (name: string | null, record) => name ?? record.employeeSfid,
    },
    { title: '투입일', dataIndex: 'scheduleDate', width: 120 },
    { title: '근무상태', dataIndex: 'workStatus', width: 80 },
    {
      title: '근무유형1',
      dataIndex: 'workType1',
      width: 100,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '근무유형3',
      dataIndex: 'workType3',
      width: 80,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '전문행사조',
      dataIndex: 'professionalPromotionTeam',
      width: 120,
      render: (v: string | null) => v ?? '-',
    },
    {
      title: '확정',
      dataIndex: 'scheduleId',
      width: 70,
      render: (v: number | null) =>
        v != null ? <Tag color="blue">확정</Tag> : <Tag>미확정</Tag>,
    },
    {
      title: '마감',
      dataIndex: 'promoCloseByTm',
      width: 60,
      render: (v: boolean) => (v ? <Tag color="red">마감</Tag> : '-'),
    },
    {
      title: '관리',
      width: 100,
      render: (_, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => openEditModal(record)}>
            수정
          </Button>
          <Popconfirm
            title="이 조원을 삭제하시겠습니까?"
            onConfirm={() => handleDeleteEmployee(record.id)}
            okText="확인"
            cancelText="취소"
          >
            <Button type="link" size="small" danger>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (error || !promotion) {
    return (
      <div style={{ padding: 24 }}>
        <Button type="link" onClick={() => navigate('/promotions')}>
          ← 목록으로
        </Button>
        <div style={{ padding: 24, textAlign: 'center', color: '#999' }}>
          행사마스터를 찾을 수 없습니다.
        </div>
      </div>
    );
  }

  const categoryColor = promotion.category ? CATEGORY_TAG[promotion.category] : undefined;
  const typeColor = promotion.promotionTypeName
    ? PROMOTION_TYPE_TAG[promotion.promotionTypeName]
    : undefined;

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Button type="link" onClick={() => navigate('/promotions')} style={{ paddingLeft: 0 }}>
          ← 목록으로
        </Button>
        <Space>
          <Button onClick={() => navigate(`/promotions/${promotionId}/edit`)}>수정</Button>
          <Button danger onClick={handleDelete}>
            삭제
          </Button>
        </Space>
      </div>

      <Descriptions column={1} bordered>
        <Descriptions.Item label="행사번호">{promotion.promotionNumber}</Descriptions.Item>
        <Descriptions.Item label="행사명">
          {promotion.promotionName ? (
            <>
              {promotion.promotionName}
              <Typography.Text type="secondary"> (자동)</Typography.Text>
            </>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="행사유형">
          {promotion.promotionTypeName ? (
            <Tag color={typeColor}>{promotion.promotionTypeName}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="거래처">{promotion.accountName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="기간">
          {promotion.startDate} ~ {promotion.endDate}
        </Descriptions.Item>
        <Descriptions.Item label="대표상품">
          {promotion.primaryProductName ?? '-'}
        </Descriptions.Item>
        <Descriptions.Item label="기타상품">{promotion.otherProduct ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="비고">{promotion.remark ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="카테고리">
          {promotion.category ? (
            <Tag color={categoryColor}>{promotion.category}</Tag>
          ) : (
            '-'
          )}
        </Descriptions.Item>
        <Descriptions.Item label="제품유형">{promotion.productType ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="지점명">{promotion.branchName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="매대위치">{promotion.standLocation ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="목표금액">
          {promotion.targetAmount != null
            ? `${promotion.targetAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="실적금액">
          {promotion.actualAmount != null
            ? `${promotion.actualAmount.toLocaleString()}원`
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="마감 여부">
          {promotion.isClosed ? <Tag color="red">마감</Tag> : '미마감'}
        </Descriptions.Item>
        <Descriptions.Item label="메시지">{promotion.message ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="CC코드">{promotion.costCenterCode ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="생성일">
          {promotion.createdAt?.substring(0, 16)}
        </Descriptions.Item>
        <Descriptions.Item label="수정일">
          {promotion.updatedAt?.substring(0, 16)}
        </Descriptions.Item>
      </Descriptions>

      {/* 행사조원 섹션 */}
      <div style={{ marginTop: 32 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 16,
          }}
        >
          <Title level={5} style={{ margin: 0 }}>
            행사조원
          </Title>
          <Space>
            <Button disabled title="Spec #191에서 구현 예정">
              일정 확정
            </Button>
            <Button type="primary" onClick={openCreateModal}>
              + 조원 추가
            </Button>
          </Space>
        </div>

        <Table<PromotionEmployee>
          columns={employeeColumns}
          dataSource={employees ?? []}
          rowKey="id"
          size="small"
          pagination={false}
          loading={employeesLoading}
          locale={{ emptyText: '등록된 조원이 없습니다' }}
          footer={() => `총 ${employees?.length ?? 0}건`}
        />
      </div>

      {/* 조원 추가/수정 Modal */}
      <Modal
        title={editingEmployee ? '조원 수정' : '조원 추가'}
        open={modalOpen}
        onOk={handleModalOk}
        onCancel={() => setModalOpen(false)}
        okText="저장"
        cancelText="취소"
        confirmLoading={createPeMutation.isPending || updatePeMutation.isPending}
        width={520}
        destroyOnClose
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            label="여사원 SF ID"
            name="employee_sfid"
            rules={[
              { required: true, message: '여사원 SF ID는 필수입니다' },
              { max: 18, message: '최대 18자입니다' },
            ]}
          >
            <Input placeholder="a0B5g00000XYZabc" maxLength={18} />
          </Form.Item>

          <Form.Item
            label="투입일"
            name="schedule_date"
            rules={[{ required: true, message: '투입일은 필수입니다' }]}
          >
            <DatePicker format="YYYY-MM-DD" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="근무상태"
            name="work_status"
            rules={[{ required: true, message: '근무상태는 필수입니다' }]}
          >
            <Select options={WORK_STATUS_OPTIONS} placeholder="선택" />
          </Form.Item>

          <Form.Item
            label="근무유형1"
            name="work_type1"
            rules={[
              { required: true, message: '근무유형1은 필수입니다' },
              { max: 100, message: '최대 100자입니다' },
            ]}
          >
            <Input placeholder="시식, 시음 등" maxLength={100} />
          </Form.Item>

          <Form.Item
            label="근무유형3"
            name="work_type3"
            rules={[{ required: true, message: '근무유형3은 필수입니다' }]}
          >
            <Select options={WORK_TYPE3_OPTIONS} placeholder="선택" />
          </Form.Item>

          <Form.Item label="근무유형4" name="work_type4">
            <Input placeholder="제품유형 (선택)" maxLength={100} />
          </Form.Item>

          <Form.Item label="전문행사조" name="professional_promotion_team">
            <Input placeholder="전문행사조명 (선택)" maxLength={100} />
          </Form.Item>

          <Form.Item label="판매단가" name="base_price">
            <InputNumber min={0} style={{ width: '100%' }} placeholder="0" />
          </Form.Item>

          <Form.Item label="일일목표수량" name="daily_target_count">
            <InputNumber min={0} precision={0} style={{ width: '100%' }} placeholder="0" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
