import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Input, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useClaims } from '@/hooks/claims/useClaims';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { ClaimListItem } from '@/api/claims';

const { RangePicker } = DatePicker;

const STATUS_TAG: Record<string, { color: string; label: string }> = {
  SUBMITTED: { color: 'blue', label: '접수' },
  IN_PROGRESS: { color: 'orange', label: '처리중' },
  RESOLVED: { color: 'green', label: '처리완료' },
  REJECTED: { color: 'red', label: '반려' },
};

const STATUS_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'SUBMITTED', label: '접수' },
  { value: 'IN_PROGRESS', label: '처리중' },
  { value: 'RESOLVED', label: '처리완료' },
  { value: 'REJECTED', label: '반려' },
];

const PAGE_SIZE = 20;

export default function ClaimListPage() {
  const navigate = useNavigate();
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [status, setStatus] = useState<string>('');
  const [employeeName, setEmployeeName] = useState('');
  const [storeName, setStoreName] = useState('');
  const [page, setPage] = useState(0);

  // 검색 시 적용되는 파라미터 (검색 버튼 클릭 시 갱신)
  const [searchParams, setSearchParams] = useState({
    start_date: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    end_date: dayjs().format('YYYY-MM-DD'),
    status: undefined as string | undefined,
    employee_name: undefined as string | undefined,
    store_name: undefined as string | undefined,
    page: 0,
    size: PAGE_SIZE,
  });

  const { data, isLoading } = useClaims(searchParams);
  const handleRowClick = useThrottleClick((claimId: number) => navigate(`/claims/${claimId}`));

  const handleSearch = () => {
    setPage(0);
    setSearchParams({
      start_date: dateRange[0].format('YYYY-MM-DD'),
      end_date: dateRange[1].format('YYYY-MM-DD'),
      status: status || undefined,
      employee_name: employeeName || undefined,
      store_name: storeName || undefined,
      page: 0,
      size: PAGE_SIZE,
    });
  };

  const handlePageChange = (newPage: number) => {
    const zeroIndexedPage = newPage - 1;
    setPage(zeroIndexedPage);
    setSearchParams((prev) => ({ ...prev, page: zeroIndexedPage }));
  };

  const columns: ColumnsType<ClaimListItem> = [
    {
      title: 'No',
      width: 60,
      render: (_v, _r, index) => (searchParams.page) * PAGE_SIZE + index + 1,
    },
    {
      title: '사원명',
      dataIndex: 'employeeName',
      width: 100,
    },
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 90,
    },
    {
      title: '거래처명',
      dataIndex: 'storeName',
      width: 150,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '제품명',
      dataIndex: 'productName',
      width: 180,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '대분류',
      dataIndex: 'categoryName',
      width: 100,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '소분류',
      dataIndex: 'subcategoryName',
      width: 100,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '수량',
      dataIndex: 'defectQuantity',
      width: 70,
      render: (val: number | null) => val ?? '-',
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      render: (val: string) => {
        const tag = STATUS_TAG[val];
        return tag ? <Tag color={tag.color}>{tag.label}</Tag> : <Tag>{val}</Tag>;
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
          <span>상태:</span>
          <Select
            style={{ width: 120 }}
            value={status}
            options={STATUS_OPTIONS}
            onChange={setStatus}
          />
        </Space>
      </div>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 16, alignItems: 'center' }}>
        <Space wrap>
          <span>사원명:</span>
          <Input
            placeholder="사원명 검색"
            value={employeeName}
            onChange={(e) => setEmployeeName(e.target.value)}
            style={{ width: 160 }}
            onPressEnter={handleSearch}
          />
          <span>거래처명:</span>
          <Input
            placeholder="거래처명 검색"
            value={storeName}
            onChange={(e) => setStoreName(e.target.value)}
            style={{ width: 160 }}
            onPressEnter={handleSearch}
          />
          <Button type="primary" onClick={handleSearch}>
            검색
          </Button>
        </Space>
      </div>

      <Table
        rowKey="claimId"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: handlePageChange,
        }}
        onRow={(record) => ({
          onClick: () => handleRowClick(record.claimId),
          style: { cursor: 'pointer' },
        })}
      />
    </div>
  );
}
