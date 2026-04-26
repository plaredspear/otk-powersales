import { useState } from 'react';
import { Alert, Empty, Spin, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ScheduleFilterBar from '@/components/schedules/ScheduleFilterBar';
import { useMonthlyIntegrationSchedule } from '@/hooks/schedules/useMonthlyIntegrationSchedule';
import { useMonthlyIntegrationExport } from '@/hooks/schedules/useMonthlyIntegrationExport';
import type { MonthlyIntegrationScheduleItem } from '@/api/monthlyIntegration';

const { Text } = Typography;

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

export default function MonthlyIntegrationSchedulePage() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<{
    year: number;
    month: number;
    codes: string[];
  } | null>(null);

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
          <Table
            rowKey={(_, index) => String(index)}
            columns={columns}
            dataSource={data.items}
            pagination={false}
            scroll={{ x: 1600 }}
            size="small"
            sticky
          />
        </>
      ) : null}
    </div>
  );
}
