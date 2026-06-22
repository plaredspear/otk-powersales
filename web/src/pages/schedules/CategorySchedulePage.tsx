import { useEffect, useRef, useState } from 'react';
import { Alert, message, Spin } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import { useCategorySchedule } from '@/hooks/schedules/useCategorySchedule';
import { useCategoryExport } from '@/hooks/schedules/useCategoryExport';
import type { CategoryScheduleItem } from '@/api/monthlyIntegration';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

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

// 각 leaf 컬럼에 기본 width 를 지정해야 ResizableTable 의 헤더 우측 경계 드래그(리사이즈)가 활성화된다.
// width 미지정 컬럼은 일반 <th> 로 렌더되어 리사이즈 핸들이 붙지 않는다 (그룹 헤더는 children 합으로 자동 계산).
// 좁아진 컬럼은 ellipsis 로 텍스트를 "..." 축약하고, 드래그로 폭을 넓혀 확인할 수 있다.
const columns: ColumnsType<CategoryScheduleItem> = [
  { title: '지점명', dataIndex: 'branchName', width: 140, ellipsis: true },
  {
    title: '총계',
    children: [
      { title: '당월총합계', dataIndex: 'currentMonthTotal', width: 110, align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전월마감합계', dataIndex: 'previousMonthTotal', width: 120, align: 'right', ellipsis: true, render: (v: number) => formatDecimal1(v) },
      { title: '전체증감수', dataIndex: 'totalChange', width: 110, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal1) },
    ],
  },
  {
    title: '진열',
    children: [
      { title: '고정', dataIndex: 'displayFixed', width: 90, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '격고', dataIndex: 'displayAlternate', width: 90, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '순회', dataIndex: 'displayPatrol', width: 90, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월진열합계', dataIndex: 'currentMonthDisplayTotal', width: 120, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월진열합계', dataIndex: 'previousMonthDisplayTotal', width: 120, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '진열증감수', dataIndex: 'displayChange', width: 110, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
    ],
  },
  {
    title: '행사',
    children: [
      { title: '상온', dataIndex: 'eventAmbient', width: 90, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '냉동/냉장', dataIndex: 'eventFrozenChilled', width: 100, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '당월행사합계', dataIndex: 'currentMonthEventTotal', width: 120, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '전월행사합계', dataIndex: 'previousMonthEventTotal', width: 120, align: 'right', ellipsis: true, render: (v: number) => formatDecimal3(v) },
      { title: '행사증감수', dataIndex: 'eventChange', width: 110, align: 'right', ellipsis: true, render: (v: number) => renderChange(v, formatDecimal3) },
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

  const { data, isLoading, isError, error, refetch, isFetching } = useCategorySchedule(
    queryParams?.year ?? year,
    queryParams?.month ?? month,
    queryParams?.codes ?? [],
    queryParams != null,
  );

  const exportMutation = useCategoryExport();

  const emptyNoticeShownForRef = useRef<string | null>(null);
  const autoSearchedRef = useRef(false);

  // 페이지 진입 시 현재 년/월로 자동 조회. 단일지점 사용자는 본인 지점이 자동 선택되므로
  // 지점 코드가 채워지는 시점에 최초 1회만 조회를 트리거한다. (codes 빈 배열은 backend 에서 거부)
  useEffect(() => {
    if (autoSearchedRef.current) return;
    if (selectedCodes.length === 0) return;
    autoSearchedRef.current = true;
    setQueryParams({ year, month, codes: selectedCodes });
  }, [selectedCodes, year, month]);

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
        extraActions={
          queryParams != null ? (
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
      ) : (
        <ResizableTable
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
