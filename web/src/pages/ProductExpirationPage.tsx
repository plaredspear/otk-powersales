import { useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
} from 'antd';
import { CalendarOutlined, DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import {
  useProductExpirations,
  useProductExpirationSummary,
  useCreateProductExpiration,
  useUpdateProductExpiration,
  useDeleteProductExpiration,
  useBatchDeleteProductExpiration,
} from '@/hooks/productExpiration/useProductExpirations';
import { fetchEmployeesForProductLookup } from '@/api/employee';
import { fetchAccountsForProductLookup } from '@/api/account';
import { fetchProducts } from '@/api/product';
import type { ProductExpiration, CreateProductExpirationRequest, UpdateProductExpirationRequest } from '@/api/productExpiration';
import { useAuthStore } from '@/stores/authStore';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { RangePicker } = DatePicker;

const STATUS_OPTIONS = [
  { label: '전체', value: '' },
  { label: '만료', value: 'EXPIRED' },
  { label: '임박', value: 'IMMINENT' },
  { label: '정상', value: 'NORMAL' },
];

export default function ProductExpirationPage() {
  const user = useAuthStore((s) => s.user);

  // Filter state
  const [employeeKeyword, setEmployeeKeyword] = useState('');
  const [accountKeyword, setAccountKeyword] = useState('');
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs] | null>(null);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(1);

  // Applied filter (only updates on search click)
  const [appliedFilter, setAppliedFilter] = useState({
    employeeKeyword: '',
    accountKeyword: '',
    fromDate: '',
    toDate: '',
    status: '',
  });

  // Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editRecord, setEditRecord] = useState<ProductExpiration | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);

  // Queries
  const { data, isLoading, refetch, isFetching } = useProductExpirations({
    ...appliedFilter,
    fromDate: appliedFilter.fromDate || undefined,
    toDate: appliedFilter.toDate || undefined,
    status: appliedFilter.status || undefined,
    employeeKeyword: appliedFilter.employeeKeyword || undefined,
    accountKeyword: appliedFilter.accountKeyword || undefined,
    page: page - 1,
    size: 20,
  });
  const { data: summary } = useProductExpirationSummary();

  // Mutations
  const createMutation = useCreateProductExpiration();
  const updateMutation = useUpdateProductExpiration();
  const deleteMutation = useDeleteProductExpiration();
  const batchDeleteMutation = useBatchDeleteProductExpiration();

  const handleSearch = () => {
    setPage(1);
    setAppliedFilter({
      employeeKeyword: employeeKeyword,
      accountKeyword: accountKeyword,
      fromDate: dateRange ? dateRange[0].format('YYYY-MM-DD') : '',
      toDate: dateRange ? dateRange[1].format('YYYY-MM-DD') : '',
      status,
    });
  };

  const handleReset = () => {
    setEmployeeKeyword('');
    setAccountKeyword('');
    setDateRange(null);
    setStatus('');
    setPage(1);
    setAppliedFilter({
      employeeKeyword: '',
      accountKeyword: '',
      fromDate: '',
      toDate: '',
      status: '',
    });
  };

  const handleSummaryClick = (statusValue: string) => {
    setStatus(statusValue);
    setPage(1);
    setAppliedFilter((prev) => ({ ...prev, status: statusValue }));
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '삭제 확인',
      content: '이 유통기한 데이터를 삭제하시겠습니까?',
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteMutation.mutateAsync(id);
        message.success('유통기한이 삭제되었습니다.');
      },
    });
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) return;
    Modal.confirm({
      title: '일괄 삭제 확인',
      content: `선택한 ${selectedRowKeys.length}건의 유통기한 데이터를 삭제하시겠습니까?`,
      okButtonProps: { danger: true },
      onOk: async () => {
        const count = await batchDeleteMutation.mutateAsync(selectedRowKeys);
        setSelectedRowKeys([]);
        message.success(`${count}건의 유통기한이 삭제되었습니다.`);
      },
    });
  };

  const columns: ColumnsType<ProductExpiration> = [
    {
      title: '제품명',
      width: 200,
      render: (_, r) => (
        <div>
          <div>{r.productName}</div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {r.productCode}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: '거래처',
      width: 160,
      render: (_, r) => (
        <div>
          <div>{r.accountName}</div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {r.accountCode}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: '사원명(사번)',
      width: 130,
      render: (_, r) => (
        <div>
          <div>{r.employeeName}</div>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            {r.employeeCode}
          </Typography.Text>
        </div>
      ),
    },
    {
      title: '유통기한',
      dataIndex: 'expirationDate',
      width: 110,
    },
    {
      title: '알림일',
      dataIndex: 'alarmDate',
      width: 110,
    },
    {
      title: 'D-day',
      dataIndex: 'dDay',
      width: 80,
      render: (dDay: number) => {
        if (dDay > 0) return <Tag>{dDay}일</Tag>;
        if (dDay === 0) return <Tag color="error">D-DAY</Tag>;
        return <Tag color="error">+{Math.abs(dDay)}일</Tag>;
      },
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 80,
      render: (s: string) => {
        const colorMap: Record<string, string> = {
          NORMAL: 'default',
          IMMINENT: 'warning',
          EXPIRED: 'error',
        };
        const labelMap: Record<string, string> = {
          NORMAL: '정상',
          IMMINENT: '임박',
          EXPIRED: '만료',
        };
        return <Tag color={colorMap[s]}>{labelMap[s]}</Tag>;
      },
    },
    {
      title: '설명',
      dataIndex: 'description',
      width: 150,
      ellipsis: true,
      render: (v: string | null) => v || '-',
    },
    {
      title: '액션',
      width: 80,
      render: (_, r) => (
        <Space>
          <Button type="link" size="small" icon={<EditOutlined />} onClick={() => setEditRecord(r)} />
          <Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => handleDelete(r.id)} />
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* Scope Badge */}
      {user?.role === '조장' && (
        <Tag color="blue" style={{ marginBottom: 16, fontSize: 14, padding: '4px 12px' }}>
          내 팀 ({user.orgName ?? ''})
        </Tag>
      )}
      {user?.role === '여사원' && (
        <Tag style={{ marginBottom: 16, fontSize: 14, padding: '4px 12px' }}>
          내 데이터
        </Tag>
      )}

      {/* Summary Cards */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}>
          <Card hoverable onClick={() => handleSummaryClick('')} style={{ cursor: 'pointer' }}>
            <Statistic title="전체" value={summary?.totalCount ?? '-'} prefix={<CalendarOutlined />} />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable onClick={() => handleSummaryClick('EXPIRED')} style={{ cursor: 'pointer' }}>
            <Statistic title="만료" value={summary?.expiredCount ?? '-'} valueStyle={{ color: '#ff4d4f' }} />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable onClick={() => handleSummaryClick('IMMINENT')} style={{ cursor: 'pointer' }}>
            <Statistic title="임박" value={summary?.imminentCount ?? '-'} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
      </Row>

      {/* Filter Area */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 12]}>
          <Col span={6}>
            <Input
              placeholder="사원명/사번"
              value={employeeKeyword}
              onChange={(e) => setEmployeeKeyword(e.target.value)}
              onPressEnter={handleSearch}
            />
          </Col>
          <Col span={6}>
            <Input
              placeholder="거래처명/코드"
              value={accountKeyword}
              onChange={(e) => setAccountKeyword(e.target.value)}
              onPressEnter={handleSearch}
            />
          </Col>
          <Col span={6}>
            <RangePicker
              value={dateRange}
              onChange={(dates) => {
                if (dates && dates[0] && dates[1]) {
                  setDateRange([dates[0], dates[1]]);
                } else {
                  setDateRange(null);
                }
              }}
              placeholder={['시작일', '종료일']}
              style={{ width: '100%' }}
            />
          </Col>
          <Col span={3}>
            <Select
              value={status}
              onChange={setStatus}
              options={STATUS_OPTIONS}
              style={{ width: '100%' }}
            />
          </Col>
          <Col span={3}>
            <Space>
              <Button onClick={handleReset}>초기화</Button>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                검색
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Action Buttons */}
      <Space style={{ marginBottom: 16 }}>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>
          등록
        </Button>
        <Button danger disabled={selectedRowKeys.length === 0} onClick={handleBatchDelete}>
          선택 삭제 ({selectedRowKeys.length})
        </Button>
      </Space>

      {/* Table */}
      <ResizableTable<ProductExpiration>
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as number[]),
        }}
        pagination={{
          current: page,
          pageSize: 20,
          total: data?.totalElements,
          showTotal: (total) => `총 ${total}건`,
          onChange: setPage,
        }}
        scroll={{ x: 1200 }}
      />

      {/* Create Modal */}
      <CreateModal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        mutation={createMutation}
      />

      {/* Edit Modal */}
      <EditModal
        record={editRecord}
        onClose={() => setEditRecord(null)}
        mutation={updateMutation}
      />
    </div>
  );
}

// --- Create Modal ---

interface SearchOption {
  label: string;
  value: string;
}

function CreateModal({
  open,
  onClose,
  mutation,
}: {
  open: boolean;
  onClose: () => void;
  mutation: ReturnType<typeof useCreateProductExpiration>;
}) {
  const [form] = Form.useForm();
  const [employeeOptions, setEmployeeOptions] = useState<SearchOption[]>([]);
  const [accountOptions, setAccountOptions] = useState<SearchOption[]>([]);
  const [productOptions, setProductOptions] = useState<SearchOption[]>([]);
  const [employeeLoading, setEmployeeLoading] = useState(false);
  const [accountLoading, setAccountLoading] = useState(false);
  const [productLoading, setProductLoading] = useState(false);

  const searchEmployees = debounce(async (keyword: string) => {
    if (!keyword) return;
    setEmployeeLoading(true);
    try {
      const data = await fetchEmployeesForProductLookup({ keyword, size: 20 });
      setEmployeeOptions(data.content.map((e) => ({
        label: `${e.name} (${e.employeeCode})`,
        value: e.employeeCode,
      })));
    } finally {
      setEmployeeLoading(false);
    }
  }, 300);

  const searchAccounts = debounce(async (keyword: string) => {
    if (!keyword) return;
    setAccountLoading(true);
    try {
      const data = await fetchAccountsForProductLookup({ keyword, size: 20 });
      setAccountOptions(data.content.map((a) => ({
        label: `${a.name ?? ''} (${a.externalKey ?? ''})`,
        value: a.externalKey ?? '',
      })));
    } finally {
      setAccountLoading(false);
    }
  }, 300);

  const searchProducts = debounce(async (keyword: string) => {
    if (!keyword) return;
    setProductLoading(true);
    try {
      const data = await fetchProducts({ keyword, size: 20 });
      setProductOptions(data.content.map((p) => ({
        label: `${p.name ?? ''} (${p.productCode ?? ''})`,
        value: p.productCode ?? '',
      })));
    } finally {
      setProductLoading(false);
    }
  }, 300);

  const handleExpirationDateChange = (date: Dayjs | null) => {
    if (date) {
      form.setFieldsValue({ alarmDate: date.subtract(1, 'day') });
    }
  };

  const handleFinish = async (values: {
    employeeCode: string;
    accountCode: string;
    productCode: string;
    expirationDate: Dayjs;
    alarmDate: Dayjs;
    description?: string;
  }) => {
    const payload: CreateProductExpirationRequest = {
      employeeCode: values.employeeCode,
      accountCode: values.accountCode,
      productCode: values.productCode,
      expirationDate: values.expirationDate.format('YYYY-MM-DD'),
      alarmDate: values.alarmDate.format('YYYY-MM-DD'),
      description: values.description,
    };
    await mutation.mutateAsync(payload);
    message.success('유통기한이 등록되었습니다.');
    form.resetFields();
    setEmployeeOptions([]);
    setAccountOptions([]);
    setProductOptions([]);
    onClose();
  };

  const handleCancel = () => {
    form.resetFields();
    setEmployeeOptions([]);
    setAccountOptions([]);
    setProductOptions([]);
    onClose();
  };

  const expirationDate: Dayjs | undefined = Form.useWatch('expirationDate', form);

  return (
    <Modal
      title="유통기한 등록"
      open={open}
      onCancel={handleCancel}
      footer={null}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" onFinish={handleFinish}>
        <Form.Item name="employeeCode" label="사원" rules={[{ required: true, message: '사원을 선택해주세요' }]}>
          <Select
            showSearch
            filterOption={false}
            onSearch={searchEmployees}
            options={employeeOptions}
            loading={employeeLoading}
            placeholder="사번 또는 이름 검색"
            notFoundContent={employeeLoading ? '검색 중...' : '검색어를 입력하세요'}
          />
        </Form.Item>
        <Form.Item name="accountCode" label="거래처" rules={[{ required: true, message: '거래처를 선택해주세요' }]}>
          <Select
            showSearch
            filterOption={false}
            onSearch={searchAccounts}
            options={accountOptions}
            loading={accountLoading}
            placeholder="거래처명 또는 코드 검색"
            notFoundContent={accountLoading ? '검색 중...' : '검색어를 입력하세요'}
          />
        </Form.Item>
        <Form.Item name="productCode" label="제품" rules={[{ required: true, message: '제품을 선택해주세요' }]}>
          <Select
            showSearch
            filterOption={false}
            onSearch={searchProducts}
            options={productOptions}
            loading={productLoading}
            placeholder="제품명 또는 코드 검색"
            notFoundContent={productLoading ? '검색 중...' : '검색어를 입력하세요'}
          />
        </Form.Item>
        <Form.Item name="expirationDate" label="유통기한" rules={[{ required: true, message: '유통기한을 선택해주세요' }]}>
          <DatePicker
            style={{ width: '100%' }}
            disabledDate={(d) => d.isBefore(dayjs(), 'day')}
            onChange={handleExpirationDateChange}
          />
        </Form.Item>
        <Form.Item name="alarmDate" label="알림일" rules={[{ required: true, message: '알림일을 선택해주세요' }]}>
          <DatePicker
            style={{ width: '100%' }}
            disabledDate={(d) => {
              if (d.isBefore(dayjs(), 'day')) return true;
              if (expirationDate && !d.isBefore(expirationDate, 'day')) return true;
              return false;
            }}
          />
        </Form.Item>
        <Form.Item name="description" label="설명">
          <Input.TextArea maxLength={500} showCount rows={3} />
        </Form.Item>
        <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
          <Space>
            <Button onClick={handleCancel}>취소</Button>
            <Button type="primary" htmlType="submit" loading={mutation.isPending}>
              등록
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Modal>
  );
}

// --- Edit Modal ---

function EditModal({
  record,
  onClose,
  mutation,
}: {
  record: ProductExpiration | null;
  onClose: () => void;
  mutation: ReturnType<typeof useUpdateProductExpiration>;
}) {
  const [form] = Form.useForm();
  const open = !!record;

  const handleFinish = async (values: {
    expirationDate: Dayjs;
    alarmDate: Dayjs;
    description?: string;
  }) => {
    if (!record) return;
    const payload: UpdateProductExpirationRequest = {
      expirationDate: values.expirationDate.format('YYYY-MM-DD'),
      alarmDate: values.alarmDate.format('YYYY-MM-DD'),
      description: values.description,
    };
    await mutation.mutateAsync({ id: record.id, data: payload });
    message.success('유통기한이 수정되었습니다.');
    onClose();
  };

  const handleExpirationDateChange = (date: Dayjs | null) => {
    if (date) {
      form.setFieldsValue({ alarmDate: date.subtract(1, 'day') });
    }
  };

  const expirationDate: Dayjs | undefined = Form.useWatch('expirationDate', form);

  return (
    <Modal
      title="유통기한 수정"
      open={open}
      onCancel={onClose}
      footer={null}
      destroyOnHidden
    >
      {record && (
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFinish}
          initialValues={{
            expirationDate: record.expirationDate ? dayjs(record.expirationDate) : undefined,
            alarmDate: record.alarmDate ? dayjs(record.alarmDate) : undefined,
            description: record.description ?? '',
          }}
        >
          <div style={{ marginBottom: 16 }}>
            <Row gutter={[16, 8]}>
              <Col span={8}><Typography.Text type="secondary">사원</Typography.Text></Col>
              <Col span={16}>{record.employeeName} ({record.employeeCode})</Col>
              <Col span={8}><Typography.Text type="secondary">거래처</Typography.Text></Col>
              <Col span={16}>{record.accountName} ({record.accountCode})</Col>
              <Col span={8}><Typography.Text type="secondary">제품</Typography.Text></Col>
              <Col span={16}>{record.productName} ({record.productCode})</Col>
            </Row>
          </div>
          <Form.Item name="expirationDate" label="유통기한" rules={[{ required: true, message: '유통기한을 선택해주세요' }]}>
            <DatePicker style={{ width: '100%' }} onChange={handleExpirationDateChange} />
          </Form.Item>
          <Form.Item name="alarmDate" label="알림일" rules={[{ required: true, message: '알림일을 선택해주세요' }]}>
            <DatePicker
              style={{ width: '100%' }}
              disabledDate={(d) => {
                if (expirationDate && !d.isBefore(expirationDate, 'day')) return true;
                return false;
              }}
            />
          </Form.Item>
          <Form.Item name="description" label="설명">
            <Input.TextArea maxLength={500} showCount rows={3} />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={onClose}>취소</Button>
              <Button type="primary" htmlType="submit" loading={mutation.isPending}>
                저장
              </Button>
            </Space>
          </Form.Item>
        </Form>
      )}
    </Modal>
  );
}

// --- Utility ---

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function debounce<T extends (...args: any[]) => any>(fn: T, delay: number) {
  let timer: ReturnType<typeof setTimeout>;
  return (...args: Parameters<T>) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}
