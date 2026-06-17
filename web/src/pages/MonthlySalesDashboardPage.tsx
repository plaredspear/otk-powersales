import { useMemo, useState } from 'react';
import { Alert, Card, Col, Empty, Input, Row, Spin, Statistic, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  EXPORT_LIST_PATH,
  exportListParams,
  fetchList,
  fetchSummary,
  type MonthlySalesDashboardListItem,
} from '@/api/monthlySalesDashboard';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import MonthlyTrendChart from '@/components/charts/MonthlyTrendChart';
import MonthlySalesDashboardDetailModal from './MonthlySalesDashboardDetailModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;

interface QueryParams {
  year: number;
  month: number;
  codes: string[];
  customerKeyword?: string;
}

/**
 * 월 매출(물류배부) — web admin 대시보드 페이지.
 *
 * 상단 = 권한 범위 거래처 합산 KPI 5종 + 월별 추이 차트 (최근 6개월).
 * 하단 = 거래처별 명세 table + 페이징 + 정렬 + 엑셀 export. row 클릭 시 모바일 동등 상세 modal.
 */
export default function MonthlySalesDashboardPage() {
  const today = new Date();
  const [year, setYear] = useState<number>(today.getFullYear());
  const [month, setMonth] = useState<number>(today.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<MonthlySalesDashboardListItem | null>(null);

  const summaryQuery = useQuery({
    queryKey: ['monthlySalesDashboard', 'summary', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchSummary(p.year, p.month, p.codes, p.customerKeyword);
    },
    enabled: queryParams != null,
  });

  const listQuery = useQuery({
    queryKey: ['monthlySalesDashboard', 'list', queryParams, page, pageSize, sort],
    queryFn: () => {
      const p = queryParams!;
      return fetchList({
        year: p.year,
        month: p.month,
        costCenterCodes: p.codes,
        customerKeyword: p.customerKeyword,
        page,
        size: pageSize,
        sort,
      });
    },
    enabled: queryParams != null,
    placeholderData: (prev) => prev,
  });

  const handleSearch = () => {
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    setPage(0);
    setQueryParams({
      year,
      month,
      codes: selectedCodes,
      customerKeyword: customerKeyword.trim() || undefined,
    });
  };

  const { run: runExport, downloading: exporting } = useExcelDownload();

  const handleExport = () => {
    if (!queryParams) return;
    const monthStr = String(queryParams.month).padStart(2, '0');
    runExport(
      EXPORT_LIST_PATH,
      `monthly-sales-${queryParams.year}-${monthStr}.xlsx`,
      {
        params: exportListParams({
          year: queryParams.year,
          month: queryParams.month,
          costCenterCodes: queryParams.codes,
          customerKeyword: queryParams.customerKeyword,
          sort,
        }),
        totalCount: list?.pageInfo.totalElements,
        maxRows: EXCEL_EXPORT_MAX_ROWS,
      },
    );
  };

  const summary = summaryQuery.data;
  const list = listQuery.data;

  const formatWon = (v: number | null | undefined) =>
    v == null ? '-' : `${v.toLocaleString()}원`;
  const formatPct = (v: number | null | undefined) =>
    v == null ? '-' : `${v.toFixed(1)}%`;

  const columns: ColumnsType<MonthlySalesDashboardListItem> = useMemo(
    () => [
      {
        title: '거래처',
        dataIndex: 'accountName',
        width: 180,
        fixed: 'left',
        render: (v, row) => (
          <a onClick={() => setDetailTarget(row)} role="button">
            {v ?? '-'}
          </a>
        ),
      },
      { title: 'SAP코드', dataIndex: 'sapAccountCode', width: 110, render: (v) => v ?? '-' },
      { title: '지점', dataIndex: 'branchName', width: 120, render: (v) => v ?? '-' },
      {
        title: '목표',
        dataIndex: 'targetAmount',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatWon(v),
      },
      {
        title: '실적',
        dataIndex: 'totalAchievedAmount',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatWon(v),
      },
      {
        title: '진도율',
        dataIndex: 'achievementRate',
        width: 90,
        sorter: true,
        align: 'right',
        render: (v: number | null) => {
          if (v == null) return '-';
          const reference = summary?.referenceAchievementRate ?? 0;
          const color = v >= reference ? '#1677ff' : '#ff4d4f';
          return <span style={{ color, fontWeight: 600 }}>{formatPct(v)}</span>;
        },
      },
      {
        title: '상온',
        children: [
          { title: '목표', dataIndex: 'ambientTargetAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
          { title: '실적', dataIndex: 'ambientAchievedAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
        ],
      },
      {
        title: '라면',
        children: [
          { title: '목표', dataIndex: 'noodleTargetAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
          { title: '실적', dataIndex: 'noodleAchievedAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
        ],
      },
      {
        title: '냉동/냉장',
        children: [
          { title: '목표', dataIndex: 'frozenRefrigeratedTargetAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
          { title: '실적', dataIndex: 'frozenRefrigeratedAchievedAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
        ],
      },
      {
        title: '유지류',
        children: [
          { title: '목표', dataIndex: 'oilFatTargetAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
          { title: '실적', dataIndex: 'oilFatAchievedAmount', width: 110, align: 'right', render: (v) => formatWon(v) },
        ],
      },
      { title: '전년 동월', dataIndex: 'lastYearAchievedAmount', width: 130, align: 'right', render: (v) => formatWon(v) },
      {
        title: '전년 대비',
        dataIndex: 'lastYearComparisonRatio',
        width: 100,
        align: 'right',
        render: (v) => formatPct(v),
      },
      {
        title: '마감',
        dataIndex: 'isConfirmed',
        width: 80,
        render: (v: boolean) =>
          v ? <Tag color="green">마감</Tag> : <Tag color="red">미마감</Tag>,
      },
    ],
    [summary?.referenceAchievementRate],
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        year={year}
        month={month}
        selectedCodes={selectedCodes}
        onYearChange={setYear}
        onMonthChange={setMonth}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!list || list.items.length === 0}
        exportLoading={exporting}
        searchLoading={summaryQuery.isLoading || listQuery.isLoading}
        extraFilters={
          <div>
            <span>거래처 검색:</span>
            <div style={{ marginTop: 4 }}>
              <Input
                placeholder="거래처명 부분 일치"
                value={customerKeyword}
                onChange={(e) => setCustomerKeyword(e.target.value)}
                style={{ width: 220 }}
                allowClear
              />
            </div>
          </div>
        }
      />

      {summaryQuery.isError && (
        <Alert
          type="error"
          message={(summaryQuery.error as Error)?.message ?? '요약 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {summary && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Row gutter={16}>
            <Col span={5}>
              <Statistic title="목표 합계" value={summary.totalTargetAmount.toLocaleString()} suffix="원" />
            </Col>
            <Col span={5}>
              <Statistic title="실적 합계" value={summary.totalAchievedAmount.toLocaleString()} suffix="원" />
            </Col>
            <Col span={5}>
              <Statistic
                title="진도율"
                value={summary.overallAchievementRate.toFixed(1)}
                suffix="%"
                valueStyle={{
                  color:
                    summary.overallAchievementRate >= summary.referenceAchievementRate
                      ? '#1677ff'
                      : '#ff4d4f',
                }}
              />
            </Col>
            <Col span={5}>
              <Statistic
                title="기준 진도율"
                value={summary.referenceAchievementRate.toFixed(1)}
                suffix="%"
              />
            </Col>
            <Col span={4}>
              <Statistic
                title="전년 대비"
                value={summary.lastYearComparisonRatio?.toFixed(1) ?? '-'}
                suffix={summary.lastYearComparisonRatio != null ? '%' : ''}
              />
            </Col>
          </Row>
        </Card>
      )}

      {summary && (
        <Card size="small" title="월별 추이 (최근 6개월)" style={{ marginBottom: 12 }}>
          <MonthlyTrendChart data={summary.monthlyTrend} />
        </Card>
      )}

      {queryParams != null && (
        <div
          style={{
            marginBottom: 8,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Text type="secondary">
            {queryParams.year}-{String(queryParams.month).padStart(2, '0')} · {queryParams.codes.length}개 지점
            {queryParams.customerKeyword && ` · 거래처: ${queryParams.customerKeyword}`}
          </Text>
          <RefreshButton
            onRefresh={() => {
              summaryQuery.refetch();
              listQuery.refetch();
            }}
            refreshing={summaryQuery.isFetching || listQuery.isFetching}
          />
        </div>
      )}

      {listQuery.isError && (
        <Alert
          type="error"
          message={(listQuery.error as Error)?.message ?? '명세 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {queryParams == null ? (
        <Empty description="조회 조건을 설정하고 조회 버튼을 눌러주세요" />
      ) : listQuery.isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : (
        <ResizableTable
          rowKey={(r) => r.accountId}
          size="small"
          columns={columns}
          dataSource={list?.items ?? []}
          pagination={{
            current: page + 1,
            pageSize,
            total: list?.pageInfo.totalElements ?? 0,
            showSizeChanger: true,
            pageSizeOptions: [10, 20, 50, 100],
            onChange: (p, ps) => {
              setPage(p - 1);
              setPageSize(ps);
            },
          }}
          onChange={(_pagination, _filters, sorter) => {
            if (!Array.isArray(sorter) && sorter.order && sorter.field) {
              const field = String(sorter.field);
              const direction = sorter.order === 'descend' ? 'desc' : 'asc';
              setSort(`${field},${direction}`);
              setPage(0);
            } else {
              setSort(undefined);
            }
          }}
          scroll={{ x: 'max-content' }}
          locale={{ emptyText: '조회 결과가 없습니다' }}
        />
      )}

      <MonthlySalesDashboardDetailModal
        open={detailTarget != null}
        onClose={() => setDetailTarget(null)}
        customerId={detailTarget?.accountId ?? null}
        customerName={detailTarget?.accountName ?? null}
        year={queryParams?.year ?? year}
        month={queryParams?.month ?? month}
      />
    </div>
  );
}
