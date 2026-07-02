import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Empty,
  InputNumber,
  Radio,
  Space,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  BorderOutlined,
  CheckSquareOutlined,
  ClearOutlined,
  DownloadOutlined,
  MinusSquareOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchSummary,
  fetchMiddle,
  fetchDetail,
  exportSummary as apiExportSummary,
  exportMiddle as apiExportMiddle,
  exportDetail as apiExportDetail,
  SUMMARY_CATEGORY_COLUMNS,
  CATEGORY_COLUMN_CODES,
  categoryColumnLabel,
  type SalesComparisonSummaryRow,
  type SalesComparisonMiddleItem,
  type SalesComparisonDetailItem,
  type SummaryFilter,
} from '@/api/salesComparison';
import { useDashboardBranches } from '@/hooks/dashboard/useDashboardBranches';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import './DeploymentPage.css';

const { Title, Text } = Typography;

type SearchMode = 'summary' | 'detail';

const SUITABILITY_OPTIONS = ['적합', '경계', '재검토'];
const WORKING_CATEGORY_3_OPTIONS = ['고정', '격고', '순회'];
const WORKING_CATEGORY_1_OPTIONS = ['진열', '행사'];
const WORKING_CATEGORY_5_OPTIONS = ['상시', '임시'];

function formatNumber(value: number | null | undefined): string {
  if (value == null) return '-';
  return value.toLocaleString('ko-KR');
}

function formatDecimal3(value: number | null | undefined): string {
  if (value == null) return '-';
  return value.toLocaleString('ko-KR', { minimumFractionDigits: 3, maximumFractionDigits: 3 });
}

function suitabilityCellStyle(suitability: string): React.CSSProperties {
  switch (suitability) {
    case '적합':
      return { backgroundColor: '#C8E6C9' };
    case '경계':
      return { backgroundColor: '#FFF59D' };
    case '재검토':
      return { backgroundColor: '#FFCDD2' };
    case '총계':
      return { backgroundColor: '#CE93D8' };
    case '소계':
    case '적합소계':
    case '경계소계':
    case '재검토소계':
      return { backgroundColor: '#FFE0B2' };
    default:
      return {};
  }
}

// 검색조건 라벨 + 전체선택 토글 아이콘을 한 줄에 배치.
// 필수 조건은 붉은색 '*', 전체선택 아이콘은 3-state(전체/일부/없음)로 현재 선택 상태를 표시한다.
function FilterLabel({
  label,
  required,
  allValues,
  selected,
  onChange,
  disabled: disabledProp,
}: {
  label: string;
  required?: boolean;
  allValues: string[];
  selected: string[];
  onChange: (vals: string[]) => void;
  disabled?: boolean;
}) {
  const checkedCount = selected.filter((v) => allValues.includes(v)).length;
  const allChecked = allValues.length > 0 && checkedCount === allValues.length;
  const indeterminate = checkedCount > 0 && checkedCount < allValues.length;
  const disabled = disabledProp || allValues.length === 0;
  const toggleAll = () => onChange(allChecked ? [] : [...allValues]);
  const icon = allChecked ? (
    <CheckSquareOutlined style={{ color: '#1677ff' }} />
  ) : indeterminate ? (
    <MinusSquareOutlined style={{ color: '#1677ff' }} />
  ) : (
    <BorderOutlined />
  );
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <Text strong>
        {label}
        {required && <span style={{ color: '#ff4d4f', marginLeft: 2 }}>*</span>}
      </Text>
      <Tooltip title={allChecked ? '전체해제' : '전체선택'}>
        <Button
          type="text"
          size="small"
          icon={icon}
          disabled={disabled}
          onClick={toggleAll}
          style={{ padding: 0, height: 'auto', backgroundColor: '#fff' }}
        />
      </Tooltip>
    </div>
  );
}

// 가로 스크롤이 필요한 넓은 테이블을 감싸고, 콘텐츠 상단에 sticky 로 고정된 커스텀 가로
// 스크롤바를 항상 노출한다. 실제 스크롤은 antd 가 scroll.x 로 만든 .ant-table-content 가
// 담당하며, 그 요소의 scrollLeft 를 커스텀 바와 양방향 동기화한다. antd 의 sticky 스크롤바가
// "테이블 높이 > 윈도우 높이" 일 때만 보이는 제약(ant-design#30271)을 우회한다.
function ScrollXContainer({ children }: { children: React.ReactNode }) {
  const barRef = useRef<HTMLDivElement>(null);
  const barInnerRef = useRef<HTMLDivElement>(null);
  const wrapRef = useRef<HTMLDivElement>(null);
  const scrollElRef = useRef<HTMLElement | null>(null);
  const [overflow, setOverflow] = useState(false);
  const syncing = useRef(false);

  // antd 가 만든 가로 스크롤 요소(.ant-table-content)를 찾아 폭을 측정하고,
  // 그 scrollLeft 변화를 커스텀 바에 반영한다.
  useLayoutEffect(() => {
    const wrap = wrapRef.current;
    const barInner = barInnerRef.current;
    if (!wrap || !barInner) return;
    // antd 5(rc-table)는 scroll.x 만 있으면 .ant-table-content, fixed header/column 이
    // 있으면 .ant-table-body 에 가로 스크롤을 만든다. 둘 다 대응한다.
    const scrollEl =
      wrap.querySelector<HTMLElement>('.ant-table-content') ??
      wrap.querySelector<HTMLElement>('.ant-table-body');
    scrollElRef.current = scrollEl;
    if (!scrollEl) return;

    const measure = () => {
      const sw = scrollEl.scrollWidth;
      const cw = scrollEl.clientWidth;
      barInner.style.width = `${sw}px`;
      setOverflow(sw > cw + 1);
    };
    measure();

    // 테이블이 가로 스크롤되면 커스텀 바도 같은 위치로 이동.
    const onScrollEl = () => {
      if (syncing.current) { syncing.current = false; return; }
      if (barRef.current) {
        syncing.current = true;
        barRef.current.scrollLeft = scrollEl.scrollLeft;
      }
    };
    scrollEl.addEventListener('scroll', onScrollEl, { passive: true });

    const ro = new ResizeObserver(measure);
    ro.observe(scrollEl);
    const innerTable = scrollEl.querySelector('table');
    if (innerTable) ro.observe(innerTable);

    return () => {
      scrollEl.removeEventListener('scroll', onScrollEl);
      ro.disconnect();
    };
  });

  // 커스텀 바를 움직이면 테이블을 같은 위치로 이동.
  const onBarScroll = () => {
    if (syncing.current) { syncing.current = false; return; }
    if (scrollElRef.current && barRef.current) {
      syncing.current = true;
      scrollElRef.current.scrollLeft = barRef.current.scrollLeft;
    }
  };

  return (
    <div ref={wrapRef} className="deployment-scroll-content">
      {children}
      <div
        ref={barRef}
        className="deployment-scrollbar"
        onScroll={onBarScroll}
        style={{ display: overflow ? 'block' : 'none' }}
      >
        <div ref={barInnerRef} style={{ height: 1 }} />
      </div>
    </div>
  );
}

function getDefaultYearMonth(): { year: number; month: number } {
  const now = new Date();
  const cm = now.getMonth() + 1;
  if (cm === 1) return { year: now.getFullYear() - 1, month: 12 };
  return { year: now.getFullYear(), month: cm - 1 };
}

interface QueryParams {
  mode: SearchMode;
  year: number;
  month: number;
  codes: string[];
  suitabilities: string[];
  categories: string[];
  wc3: string[];
  wc1: string[];
  wc5: string[];
}

export default function DeploymentPage() {
  const defaults = getDefaultYearMonth();
  const [mode, setMode] = useState<SearchMode>('summary');
  const [year, setYear] = useState(defaults.year);
  const [month, setMonth] = useState(defaults.month);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [selectedSuitabilities, setSelectedSuitabilities] = useState<string[]>([]);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [selectedWc3, setSelectedWc3] = useState<string[]>([]);
  // 근무형태1/5 — 근무형태3 과 동일한 다중 선택. 빈 배열 = 전체(무필터).
  const [wc1, setWc1] = useState<string[]>([]);
  const [wc5, setWc5] = useState<string[]>([]);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [clickedAccountIds, setClickedAccountIds] = useState<number[]>([]);
  const [clickedAccountForDetail, setClickedAccountForDetail] = useState<number | null>(null);

  const { data: branches = [] } = useDashboardBranches();

  // 접근 가능한 지점이 1개뿐이면 항상 그 지점으로 고정 — 선택 변경 불가.
  const singleBranch = branches.length === 1;

  // 지점이 1개일 때는 해당 지점을 자동 선택해 사용자가 따로 고를 필요 없도록 한다.
  useEffect(() => {
    if (singleBranch) {
      const onlyCode = branches[0].branchCode;
      setSelectedCodes((prev) => (prev.length === 1 && prev[0] === onlyCode ? prev : [onlyCode]));
    }
  }, [singleBranch, branches]);

  // 거래처유형 필터는 결과 테이블 컬럼과 동일한 SF 고정 10개(AccountCategoryColumn) 로 노출 — 좌측 필터 ↔ 컬럼 헤더 일치.
  const categoryLabels = useMemo(() => [...SUMMARY_CATEGORY_COLUMNS], []);

  // queryParams 의 검색조건 → 서버 SummaryFilter (거래처유형 컬럼 → SF accountCode 집합 변환). SF cls:567-569 정합.
  // '기타' 는 단일 코드가 없어 SF others 13개 코드 전체를 전송 (CATEGORY_COLUMN_CODES).
  const buildSummaryFilter = (p: QueryParams): SummaryFilter => ({
    suitabilities: p.suitabilities,
    categoryCodes: p.categories.flatMap((col) => CATEGORY_COLUMN_CODES[col] ?? []),
    workingCategory3: p.wc3,
  });

  const summaryQuery = useQuery({
    queryKey: ['salesComparison', 'summary', queryParams],
    queryFn: () =>
      fetchSummary(queryParams!.year, queryParams!.month, queryParams!.codes, buildSummaryFilter(queryParams!)),
    enabled: queryParams != null && queryParams.mode === 'summary',
  });

  const middleQuery = useQuery({
    queryKey: ['salesComparison', 'middle', queryParams, clickedAccountIds],
    queryFn: () =>
      fetchMiddle(queryParams!.year, queryParams!.month, queryParams!.codes, clickedAccountIds),
    enabled: queryParams != null && queryParams.mode === 'summary' && clickedAccountIds.length > 0,
  });

  const detailQuery = useQuery({
    queryKey: [
      'salesComparison',
      'detail',
      queryParams,
      clickedAccountForDetail,
      mode === 'detail' ? wc1 : null,
      mode === 'detail' ? wc5 : null,
    ],
    queryFn: () => {
      const p = queryParams!;
      const accIds = p.mode === 'summary' && clickedAccountForDetail != null ? [clickedAccountForDetail] : [];
      const filter1 = p.mode === 'detail' && p.wc1.length > 0 ? p.wc1 : undefined;
      const filter5 = p.mode === 'detail' && p.wc5.length > 0 ? p.wc5 : undefined;
      return fetchDetail(p.year, p.month, p.codes, accIds, filter1, filter5);
    },
    enabled:
      queryParams != null &&
      ((queryParams.mode === 'detail') ||
        (queryParams.mode === 'summary' && clickedAccountForDetail != null)),
  });

  const handleSearch = () => {
    if (selectedCodes.length === 0 || selectedWc3.length === 0) {
      message.warning('검색조건을 모두 설정해주세요.');
      return;
    }
    if (mode === 'summary' && (selectedSuitabilities.length === 0 || selectedCategories.length === 0)) {
      message.warning('검색조건을 모두 설정해주세요.');
      return;
    }
    setQueryParams({
      mode,
      year,
      month,
      codes: selectedCodes,
      suitabilities: selectedSuitabilities,
      categories: selectedCategories,
      wc3: selectedWc3,
      wc1,
      wc5,
    });
    setClickedAccountIds([]);
    setClickedAccountForDetail(null);
  };

  const handleResetCondition = () => {
    const dft = getDefaultYearMonth();
    setYear(dft.year);
    setMonth(dft.month);
    // 지점이 1개뿐이면 항상 그 지점으로 고정 — 초기화해도 선택을 유지한다.
    setSelectedCodes(singleBranch ? [branches[0].branchCode] : []);
    setSelectedSuitabilities([]);
    setSelectedCategories([]);
    setSelectedWc3([]);
    setWc1([]);
    setWc5([]);
  };

  const handleSelectAllConditions = () => {
    setSelectedCodes(branches.map((b) => b.branchCode));
    setSelectedSuitabilities([...SUITABILITY_OPTIONS]);
    setSelectedCategories([...categoryLabels]);
    setSelectedWc3([...WORKING_CATEGORY_3_OPTIONS]);
    if (mode === 'detail') {
      setWc1([...WORKING_CATEGORY_1_OPTIONS]);
      setWc5([...WORKING_CATEGORY_5_OPTIONS]);
    }
  };

  const handleModeChange = (newMode: SearchMode) => {
    setMode(newMode);
    setSelectedSuitabilities([]);
    setSelectedCategories([]);
    setWc1([]);
    setWc5([]);
    setQueryParams(null);
    setClickedAccountIds([]);
    setClickedAccountForDetail(null);
  };

  const summaryRows: (SalesComparisonSummaryRow & { isTotal?: boolean })[] = useMemo(() => {
    if (!summaryQuery.data) return [];
    return [
      ...summaryQuery.data.rows,
      { ...summaryQuery.data.total, suitability: '총계', isTotal: true },
    ];
  }, [summaryQuery.data]);

  const handleSummaryCellClick = (row: SalesComparisonSummaryRow & { isTotal?: boolean }, key: string) => {
    const ids = key === 'totalCount'
      ? [...new Set(Object.values(row.accountIdsByCategory).flat())]
      : (row.accountIdsByCategory[key] ?? []);
    if (ids.length === 0) {
      message.info('해당 셀에 거래처가 없습니다.');
      return;
    }
    setClickedAccountIds(ids);
    setClickedAccountForDetail(null);
  };

  const handleMiddleAccountClick = (item: SalesComparisonMiddleItem) => {
    setClickedAccountForDetail(item.accountId);
  };

  const summaryColumns: ColumnsType<SalesComparisonSummaryRow & { isTotal?: boolean }> = useMemo(() => {
    const cols: ColumnsType<SalesComparisonSummaryRow & { isTotal?: boolean }> = [
      {
        title: '구분',
        dataIndex: 'suitability',
        width: 72,
        onCell: (record) => ({ style: suitabilityCellStyle(record.suitability) }),
      },
      {
        title: '전체',
        dataIndex: 'totalCount',
        width: 64,
        align: 'right',
        render: (v: number, row) => (
          <a onClick={() => v > 0 && handleSummaryCellClick(row, 'totalCount')}>{formatNumber(v)}</a>
        ),
        onCell: (record) => ({
          style: (record as { isTotal?: boolean }).isTotal ? suitabilityCellStyle('총계') : {},
        }),
      },
      // 집계표 카테고리 컬럼 — SF 고정 10컬럼 (backend AccountCategoryColumn). 좌측 거래처유형 필터와 동일 목록.
      ...SUMMARY_CATEGORY_COLUMNS.map((label) => ({
        title: categoryColumnLabel(label),
        dataIndex: ['countsByCategory', label],
        width: 72,
        align: 'right' as const,
        render: (_: unknown, row: SalesComparisonSummaryRow & { isTotal?: boolean }) => {
          const value = row.countsByCategory[label] ?? 0;
          if (value === 0) return '';
          return (
            <a onClick={() => handleSummaryCellClick(row, label)}>{formatNumber(value)}</a>
          );
        },
        onCell: (record: SalesComparisonSummaryRow & { isTotal?: boolean }) => ({
          style: record.isTotal ? suitabilityCellStyle('총계') : {},
        }),
      })),
    ];
    return cols;
  }, []);

  const middleColumns: ColumnsType<SalesComparisonMiddleItem> = [
    { title: '거래처지점명', dataIndex: 'accountBranchName', width: 84, render: (v) => v ?? '-' },
    {
      title: '배치적합성',
      dataIndex: 'suitability',
      width: 72,
      onCell: (r) => ({ style: suitabilityCellStyle(r.suitability) }),
    },
    { title: '월평균매출', dataIndex: 'avgClosingAmount', width: 92, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열인원', dataIndex: 'totalDisplayHeadcount', width: 72, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열환산인원', dataIndex: 'totalDisplayConvertedHeadcount', width: 84, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '총 행사환산인원', dataIndex: 'totalEventConvertedHeadcount', width: 84, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '거래처유형', dataIndex: 'accountCategory', width: 76 },
    {
      title: '거래처명',
      dataIndex: 'accountName',
      width: 120,
      render: (v: string, row) => (
        <a onClick={() => handleMiddleAccountClick(row)}>{v}</a>
      ),
    },
    { title: '거래처코드', dataIndex: 'accountCode', width: 84, align: 'right' },
    { title: '고정배치기준', dataIndex: 'fixedStandardAmount', width: 92, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '격고배치기준', dataIndex: 'bifurcationHalfStandardAmount', width: 92, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '총 투입횟수', dataIndex: 'totalInputCount', width: 72, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 환산일수', dataIndex: 'totalEquivalentWorkingDays', width: 72, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '당월매출', dataIndex: 'thisMonthSalesAmount', width: 92, align: 'right', render: (v: number) => formatNumber(v) },
    { title: 'EDI/POS', dataIndex: 'ediPos', width: 72, render: (v) => v ?? '-' },
  ];

  const detailColumns: ColumnsType<SalesComparisonDetailItem> = [
    { title: '거래처지점명', dataIndex: 'accountBranchName', width: 84, render: (v) => v ?? '-' },
    {
      title: '배치적합성',
      dataIndex: 'suitability',
      width: 72,
      onCell: (r) => ({ style: suitabilityCellStyle(r.suitability) }),
    },
    { title: '월평균매출', dataIndex: 'avgClosingAmount', width: 92, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열인원', dataIndex: 'totalDisplayHeadcount', width: 72, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열환산인원', dataIndex: 'totalDisplayConvertedHeadcount', width: 84, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '총 행사환산인원', dataIndex: 'totalEventConvertedHeadcount', width: 84, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '거래처유형', dataIndex: 'accountCategory', width: 76 },
    { title: '거래처유형코드', dataIndex: 'accountCategoryCode', width: 80, align: 'right', render: (v) => v ?? '-' },
    { title: '거래처명', dataIndex: 'accountName', width: 120 },
    { title: '거래처코드', dataIndex: 'accountCode', width: 84, align: 'right' },
    { title: '사원명', dataIndex: 'employeeName', width: 72 },
    { title: '사번', dataIndex: 'employeeCode', width: 72, align: 'right' },
    { title: '직위', dataIndex: 'title', width: 60, render: (v) => v ?? '-' },
    { title: '근무형태1', dataIndex: 'workingCategory1', width: 72 },
    { title: '근무형태3', dataIndex: 'workingCategory3', width: 72, render: (v) => v ?? '-' },
    { title: '근무형태4', dataIndex: 'workingCategory4', width: 72, render: (v) => v ?? '-' },
    { title: '근무형태5', dataIndex: 'workingCategory5', width: 72, render: (v) => v ?? '-' },
    { title: '고정배치기준', dataIndex: 'fixedStandardAmount', width: 92, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '격고배치기준', dataIndex: 'bifurcationHalfStandardAmount', width: 92, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '투입횟수', dataIndex: 'inputCount', width: 72, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '환산일수', dataIndex: 'equivalentWorkingDays', width: 72, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '환산인원', dataIndex: 'convertedHeadcount', width: 72, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '당월매출', dataIndex: 'thisMonthSalesAmount', width: 92, align: 'right', render: (v: number) => formatNumber(v) },
    { title: 'EDI/POS', dataIndex: 'ediPos', width: 72, render: (v) => v ?? '-' },
  ];

  const handleExportSummary = async () => {
    if (!queryParams) return;
    try {
      await apiExportSummary(queryParams.year, queryParams.month, queryParams.codes, buildSummaryFilter(queryParams));
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const handleExportMiddle = async () => {
    if (!queryParams) return;
    try {
      await apiExportMiddle(queryParams.year, queryParams.month, queryParams.codes, clickedAccountIds);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  const handleExportDetail = async () => {
    if (!queryParams) return;
    try {
      const isDrilldown = queryParams.mode === 'summary' && clickedAccountForDetail != null;
      const accIds = isDrilldown ? [clickedAccountForDetail as number] : [];
      const filter1 = queryParams.mode === 'detail' && queryParams.wc1.length > 0 ? queryParams.wc1 : undefined;
      const filter5 = queryParams.mode === 'detail' && queryParams.wc5.length > 0 ? queryParams.wc5 : undefined;
      await apiExportDetail(queryParams.year, queryParams.month, queryParams.codes, accIds, filter1, filter5);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  return (
    <div style={{ display: 'flex', height: '100%', minHeight: 600 }}>
      <div
        style={{
          width: 160,
          padding: 16,
          borderRight: '1px solid #f0f0f0',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'stretch',
          textAlign: 'center',
        }}
      >
        <Title level={5}>검색 조건</Title>

        <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
          <Button type="primary" icon={<SearchOutlined />} block onClick={handleSearch}>
            조회
          </Button>
          <Space>
            <Tooltip title="모든조건선택" placement="bottom">
              <Button size="small" icon={<CheckSquareOutlined />} onClick={handleSelectAllConditions} />
            </Tooltip>
            <Tooltip title="조건초기화" placement="bottom">
              <Button size="small" icon={<ClearOutlined />} onClick={handleResetCondition} />
            </Tooltip>
          </Space>
        </Space>

        <div style={{ marginBottom: 12 }}>
          <Radio.Group
            value={mode}
            onChange={(e) => handleModeChange(e.target.value)}
            style={{ display: 'block' }}
          >
            <Radio value="summary">집계</Radio>
            <Radio value="detail">상세</Radio>
          </Radio.Group>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <InputNumber
              value={year}
              min={2020}
              max={2099}
              onChange={(v) => v != null && setYear(v)}
              suffix="년"
              style={{ width: '100%' }}
            />
            <InputNumber
              value={month}
              min={1}
              max={12}
              onChange={(v) => v != null && setMonth(v)}
              suffix="월"
              style={{ width: '100%' }}
            />
          </Space>
        </div>

        <div style={{ marginBottom: 12 }}>
          <FilterLabel
            label="지점"
            required
            allValues={branches.map((b) => b.branchCode)}
            selected={selectedCodes}
            onChange={setSelectedCodes}
            disabled={singleBranch}
          />
          <div
            className="deployment-filter-list deployment-filter-list--scroll"
            style={{ marginTop: 4, maxHeight: 160 }}
          >
            <Checkbox.Group
              value={selectedCodes}
              onChange={(vals) => setSelectedCodes(vals as string[])}
              options={branches.map((b) => ({
                label: b.branchName,
                value: b.branchCode,
                disabled: singleBranch,
              }))}
            />
          </div>
        </div>

        {mode === 'summary' && (
          <>
            <div style={{ marginBottom: 12 }}>
              <FilterLabel
                label="배치적합성"
                required
                allValues={SUITABILITY_OPTIONS}
                selected={selectedSuitabilities}
                onChange={setSelectedSuitabilities}
              />
              <div className="deployment-filter-list" style={{ marginTop: 4 }}>
                <Checkbox.Group
                  value={selectedSuitabilities}
                  onChange={(vals) => setSelectedSuitabilities(vals as string[])}
                  options={SUITABILITY_OPTIONS}
                />
              </div>
            </div>
            <div style={{ marginBottom: 12 }}>
              <FilterLabel
                label="거래처유형"
                required
                allValues={categoryLabels}
                selected={selectedCategories}
                onChange={setSelectedCategories}
              />
              <div
                className="deployment-filter-list deployment-filter-list--scroll"
                style={{ marginTop: 4, maxHeight: 140 }}
              >
                <Checkbox.Group
                  value={selectedCategories}
                  onChange={(vals) => setSelectedCategories(vals as string[])}
                  options={categoryLabels.map((col) => ({ label: categoryColumnLabel(col), value: col }))}
                />
              </div>
            </div>
          </>
        )}

        {mode === 'detail' && (
          <>
            <div style={{ marginBottom: 12 }}>
              <FilterLabel
                label="근무형태1"
                allValues={WORKING_CATEGORY_1_OPTIONS}
                selected={wc1}
                onChange={setWc1}
              />
              <div className="deployment-filter-list" style={{ marginTop: 4 }}>
                <Checkbox.Group
                  value={wc1}
                  onChange={(vals) => setWc1(vals as string[])}
                  options={WORKING_CATEGORY_1_OPTIONS}
                />
              </div>
            </div>
            <div style={{ marginBottom: 12 }}>
              <FilterLabel
                label="근무형태5"
                allValues={WORKING_CATEGORY_5_OPTIONS}
                selected={wc5}
                onChange={setWc5}
              />
              <div className="deployment-filter-list" style={{ marginTop: 4 }}>
                <Checkbox.Group
                  value={wc5}
                  onChange={(vals) => setWc5(vals as string[])}
                  options={WORKING_CATEGORY_5_OPTIONS}
                />
              </div>
            </div>
          </>
        )}

        <div style={{ marginBottom: 12 }}>
          <FilterLabel
            label="근무형태3"
            required
            allValues={WORKING_CATEGORY_3_OPTIONS}
            selected={selectedWc3}
            onChange={setSelectedWc3}
          />
          <div className="deployment-filter-list" style={{ marginTop: 4 }}>
            <Checkbox.Group
              value={selectedWc3}
              onChange={(vals) => setSelectedWc3(vals as string[])}
              options={WORKING_CATEGORY_3_OPTIONS}
            />
          </div>
        </div>
      </div>

      <div className="deployment-result" style={{ flex: 1, padding: 16, overflowX: 'hidden', minWidth: 0 }}>
        <Title level={4}>거래처별 진열사원 배치적합성 현황 (월 평균매출 대비)</Title>

        {queryParams == null && <Empty description="좌측에서 조건을 선택한 뒤 조회 버튼을 눌러주세요." />}

        {queryParams?.mode === 'summary' && (
          <>
            <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary">(단위: 개)</Text>
              <Button icon={<DownloadOutlined />} onClick={handleExportSummary} disabled={!summaryQuery.data}>
                엑셀다운로드
              </Button>
            </div>
            {summaryQuery.isError && <Alert type="error" message={(summaryQuery.error as Error)?.message ?? '조회 실패'} />}
            <ResizableTable
              rowKey="suitability"
              size="small"
              columns={summaryColumns}
              dataSource={summaryRows}
              loading={summaryQuery.isLoading}
              pagination={false}
              locale={listTableLocale()}
            />

            {clickedAccountIds.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text type="secondary">(단위: 원, 명)</Text>
                  <Button icon={<DownloadOutlined />} onClick={handleExportMiddle} disabled={!middleQuery.data}>
                    엑셀다운로드
                  </Button>
                </div>
                {middleQuery.isError && <Alert type="error" message={(middleQuery.error as Error)?.message ?? '조회 실패'} />}
                <ScrollXContainer>
                  <ResizableTable
                    rowKey="accountId"
                    size="small"
                    columns={middleColumns}
                    dataSource={middleQuery.data?.items ?? []}
                    loading={middleQuery.isLoading}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                    locale={listTableLocale()}
                    summary={() =>
                      middleQuery.data ? (
                        <ResizableTable.Summary>
                          {middleQuery.data.subtotals.map((sub) => (
                            <ResizableTable.Summary.Row key={`sub-${sub.suitability}`}>
                              <ResizableTable.Summary.Cell index={0} colSpan={6}>
                                <span style={{ ...suitabilityCellStyle('소계'), padding: '4px 8px' }}>{`${sub.suitability} 소계`}</span>
                              </ResizableTable.Summary.Cell>
                              <ResizableTable.Summary.Cell index={6} colSpan={9} align="right">
                                {`거래처 ${sub.accountCount}개`}
                              </ResizableTable.Summary.Cell>
                            </ResizableTable.Summary.Row>
                          ))}
                          <ResizableTable.Summary.Row>
                            <ResizableTable.Summary.Cell index={0} colSpan={6}>
                              <span style={{ ...suitabilityCellStyle('총계'), padding: '4px 8px' }}>총계</span>
                            </ResizableTable.Summary.Cell>
                            <ResizableTable.Summary.Cell index={6} colSpan={9} align="right">
                              {`거래처 ${middleQuery.data.total.accountCount}개`}
                            </ResizableTable.Summary.Cell>
                          </ResizableTable.Summary.Row>
                        </ResizableTable.Summary>
                      ) : null
                    }
                  />
                </ScrollXContainer>
              </div>
            )}

            {clickedAccountForDetail != null && (
              <div style={{ marginTop: 16 }}>
                <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text type="secondary">(단위: 원, 명) — 사원별 상세</Text>
                  <Button icon={<DownloadOutlined />} onClick={handleExportDetail} disabled={!detailQuery.data}>
                    엑셀다운로드
                  </Button>
                </div>
                {detailQuery.isError && <Alert type="error" message={(detailQuery.error as Error)?.message ?? '조회 실패'} />}
                <ScrollXContainer>
                  <ResizableTable
                    rowKey={(r, i) => `${r.accountId}-${r.employeeCode}-${i}`}
                    size="small"
                    columns={detailColumns}
                    dataSource={detailQuery.data?.items ?? []}
                    loading={detailQuery.isLoading}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                    locale={listTableLocale()}
                  />
                </ScrollXContainer>
              </div>
            )}
          </>
        )}

        {queryParams?.mode === 'detail' && (
          <>
            <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary">(단위: 원, 명) — 사원별 상세</Text>
              <Button icon={<DownloadOutlined />} onClick={handleExportDetail} disabled={!detailQuery.data}>
                엑셀다운로드
              </Button>
            </div>
            {detailQuery.isError && <Alert type="error" message={(detailQuery.error as Error)?.message ?? '조회 실패'} />}
            <ScrollXContainer>
              <ResizableTable
                rowKey={(r, i) => `${r.accountId}-${r.employeeCode}-${i}`}
                size="small"
                columns={detailColumns}
                dataSource={detailQuery.data?.items ?? []}
                loading={detailQuery.isLoading}
                pagination={false}
                scroll={{ x: 'max-content' }}
                locale={listTableLocale()}
              />
            </ScrollXContainer>
          </>
        )}
      </div>
    </div>
  );
}
