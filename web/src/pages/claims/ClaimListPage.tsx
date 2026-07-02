import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Empty, Input, Pagination, Segmented, Select, Space, Spin, Tag } from 'antd';
import { AppstoreOutlined, TableOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useClaims } from '@/hooks/claims/useClaims';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import type { ClaimListItem } from '@/api/claims';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import ClaimCard from './components/ClaimCard';
import { STATUS_TAG } from './claimDisplay';

const { RangePicker } = DatePicker;

const STATUS_OPTIONS = [
  { value: '', label: '전체' },
  { value: 'DRAFT', label: '임시저장' },
  { value: 'SF_PENDING', label: '전송대기' },
  { value: 'SENT', label: '전송완료' },
  { value: 'SEND_FAILED', label: '전송실패' },
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
  const [viewMode, setViewMode] = useState<'table' | 'card'>('table');

  // 검색 시 적용되는 파라미터 (검색 버튼 클릭 시 갱신)
  const [searchParams, setSearchParams] = useState({
    startDate: dayjs().subtract(30, 'day').format('YYYY-MM-DD'),
    endDate: dayjs().format('YYYY-MM-DD'),
    status: undefined as string | undefined,
    employeeName: undefined as string | undefined,
    storeName: undefined as string | undefined,
    page: 0,
    size: PAGE_SIZE,
  });

  const { data, isLoading, refetch, isFetching } = useClaims(searchParams);
  const handleRowClick = useThrottleClick((claimId: number) => navigate(`/claims/${claimId}`));

  const handleSearch = () => {
    setPage(0);
    setSearchParams({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      status: status || undefined,
      employeeName: employeeName || undefined,
      storeName: storeName || undefined,
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
      dataIndex: 'categoryLabel',
      width: 100,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '소분류',
      dataIndex: 'subcategoryLabel',
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
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 12 }}>
        <Segmented
          value={viewMode}
          onChange={(v) => setViewMode(v as 'table' | 'card')}
          options={[
            { label: '테이블', value: 'table', icon: <TableOutlined /> },
            { label: '카드', value: 'card', icon: <AppstoreOutlined /> },
          ]}
        />
        <Space>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          <Button type="primary" onClick={() => navigate('/claims/new')}>
            신규 등록
          </Button>
        </Space>
      </div>
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
            조회
          </Button>
        </Space>
      </div>

      {viewMode === 'table' ? (
        <ResizableTable
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
      ) : (
        <Spin spinning={isLoading}>
          {data && data.content.length > 0 ? (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                gap: 16,
              }}
            >
              {data.content.map((claim) => (
                <ClaimCard key={claim.claimId} claim={claim} onClick={handleRowClick} />
              ))}
            </div>
          ) : (
            !isLoading && <Empty description="조회된 클레임이 없습니다" />
          )}
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
            <Pagination
              current={page + 1}
              total={data?.totalElements ?? 0}
              pageSize={PAGE_SIZE}
              showSizeChanger={false}
              showTotal={(total) => `총 ${total}건`}
              onChange={handlePageChange}
            />
          </div>
        </Spin>
      )}
    </div>
  );
}
