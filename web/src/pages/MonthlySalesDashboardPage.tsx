import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, Card, Col, DatePicker, Input, Row, Select, Space, Statistic, Tooltip, Typography, message } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
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
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useMonthlySalesBranches } from '@/hooks/sales/useMonthlySalesBranches';
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
  distributionKeyword?: string;
  accountTypeKeyword?: string;
  targetRegistration?: 'registered' | 'unregistered';
  deploymentFilter?: 'deployed' | 'undeployed';
}

/**
 * 월 매출(물류배부) — web admin 대시보드 페이지.
 *
 * 상단 = 권한 범위 거래처 합산 KPI 5종 + 월별 추이 차트 (최근 6개월).
 * 하단 = 거래처별 명세 table + 페이징 + 정렬 + 엑셀 export. row 클릭 시 모바일 동등 상세 modal.
 */
export default function MonthlySalesDashboardPage() {
  const [searchParams] = useSearchParams();
  // 대시보드 등 외부 화면에서 조회월/근무등록 조건을 URL 로 전달받아 초기값으로 사용한다.
  // yearMonth=YYYY-MM, deploymentFilter=deployed|undeployed (미지정 시 '등록' 기본).
  const today = new Date();
  const initYearMonth = searchParams.get('yearMonth');
  const initYear = initYearMonth?.match(/^\d{4}-\d{2}$/)
    ? Number(initYearMonth.slice(0, 4))
    : today.getFullYear();
  const initMonth = initYearMonth?.match(/^\d{4}-\d{2}$/)
    ? Number(initYearMonth.slice(5, 7))
    : today.getMonth() + 1;
  const initDeployment =
    searchParams.get('deploymentFilter') === 'undeployed'
      ? 'undeployed'
      : searchParams.get('deploymentFilter') === 'deployed'
        ? 'deployed'
        : ('deployed' as const);
  const [year, setYear] = useState<number>(initYear);
  const [month, setMonth] = useState<number>(initMonth);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  // 월 매출(물류배부) 전용 지점 셀렉터 — 대시보드와 동일한 지점 기준(전사 권한자 34개 화이트리스트).
  const { data: branches = [] } = useMonthlySalesBranches();
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  const [distributionKeyword, setDistributionKeyword] = useState<string>('');
  const [accountTypeKeyword, setAccountTypeKeyword] = useState<string>('');
  const [targetRegistration, setTargetRegistration] = useState<'registered' | 'unregistered' | undefined>(undefined);
  // 근무등록 필터 — 첫 진입 기본값 '등록'(deployed): 조회월에 여사원이 근무등록한 거래처만.
  // URL deploymentFilter 파라미터가 있으면 그 값을 초기값으로 반영.
  const [deploymentFilter, setDeploymentFilter] = useState<'deployed' | 'undeployed' | undefined>(initDeployment);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<MonthlySalesDashboardListItem | null>(null);

  const summaryQuery = useQuery({
    queryKey: ['monthlySalesDashboard', 'summary', queryParams],
    queryFn: () => {
      const p = queryParams!;
      return fetchSummary(
        p.year, p.month, p.codes, p.customerKeyword, undefined,
        p.distributionKeyword, p.accountTypeKeyword, p.targetRegistration, p.deploymentFilter,
      );
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
        distributionKeyword: p.distributionKeyword,
        accountTypeKeyword: p.accountTypeKeyword,
        targetRegistration: p.targetRegistration,
        deploymentFilter: p.deploymentFilter,
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
      distributionKeyword: distributionKeyword.trim() || undefined,
      accountTypeKeyword: accountTypeKeyword.trim() || undefined,
      targetRegistration,
      deploymentFilter,
    });
  };

  // 단일지점 사용자는 PeriodBranchFilterBar 가 본인 지점을 자동 선택하므로,
  // 최초 진입 시(아직 조회 전) 별도 조회 버튼 클릭 없이 바로 결과를 보여준다.
  useEffect(() => {
    if (queryParams === null && selectedCodes.length === 1) {
      setQueryParams({
        year,
        month,
        codes: selectedCodes,
        customerKeyword: customerKeyword.trim() || undefined,
        distributionKeyword: distributionKeyword.trim() || undefined,
        accountTypeKeyword: accountTypeKeyword.trim() || undefined,
        targetRegistration,
        deploymentFilter,
      });
    }
    // 최초 자동 조회만 담당 — queryParams 가 채워진 뒤의 재조회는 사용자 조작에 맡긴다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCodes, queryParams]);

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
          distributionKeyword: queryParams.distributionKeyword,
          accountTypeKeyword: queryParams.accountTypeKeyword,
          targetRegistration: queryParams.targetRegistration,
          deploymentFilter: queryParams.deploymentFilter,
          sort,
        }),
        totalCount: list?.pageInfo.totalElements,
        maxRows: EXCEL_EXPORT_MAX_ROWS,
      },
    );
  };

  const summary = summaryQuery.data;
  const list = listQuery.data;

  // 금액은 천원 단위로 표시 — 1000 으로 나눈 뒤 정수(반올림)로 표기.
  const formatThousandWon = (v: number | null | undefined) =>
    v == null ? '-' : Math.round(v / 1000).toLocaleString();
  const formatPct = (v: number | null | undefined) =>
    v == null ? '-' : `${v.toFixed(1)}%`;
  // 환산인원(FTE) — 소수 1자리 표시. 투입 없음(0)도 명확히 0.0 으로 표기.
  const formatHeadcount = (v: number | null | undefined) =>
    v == null ? '-' : v.toFixed(1);

  const columns: ColumnsType<MonthlySalesDashboardListItem> = useMemo(
    () => [
      { title: '지점', dataIndex: 'branchName', width: 120, fixed: 'left', render: (v) => v ?? '-' },
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
      { title: '거래처코드', dataIndex: 'sapAccountCode', width: 110, render: (v) => v ?? '-' },
      {
        title: '판매여사원(환산인원)',
        children: [
          { title: '진열', dataIndex: 'displayHeadcount', width: 80, align: 'right', render: (v) => formatHeadcount(v) },
          { title: '행사', dataIndex: 'eventHeadcount', width: 80, align: 'right', render: (v) => formatHeadcount(v) },
          { title: '총인원', dataIndex: 'totalHeadcount', width: 80, align: 'right', render: (v) => formatHeadcount(v) },
        ],
      },
      {
        title: '목표(천원)',
        dataIndex: 'targetAmount',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatThousandWon(v),
      },
      {
        title: '실적(천원)',
        dataIndex: 'totalAchievedAmount',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatThousandWon(v),
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
      { title: '전년 동월(천원)', dataIndex: 'lastYearAchievedAmount', width: 130, align: 'right', render: (v) => formatThousandWon(v) },
      {
        title: '전년 대비',
        dataIndex: 'lastYearComparisonRatio',
        width: 100,
        align: 'right',
        render: (v) => formatPct(v),
      },
      {
        title: '상온(천원)',
        children: [
          { title: '목표', dataIndex: 'ambientTargetAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
          { title: '실적', dataIndex: 'ambientAchievedAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
        ],
      },
      {
        title: '라면(천원)',
        children: [
          { title: '목표', dataIndex: 'noodleTargetAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
          { title: '실적', dataIndex: 'noodleAchievedAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
        ],
      },
      {
        title: '냉동/냉장(천원)',
        children: [
          { title: '목표', dataIndex: 'frozenRefrigeratedTargetAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
          { title: '실적', dataIndex: 'frozenRefrigeratedAchievedAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
        ],
      },
      {
        title: '유지류(천원)',
        children: [
          { title: '목표', dataIndex: 'oilFatTargetAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
          { title: '실적', dataIndex: 'oilFatAchievedAmount', width: 110, align: 'right', render: (v) => formatThousandWon(v) },
        ],
      },
    ],
    [summary?.referenceAchievementRate],
  );

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        branches={branches}
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
        periodFilter={
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <Space direction="vertical" size={4}>
              <span>조회월:</span>
              <DatePicker
                picker="month"
                value={dayjs(`${year}-${String(month).padStart(2, '0')}-01`)}
                onChange={(value) => {
                  if (!value) return;
                  setYear(value.year());
                  setMonth(value.month() + 1);
                }}
                allowClear={false}
                format="YYYY-MM"
                style={{ width: 140 }}
              />
            </Space>
            <Space direction="vertical" size={4}>
              <span>
                근무등록:{' '}
                <Tooltip title="선택한 지점에서 조회월에 여사원이 1번이라도 근무등록(출근등록)한 거래처를 기준으로 합니다. '등록'은 근무등록된 거래처만, '미등록'은 근무등록이 없는 거래처만 조회하며, 목표·실적 합계도 해당 거래처 기준으로 집계됩니다.">
                  <InfoCircleOutlined style={{ color: '#8c8c8c', cursor: 'help', fontSize: 14 }} />
                </Tooltip>
              </span>
              <Select
                value={deploymentFilter ?? 'all'}
                onChange={(v) => setDeploymentFilter(v === 'all' ? undefined : (v as 'deployed' | 'undeployed'))}
                style={{ width: 120 }}
                options={[
                  { value: 'all', label: '전체' },
                  { value: 'deployed', label: '등록' },
                  { value: 'undeployed', label: '미등록' },
                ]}
              />
            </Space>
          </div>
        }
        extraFilters={
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            <div>
              <span>거래처 검색:</span>
              <div style={{ marginTop: 4 }}>
                <Input
                  placeholder="거래처명/거래처코드"
                  value={customerKeyword}
                  onChange={(e) => setCustomerKeyword(e.target.value)}
                  onPressEnter={handleSearch}
                  style={{ width: 220 }}
                  allowClear
                />
              </div>
            </div>
            <div>
              <span>유통형태:</span>
              <div style={{ marginTop: 4 }}>
                <Input
                  placeholder="유통형태 (예: 슈퍼)"
                  value={distributionKeyword}
                  onChange={(e) => setDistributionKeyword(e.target.value)}
                  onPressEnter={handleSearch}
                  style={{ width: 160 }}
                  allowClear
                />
              </div>
            </div>
            <div>
              <span>거래처유형:</span>
              <div style={{ marginTop: 4 }}>
                <Input
                  placeholder="거래처유형 (예: 이마트)"
                  value={accountTypeKeyword}
                  onChange={(e) => setAccountTypeKeyword(e.target.value)}
                  onPressEnter={handleSearch}
                  style={{ width: 160 }}
                  allowClear
                />
              </div>
            </div>
            <div>
              <span>목표등록:</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  value={targetRegistration}
                  onChange={(v) => setTargetRegistration(v)}
                  style={{ width: 120 }}
                  allowClear
                  placeholder="전체"
                  options={[
                    { value: 'registered', label: '등록' },
                    { value: 'unregistered', label: '미등록' },
                  ]}
                />
              </div>
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
              <Statistic title="목표 합계" value={formatThousandWon(summary.totalTargetAmount)} suffix="천원" />
            </Col>
            <Col span={5}>
              <Statistic title="실적 합계" value={formatThousandWon(summary.totalAchievedAmount)} suffix="천원" />
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
            {queryParams.distributionKeyword && ` · 유통형태: ${queryParams.distributionKeyword}`}
            {queryParams.accountTypeKeyword && ` · 거래처유형: ${queryParams.accountTypeKeyword}`}
            {queryParams.targetRegistration &&
              ` · 목표등록: ${queryParams.targetRegistration === 'registered' ? '등록' : '미등록'}`}
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

      <ResizableTable
        rowKey={(r) => r.accountId}
        size="small"
        columns={columns}
        dataSource={list?.items ?? []}
        loading={queryParams != null && listQuery.isLoading}
        pagination={buildListPagination({
          page,
          pageSize,
          total: list?.pageInfo.totalElements ?? 0,
          onPageChange: setPage,
          onSizeChange: (size) => {
            setPageSize(size);
            setPage(0);
          },
        })}
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
        locale={listTableLocale({ searched: queryParams != null })}
      />

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
