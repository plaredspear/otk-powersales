import { useEffect, useRef, useState } from 'react';
import { Alert, Card, DatePicker, Descriptions, Drawer, Empty, Grid, Input, message, Select, Space, Spin, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useMonthlyIntegrationSchedule } from '@/hooks/schedules/useMonthlyIntegrationSchedule';
import { useMonthlyIntegrationExport } from '@/hooks/schedules/useMonthlyIntegrationExport';
import { useMonthlyIntegrationDetail } from '@/hooks/schedules/useMonthlyIntegrationDetail';
import { useMonthlyIntegrationFilterOptions } from '@/hooks/schedules/useMonthlyIntegrationFilterOptions';
import type { MonthlyIntegrationScheduleItem, MonthlyIntegrationSourceScheduleItem } from '@/api/monthlyIntegration';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import { listTableLocale } from '@/lib/listTableLocale';

const { Text } = Typography;
const { useBreakpoint } = Grid;

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

function formatDecimal3(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

function formatDecimal4(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 4, maximumFractionDigits: 4 });
}

// 상세 Drawer 의 집계 근거 일정 테이블 컬럼
const sourceScheduleColumns: ColumnsType<MonthlyIntegrationSourceScheduleItem> = [
  { title: '근무일자', dataIndex: 'workingDate', width: 100 },
  { title: '거래처', dataIndex: 'accountName', width: 140, ellipsis: true, render: (v) => v ?? '-' },
  { title: '근무형태1', dataIndex: 'workingCategory1', width: 90, ellipsis: true, render: (v) => v ?? '-' },
  { title: '근무형태3', dataIndex: 'workingCategory3', width: 90, ellipsis: true, render: (v) => v ?? '-' },
  {
    title: '출근보고',
    dataIndex: 'attendanceReportedAt',
    width: 130,
    render: (v: string | null) => (v ? v.substring(0, 16).replace('T', ' ') : '-'),
  },
  {
    title: '당일 일정수',
    dataIndex: 'dailyScheduleCount',
    width: 90,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '환산 기여분',
    dataIndex: 'equivalentContribution',
    width: 100,
    align: 'right',
    render: (v: number) => formatDecimal4(v),
  },
];

// 각 컬럼에 기본 width 를 지정해야 ResizableTable 의 헤더 우측 경계 드래그(리사이즈)가 활성화된다.
// width 미지정 컬럼은 일반 <th> 로 렌더되어 리사이즈 핸들이 붙지 않는다.
// 좁아진 컬럼은 ellipsis 로 텍스트를 "..." 축약하고, 드래그로 폭을 넓혀 확인할 수 있다.
const columns: ColumnsType<MonthlyIntegrationScheduleItem> = [
  { title: '소속', dataIndex: 'branchName', width: 100, ellipsis: true },
  { title: '거래처 지점명', dataIndex: 'accountBranchName', width: 140, ellipsis: true, render: (v) => v ?? '-' },
  { title: '거래처코드', dataIndex: 'accountCode', width: 110, ellipsis: true },
  { title: '거래처명', dataIndex: 'accountName', width: 160, ellipsis: true },
  { title: '유통형태', dataIndex: 'distributionChannelLabel', width: 110, ellipsis: true, render: (v) => v ?? '-' },
  { title: '거래처유형', dataIndex: 'abcTypeLabel', width: 110, ellipsis: true, render: (v) => v ?? '-' },
  { title: '사번', dataIndex: 'employeeCode', width: 90, ellipsis: true },
  { title: '직위', dataIndex: 'title', width: 90, ellipsis: true, render: (v) => v ?? '-' },
  { title: '이름', dataIndex: 'employeeName', width: 100, ellipsis: true },
  { title: '근무형태1', dataIndex: 'workingCategory1', width: 100, ellipsis: true },
  { title: '근무형태3', dataIndex: 'workingCategory3', width: 100, ellipsis: true, render: (v) => v ?? '-' },
  { title: '근무형태4', dataIndex: 'workingCategory4', width: 100, ellipsis: true, render: (v) => v ?? '-' },
  { title: '근무형태5', dataIndex: 'workingCategory5', width: 100, ellipsis: true, render: (v) => v ?? '-' },
  {
    title: '총 투입횟수',
    dataIndex: 'totalInputCount',
    width: 110,
    align: 'right',
    ellipsis: true,
    render: (v: number) => formatNumber(v),
  },
  {
    title: '총 환산근무일수',
    dataIndex: 'equivalentWorkingDays',
    width: 130,
    align: 'right',
    ellipsis: true,
    render: (v: number) => formatDecimal3(v),
  },
  {
    title: '총 환산인원',
    dataIndex: 'convertedHeadcount',
    width: 110,
    align: 'right',
    ellipsis: true,
    render: (v: number) => formatDecimal3(v),
  },
  {
    title: '월 평균 매출(6개월)',
    dataIndex: 'avgClosingAmount',
    width: 150,
    align: 'right',
    ellipsis: true,
    render: (v: number) => formatNumber(v),
  },
];

function MonthlyIntegrationDetailDrawer({
  detailId,
  onClose,
}: {
  detailId: number | null;
  onClose: () => void;
}) {
  const { data: detail, isLoading } = useMonthlyIntegrationDetail(detailId);

  return (
    <Drawer
      title="통합일정 상세 — 계산 근거"
      width={760}
      open={detailId != null}
      onClose={onClose}
      loading={isLoading}
    >
      {detail && (
        <>
          <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="년월">{detail.year}년 {detail.month}월</Descriptions.Item>
            <Descriptions.Item label="소속">{detail.branchName ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="사원">
              {detail.employeeName ?? '-'} ({detail.employeeCode ?? '-'})
            </Descriptions.Item>
            <Descriptions.Item label="거래처">
              {detail.accountName ?? '-'} ({detail.accountCode ?? '-'})
            </Descriptions.Item>
            <Descriptions.Item label="근무형태">
              {[detail.workingCategory1, detail.workingCategory3, detail.workingCategory4, detail.workingCategory5]
                .filter((v) => v != null && v !== '')
                .join(' / ') || '-'}
            </Descriptions.Item>
            <Descriptions.Item label="당월 근무일수">{formatNumber(detail.workingDaysMonth)}</Descriptions.Item>
            <Descriptions.Item label="총 투입횟수">{formatNumber(detail.totalInputCount)}</Descriptions.Item>
            <Descriptions.Item label="총 환산근무일수">{formatDecimal4(detail.equivalentWorkingDays)}</Descriptions.Item>
            <Descriptions.Item label="총 환산인원">{formatDecimal4(detail.convertedHeadcount)}</Descriptions.Item>
          </Descriptions>

          <Alert
            type="info"
            showIcon
            style={{ marginBottom: 12 }}
            message="계산 방식"
            description={
              <>
                총 환산근무일수 = Σ(1 ÷ 당일 출근 일정수) · 당월 근무일수 = 사원 기준 출근한 날짜 수(거래처 무관) ·
                총 환산인원 = 총 환산근무일수 ÷ 당월 근무일수 · 총 투입횟수 = 이 거래처+근무형태 조합으로 출근한 날짜 수.
                아래 목록은 출근등록이 완료된 일정만 포함합니다 (미출근 일정은 집계 제외).
              </>
            }
          />

          <Text strong style={{ display: 'block', marginBottom: 8 }}>
            집계 근거 일정 ({formatNumber(detail.schedules.length)}건)
          </Text>
          {detail.schedules.length === 0 ? (
            <Empty description="집계 근거 일정이 없습니다" />
          ) : (
            <Table
              rowKey="scheduleId"
              columns={sourceScheduleColumns}
              dataSource={detail.schedules}
              pagination={false}
              size="small"
              scroll={{ x: 740 }}
              summary={(rows) => (
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={6}>
                    <Text strong>환산 기여분 합계</Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={1} align="right">
                    <Text strong>
                      {formatDecimal4(rows.reduce((acc, r) => acc + r.equivalentContribution, 0))}
                    </Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              )}
            />
          )}
        </>
      )}
    </Drawer>
  );
}

function MobileItemCard({ item, onClick }: { item: MonthlyIntegrationScheduleItem; onClick?: () => void }) {
  const rows: Array<{ label: string; value: string }> = [
    { label: '거래처지점명', value: item.accountBranchName ?? '-' },
    { label: '거래처코드', value: item.accountCode },
    { label: '거래처명', value: item.accountName },
    { label: '유통형태', value: item.distributionChannelLabel ?? '-' },
    { label: '거래처유형', value: item.abcTypeLabel ?? '-' },
    { label: '근무형태1', value: item.workingCategory1 },
    { label: '근무형태3', value: item.workingCategory3 ?? '-' },
    { label: '근무형태4', value: item.workingCategory4 ?? '-' },
    { label: '근무형태5', value: item.workingCategory5 ?? '-' },
    { label: '총 투입횟수', value: formatNumber(item.totalInputCount) },
    { label: '총 환산근무일수', value: formatDecimal3(item.equivalentWorkingDays) },
    { label: '총 환산인원', value: formatDecimal3(item.convertedHeadcount) },
    { label: '월 평균 매출(6개월)', value: formatNumber(item.avgClosingAmount) },
  ];

  return (
    <Card size="small" style={{ marginBottom: 8, cursor: onClick ? 'pointer' : undefined }} onClick={onClick}>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>
        {item.branchName} / {item.employeeName} / {item.title ?? '-'} / {item.employeeCode}
      </div>
      {rows.map((row) => (
        <div key={row.label} style={{ display: 'flex', justifyContent: 'space-between', padding: '2px 0' }}>
          <span style={{ color: '#666' }}>{row.label}</span>
          <span>{row.value}</span>
        </div>
      ))}
    </Card>
  );
}

export default function MonthlyIntegrationSchedulePage() {
  const now = dayjs();
  const [year, setYear] = useState(now.year());
  const [month, setMonth] = useState(now.month() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [keyword, setKeyword] = useState('');
  const [accountKeyword, setAccountKeyword] = useState('');
  const [distributionKeyword, setDistributionKeyword] = useState('');
  const [accountTypeKeyword, setAccountTypeKeyword] = useState('');
  const [queryParams, setQueryParams] = useState<{
    year: number;
    month: number;
    codes: string[];
    keyword: string;
    accountKeyword: string;
    distributionKeyword: string;
    accountTypeKeyword: string;
  } | null>(null);
  const [detailId, setDetailId] = useState<number | null>(null);

  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const { data: filterOptions } = useMonthlyIntegrationFilterOptions();

  // 거래처유형 셀렉트 옵션 — 유통형태 선택 시 종속 목록, 미선택 시 전체 목록.
  const accountTypeOptions =
    distributionKeyword && filterOptions
      ? filterOptions.dependentAccountTypes[distributionKeyword] ?? []
      : filterOptions?.accountTypes ?? [];

  // 선택된 거래처유형이 현재 옵션 목록에 없으면(유통형태 종속으로 사라진 값) 렌더에서 제외.
  // filterOptions 로딩 전에는 옵션이 비어 있으므로 이때는 선택값을 그대로 유지한다.
  const accountTypeValue =
    !filterOptions || accountTypeOptions.includes(accountTypeKeyword)
      ? accountTypeKeyword || undefined
      : undefined;

  // 유통형태 변경 시, 새 유통형태에 속하지 않는 거래처유형 선택값은 초기화.
  const handleDistributionChange = (value: string | undefined) => {
    const next = value ?? '';
    setDistributionKeyword(next);
    if (accountTypeKeyword) {
      const dependent = next && filterOptions ? filterOptions.dependentAccountTypes[next] ?? [] : null;
      // 유통형태를 비우면(전체) 기존 거래처유형 선택은 유지, 특정 유통형태 선택 시 종속 목록에 없으면 초기화.
      if (dependent != null && !dependent.includes(accountTypeKeyword)) {
        setAccountTypeKeyword('');
      }
    }
  };

  const { data, isLoading, isError, error, refetch, isFetching } = useMonthlyIntegrationSchedule(
    queryParams?.year ?? year,
    queryParams?.month ?? month,
    queryParams?.codes ?? [],
    queryParams != null,
    queryParams?.keyword,
    queryParams?.accountKeyword,
    queryParams?.distributionKeyword,
    queryParams?.accountTypeKeyword,
  );

  const exportMutation = useMonthlyIntegrationExport();

  const emptyNoticeShownForRef = useRef<string | null>(null);
  const autoSearchedRef = useRef(false);

  // 페이지 진입 시 현재 년/월로 자동 조회. 단일지점 사용자는 본인 지점이 자동 선택되므로
  // 지점 코드가 채워지는 시점에 최초 1회만 조회를 트리거한다. (codes 빈 배열은 backend 에서 거부)
  useEffect(() => {
    if (autoSearchedRef.current) return;
    if (selectedCodes.length === 0) return;
    autoSearchedRef.current = true;
    setQueryParams({
      year, month, codes: selectedCodes, keyword, accountKeyword, distributionKeyword, accountTypeKeyword,
    });
  }, [selectedCodes, year, month, keyword, accountKeyword, distributionKeyword, accountTypeKeyword]);

  useEffect(() => {
    if (isLoading || isError) return;
    if (queryParams == null || data == null) return;
    const key = `${queryParams.year}-${queryParams.month}-${queryParams.codes.join(',')}`;
    if (data.items.length === 0 && emptyNoticeShownForRef.current !== key) {
      emptyNoticeShownForRef.current = key;
      message.info('조회 결과가 없습니다');
    }
  }, [data, isLoading, isError, queryParams]);

  const handleSearch = () => {
    if (year == null || month == null) {
      message.warning('년도와 월을 입력해주세요');
      return;
    }
    setQueryParams({
      year, month, codes: selectedCodes, keyword, accountKeyword, distributionKeyword, accountTypeKeyword,
    });
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate({
      year: queryParams.year,
      month: queryParams.month,
      costCenterCodes: queryParams.codes,
      keyword: queryParams.keyword,
      accountKeyword: queryParams.accountKeyword,
      distributionKeyword: queryParams.distributionKeyword,
      accountTypeKeyword: queryParams.accountTypeKeyword,
    });
  };

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
        exportDisabled={!data || data.items.length === 0}
        exportLoading={exportMutation.isPending}
        searchLoading={isLoading}
        hideExport={isMobile}
        periodFilter={
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
        }
        extraFilters={
          <>
            <Space direction="vertical" size={4}>
              <span>거래처명/코드:</span>
              <Input
                allowClear
                placeholder="거래처명 또는 코드"
                value={accountKeyword}
                onChange={(e) => setAccountKeyword(e.target.value)}
                onPressEnter={handleSearch}
                style={{ width: 160 }}
              />
            </Space>
            <Space direction="vertical" size={4}>
              <span>유통형태:</span>
              <Select
                allowClear
                showSearch
                placeholder="유통형태 선택"
                value={distributionKeyword || undefined}
                onChange={handleDistributionChange}
                optionFilterProp="label"
                options={(filterOptions?.distributions ?? []).map((v) => ({ label: v, value: v }))}
                style={{ width: 170 }}
              />
            </Space>
            <Space direction="vertical" size={4}>
              <span>거래처유형:</span>
              <Select
                allowClear
                showSearch
                placeholder="거래처유형 선택"
                value={accountTypeValue}
                onChange={(value) => setAccountTypeKeyword(value ?? '')}
                optionFilterProp="label"
                options={accountTypeOptions.map((v) => ({ label: v, value: v }))}
                style={{ width: 170 }}
              />
            </Space>
            <Space direction="vertical" size={4}>
              <span>사번/이름:</span>
              <Input
                allowClear
                placeholder="사번 또는 이름"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onPressEnter={handleSearch}
                style={{ width: 160 }}
              />
            </Space>
          </>
        }
        extraActions={
          queryParams != null && !isMobile ? (
            <RefreshButton onRefresh={refetch} refreshing={isFetching} />
          ) : undefined
        }
      />

      {isError && (
        <Alert
          type="error"
          message="조회 실패"
          description={error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
          style={{ marginBottom: 16 }}
        />
      )}

      {isMobile ? (
        isLoading ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <Spin size="large" />
          </div>
        ) : queryParams == null ? null : data && data.items.length === 0 ? (
          <Empty description="조회 결과가 없습니다" />
        ) : data ? (
          <>
            <Text style={{ marginBottom: 8, display: 'block' }}>
              총 {formatNumber(data.totalCount)}건
            </Text>
            <Space direction="vertical" style={{ width: '100%' }} size={0}>
              {data.items.map((item) => (
                <MobileItemCard
                  key={`${item.accountCode}-${item.employeeCode}`}
                  item={item}
                  onClick={item.id != null ? () => setDetailId(item.id) : undefined}
                />
              ))}
            </Space>
          </>
        ) : null
      ) : (
        <>
          {queryParams != null && data && (
            <Text style={{ marginBottom: 8, display: 'block' }}>
              총 {formatNumber(data.totalCount)}건
            </Text>
          )}
          <ResizableTable
            rowKey={(record) => `${record.accountCode}-${record.employeeCode}`}
            columns={columns}
            dataSource={data?.items ?? []}
            loading={isLoading}
            pagination={false}
            size="small"
            sticky
            tableLayout="fixed"
            locale={listTableLocale({ searched: queryParams != null })}
            onRow={(record) => ({
              onClick: record.id != null ? () => setDetailId(record.id) : undefined,
              style: record.id != null ? { cursor: 'pointer' } : undefined,
            })}
          />
        </>
      )}

      <MonthlyIntegrationDetailDrawer detailId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}
