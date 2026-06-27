import { useEffect, useMemo, useRef, useState, type Key } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Empty,
  Input,
  InputNumber,
  message,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd';
import { CaretRightOutlined, DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
  useAttendInfoBranches,
  useWorkHistoryPeriodSummary,
  useWorkHistoryPeriodSummaryExport,
} from '@/hooks/attend-info/useAttendInfo';
import type { WorkHistoryMonthlyStat, WorkHistoryPeriodSummaryItem } from '@/api/attendInfo';
import ResizableTable from '@/components/common/ResizableTable';

const { Text } = Typography;

function formatNumber(value: number): string {
  return value.toLocaleString('ko-KR');
}

// 부모 테이블 맨 왼쪽 펼침(expand) 아이콘 컬럼 폭. 자식(월별) 테이블을 이만큼 들여써
// 부모의 데이터 컬럼 시작점과 자식 컬럼 시작점을 정렬한다.
const EXPAND_COLUMN_WIDTH = 48;

// 조회 가능한 최대 기간(개월). backend WorkHistoryPeriodSummaryService.MAX_RANGE_MONTHS 와 정합.
const MAX_RANGE_MONTHS = 6;

// 각 컬럼에 기본 width 를 지정해야 ResizableTable 의 헤더 리사이즈 핸들이 활성화된다.
const COLUMNS: ColumnsType<WorkHistoryPeriodSummaryItem> = [
  { title: '소속지점', dataIndex: 'orgName', width: 120, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '사번', dataIndex: 'employeeCode', width: 100, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '이름', dataIndex: 'employeeName', width: 100, ellipsis: true, render: (v: string | null) => v ?? '-' },
  { title: '직위', dataIndex: 'title', width: 90, ellipsis: true, render: (v: string | null) => v ?? '-' },
  {
    title: '총 근무일수',
    dataIndex: 'totalWorkingDays',
    width: 110,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  {
    title: '근무 거래처 수',
    dataIndex: 'workingAccountCount',
    width: 120,
    align: 'right',
    render: (v: number) => formatNumber(v),
  },
  { title: '진열', dataIndex: 'displayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '행사', dataIndex: 'eventDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '근무', dataIndex: 'workDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '연차', dataIndex: 'annualLeaveDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '대휴', dataIndex: 'altHolidayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
];

// 펼침(월별 통계) 행 컬럼 — 부모 테이블과 컬럼 수/폭/정렬을 1:1 로 일치시킨다.
// 부모의 식별부(소속지점/사번/이름/직위) 자리: 첫 컬럼(소속지점 폭)에 '년도-월'(yyyy-MM) 을 회색 배경으로
// 표시하고, 나머지 식별 컬럼(사번/이름/직위 폭)은 빈 칸으로 두어 지표 컬럼이 부모와 세로 정렬되게 한다.
const MONTHLY_COLUMNS: ColumnsType<WorkHistoryMonthlyStat> = [
  {
    title: '년월',
    dataIndex: 'yearMonth',
    width: 120,
    onCell: () => ({ style: { backgroundColor: '#fafafa' } }),
  },
  { title: '', dataIndex: 'employeeCodeSpacer', width: 100, render: () => null },
  { title: '', dataIndex: 'employeeNameSpacer', width: 100, render: () => null },
  { title: '', dataIndex: 'titleSpacer', width: 90, render: () => null },
  { title: '총 근무일수', dataIndex: 'totalWorkingDays', width: 110, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '근무 거래처 수', dataIndex: 'workingAccountCount', width: 120, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '진열', dataIndex: 'displayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '행사', dataIndex: 'eventDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '근무', dataIndex: 'workDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '연차', dataIndex: 'annualLeaveDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
  { title: '대휴', dataIndex: 'altHolidayDays', width: 80, align: 'right', render: (v: number) => formatNumber(v) },
];

/**
 * 부모 컬럼 리사이즈 결과(columnWidths: leaf path → px)를 자식(월별) 컬럼 폭에 반영.
 * 부모/자식 컬럼은 인덱스가 1:1 대응하므로 path "0"~"10" 을 그대로 자식 i 번째 폭으로 덮어쓴다.
 * 드래그 전(맵에 키 없음) 컬럼은 base 정의 폭을 유지.
 */
function buildMonthlyColumns(
  columnWidths: Record<string, number>,
): ColumnsType<WorkHistoryMonthlyStat> {
  return MONTHLY_COLUMNS.map((col, index) => {
    const overridden = columnWidths[`${index}`];
    return overridden != null ? { ...col, width: overridden } : col;
  });
}

interface QueryParams {
  fromYearMonth: string;
  toYearMonth: string;
  costCenterCodes: string[];
  keyword: string;
}

function toYearMonth(year: number, month: number): string {
  return `${year}-${String(month).padStart(2, '0')}`;
}

/** 집계 행 식별 키 — 테이블 rowKey / 확장 상태 / row 클릭에서 동일하게 사용. */
function rowKeyOf(record: WorkHistoryPeriodSummaryItem): string {
  return record.employeeCode ?? record.employeeName ?? '';
}

export default function WorkHistoryPeriodPage() {
  const now = dayjs();
  const [fromYear, setFromYear] = useState(now.year());
  const [fromMonth, setFromMonth] = useState(now.month() + 1);
  const [toYear, setToYear] = useState(now.year());
  const [toMonth, setToMonth] = useState(now.month() + 1);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [keyword, setKeyword] = useState('');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [expandedKeys, setExpandedKeys] = useState<readonly Key[]>([]);
  // 부모 테이블 컬럼 리사이즈 결과 (leaf path "0"~"10" → 폭px). 자식(월별) 테이블 폭 동기화에 사용.
  const [columnWidths, setColumnWidths] = useState<Record<string, number>>({});

  const { data: branches = [] } = useAttendInfoBranches();
  const branchOptions = useMemo(
    () => branches.map((b) => ({ value: b.branchCode, label: b.branchName })),
    [branches],
  );
  const allCodes = useMemo(() => branches.map((b) => b.branchCode), [branches]);
  const allSelected = allCodes.length > 0 && selectedCodes.length === allCodes.length;
  const someSelected = selectedCodes.length > 0 && !allSelected;
  const singleBranch = branches.length === 1;

  const { data, isLoading, isError, error } = useWorkHistoryPeriodSummary(queryParams);
  const exportMutation = useWorkHistoryPeriodSummaryExport();

  const fromYm = toYearMonth(fromYear, fromMonth);
  const toYm = toYearMonth(toYear, toMonth);
  const reversed = fromYm > toYm;
  // 시작~종료 포함 개월 수 (예: 2026-01 ~ 2026-06 = 6). 역순일 땐 의미 없으므로 0 처리.
  const inclusiveMonths = reversed
    ? 0
    : (toYear - fromYear) * 12 + (toMonth - fromMonth) + 1;
  const exceedsMax = inclusiveMonths > MAX_RANGE_MONTHS;
  const rangeInvalid = reversed || exceedsMax;

  const handleSearch = () => {
    if (rangeInvalid) return;
    // 단일지점 사용자는 본인 지점 자동 선택, 미선택이면 빈 배열(권한 스코프 전체)로 조회.
    const codes = singleBranch && selectedCodes.length === 0 ? allCodes : selectedCodes;
    setQueryParams({ fromYearMonth: fromYm, toYearMonth: toYm, costCenterCodes: codes, keyword });
  };

  // 지점이 하나뿐인 사용자는 선택할 지점이 없으므로 페이지 진입 시(지점 목록 로드 후) 현재 년월로 1회 자동 조회.
  const autoSearchedRef = useRef(false);
  useEffect(() => {
    if (autoSearchedRef.current) return;
    if (!singleBranch || rangeInvalid) return;
    autoSearchedRef.current = true;
    setQueryParams({ fromYearMonth: fromYm, toYearMonth: toYm, costCenterCodes: allCodes, keyword });
  }, [singleBranch, rangeInvalid, fromYm, toYm, allCodes, keyword]);

  const handleToggleAll = () => {
    setSelectedCodes(allSelected ? [] : allCodes);
  };

  // 펼침 가능한(월별 분해가 있는) 행 클릭 시 확장 토글.
  const toggleRow = (record: WorkHistoryPeriodSummaryItem) => {
    if (record.monthlyBreakdown.length === 0) return;
    const key = rowKeyOf(record);
    setExpandedKeys((prev) =>
      prev.includes(key) ? prev.filter((k) => k !== key) : [...prev, key],
    );
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate(queryParams, {
      onError: (e) =>
        message.error(e instanceof Error ? e.message : '엑셀 다운로드에 실패했습니다'),
    });
  };

  const exportDisabled = queryParams == null || !data || data.items.length === 0;

  // 부모 컬럼 리사이즈 폭을 반영한 자식(월별) 테이블 컬럼.
  const monthlyColumns = useMemo(() => buildMonthlyColumns(columnWidths), [columnWidths]);

  const renderMonthlyBreakdown = (record: WorkHistoryPeriodSummaryItem) => (
    <Table<WorkHistoryMonthlyStat>
      rowKey="yearMonth"
      size="small"
      columns={monthlyColumns}
      dataSource={record.monthlyBreakdown}
      pagination={false}
      // 부모 헤더와 중복이므로 자식 헤더는 숨김 (년월 값은 헤더 없이도 인식 가능).
      showHeader={false}
      tableLayout="fixed"
      scroll={{ x: 'max-content' }}
      // 부모의 펼침 아이콘 컬럼 폭만큼 들여써 데이터 컬럼이 부모와 세로 정렬되게 한다.
      style={{ margin: `0 0 0 ${EXPAND_COLUMN_WIDTH}px` }}
    />
  );

  return (
    <div style={{ padding: 16 }}>
      <div
        style={{
          marginBottom: 16,
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          gap: 8,
        }}
      >
        <Space wrap align="end">
          <Space direction="vertical" size={4}>
            <span>지점명:</span>
            {singleBranch ? (
              <Tag color="geekblue" style={{ fontSize: 14, padding: '5px 12px', marginInlineEnd: 0 }}>
                지점: {branches[0].branchName}
              </Tag>
            ) : (
              <Select
                mode="multiple"
                value={selectedCodes}
                onChange={(values) => setSelectedCodes(values as string[])}
                options={branchOptions}
                placeholder="지점 선택 (미선택 시 전체)"
                style={{ minWidth: 280, maxWidth: 480 }}
                maxTagCount="responsive"
                allowClear
                showSearch
                optionFilterProp="label"
                filterOption={(input, option) => (option?.label ?? '').toString().includes(input)}
                popupRender={(menu) => (
                  <>
                    <div style={{ padding: '4px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <Checkbox checked={allSelected} indeterminate={someSelected} onChange={handleToggleAll}>
                        전체 ({selectedCodes.length}/{allCodes.length})
                      </Checkbox>
                    </div>
                    {menu}
                  </>
                )}
                notFoundContent="항목 없음"
              />
            )}
          </Space>
          <Space direction="vertical" size={4}>
            <span>시작 년월:</span>
            <Space.Compact>
              <InputNumber
                value={fromYear}
                min={2020}
                max={2099}
                onChange={(v) => v != null && setFromYear(v)}
                style={{ width: 90 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
              <InputNumber
                value={fromMonth}
                min={1}
                max={12}
                onChange={(v) => v != null && setFromMonth(v)}
                style={{ width: 64 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
            </Space.Compact>
          </Space>
          <Space direction="vertical" size={4}>
            <span>종료 년월:</span>
            <Space.Compact>
              <InputNumber
                value={toYear}
                min={2020}
                max={2099}
                onChange={(v) => v != null && setToYear(v)}
                style={{ width: 90 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
              <InputNumber
                value={toMonth}
                min={1}
                max={12}
                onChange={(v) => v != null && setToMonth(v)}
                style={{ width: 64 }}
                parser={(value) => Number((value ?? '').toString().replace(/[^0-9]/g, ''))}
              />
            </Space.Compact>
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
        </Space>
        <Space>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
            disabled={rangeInvalid}
            loading={isLoading}
          >
            조회
          </Button>
          <Button
            icon={<DownloadOutlined />}
            onClick={handleExport}
            disabled={exportDisabled}
            loading={exportMutation.isPending}
          >
            엑셀 다운로드
          </Button>
        </Space>
      </div>

      {reversed && (
        <Alert
          type="warning"
          message="시작 년월은 종료 년월보다 이후일 수 없습니다"
          style={{ marginBottom: 16 }}
        />
      )}
      {!reversed && exceedsMax && (
        <Alert
          type="warning"
          message={`조회 기간은 최대 ${MAX_RANGE_MONTHS}개월까지 가능합니다`}
          style={{ marginBottom: 16 }}
        />
      )}

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
        <>
          {queryParams != null && data && (
            <Text style={{ marginBottom: 8, display: 'block' }}>총 {formatNumber(data.totalCount)}명</Text>
          )}
          <ResizableTable
            rowKey={rowKeyOf}
            columns={COLUMNS}
            dataSource={data?.items ?? []}
            pagination={false}
            size="small"
            sticky
            scroll={{ x: 'max-content' }}
            // 컬럼 리사이즈 시 폭을 받아 펼침(월별) 테이블 컬럼에 동기화.
            onColumnWidthsChange={setColumnWidths}
            // 펼침 가능한 행은 row 어디를 클릭해도 확장 토글 + 포인터 커서.
            onRow={(record) => ({
              onClick: () => toggleRow(record),
              style: record.monthlyBreakdown.length > 0 ? { cursor: 'pointer' } : undefined,
            })}
            expandable={{
              // 월별 분해가 있는 행(2개월 이상 조회)만 펼침 가능. 단일 월 조회는 분해가 비어 펼침 아이콘 숨김.
              expandedRowRender: renderMonthlyBreakdown,
              rowExpandable: (record) => record.monthlyBreakdown.length > 0,
              expandedRowKeys: expandedKeys,
              // 펼침 아이콘 컬럼 폭 고정 — 자식 테이블 들여쓰기(EXPAND_COLUMN_WIDTH)와 정합.
              columnWidth: EXPAND_COLUMN_WIDTH,
              // + 아이콘 대신 삼각형(▶). 펼쳐지면 90° 회전. 펼침 불가 행은 아이콘 없음.
              // 클릭 토글은 onRow.onClick 이 처리하므로 아이콘은 시각 표현만 담당.
              expandIcon: ({ expandable, expanded }) =>
                expandable ? (
                  <CaretRightOutlined
                    style={{
                      cursor: 'pointer',
                      transition: 'transform 0.2s',
                      transform: expanded ? 'rotate(90deg)' : 'rotate(0deg)',
                    }}
                  />
                ) : null,
            }}
            locale={{
              emptyText:
                queryParams == null ? (
                  '조회 조건을 설정하고 조회 버튼을 눌러주세요'
                ) : (
                  <Empty description="조회 결과가 없습니다" />
                ),
            }}
          />
        </>
      )}
    </div>
  );
}
