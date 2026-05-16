import { useState } from 'react';
import { Alert, Card, Empty, Grid, Space, Spin, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ScheduleFilterBar from '@/components/schedules/ScheduleFilterBar';
import { useMonthlyIntegrationSchedule } from '@/hooks/schedules/useMonthlyIntegrationSchedule';
import { useMonthlyIntegrationExport } from '@/hooks/schedules/useMonthlyIntegrationExport';
import type { MonthlyIntegrationScheduleItem } from '@/api/monthlyIntegration';

const { Text } = Typography;
const { useBreakpoint } = Grid;

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

function formatDecimal3(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

const columns: ColumnsType<MonthlyIntegrationScheduleItem> = [
  { title: '소속', dataIndex: 'branchName', width: 100, fixed: 'left' },
  { title: '거래처 지점명', dataIndex: 'accountBranchName', width: 110, render: (v) => v ?? '-' },
  { title: '거래처코드', dataIndex: 'accountCode', width: 100 },
  { title: '거래처명', dataIndex: 'accountName', width: 140 },
  { title: '사번', dataIndex: 'employeeCode', width: 90 },
  { title: '직위', dataIndex: 'title', width: 70, render: (v) => v ?? '-' },
  { title: '이름', dataIndex: 'employeeName', width: 80 },
  { title: '근무형태1', dataIndex: 'workingCategory1', width: 90 },
  { title: '근무형태3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
  { title: '근무형태4', dataIndex: 'workingCategory4', width: 90, render: (v) => v ?? '-' },
  { title: '근무형태5', dataIndex: 'workingCategory5', width: 90, render: (v) => v ?? '-' },
  {
    title: '총 투입횟수',
    dataIndex: 'totalInputCount',
    width: 100,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '총 환산근무일수',
    dataIndex: 'equivalentWorkingDays',
    width: 120,
    align: 'right',
    render: (v: number) => formatDecimal3(v),
  },
  {
    title: '총 환산인원',
    dataIndex: 'convertedHeadcount',
    width: 100,
    align: 'right',
    render: (v: number) => formatDecimal3(v),
  },
  {
    title: 'ABC마감실적',
    dataIndex: 'avgClosingAmount',
    width: 120,
    align: 'right',
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
    { label: 'ABC마감실적', value: formatNumber(item.avgClosingAmount) },
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
  const previousMonth = dayjs().subtract(1, 'month');
  const [year, setYear] = useState(previousMonth.year());
  const [month, setMonth] = useState(previousMonth.month() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<{
    year: number;
    month: number;
    codes: string[];
  } | null>(null);

  const screens = useBreakpoint();
  const isMobile = !screens.md;

  const { data, isLoading, isError, error } = useMonthlyIntegrationSchedule(
    queryParams?.year ?? year,
    queryParams?.month ?? month,
    queryParams?.codes ?? [],
    queryParams != null,
  );

  const exportMutation = useMonthlyIntegrationExport();

  const handleSearch = () => {
    setQueryParams({ year, month, codes: selectedCodes });
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate({
      year: queryParams.year,
      month: queryParams.month,
      costCenterCodes: queryParams.codes,
    });
  };

  return (
    <div style={{ padding: 16 }}>
      <ScheduleFilterBar
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
      />

      {isError && (
        <Alert
          type="error"
          message="조회 실패"
          description={error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다'}
          style={{ marginBottom: 16 }}
        />
      )}

      {queryParams == null ? null : isLoading ? (
        <div style={{ textAlign: 'center', padding: 48 }}>
          <Spin size="large" />
        </div>
      ) : data && data.items.length === 0 ? (
        <Empty description="조회 결과가 없습니다" />
      ) : data ? (
        <>
          <Text style={{ marginBottom: 8, display: 'block' }}>
            총 {formatNumber(data.totalCount)}건
          </Text>
          {isMobile ? (
            <Space direction="vertical" style={{ width: '100%' }} size={0}>
              {data.items.map((item, index) => (
                <MobileItemCard key={index} item={item} />
              ))}
            </Space>
          ) : (
            <Table
              rowKey={(_, index) => String(index)}
              columns={columns}
              dataSource={data.items}
              pagination={false}
              scroll={{ x: 1600 }}
              size="small"
              sticky
            />
          )}
        </>
      ) : null}
    </div>
  );
}
