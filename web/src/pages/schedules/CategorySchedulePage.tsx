import { useEffect, useRef, useState } from 'react';
import { Alert, Empty, message, Spin, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ScheduleFilterBar from '@/components/schedules/ScheduleFilterBar';
import { useCategorySchedule } from '@/hooks/schedules/useCategorySchedule';
import { useCategoryExport } from '@/hooks/schedules/useCategoryExport';
import type { CategoryScheduleItem } from '@/api/monthlyIntegration';

function formatDecimal1(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
}

function formatDecimal3(value: number): string {
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

function renderChange(value: number, formatter: (v: number) => string) {
  const style = value < 0 ? { color: 'red' } : undefined;
  return <span style={style}>{formatter(value)}</span>;
}

const columns: ColumnsType<CategoryScheduleItem> = [
  { title: '지점명', dataIndex: 'branchName', width: 100, fixed: 'left' },
  {
    title: '총계',
    children: [
      { title: '당월 총합계', dataIndex: 'currentMonthTotal', width: 100, align: 'right', render: (v: number) => formatDecimal1(v) },
      { title: '전월 마감합계', dataIndex: 'previousMonthTotal', width: 110, align: 'right', render: (v: number) => formatDecimal1(v) },
      { title: '증감', dataIndex: 'totalChange', width: 80, align: 'right', render: (v: number) => renderChange(v, formatDecimal1) },
    ],
  },
  {
    title: '진열',
    children: [
      { title: '고정', dataIndex: 'displayFixed', width: 80, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '격고', dataIndex: 'displayAlternate', width: 80, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '순회', dataIndex: 'displayPatrol', width: 80, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '당월 진열합계', dataIndex: 'currentMonthDisplayTotal', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '전월 진열합계', dataIndex: 'previousMonthDisplayTotal', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '진열 증감', dataIndex: 'displayChange', width: 90, align: 'right', render: (v: number) => renderChange(v, formatDecimal3) },
    ],
  },
  {
    title: '행사',
    children: [
      { title: '상온', dataIndex: 'eventAmbient', width: 80, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '냉동/냉장', dataIndex: 'eventFrozenChilled', width: 100, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '당월 행사합계', dataIndex: 'currentMonthEventTotal', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '전월 행사합계', dataIndex: 'previousMonthEventTotal', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
      { title: '행사 증감', dataIndex: 'eventChange', width: 90, align: 'right', render: (v: number) => renderChange(v, formatDecimal3) },
    ],
  },
];

export default function CategorySchedulePage() {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<{
    year: number;
    month: number;
    codes: string[];
  } | null>(null);

  const { data, isLoading, isError, error } = useCategorySchedule(
    queryParams?.year ?? year,
    queryParams?.month ?? month,
    queryParams?.codes ?? [],
    queryParams != null,
  );

  const exportMutation = useCategoryExport();

  const emptyNoticeShownForRef = useRef<string | null>(null);

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
        <Table
          rowKey={(_, index) => String(index)}
          columns={columns}
          dataSource={data.items}
          pagination={false}
          scroll={{ x: 1500 }}
          size="small"
          sticky
          bordered
        />
      ) : null}
    </div>
  );
}
