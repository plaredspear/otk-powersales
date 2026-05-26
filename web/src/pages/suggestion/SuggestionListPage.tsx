import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useSuggestions } from '@/hooks/suggestions/useSuggestions';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type {
  SuggestionActionStatus,
  SuggestionCategory,
  SuggestionListItem,
  SuggestionListParams,
} from '@/api/suggestions';

const { RangePicker } = DatePicker;

const CATEGORY_OPTIONS: Array<{ value: SuggestionCategory | ''; label: string }> = [
  { value: '', label: '전체' },
  { value: 'LOGISTICS_CLAIM', label: '물류 클레임' },
  { value: 'NEW_PRODUCT', label: '신제품 제안' },
  { value: 'EXISTING_PRODUCT', label: '기존제품 상품가치 향상' },
];

const ACTION_STATUS_OPTIONS: Array<{ value: SuggestionActionStatus | ''; label: string }> = [
  { value: '', label: '전체' },
  { value: 'UNCONFIRMED', label: '미확인' },
  { value: 'IN_PROGRESS', label: '조치중' },
  { value: 'COMPLETED', label: '조치완료' },
  { value: 'DUPLICATE_RECEPTION', label: '중복접수' },
];

const ACTION_STATUS_TAG: Record<SuggestionActionStatus, { color: string; label: string }> = {
  UNCONFIRMED: { color: 'default', label: '미확인' },
  IN_PROGRESS: { color: 'orange', label: '조치중' },
  COMPLETED: { color: 'green', label: '조치완료' },
  DUPLICATE_RECEPTION: { color: 'red', label: '중복접수' },
};

const PAGE_SIZE = 20;

export default function SuggestionListPage() {
  const navigate = useNavigate();
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [category, setCategory] = useState<SuggestionCategory | ''>('');
  const [actionStatus, setActionStatus] = useState<SuggestionActionStatus | ''>('');
  const [employeeName, setEmployeeName] = useState('');
  const [accountCode, setAccountCode] = useState('');
  const [productCode, setProductCode] = useState('');
  const [page, setPage] = useState(0);

  const [searchParams, setSearchParams] = useState<SuggestionListParams>({
    startDate: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD'),
    page: 0,
    size: PAGE_SIZE,
  });

  const { data, isLoading } = useSuggestions(searchParams);
  const handleRowClick = useThrottleClick((id: number) => navigate(`/suggestion/${id}`));

  const handleSearch = () => {
    setPage(0);
    setSearchParams({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      category: (category || undefined) as SuggestionCategory | undefined,
      actionStatus: (actionStatus || undefined) as SuggestionActionStatus | undefined,
      employeeName: employeeName || undefined,
      accountCode: accountCode || undefined,
      productCode: productCode || undefined,
      page: 0,
      size: PAGE_SIZE,
    });
  };

  const handleReset = () => {
    setDateRange([dayjs().subtract(30, 'day'), dayjs()]);
    setCategory('');
    setActionStatus('');
    setEmployeeName('');
    setAccountCode('');
    setProductCode('');
  };

  const handlePageChange = (newPage: number) => {
    const zeroIndexedPage = newPage - 1;
    setPage(zeroIndexedPage);
    setSearchParams((prev) => ({ ...prev, page: zeroIndexedPage }));
  };

  const columns: ColumnsType<SuggestionListItem> = [
    {
      title: 'No',
      width: 60,
      render: (_v, _r, index) => (searchParams.page ?? 0) * PAGE_SIZE + index + 1,
    },
    { title: '제안번호', dataIndex: 'proposalNumber', width: 160 },
    { title: '카테고리', dataIndex: 'categoryName', width: 140 },
    { title: '제목', dataIndex: 'title', width: 220, ellipsis: true },
    {
      title: '작성자',
      dataIndex: 'employeeName',
      width: 100,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 90,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '제품',
      dataIndex: 'productName',
      width: 160,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '클레임 항목',
      dataIndex: 'claimType',
      width: 120,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '조치상태',
      dataIndex: 'actionStatus',
      width: 100,
      render: (val: SuggestionActionStatus | null) => {
        if (!val) return '-';
        const tag = ACTION_STATUS_TAG[val];
        return <Tag color={tag.color}>{tag.label}</Tag>;
      },
    },
    {
      title: '등록일',
      dataIndex: 'createdAt',
      width: 100,
      render: (val: string) => val?.substring(0, 10),
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <Space wrap>
          <span>등록일:</span>
          <RangePicker
            value={dateRange}
            onChange={(dates) => {
              if (dates && dates[0] && dates[1]) {
                setDateRange([dates[0], dates[1]]);
              }
            }}
            format="YYYY-MM-DD"
          />
          <span>카테고리:</span>
          <Select
            style={{ width: 180 }}
            value={category}
            options={CATEGORY_OPTIONS}
            onChange={(v) => setCategory(v as SuggestionCategory | '')}
          />
          <span>조치상태:</span>
          <Select
            style={{ width: 130 }}
            value={actionStatus}
            options={ACTION_STATUS_OPTIONS}
            onChange={(v) => setActionStatus(v as SuggestionActionStatus | '')}
          />
        </Space>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <Space wrap>
          <span>작성자명:</span>
          <Input
            placeholder="작성자명"
            value={employeeName}
            onChange={(e) => setEmployeeName(e.target.value)}
            style={{ width: 140 }}
            onPressEnter={handleSearch}
          />
          <span>거래처 코드:</span>
          <Input
            placeholder="거래처 코드"
            value={accountCode}
            onChange={(e) => setAccountCode(e.target.value)}
            style={{ width: 140 }}
            onPressEnter={handleSearch}
          />
          <span>제품 코드:</span>
          <Input
            placeholder="제품 코드"
            value={productCode}
            onChange={(e) => setProductCode(e.target.value)}
            style={{ width: 140 }}
            onPressEnter={handleSearch}
          />
          <Button type="primary" onClick={handleSearch}>검색</Button>
          <Button onClick={handleReset}>초기화</Button>
          <Button type="default" onClick={() => navigate('/suggestion/new')}>신규 등록</Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 1500 }}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: handlePageChange,
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.id),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
