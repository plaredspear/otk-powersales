import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  DatePicker,
  Form,
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import { CheckCircleFilled, ClockCircleFilled, CloseCircleFilled, PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useAccountCategoryOptions,
  useBulkConfirmEmployeeInputCriteriaMasters,
  useConfirmEmployeeInputCriteriaMaster,
  useCreateEmployeeInputCriteriaMaster,
  useDeleteEmployeeInputCriteriaMaster,
  useEmployeeInputCriteriaMasters,
  useUpdateEmployeeInputCriteriaMaster,
} from '@/hooks/employee-input-criteria-master/useEmployeeInputCriteriaMasters';
import type {
  EmployeeInputCriteriaMaster,
  EmployeeInputCriteriaMasterRequest,
  TypeOfWork1,
  ValidStatusFilter,
} from '@/api/employeeInputCriteriaMaster';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Title, Text } = Typography;

const TYPE_OF_WORK_OPTIONS: { value: TypeOfWork1; label: string }[] = [{ value: '진열', label: '진열' }];

const STATUS_OPTIONS: { value: ValidStatusFilter; label: string }[] = [
  { value: 'ALL', label: '전체' },
  { value: 'VALID', label: '유효' },
  { value: 'PLANNED', label: '예정' },
  { value: 'ENDED', label: '종료' },
];

interface FormValues {
  categoryId: number;
  typeOfWork1?: TypeOfWork1 | null;
  startDate: dayjs.Dayjs;
  endDate?: dayjs.Dayjs | null;
  boundary: number;
  fixed1PersonStandardAmount: number;
  bifurcationHalfPersonStandard: number;
}

function ValidIcon({ status }: { status: EmployeeInputCriteriaMaster['validData'] }) {
  if (status === '유효') return <CheckCircleFilled style={{ color: '#52c41a', fontSize: 18 }} title="유효" />;
  if (status === '예정') return <ClockCircleFilled style={{ color: '#faad14', fontSize: 18 }} title="예정" />;
  if (status === '종료') return <CloseCircleFilled style={{ color: '#ff4d4f', fontSize: 18 }} title="종료" />;
  return <Text type="secondary">-</Text>;
}

function formatNumber(value: string | null | undefined): string {
  if (value == null || value === '') return '-';
  const num = Number(value);
  if (Number.isNaN(num)) return value;
  return num.toLocaleString();
}

export default function EmployeeInputCriteriaMasterListPage() {
  const [status, setStatus] = useState<ValidStatusFilter>('ALL');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<EmployeeInputCriteriaMaster | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [form] = Form.useForm<FormValues>();

  const { data: items, isLoading, refetch, isFetching } = useEmployeeInputCriteriaMasters(status);
  const { data: categories } = useAccountCategoryOptions();
  const createMutation = useCreateEmployeeInputCriteriaMaster();
  const updateMutation = useUpdateEmployeeInputCriteriaMaster();
  const confirmMutation = useConfirmEmployeeInputCriteriaMaster();
  const bulkConfirmMutation = useBulkConfirmEmployeeInputCriteriaMasters();
  const deleteMutation = useDeleteEmployeeInputCriteriaMaster();

  const categoryOptions = useMemo(
    () =>
      (categories ?? []).map((c) => ({
        value: c.id,
        label: `${c.accountCode} · ${c.name}`,
      })),
    [categories],
  );

  const handleAdd = () => {
    setEditingItem(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record: EmployeeInputCriteriaMaster) => {
    setEditingItem(record);
    form.setFieldsValue({
      categoryId: record.categoryId ?? undefined!,
      typeOfWork1: record.typeOfWork1,
      startDate: record.startDate ? dayjs(record.startDate) : undefined!,
      endDate: record.endDate ? dayjs(record.endDate) : null,
      boundary: record.boundary != null ? Number(record.boundary) : undefined!,
      fixed1PersonStandardAmount:
        record.fixed1PersonStandardAmount != null ? Number(record.fixed1PersonStandardAmount) : undefined!,
      bifurcationHalfPersonStandard:
        record.bifurcationHalfPersonStandard != null
          ? Number(record.bifurcationHalfPersonStandard)
          : undefined!,
    });
    setModalOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      const payload: EmployeeInputCriteriaMasterRequest = {
        categoryId: values.categoryId,
        typeOfWork1: values.typeOfWork1 ?? null,
        startDate: values.startDate.format('YYYY-MM-DD'),
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : null,
        boundary: String(values.boundary),
        fixed1PersonStandardAmount: String(values.fixed1PersonStandardAmount),
        bifurcationHalfPersonStandard: String(values.bifurcationHalfPersonStandard),
      };
      if (editingItem) {
        await updateMutation.mutateAsync({ id: editingItem.id, data: payload });
        message.success('수정되었습니다');
      } else {
        await createMutation.mutateAsync(payload);
        message.success('등록되었습니다');
      }
      setModalOpen(false);
    } catch (err) {
      if (err && typeof err === 'object' && 'message' in err && typeof (err as { message: unknown }).message === 'string') {
        message.error((err as { message: string }).message);
      }
    }
  };

  const handleConfirm = async (record: EmployeeInputCriteriaMaster) => {
    try {
      await confirmMutation.mutateAsync(record.id);
      message.success('확정되었습니다');
    } catch (err) {
      const msg = (err as { message?: string })?.message ?? '확정에 실패했습니다';
      message.error(msg);
    }
  };

  const handleBulkConfirm = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('확정할 레코드를 선택해주세요');
      return;
    }
    try {
      await bulkConfirmMutation.mutateAsync(selectedRowKeys.map((k) => Number(k)));
      message.success('일괄 확정이 완료되었습니다');
      setSelectedRowKeys([]);
    } catch (err) {
      const msg = (err as { message?: string })?.message ?? '일괄 확정에 실패했습니다';
      message.error(msg);
    }
  };

  const handleDelete = async (record: EmployeeInputCriteriaMaster) => {
    try {
      await deleteMutation.mutateAsync(record.id);
      message.success('삭제되었습니다');
    } catch (err) {
      const msg = (err as { message?: string })?.message ?? '삭제에 실패했습니다';
      message.error(msg);
    }
  };

  const columns: ColumnsType<EmployeeInputCriteriaMaster> = [
    {
      title: '유효',
      dataIndex: 'validData',
      width: 60,
      align: 'center',
      render: (value: EmployeeInputCriteriaMaster['validData']) => <ValidIcon status={value} />,
    },
    {
      title: '거래처유형코드',
      dataIndex: 'accountCategorizedCode',
      width: 130,
      render: (value: string | null) => value ?? '-',
    },
    {
      title: '구분',
      dataIndex: 'categoryName',
      width: 160,
      render: (value: string | null) => value ?? '-',
    },
    {
      title: '근무형태',
      dataIndex: 'typeOfWork1',
      width: 100,
      render: (value: TypeOfWork1 | null) => value ?? '-',
    },
    {
      title: '경계율',
      dataIndex: 'boundary',
      width: 90,
      align: 'right',
      render: (value: string | null) => (value != null ? `${value}%` : '-'),
    },
    {
      title: '고정1명 기준금액',
      dataIndex: 'fixed1PersonStandardAmount',
      width: 140,
      align: 'right',
      render: formatNumber,
    },
    {
      title: '격고0.5명 기준금액',
      dataIndex: 'bifurcationHalfPersonStandard',
      width: 150,
      align: 'right',
      render: formatNumber,
    },
    {
      title: '시작일',
      dataIndex: 'startDate',
      width: 110,
      render: (value: string | null) => value ?? '-',
    },
    {
      title: '종료일',
      dataIndex: 'endDate',
      width: 110,
      render: (value: string | null) => value ?? '무기한',
    },
    {
      title: '확정',
      dataIndex: 'confirmed',
      width: 80,
      align: 'center',
      render: (value: boolean) =>
        value ? <Tag color="green">확정</Tag> : <Tag color="default">미확정</Tag>,
    },
    {
      title: '관리',
      width: 220,
      align: 'center',
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            수정
          </Button>
          {!record.confirmed && (
            <Popconfirm
              title="확정"
              description="확정 후에는 「종료일」이외는 편집할 수 없습니다. 진행하시겠습니까?"
              onConfirm={() => handleConfirm(record)}
              okText="확인"
              cancelText="취소"
            >
              <Button type="link" size="small">
                확정
              </Button>
            </Popconfirm>
          )}
          <Popconfirm
            title="삭제"
            description="해당 레코드를 참조하던 월별 여사원 통합일정의 참조가 비워집니다. 진행하시겠습니까?"
            onConfirm={() => handleDelete(record)}
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

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <div style={{ padding: 16 }}>
      <Title level={4} style={{ marginTop: 0 }}>
        진열사원 투입기준 마스터
      </Title>
      <Alert
        type="warning"
        showIcon
        style={{ marginBottom: 12 }}
        message="일괄 확정 후에는 편집할 수 없습니다. 수정하려면 영업지원실에 연락하십시오."
      />

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
          flexWrap: 'wrap',
          gap: 8,
        }}
      >
        <Radio.Group
          value={status}
          onChange={(e) => setStatus(e.target.value as ValidStatusFilter)}
          optionType="button"
          options={STATUS_OPTIONS}
        />
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Popconfirm
            title="일괄 확정"
            description={`선택한 ${selectedRowKeys.length}건을 일괄 확정합니다. 진행하시겠습니까?`}
            onConfirm={handleBulkConfirm}
            okText="확인"
            cancelText="취소"
            disabled={selectedRowKeys.length === 0}
          >
            <Button disabled={selectedRowKeys.length === 0} loading={bulkConfirmMutation.isPending}>
              일괄 확정 ({selectedRowKeys.length})
            </Button>
          </Popconfirm>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            신규 등록
          </Button>
        </Space>
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={items}
        loading={isLoading}
        pagination={false}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
          getCheckboxProps: (record) => ({ disabled: record.confirmed }),
        }}
        scroll={{ x: 1400 }}
      />

      <Modal
        title={editingItem ? '진열사원 투입기준 마스터 수정' : '진열사원 투입기준 마스터 등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        width={640}
        footer={
          <Space>
            <Button onClick={() => setModalOpen(false)}>취소</Button>
            <Button type="primary" onClick={handleSave} loading={isSaving}>
              저장
            </Button>
          </Space>
        }
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="categoryId"
            label="구분 (거래처유형마스터)"
            rules={[{ required: true, message: '구분을 선택해주세요' }]}
          >
            <Select
              options={categoryOptions}
              placeholder="거래처유형을 선택해주세요"
              showSearch
              filterOption={(input, option) =>
                String(option?.label ?? '').toLowerCase().includes(input.toLowerCase())
              }
            />
          </Form.Item>
          <Form.Item name="typeOfWork1" label="근무형태1">
            <Select allowClear options={TYPE_OF_WORK_OPTIONS} placeholder="선택" />
          </Form.Item>
          <Form.Item
            name="startDate"
            label="시작일 (월 단위 — 자동 보정)"
            rules={[{ required: true, message: '시작일을 선택해주세요' }]}
            extra="저장 시 해당 월의 1일로 자동 보정됩니다"
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="endDate"
            label="종료일 (월 단위 — 자동 보정, 비워두면 무기한)"
            extra="저장 시 해당 월의 말일로 자동 보정됩니다"
          >
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item
            name="boundary"
            label="경계율 (%)"
            rules={[{ required: true, message: '경계율을 입력해주세요' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} max={100} step={1} addonAfter="%" />
          </Form.Item>
          <Form.Item
            name="fixed1PersonStandardAmount"
            label="고정1명 기준금액"
            rules={[{ required: true, message: '고정1명 기준금액을 입력해주세요' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} step={100000} />
          </Form.Item>
          <Form.Item
            name="bifurcationHalfPersonStandard"
            label="격고0.5명 기준금액"
            rules={[{ required: true, message: '격고0.5명 기준금액을 입력해주세요' }]}
          >
            <InputNumber style={{ width: '100%' }} min={0} step={100000} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
