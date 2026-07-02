import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Empty, Input, Pagination, Segmented, Select, Space, Spin, Tag } from 'antd';
import { AppstoreOutlined, TableOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs, { type Dayjs } from 'dayjs';
import { useClaims } from '@/hooks/claims/useClaims';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { ClaimListItem } from '@/api/claims';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination, PAGE_SIZE_OPTIONS } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
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

const DEFAULT_START_DATE = dayjs().subtract(30, 'day').format('YYYY-MM-DD');
const DEFAULT_END_DATE = dayjs().format('YYYY-MM-DD');

export default function ClaimListPage() {
  const navigate = useNavigate();
  // page/필터/사이즈를 URL query string 에 보관 — 상세 진입 후 복귀/새로고침/링크 공유 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: {
      startDate: DEFAULT_START_DATE,
      endDate: DEFAULT_END_DATE,
      status: '',
      employeeName: '',
      storeName: '',
    },
  });

  // 입력 위젯은 편집 버퍼(로컬 state). URL filters 가 source of truth → 마운트 시 URL 값으로 초기화.
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>(() => [
    dayjs(filters.startDate),
    dayjs(filters.endDate),
  ]);
  const [status, setStatus] = useState<string>(() => filters.status);
  const [employeeName, setEmployeeName] = useState(() => filters.employeeName);
  const [storeName, setStoreName] = useState(() => filters.storeName);
  const [viewMode, setViewMode] = useState<'table' | 'card'>('table');

  const { data, isLoading, refetch, isFetching } = useClaims({
    startDate: filters.startDate,
    endDate: filters.endDate,
    status: filters.status || undefined,
    employeeName: filters.employeeName || undefined,
    storeName: filters.storeName || undefined,
    page,
    size,
  });
  const handleRowClick = useThrottleClick((claimId: number) => navigate(`/claims/${claimId}`));

  const handleSearch = () => {
    setFilters({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      status,
      employeeName,
      storeName,
    });
  };

  const columns: ColumnsType<ClaimListItem> = [
    {
      title: 'No',
      width: 60,
      render: (_v, _r, index) => page * size + index + 1,
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
          locale={listTableLocale()}
          pagination={buildListPagination({
            page,
            pageSize: size,
            total: data?.totalElements ?? 0,
            // 사이즈 변경 시 setSize 가 page 를 0 으로 자동 리셋(useListQueryParams). 순수 이동은 setPage.
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
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
              pageSize={size}
              showSizeChanger
              pageSizeOptions={PAGE_SIZE_OPTIONS}
              showTotal={(total) => `총 ${total}건`}
              onChange={(nextPage, nextSize) => {
                if (nextSize !== size) {
                  setSize(nextSize);
                } else {
                  setPage(nextPage - 1);
                }
              }}
            />
          </div>
        </Spin>
      )}
    </div>
  );
}
