import { useEffect, useRef, useState } from 'react';
import { Alert, Card, Empty, Grid, Input, message, Space, Spin, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useMonthlyIntegrationSchedule } from '@/hooks/schedules/useMonthlyIntegrationSchedule';
import { useMonthlyIntegrationExport } from '@/hooks/schedules/useMonthlyIntegrationExport';
import type { MonthlyIntegrationScheduleItem } from '@/api/monthlyIntegration';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const { Text } = Typography;
const { useBreakpoint } = Grid;

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

function formatDecimal3(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

// 각 컬럼에 기본 width 를 지정해야 ResizableTable 의 헤더 우측 경계 드래그(리사이즈)가 활성화된다.
// width 미지정 컬럼은 일반 <th> 로 렌더되어 리사이즈 핸들이 붙지 않는다.
// 좁아진 컬럼은 ellipsis 로 텍스트를 "..." 축약하고, 드래그로 폭을 넓혀 확인할 수 있다.
const columns: ColumnsType<MonthlyIntegrationScheduleItem> = [
  { title: '소속', dataIndex: 'branchName', width: 100, ellipsis: true },
  { title: '거래처 지점명', dataIndex: 'accountBranchName', width: 140, ellipsis: true, render: (v) => v ?? '-' },
  { title: '거래처코드', dataIndex: 'accountCode', width: 110, ellipsis: true },
  { title: '거래처명', dataIndex: 'accountName', width: 160, ellipsis: true },
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

function MobileItemCard({ item }: { item: MonthlyIntegrationScheduleItem }) {
  const rows: Array<{ label: string; value: string }> = [
    { label: '거래처지점명', value: item.accountBranchName ?? '-' },
    { label: '거래처코드', value: item.accountCode },
    { label: '거래처명', value: item.accountName },
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
    <Card size="small" style={{ marginBottom: 8 }}>
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
  const [queryParams, setQueryParams] = useState<{
    year: number;
    month: number;
    codes: string[];
    keyword: string;
  } | null>(null);

  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const { data, isLoading, isError, error, refetch, isFetching } = useMonthlyIntegrationSchedule(
    queryParams?.year ?? year,
    queryParams?.month ?? month,
    queryParams?.codes ?? [],
    queryParams != null,
    queryParams?.keyword,
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
    setQueryParams({ year, month, codes: selectedCodes, keyword });
  }, [selectedCodes, year, month, keyword]);

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
    setQueryParams({ year, month, codes: selectedCodes, keyword });
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate({
      year: queryParams.year,
      month: queryParams.month,
      costCenterCodes: queryParams.codes,
      keyword: queryParams.keyword,
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
        extraFilters={
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

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : isMobile ? (
        queryParams == null ? null : data && data.items.length === 0 ? (
          <Empty description="조회 결과가 없습니다" />
        ) : data ? (
          <>
            <Text style={{ marginBottom: 8, display: 'block' }}>
              총 {formatNumber(data.totalCount)}건
            </Text>
            <Space direction="vertical" style={{ width: '100%' }} size={0}>
              {data.items.map((item, index) => (
                <MobileItemCard key={index} item={item} />
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
            pagination={false}
            size="small"
            sticky
            tableLayout="fixed"
            locale={{
              emptyText:
                queryParams == null ? '조회 조건을 설정하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
            }}
          />
        </>
      )}
    </div>
  );
}
