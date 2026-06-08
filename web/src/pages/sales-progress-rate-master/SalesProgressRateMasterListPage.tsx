import { useLocation, useNavigate } from 'react-router-dom';
import { Input, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSalesProgressRateMasters } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMasters';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import type { SalesProgressRateMasterListItem } from '@/api/salesProgressRateMaster';
import ResizableTable from '@/components/common/ResizableTable';

function formatAmount(value: number | null): string {
  return value != null ? value.toLocaleString() : '-';
}

function formatRate(value: number | null): string {
  // SF ProgressRate__c (Percent, scale=0) — 서버는 비율(예: 0.85)을 전달, 화면은 소수점 없이 반올림한 정수 % 표기.
  return value != null ? `${Math.round(value * 100)}%` : '-';
}

export default function SalesProgressRateMasterListPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, filters, setFilter } = useListQueryParams({
    defaultFilters: { targetYear: '', targetMonth: '', keyword: '' },
  });
  const { targetYear, targetMonth, keyword } = filters;

  const goToDetail = useThrottleClick((id: number) =>
    navigate(`/sales-progress-rate-masters/${id}`, { state: { listSearch: location.search } }),
  );

  const { data, isLoading } = useSalesProgressRateMasters({
    keyword: keyword || undefined,
    targetYear: targetYear || undefined,
    targetMonth: targetMonth || undefined,
    page,
    size: 20,
  });

  const columns: ColumnsType<SalesProgressRateMasterListItem> = [
    {
      title: '이름',
      dataIndex: 'name',
      width: 140,
      fixed: 'left',
      render: (val: string | null, record) =>
        val ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            <a onClick={() => goToDetail(record.id)}>{val}</a>
            <Typography.Text copyable={{ text: val, tooltips: ['이름 복사', '복사됨'] }} />
          </span>
        ) : (
          <a onClick={() => goToDetail(record.id)}>(이름 없음)</a>
        ),
    },
    {
      title: '목표 년도',
      dataIndex: 'targetYear',
      width: 90,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '목표 월',
      dataIndex: 'targetMonth',
      width: 80,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처',
      dataIndex: 'accountName',
      width: 160,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처지점명',
      dataIndex: 'accountBranchName',
      width: 130,
      ellipsis: true,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '거래처코드',
      dataIndex: 'accountCode',
      width: 120,
      align: 'center',
      render: (val: string | null) =>
        val ? (
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
            {val}
            <Typography.Text copyable={{ text: val, tooltips: ['거래처코드 복사', '복사됨'] }} />
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '거래처유형',
      dataIndex: 'accountType',
      width: 100,
      align: 'center',
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '상온 목표 금액',
      dataIndex: 'rtTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '라면 목표 금액',
      dataIndex: 'rmTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '냉동/냉장 목표 금액',
      dataIndex: 'frTargetAmount',
      width: 140,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '유지 목표 금액',
      dataIndex: 'foTargetAmount',
      width: 120,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '합계 목표 금액',
      dataIndex: 'targetSum',
      width: 130,
      align: 'right',
      render: (val: number) => (val != null ? val.toLocaleString() : '-'),
    },
    {
      title: '당월 매출 실적',
      dataIndex: 'currentMonthSalesAmount',
      width: 130,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '전월 매출 실적',
      dataIndex: 'previousMonthSalesAmount',
      width: 130,
      align: 'right',
      render: formatAmount,
    },
    {
      title: '매출 진도율',
      dataIndex: 'progressRate',
      width: 100,
      align: 'right',
      render: formatRate,
    },
  ];

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Input
          placeholder="목표 년도"
          allowClear
          style={{ width: 110 }}
          value={targetYear ?? ''}
          onChange={(e) => setFilter('targetYear', e.target.value)}
        />
        <Input
          placeholder="목표 월"
          allowClear
          style={{ width: 90 }}
          value={targetMonth ?? ''}
          onChange={(e) => setFilter('targetMonth', e.target.value)}
        />
        <Input.Search
          placeholder="이름/거래처명 검색"
          allowClear
          defaultValue={keyword ?? ''}
          style={{ width: 250 }}
          onSearch={(val) => setFilter('keyword', val)}
        />
      </div>

      <ResizableTable
        rowKey="id"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        scroll={{ x: 1900 }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: 20,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </div>
  );
}
