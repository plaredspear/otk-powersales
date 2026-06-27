import { useEffect, useMemo, useRef, useState } from 'react';
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

// 조회 가능한 최대 기간(개월). backend WorkHistoryPeriodSummaryService.MAX_RANGE_MONTHS 와 정합.
const MAX_RANGE_MONTHS = 6;

const MONTHLY_CELL_BG = '#fafafa';

/**
 * 단일 테이블의 통합 행 — 여사원 합계 행(summary) 과, 그 아래 끼워 넣는 월별 통계 행(monthly).
 * 별도 자식 테이블 대신 같은 테이블의 일반 행으로 편입해 컬럼 정렬을 구조적으로 일치시킨다.
 */
type SummaryRow = WorkHistoryPeriodSummaryItem & { __kind: 'summary'; __key: string };
type MonthlyRow = WorkHistoryMonthlyStat & { __kind: 'monthly'; __key: string };
type TableRow = SummaryRow | MonthlyRow;

const isSummary = (r: TableRow): r is SummaryRow => r.__kind === 'summary';

// 월별(monthly) 행에서 식별부(소속지점~직위) 자리를 회색 배경으로 표시하기 위한 onCell.
const monthlyIdentityCell = (record: TableRow) =>
  isSummary(record) ? {} : { style: { backgroundColor: MONTHLY_CELL_BG } };

// 숫자 지표 컬럼 정의 생성 — summary/monthly 양쪽 동일 dataIndex 라 render 분기 불필요.
function numericColumn(
  title: string,
  dataIndex: keyof WorkHistoryMonthlyStat,
  width: number,
): ColumnsType<TableRow>[number] {
  return {
    title,
    dataIndex,
    width,
    align: 'right',
    render: (v: number) => formatNumber(v),
  };
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

/** 여사원 합계 행 식별 키 — 확장 상태 / 월별 행 부모 매칭에 사용. */
function summaryKeyOf(item: WorkHistoryPeriodSummaryItem): string {
  return item.employeeCode ?? item.employeeName ?? '';
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
  // 펼쳐진 여사원 합계 행 키 집합 (월별 통계 행 노출 대상).
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());

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
  const toggleRow = (item: WorkHistoryPeriodSummaryItem) => {
    if (item.monthlyBreakdown.length === 0) return;
    const key = summaryKeyOf(item);
    setExpandedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  const handleExport = () => {
    if (!queryParams) return;
    exportMutation.mutate(queryParams, {
      onError: (e) =>
        message.error(e instanceof Error ? e.message : '엑셀 다운로드에 실패했습니다'),
    });
  };

  const exportDisabled = queryParams == null || !data || data.items.length === 0;

  // 펼친 여사원 합계 행 뒤에 그 월별 통계 행을 끼워 넣은 단일 테이블 데이터.
  // 별도 자식 테이블이 아니라 같은 테이블의 일반 행이라 컬럼 정렬이 구조적으로 항상 일치한다.
  const tableRows = useMemo<TableRow[]>(() => {
    const items = data?.items ?? [];
    const rows: TableRow[] = [];
    for (const item of items) {
      const key = summaryKeyOf(item);
      rows.push({ ...item, __kind: 'summary', __key: key });
      if (expandedKeys.has(key)) {
        for (const m of item.monthlyBreakdown) {
          rows.push({ ...m, __kind: 'monthly', __key: `${key}__${m.yearMonth}` });
        }
      }
    }
    return rows;
  }, [data, expandedKeys]);

  // 단일 컬럼 정의 — summary/monthly 행을 같은 컬럼으로 렌더. 식별부 첫 컬럼은 행 종류에 따라
  // (summary) 펼침 삼각형+소속지점 / (monthly) 회색 배경의 년월 을 표시한다.
  const columns: ColumnsType<TableRow> = [
    {
      title: '소속지점',
      dataIndex: 'orgName',
      width: 120,
      ellipsis: true,
      onCell: monthlyIdentityCell,
      render: (_v, record) => {
        if (!isSummary(record)) return record.yearMonth; // 월별 행: 년월(yyyy-MM)
        const expandable = record.monthlyBreakdown.length > 0;
        const expanded = expandedKeys.has(record.__key);
        return (
          <Space size={4}>
            {expandable ? (
              <CaretRightOutlined
                style={{
                  transition: 'transform 0.2s',
                  transform: expanded ? 'rotate(90deg)' : 'rotate(0deg)',
                }}
              />
            ) : (
              <span style={{ display: 'inline-block', width: 14 }} />
            )}
            <span>{record.orgName ?? '-'}</span>
          </Space>
        );
      },
    },
    {
      title: '사번',
      dataIndex: 'employeeCode',
      width: 100,
      ellipsis: true,
      onCell: monthlyIdentityCell,
      render: (_v, record) => (isSummary(record) ? record.employeeCode ?? '-' : null),
    },
    {
      title: '이름',
      dataIndex: 'employeeName',
      width: 100,
      ellipsis: true,
      onCell: monthlyIdentityCell,
      render: (_v, record) => (isSummary(record) ? record.employeeName ?? '-' : null),
    },
    {
      title: '직위',
      dataIndex: 'title',
      width: 90,
      ellipsis: true,
      onCell: monthlyIdentityCell,
      render: (_v, record) => (isSummary(record) ? record.title ?? '-' : null),
    },
    numericColumn('총 근무일수', 'totalWorkingDays', 110),
    numericColumn('근무 거래처 수', 'workingAccountCount', 120),
    numericColumn('진열', 'displayDays', 80),
    numericColumn('행사', 'eventDays', 80),
    numericColumn('근무', 'workDays', 80),
    numericColumn('연차', 'annualLeaveDays', 80),
    numericColumn('대휴', 'altHolidayDays', 80),
  ];

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
            rowKey="__key"
            columns={columns}
            dataSource={tableRows}
            pagination={false}
            size="small"
            sticky
            scroll={{ x: 'max-content' }}
            // summary 행(월별 분해 보유) 클릭 시 확장 토글 + 포인터 커서. monthly 행은 클릭 무반응.
            onRow={(record) =>
              isSummary(record) && record.monthlyBreakdown.length > 0
                ? { onClick: () => toggleRow(record), style: { cursor: 'pointer' } }
                : {}
            }
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
