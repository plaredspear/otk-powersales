import { useEffect, useRef, useState } from 'react';
import { Alert, message, Spin, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
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

// 가용 영역에 맞춰 자동 분배 — width 미지정 시 antd 가 부모 wrapper 폭에 맞춰 균등 분배.
// SF 화면처럼 좁아져도 ellipsis 로 텍스트 축약하여 가로 스크롤 없이 한 화면 표시.
const columns: ColumnsType<CategoryScheduleItem> = [
  { title: '지점명', dataIndex: 'branchName', ellipsis: true },
  {
    title: '총계',
    children: [
      { title: '당월총합계', dataIndex: 'currentMonthTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전월마감합계', dataIndex: 'previousMonthTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전체증감수', dataIndex: 'totalChange', align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal1) },
    ],
  },
  {
    title: '진열',
    children: [
      { title: '고정', dataIndex: 'displayFixed', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '격고', dataIndex: 'displayAlternate', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '순회', dataIndex: 'displayPatrol', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월진열합계', dataIndex: 'currentMonthDisplayTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월진열합계', dataIndex: 'previousMonthDisplayTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '진열증감수', dataIndex: 'displayChange', align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
    ],
  },
  {
    title: '행사',
    children: [
      { title: '상온', dataIndex: 'eventAmbient', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '냉동/냉장', dataIndex: 'eventFrozenChilled', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월행사합계', dataIndex: 'currentMonthEventTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월행사합계', dataIndex: 'previousMonthEventTotal', align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '행사증감수', dataIndex: 'eventChange', align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
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
      ) : (
        <Table
          rowKey="branchName"
          columns={columns}
          dataSource={data?.items ?? []}
          pagination={false}
          size="small"
          sticky
          bordered
          tableLayout="fixed"
          locale={{
            emptyText:
              queryParams == null ? '조회 조건을 설정하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
