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

// SF 화면 비율 정합 — 사이드바 240px 제외 가용 영역에 15개 컬럼이 가로 스크롤 없이 들어가도록 폭 축소.
// 셀 ellipsis + 숫자 우측 정렬 유지로 좁힌 폭에서도 가독성 확보. 헤더가 잘릴 경우 antd 가 자동 줄바꿈.
const NUM_COL_WIDTH = 70;
const SUM_COL_WIDTH = 90;
const columns: ColumnsType<CategoryScheduleItem> = [
  { title: '지점명', dataIndex: 'branchName', width: 80, fixed: 'left', ellipsis: true },
  {
    title: '총계',
    children: [
      { title: '당월총합계', dataIndex: 'currentMonthTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전월마감합계', dataIndex: 'previousMonthTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전체증감수', dataIndex: 'totalChange', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal1) },
    ],
  },
  {
    title: '진열',
    children: [
      { title: '고정', dataIndex: 'displayFixed', width: NUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '격고', dataIndex: 'displayAlternate', width: NUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '순회', dataIndex: 'displayPatrol', width: NUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월진열합계', dataIndex: 'currentMonthDisplayTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월진열합계', dataIndex: 'previousMonthDisplayTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '진열증감수', dataIndex: 'displayChange', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
    ],
  },
  {
    title: '행사',
    children: [
      { title: '상온', dataIndex: 'eventAmbient', width: NUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '냉동/냉장', dataIndex: 'eventFrozenChilled', width: NUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월행사합계', dataIndex: 'currentMonthEventTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월행사합계', dataIndex: 'previousMonthEventTotal', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '행사증감수', dataIndex: 'eventChange', width: SUM_COL_WIDTH, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
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
          locale={{
            emptyText:
              queryParams == null ? '조회 조건을 설정하고 조회 버튼을 눌러주세요' : '조회 결과가 없습니다',
          }}
        />
      )}
    </div>
  );
}
