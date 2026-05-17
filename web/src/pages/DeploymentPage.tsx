import { useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Checkbox,
  Empty,
  InputNumber,
  Radio,
  Space,
  Spin,
  Table,
  Typography,
  message,
} from 'antd';
import { DownloadOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import {
  fetchSummary,
  fetchMiddle,
  fetchDetail,
  exportSummary as apiExportSummary,
  exportMiddle as apiExportMiddle,
  exportDetail as apiExportDetail,
  type SalesComparisonSummaryRow,
  type SalesComparisonMiddleItem,
  type SalesComparisonDetailItem,
} from '@/api/salesComparison';
import { useTeamScheduleBranches } from '@/hooks/team-schedule/useTeamScheduleBranches';

const { Title, Text } = Typography;

type SearchMode = 'summary' | 'detail';

const CATEGORY_COLUMNS = [
  '대형마트',
  '농협',
  '체인',
  '슈퍼',
  '대리점',
  '백화점',
  '홀세일',
  '군납',
  '식자재',
  '기타',
];

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
  wc1: string;
  wc5: string;
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
  const [wc1, setWc1] = useState<string>('모두');
  const [wc5, setWc5] = useState<string>('모두');
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [clickedAccountIds, setClickedAccountIds] = useState<number[]>([]);
  const [clickedAccountForDetail, setClickedAccountForDetail] = useState<number | null>(null);

  const { data: branches = [] } = useTeamScheduleBranches();

  const summaryQuery = useQuery({
    queryKey: ['salesComparison', 'summary', queryParams],
    queryFn: () => fetchSummary(queryParams!.year, queryParams!.month, queryParams!.codes),
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
      const filter1 = p.mode === 'detail' && p.wc1 !== '모두' ? p.wc1 : undefined;
      const filter5 = p.mode === 'detail' && p.wc5 !== '모두' ? p.wc5 : undefined;
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
    setSelectedCodes([]);
    setSelectedSuitabilities([]);
    setSelectedCategories([]);
    setSelectedWc3([]);
    if (mode === 'detail') {
      setWc1('모두');
      setWc5('모두');
    } else {
      setWc1('모두');
      setWc5('모두');
    }
  };

  const handleSelectAllConditions = () => {
    setSelectedCodes(branches.map((b) => b.branchCode));
    setSelectedSuitabilities([...SUITABILITY_OPTIONS]);
    setSelectedCategories([...CATEGORY_COLUMNS]);
    setSelectedWc3([...WORKING_CATEGORY_3_OPTIONS]);
    if (mode === 'detail') {
      setWc1('모두');
      setWc5('모두');
    }
  };

  const handleModeChange = (newMode: SearchMode) => {
    setMode(newMode);
    setSelectedSuitabilities([]);
    setSelectedCategories([]);
    if (newMode === 'detail') {
      setWc1('모두');
      setWc5('모두');
    } else {
      setWc1('모두');
      setWc5('모두');
    }
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
    if (row.isTotal && key === 'totalCount') return;
    const ids = key === 'totalCount'
      ? Object.values(row.accountIdsByCategory).flat()
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
        width: 100,
        fixed: 'left',
        onCell: (record) => ({ style: suitabilityCellStyle(record.suitability) }),
      },
      {
        title: '전체',
        dataIndex: 'totalCount',
        width: 100,
        align: 'right',
        render: (v: number, row) => (
          <a onClick={() => v > 0 && handleSummaryCellClick(row, 'totalCount')}>{formatNumber(v)}</a>
        ),
        onCell: (record) => ({
          style: (record as { isTotal?: boolean }).isTotal ? suitabilityCellStyle('총계') : {},
        }),
      },
      ...CATEGORY_COLUMNS.map((label) => ({
        title: label,
        dataIndex: ['countsByCategory', label],
        width: 90,
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
    { title: '거래처지점명', dataIndex: 'accountBranchName', width: 110, render: (v) => v ?? '-' },
    {
      title: '배치적합성',
      dataIndex: 'suitability',
      width: 100,
      onCell: (r) => ({ style: suitabilityCellStyle(r.suitability) }),
    },
    { title: '월평균매출', dataIndex: 'avgClosingAmount', width: 110, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열인원', dataIndex: 'totalDisplayHeadcount', width: 100, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열환산인원', dataIndex: 'totalDisplayConvertedHeadcount', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '총 행사환산인원', dataIndex: 'totalEventConvertedHeadcount', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '거래처유형', dataIndex: 'accountCategory', width: 100 },
    {
      title: '거래처명',
      dataIndex: 'accountName',
      width: 140,
      render: (v: string, row) => (
        <a onClick={() => handleMiddleAccountClick(row)}>{v}</a>
      ),
    },
    { title: '거래처코드', dataIndex: 'accountCode', width: 110 },
    { title: '고정배치기준', dataIndex: 'fixedStandardAmount', width: 110, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '격고배치기준', dataIndex: 'bifurcationHalfStandardAmount', width: 110, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '총 투입횟수', dataIndex: 'totalInputCount', width: 100, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 환산일수', dataIndex: 'totalEquivalentWorkingDays', width: 100, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '당월매출', dataIndex: 'thisMonthSalesAmount', width: 110, align: 'right', render: (v: number) => formatNumber(v) },
    { title: 'EDI/POS', dataIndex: 'ediPos', width: 80, render: (v) => v ?? '-' },
  ];

  const detailColumns: ColumnsType<SalesComparisonDetailItem> = [
    { title: '거래처지점명', dataIndex: 'accountBranchName', width: 110, render: (v) => v ?? '-' },
    {
      title: '배치적합성',
      dataIndex: 'suitability',
      width: 100,
      onCell: (r) => ({ style: suitabilityCellStyle(r.suitability) }),
    },
    { title: '월평균매출', dataIndex: 'avgClosingAmount', width: 110, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열인원', dataIndex: 'totalDisplayHeadcount', width: 100, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '총 진열환산인원', dataIndex: 'totalDisplayConvertedHeadcount', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '총 행사환산인원', dataIndex: 'totalEventConvertedHeadcount', width: 110, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '거래처유형', dataIndex: 'accountCategory', width: 100 },
    { title: '거래처유형코드', dataIndex: 'accountCategoryCode', width: 100, render: (v) => v ?? '-' },
    { title: '거래처명', dataIndex: 'accountName', width: 140 },
    { title: '거래처코드', dataIndex: 'accountCode', width: 110 },
    { title: '사원명', dataIndex: 'employeeName', width: 90 },
    { title: '사번', dataIndex: 'employeeCode', width: 90 },
    { title: '직위', dataIndex: 'title', width: 70, render: (v) => v ?? '-' },
    { title: '근무형태1', dataIndex: 'workingCategory1', width: 90 },
    { title: '근무형태3', dataIndex: 'workingCategory3', width: 90, render: (v) => v ?? '-' },
    { title: '근무형태4', dataIndex: 'workingCategory4', width: 90, render: (v) => v ?? '-' },
    { title: '근무형태5', dataIndex: 'workingCategory5', width: 90, render: (v) => v ?? '-' },
    { title: '고정배치기준', dataIndex: 'fixedStandardAmount', width: 110, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '격고배치기준', dataIndex: 'bifurcationHalfStandardAmount', width: 110, align: 'right', render: (v: number | null) => formatNumber(v ?? 0) },
    { title: '투입횟수', dataIndex: 'inputCount', width: 90, align: 'right', render: (v: number) => formatNumber(v) },
    { title: '환산일수', dataIndex: 'equivalentWorkingDays', width: 90, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '환산인원', dataIndex: 'convertedHeadcount', width: 90, align: 'right', render: (v: number) => formatDecimal3(v) },
    { title: '당월매출', dataIndex: 'thisMonthSalesAmount', width: 110, align: 'right', render: (v: number) => formatNumber(v) },
    { title: 'EDI/POS', dataIndex: 'ediPos', width: 80, render: (v) => v ?? '-' },
  ];

  const handleExportSummary = async () => {
    if (!queryParams) return;
    try {
      await apiExportSummary(queryParams.year, queryParams.month, queryParams.codes);
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
      const filter1 = queryParams.mode === 'detail' && queryParams.wc1 !== '모두' ? queryParams.wc1 : undefined;
      const filter5 = queryParams.mode === 'detail' && queryParams.wc5 !== '모두' ? queryParams.wc5 : undefined;
      await apiExportDetail(queryParams.year, queryParams.month, queryParams.codes, accIds, filter1, filter5);
    } catch (e) {
      message.error(e instanceof Error ? e.message : '엑셀 다운로드 실패');
    }
  };

  return (
    <div style={{ display: 'flex', height: '100%', minHeight: 600 }}>
      <div style={{ width: 280, padding: 16, borderRight: '1px solid #f0f0f0', overflowY: 'auto' }}>
        <Title level={5}>검색 조건</Title>

        <div style={{ marginBottom: 12 }}>
          <Text strong>검색유형</Text>
          <Radio.Group
            value={mode}
            onChange={(e) => handleModeChange(e.target.value)}
            style={{ marginTop: 4, display: 'block' }}
          >
            <Radio value="summary">집계</Radio>
            <Radio value="detail">상세</Radio>
          </Radio.Group>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text strong>년도 / 월</Text>
          <Space style={{ display: 'flex', marginTop: 4 }}>
            <InputNumber value={year} min={2020} max={2099} onChange={(v) => v != null && setYear(v)} style={{ width: 100 }} />
            <InputNumber value={month} min={1} max={12} onChange={(v) => v != null && setMonth(v)} style={{ width: 70 }} />
          </Space>
        </div>

        <div style={{ marginBottom: 12 }}>
          <Text strong>지점</Text>
          <div style={{ marginTop: 4, maxHeight: 160, overflowY: 'auto', border: '1px solid #f0f0f0', padding: 4 }}>
            <Checkbox.Group
              value={selectedCodes}
              onChange={(vals) => setSelectedCodes(vals as string[])}
              style={{ display: 'flex', flexDirection: 'column' }}
            >
              {branches.map((b) => (
                <Checkbox key={b.branchCode} value={b.branchCode}>
                  {b.branchName}
                </Checkbox>
              ))}
            </Checkbox.Group>
          </div>
        </div>

        {mode === 'summary' && (
          <>
            <div style={{ marginBottom: 12 }}>
              <Text strong>배치적합성</Text>
              <Checkbox.Group
                value={selectedSuitabilities}
                onChange={(vals) => setSelectedSuitabilities(vals as string[])}
                options={SUITABILITY_OPTIONS}
                style={{ display: 'flex', flexDirection: 'column', marginTop: 4 }}
              />
            </div>
            <div style={{ marginBottom: 12 }}>
              <Text strong>거래처유형</Text>
              <div style={{ marginTop: 4, maxHeight: 140, overflowY: 'auto', border: '1px solid #f0f0f0', padding: 4 }}>
                <Checkbox.Group
                  value={selectedCategories}
                  onChange={(vals) => setSelectedCategories(vals as string[])}
                  options={CATEGORY_COLUMNS}
                  style={{ display: 'flex', flexDirection: 'column' }}
                />
              </div>
            </div>
          </>
        )}

        {mode === 'detail' && (
          <>
            <div style={{ marginBottom: 12 }}>
              <Text strong>근무형태1</Text>
              <Radio.Group value={wc1} onChange={(e) => setWc1(e.target.value)} style={{ marginTop: 4, display: 'block' }}>
                <Radio value="모두">모두</Radio>
                {WORKING_CATEGORY_1_OPTIONS.map((v) => (
                  <Radio key={v} value={v}>
                    {v}
                  </Radio>
                ))}
              </Radio.Group>
            </div>
            <div style={{ marginBottom: 12 }}>
              <Text strong>근무형태5</Text>
              <Radio.Group value={wc5} onChange={(e) => setWc5(e.target.value)} style={{ marginTop: 4, display: 'block' }}>
                <Radio value="모두">모두</Radio>
                {WORKING_CATEGORY_5_OPTIONS.map((v) => (
                  <Radio key={v} value={v}>
                    {v}
                  </Radio>
                ))}
              </Radio.Group>
            </div>
          </>
        )}

        <div style={{ marginBottom: 12 }}>
          <Text strong>근무형태3</Text>
          <Checkbox.Group
            value={selectedWc3}
            onChange={(vals) => setSelectedWc3(vals as string[])}
            options={WORKING_CATEGORY_3_OPTIONS}
            style={{ display: 'flex', flexDirection: 'column', marginTop: 4 }}
          />
        </div>

        <Space direction="vertical" style={{ width: '100%' }}>
          <Button type="primary" icon={<SearchOutlined />} block onClick={handleSearch}>
            조회하기
          </Button>
          <Space style={{ width: '100%', justifyContent: 'space-between' }}>
            <Button size="small" onClick={handleSelectAllConditions}>
              모든조건선택
            </Button>
            <Button size="small" onClick={handleResetCondition}>
              조건초기화
            </Button>
          </Space>
        </Space>
      </div>

      <div style={{ flex: 1, padding: 16, overflowX: 'auto', minWidth: 0 }}>
        <Title level={4}>거래처별 진열사원 배치적합성 현황 (월 평균매출 대비)</Title>

        {queryParams == null && <Empty description="좌측에서 조건을 선택한 뒤 조회하기를 눌러주세요." />}

        {queryParams?.mode === 'summary' && (
          <>
            <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Text type="secondary">(단위: 개)</Text>
              <Button icon={<DownloadOutlined />} onClick={handleExportSummary} disabled={!summaryQuery.data}>
                엑셀다운로드
              </Button>
            </div>
            {summaryQuery.isLoading && <Spin />}
            {summaryQuery.isError && <Alert type="error" message={(summaryQuery.error as Error)?.message ?? '조회 실패'} />}
            {summaryQuery.data && (
              <Table
                rowKey="suitability"
                size="small"
                columns={summaryColumns}
                dataSource={summaryRows}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            )}

            {clickedAccountIds.length > 0 && (
              <div style={{ marginTop: 16 }}>
                <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text type="secondary">(단위: 원, 명)</Text>
                  <Button icon={<DownloadOutlined />} onClick={handleExportMiddle} disabled={!middleQuery.data}>
                    엑셀다운로드
                  </Button>
                </div>
                {middleQuery.isLoading && <Spin />}
                {middleQuery.isError && <Alert type="error" message={(middleQuery.error as Error)?.message ?? '조회 실패'} />}
                {middleQuery.data && (
                  <Table
                    rowKey="accountId"
                    size="small"
                    columns={middleColumns}
                    dataSource={middleQuery.data.items}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                    summary={() =>
                      middleQuery.data ? (
                        <Table.Summary fixed>
                          {middleQuery.data.subtotals.map((sub) => (
                            <Table.Summary.Row key={`sub-${sub.suitability}`}>
                              <Table.Summary.Cell index={0} colSpan={6}>
                                <span style={{ ...suitabilityCellStyle('소계'), padding: '4px 8px' }}>{`${sub.suitability} 소계`}</span>
                              </Table.Summary.Cell>
                              <Table.Summary.Cell index={6} colSpan={9} align="right">
                                {`거래처 ${sub.accountCount}개`}
                              </Table.Summary.Cell>
                            </Table.Summary.Row>
                          ))}
                          <Table.Summary.Row>
                            <Table.Summary.Cell index={0} colSpan={6}>
                              <span style={{ ...suitabilityCellStyle('총계'), padding: '4px 8px' }}>총계</span>
                            </Table.Summary.Cell>
                            <Table.Summary.Cell index={6} colSpan={9} align="right">
                              {`거래처 ${middleQuery.data.total.accountCount}개`}
                            </Table.Summary.Cell>
                          </Table.Summary.Row>
                        </Table.Summary>
                      ) : null
                    }
                  />
                )}
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
                {detailQuery.isLoading && <Spin />}
                {detailQuery.isError && <Alert type="error" message={(detailQuery.error as Error)?.message ?? '조회 실패'} />}
                {detailQuery.data && (
                  <Table
                    rowKey={(r) => `${r.accountId}-${r.employeeCode}`}
                    size="small"
                    columns={detailColumns}
                    dataSource={detailQuery.data.items}
                    pagination={false}
                    scroll={{ x: 'max-content' }}
                  />
                )}
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
            {detailQuery.isLoading && <Spin />}
            {detailQuery.isError && <Alert type="error" message={(detailQuery.error as Error)?.message ?? '조회 실패'} />}
            {detailQuery.data && (
              <Table
                rowKey={(r) => `${r.accountId}-${r.employeeCode}`}
                size="small"
                columns={detailColumns}
                dataSource={detailQuery.data.items}
                pagination={false}
                scroll={{ x: 'max-content' }}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}
