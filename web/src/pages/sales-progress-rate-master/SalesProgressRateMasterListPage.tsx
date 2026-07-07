import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Input, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useSalesProgressRateMasters } from '@/hooks/sales-progress-rate-master/useSalesProgressRateMasters';
import { useThrottleClick } from '@/hooks/common/useThrottleClick';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useFlexTableScrollY } from '@/hooks/common/useFlexTableScrollY';
import type { SalesProgressRateMasterListItem } from '@/api/salesProgressRateMaster';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';

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
  // 페이지 전체 스크롤 제거 — 필터/툴바는 고정, 테이블 body(행) 만 세로 스크롤. 높이는 상단 가변 요소를
  // 실측 반영. headerReserve = 테이블 헤더 행(≈39) + 페이지네이션(≈56).
  const { containerRef, containerHeight, tableWrapperRef, scrollY } = useFlexTableScrollY(4, 95);
  // page/size/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입/새로고침 시 직전 조건 복원.
  const { page, setPage, size, setSize, filters, setFilters } = useListQueryParams({
    defaultFilters: { targetYear: '', targetMonth: '', keyword: '' },
  });
  const { targetYear, targetMonth, keyword } = filters;
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [targetYearInput, setTargetYearInput] = useState(targetYear);
  const [targetMonthInput, setTargetMonthInput] = useState(targetMonth);
  const [keywordInput, setKeywordInput] = useState(keyword);

  const handleSearch = () => {
    setFilters({ targetYear: targetYearInput, targetMonth: targetMonthInput, keyword: keywordInput });
  };

  const goToDetail = useThrottleClick((id: number) =>
    navigate(`/sales-progress-rate-masters/${id}`, { state: { listSearch: location.search } }),
  );

  const { data, isLoading, refetch, isFetching } = useSalesProgressRateMasters({
    keyword: keyword || undefined,
    targetYear: targetYear || undefined,
    targetMonth: targetMonth || undefined,
    page,
    size,
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
    <div
      ref={containerRef}
      style={{
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        height: containerHeight,
        boxSizing: 'border-box',
        minHeight: 0,
      }}
    >
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap', flexShrink: 0 }}>
        <Input
          placeholder="목표 년도"
          allowClear
          style={{ width: 110 }}
          value={targetYearInput ?? ''}
          onChange={(e) => setTargetYearInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Input
          placeholder="목표 월"
          allowClear
          style={{ width: 90 }}
          value={targetMonthInput ?? ''}
          onChange={(e) => setTargetMonthInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Input
          placeholder="이름/거래처명 검색"
          allowClear
          value={keywordInput ?? ''}
          style={{ width: 250 }}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
        <div style={{ marginLeft: 'auto' }}>
          <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        </div>
      </div>

      {/* flex:1 로 남은 높이를 채우는 테이블 wrapper. 실측 높이가 scrollY 로 body 스크롤. */}
      <div ref={tableWrapperRef} style={{ flex: 1, minHeight: 0 }}>
        <ResizableTable
          rowKey="id"
          columns={columns}
          dataSource={data?.content}
          loading={isLoading}
          locale={listTableLocale()}
          scroll={{ x: 1900, y: scrollY }}
          pagination={buildListPagination({
            page: data?.page ?? page,
            pageSize: size,
            total: data?.totalElements ?? 0,
            // 사이즈 변경 시 setSize 가 page 를 0 으로 자동 리셋(useListQueryParams).
            onPageChange: setPage,
            onSizeChange: setSize,
          })}
        />
      </div>
    </div>
  );
}
