import { useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
  message,
} from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { TableRowSelection } from 'antd/es/table/interface';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  MAX_SELECTABLE_ACCOUNTS,
  POS_SALES_EXPORT_LIST_PATH,
  exportPosSalesListParams,
  fetchPosSalesAccounts,
  fetchPosSalesList,
  type PosSalesAccountItem,
  type PosSalesDashboardListItem,
} from '@/api/posSales';
import {
  fetchFilterOptions,
  fetchProductLookup,
  type ElectronicSalesProductLookupItem,
} from '@/api/electronicSalesDashboard';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import PosSalesDetailModal from './PosSalesDetailModal';

const { Text } = Typography;

// 조회 가능한 최대 기간(두 끝점 일수 차이) — 레거시 posmain.jsp daterangepicker maxSpan: { days: 31 }
// 정합. backend PosSalesAdminQueryService.MAX_RANGE_DAYS 정합.
const MAX_RANGE_DAYS = 31;

/** 1단 거래처 조회 조건 (조회 버튼 클릭 시점에 확정). */
interface AccountQueryParams {
  codes: string[];
  customerKeyword?: string;
  distributionChannels: string[];
  accountTypes: string[];
}

/** 2단 POS 조회 조건 (선택 거래처 + 기간 + 제품/분류). */
interface PosQueryParams {
  startDate: string;
  endDate: string;
  accountIds: number[];
  productIds: number[];
  category2?: string;
  category3?: string;
}

/**
 * POS매출 — web admin 2단 조회 페이지.
 *
 * 레거시 「POS매출 조회」(`/sales/posMain` → `posmain.jsp`, POS `live_pos_sales_dh`) 의 거래처별 확장.
 * 외부 POS DB 부하를 줄이기 위해 조회를 2단으로 분리한다:
 * - 1단: 지점/거래처명/유통형태/거래처유형으로 메인 DB 거래처 목록만 조회 (POS 미접촉, 즉시 응답).
 *        결과 목록에서 거래처를 최대 20개 선택.
 * - 2단: 선택 거래처(+기간, 최대 31일 + 제품/중분류/소분류)만 외부 POS DB 로 집계. 상단 합계 카드는
 *        선택 거래처 기준. 페이징 + 정렬 + 엑셀 export. row 클릭 시 제품별 상세 modal.
 *
 * 조건 옵션/제품 검색은 월매출(전산실적)과 동일 endpoint 재사용 (동일 권한 `monthly_sales_history`).
 */
export default function SalesQueryPage() {
  // 1단 거래처 조회 조건 (버퍼)
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  const [distributionChannels, setDistributionChannels] = useState<string[]>([]);
  const [accountTypes, setAccountTypes] = useState<string[]>([]);
  const [accountQuery, setAccountQuery] = useState<AccountQueryParams | null>(null);

  // 거래처 선택 (1단 결과에서 체크)
  const [selectedAccountIds, setSelectedAccountIds] = useState<number[]>([]);

  // 2단 POS 조회 조건 (버퍼)
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()]);
  const [selectedProducts, setSelectedProducts] = useState<ElectronicSalesProductLookupItem[]>([]);
  const [category2, setCategory2] = useState<string | undefined>(undefined);
  const [category3, setCategory3] = useState<string | undefined>(undefined);
  const [posQuery, setPosQuery] = useState<PosQueryParams | null>(null);

  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<PosSalesDashboardListItem | null>(null);

  // 두 끝점 일수 차이 31 초과 여부 — 초과 시 경고 + 조회 차단 (외부 POS DB 스캔 보호 + 레거시 maxSpan 정합).
  const rangeExceedsMax = dateRange[1].diff(dateRange[0], 'day') > MAX_RANGE_DAYS;

  // 유통형태/거래처유형/중·소분류 옵션 — 메인 DB distinct, 메타 데이터라 10분 캐시.
  // 전산실적과 동일 옵션이므로 동일 queryKey 로 캐시 공유.
  const filterOptionsQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'filter-options'],
    queryFn: fetchFilterOptions,
    staleTime: 10 * 60 * 1000,
  });
  const filterOptions = filterOptionsQuery.data;
  const category3Options = useMemo(() => {
    if (!category2) return [];
    return (
      filterOptions?.categories.find((c) => c.category2 === category2)?.category3s ?? []
    ).map((c3) => ({ value: c3, label: c3 }));
  }, [filterOptions, category2]);

  // 제품 검색 — 입력 300ms 디바운스 후 제품명/제품코드/바코드 부분일치 조회 (최대 50건).
  const [productKeyword, setProductKeyword] = useState('');
  const productDebounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const handleProductSearch = (value: string) => {
    clearTimeout(productDebounceRef.current);
    productDebounceRef.current = setTimeout(() => setProductKeyword(value.trim()), 300);
  };
  const productLookupQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'product-lookup', productKeyword],
    queryFn: () => fetchProductLookup(productKeyword),
    enabled: productKeyword.length > 0,
    staleTime: 60 * 1000,
  });

  // 선택된 제품 + 현재 검색 결과를 병합해 옵션 구성 — 선택 항목의 라벨이 검색어 변경 후에도 유지.
  const productOptions = useMemo(() => {
    const byId = new Map<number, ElectronicSalesProductLookupItem>();
    selectedProducts.forEach((p) => byId.set(p.productId, p));
    (productLookupQuery.data ?? []).forEach((p) => {
      if (!byId.has(p.productId)) byId.set(p.productId, p);
    });
    return [...byId.values()].map((p) => ({
      value: p.productId,
      label: `${p.name ?? '-'} (${p.productCode ?? '-'} / ${p.barcode})`,
      item: p,
    }));
  }, [selectedProducts, productLookupQuery.data]);

  const handleProductChange = (ids: number[]) => {
    const pool = new Map<number, ElectronicSalesProductLookupItem>();
    productOptions.forEach((o) => pool.set(o.value, o.item));
    setSelectedProducts(
      ids.map((id) => pool.get(id)).filter((p): p is ElectronicSalesProductLookupItem => p != null),
    );
  };

  // 1단 — 거래처 목록 조회
  const accountsQuery = useQuery({
    queryKey: ['posSalesDashboard', 'accounts', accountQuery],
    queryFn: () => {
      const q = accountQuery!;
      return fetchPosSalesAccounts({
        costCenterCodes: q.codes,
        customerKeyword: q.customerKeyword,
        distributionChannels: q.distributionChannels,
        accountTypes: q.accountTypes,
      });
    },
    enabled: accountQuery != null,
    placeholderData: (prev) => prev,
  });
  const accounts = accountsQuery.data;

  // 2단 — 선택 거래처 POS 집계
  const listQuery = useQuery({
    queryKey: ['posSalesDashboard', 'list', posQuery, page, pageSize, sort],
    queryFn: () => {
      const p = posQuery!;
      return fetchPosSalesList({
        startDate: p.startDate,
        endDate: p.endDate,
        accountIds: p.accountIds,
        productIds: p.productIds,
        category2: p.category2,
        category3: p.category3,
        page,
        size: pageSize,
        sort,
      });
    },
    enabled: posQuery != null,
    placeholderData: (prev) => prev,
  });
  const list = listQuery.data;

  // 1단 조회 — 거래처 목록 갱신 (선택/2단 결과 초기화)
  const handleSearchAccounts = () => {
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    setSelectedAccountIds([]);
    setPosQuery(null);
    setAccountQuery({
      codes: selectedCodes,
      customerKeyword: customerKeyword.trim() || undefined,
      distributionChannels,
      accountTypes,
    });
  };

  // 2단 조회 — 선택 거래처 POS 집계
  const handleSearchPos = () => {
    if (selectedAccountIds.length === 0) {
      message.warning('조회할 거래처를 선택해주세요.');
      return;
    }
    if (rangeExceedsMax) return;
    setPage(0);
    setPosQuery({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      accountIds: selectedAccountIds,
      productIds: selectedProducts.map((p) => p.productId),
      category2,
      category3,
    });
  };

  const { run: runExport, downloading: exporting } = useExcelDownload();

  const handleExport = () => {
    if (!posQuery) return;
    runExport(
      POS_SALES_EXPORT_LIST_PATH,
      `pos-sales-${posQuery.startDate}-${posQuery.endDate}.xlsx`,
      {
        params: exportPosSalesListParams({
          startDate: posQuery.startDate,
          endDate: posQuery.endDate,
          accountIds: posQuery.accountIds,
          productIds: posQuery.productIds,
          category2: posQuery.category2,
          category3: posQuery.category3,
          sort,
        }),
        totalCount: list?.pageInfo.totalElements,
        maxRows: EXCEL_EXPORT_MAX_ROWS,
      },
    );
  };

  const formatWon = (v: number | null | undefined) =>
    v == null ? '-' : `${v.toLocaleString()}원`;
  const formatQty = (v: number | null | undefined) =>
    v == null ? '-' : v.toLocaleString();

  // 1단 거래처 목록 컬럼 (체크박스 선택)
  const accountColumns: ColumnsType<PosSalesAccountItem> = useMemo(
    () => [
      { title: '거래처', dataIndex: 'accountName', width: 200, render: (v) => v ?? '-' },
      { title: 'SAP코드', dataIndex: 'sapAccountCode', width: 120, render: (v) => v ?? '-' },
      { title: '지점', dataIndex: 'branchName', width: 140, render: (v) => v ?? '-' },
    ],
    [],
  );

  const rowSelection: TableRowSelection<PosSalesAccountItem> = {
    // 최대 20개 상한이라 "전체 선택"은 무의미(20개 초과 목록에서 누르면 상한 초과로 0개 선택됨) → 숨김.
    hideSelectAll: true,
    selectedRowKeys: selectedAccountIds,
    onChange: (keys) => {
      const next = keys as number[];
      if (next.length > MAX_SELECTABLE_ACCOUNTS) {
        message.warning(`거래처는 최대 ${MAX_SELECTABLE_ACCOUNTS}개까지 선택할 수 있습니다.`);
        return;
      }
      setSelectedAccountIds(next);
    },
    getCheckboxProps: (record) => ({
      // 상한 도달 시 미선택 행은 체크 비활성 (이미 선택된 행은 해제 가능하도록 유지)
      disabled:
        selectedAccountIds.length >= MAX_SELECTABLE_ACCOUNTS &&
        !selectedAccountIds.includes(record.accountId),
    }),
  };

  // 2단 POS 결과 컬럼
  const columns: ColumnsType<PosSalesDashboardListItem> = useMemo(
    () => [
      {
        title: '거래처',
        dataIndex: 'accountName',
        width: 180,
        fixed: 'left',
        render: (v, row) => (
          <a onClick={() => setDetailTarget(row)} role="button">
            {v ?? '-'}
          </a>
        ),
      },
      { title: 'SAP코드', dataIndex: 'sapAccountCode', width: 110, render: (v) => v ?? '-' },
      { title: '지점', dataIndex: 'branchName', width: 120, render: (v) => v ?? '-' },
      {
        title: 'POS매출 금액',
        dataIndex: 'salesAmount',
        width: 150,
        sorter: true,
        align: 'right',
        render: (v) => formatWon(v),
      },
      {
        title: 'POS매출 수량',
        dataIndex: 'salesQuantity',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatQty(v),
      },
    ],
    [],
  );

  return (
    <div style={{ padding: 16 }}>
      {/* ── 1단: 거래처 조회 조건 ── */}
      <PeriodBranchFilterBar
        selectedCodes={selectedCodes}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearchAccounts}
        onExport={() => {}}
        hideExport
        searchLoading={accountsQuery.isFetching}
        periodFilter={
          <div>
            <span>조회기간:</span>
            <div style={{ marginTop: 4 }}>
              <DatePicker.RangePicker
                value={dateRange}
                onChange={(range) => {
                  if (range?.[0] && range?.[1]) setDateRange([range[0], range[1]]);
                }}
                allowClear={false}
                style={{ width: 260 }}
              />
            </div>
          </div>
        }
        extraFilters={
          <>
            <div>
              <span>유통형태:</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  mode="multiple"
                  value={distributionChannels}
                  onChange={setDistributionChannels}
                  options={(filterOptions?.distributionChannels ?? []).map((v) => ({ value: v, label: v }))}
                  placeholder="전체"
                  style={{ minWidth: 160, maxWidth: 280 }}
                  maxTagCount="responsive"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  loading={filterOptionsQuery.isLoading}
                  notFoundContent="항목 없음"
                />
              </div>
            </div>
            <div>
              <span>거래처유형:</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  mode="multiple"
                  value={accountTypes}
                  onChange={setAccountTypes}
                  options={(filterOptions?.accountTypes ?? []).map((v) => ({ value: v, label: v }))}
                  placeholder="전체"
                  style={{ minWidth: 160, maxWidth: 280 }}
                  maxTagCount="responsive"
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  loading={filterOptionsQuery.isLoading}
                  notFoundContent="항목 없음"
                />
              </div>
            </div>
            <div>
              <span>거래처 검색:</span>
              <div style={{ marginTop: 4 }}>
                <Input
                  placeholder="거래처명 부분 일치"
                  value={customerKeyword}
                  onChange={(e) => setCustomerKeyword(e.target.value)}
                  onPressEnter={handleSearchAccounts}
                  style={{ width: 180 }}
                  allowClear
                />
              </div>
            </div>
          </>
        }
      />

      {rangeExceedsMax && (
        <Alert
          type="warning"
          message={`조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다`}
          style={{ marginBottom: 16 }}
        />
      )}

      {accountsQuery.isError && (
        <Alert
          type="error"
          message={(accountsQuery.error as Error)?.message ?? '거래처 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {/* ── 1단 결과: 거래처 목록 (체크박스 선택) ── */}
      {accountQuery != null && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <div
            style={{
              marginBottom: 8,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <Space>
              <Text strong>거래처 선택</Text>
              <Text type="secondary">
                {accounts?.totalElements ?? 0}건 · 선택 {selectedAccountIds.length}/
                {MAX_SELECTABLE_ACCOUNTS}
              </Text>
            </Space>
            <Button
              type="primary"
              icon={<SearchOutlined />}
              onClick={handleSearchPos}
              disabled={selectedAccountIds.length === 0 || rangeExceedsMax}
              loading={listQuery.isFetching}
            >
              선택 거래처 POS 조회
            </Button>
          </div>
          <ResizableTable
            rowKey={(r) => r.accountId}
            size="small"
            columns={accountColumns}
            dataSource={accounts?.items ?? []}
            loading={accountsQuery.isLoading}
            rowSelection={rowSelection}
            scroll={{ y: 280 }}
            pagination={false}
            locale={listTableLocale({ searched: accountQuery != null })}
          />
        </Card>
      )}

      {/* ── 2단: 제품/분류 필터 ── */}
      {accountQuery != null && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Space wrap align="end">
            <div>
              <span>중분류:</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  value={category2}
                  onChange={(v) => {
                    setCategory2(v);
                    setCategory3(undefined); // 중분류 변경 시 종속 소분류 초기화
                  }}
                  options={(filterOptions?.categories ?? []).map((c) => ({
                    value: c.category2,
                    label: c.category2,
                  }))}
                  placeholder="전체"
                  style={{ width: 140 }}
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  loading={filterOptionsQuery.isLoading}
                  notFoundContent="항목 없음"
                />
              </div>
            </div>
            <div>
              <span>소분류:</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  value={category3}
                  onChange={setCategory3}
                  options={category3Options}
                  placeholder={category2 ? '전체' : '중분류 먼저 선택'}
                  style={{ width: 140 }}
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  disabled={!category2}
                  notFoundContent="항목 없음"
                />
              </div>
            </div>
            <div>
              <span>제품 (제품명/제품코드/바코드):</span>
              <div style={{ marginTop: 4 }}>
                <Select
                  mode="multiple"
                  value={selectedProducts.map((p) => p.productId)}
                  onChange={handleProductChange}
                  onSearch={handleProductSearch}
                  options={productOptions}
                  placeholder="검색 후 추가 (미선택 시 전체)"
                  style={{ minWidth: 260, maxWidth: 420 }}
                  maxTagCount="responsive"
                  allowClear
                  showSearch
                  filterOption={false}
                  loading={productLookupQuery.isFetching}
                  notFoundContent={
                    productKeyword ? '검색 결과 없음' : '제품명, 제품코드 또는 바코드를 입력하세요'
                  }
                />
              </div>
            </div>
          </Space>
        </Card>
      )}

      {/* ── 2단 결과 ── */}
      {posQuery != null && (
        <div
          style={{
            marginBottom: 8,
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
          }}
        >
          <Space>
            <RefreshButton onRefresh={() => listQuery.refetch()} refreshing={listQuery.isFetching} />
            <Button onClick={handleExport} loading={exporting} disabled={!list || list.items.length === 0}>
              엑셀 다운로드
            </Button>
          </Space>
        </div>
      )}

      {/* 최종 합계 — 선택 거래처(페이징 무관)의 POS매출 금액/수량 */}
      {posQuery != null && list && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Statistic
                title="POS매출 금액 합계"
                value={list.totalSalesAmount.toLocaleString()}
                suffix="원"
              />
            </Col>
            <Col span={12}>
              <Statistic title="POS매출 수량 합계" value={list.totalSalesQuantity.toLocaleString()} />
            </Col>
          </Row>
        </Card>
      )}

      {listQuery.isError && (
        <Alert
          type="error"
          message={(listQuery.error as Error)?.message ?? '명세 조회 실패'}
          style={{ marginBottom: 8 }}
        />
      )}

      {posQuery != null && (
        <ResizableTable
          rowKey={(r) => r.accountId}
          size="small"
          columns={columns}
          dataSource={list?.items ?? []}
          loading={listQuery.isLoading}
          pagination={buildListPagination({
            page,
            pageSize,
            total: list?.pageInfo.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: (size) => {
              setPageSize(size);
              setPage(0);
            },
          })}
          onChange={(_pagination, _filters, sorter) => {
            if (!Array.isArray(sorter) && sorter.order && sorter.field) {
              const field = String(sorter.field);
              const direction = sorter.order === 'descend' ? 'desc' : 'asc';
              setSort(`${field},${direction}`);
              setPage(0);
            } else {
              setSort(undefined);
            }
          }}
          locale={listTableLocale({ searched: posQuery != null })}
        />
      )}

      {/* 조회 전 안내 — 어떤 조건도 조회하지 않은 초기 상태 */}
      {accountQuery == null && (
        <Card size="small">
          <Text type="secondary">
            지점·기간 등 조건을 선택하고 <Tag color="blue">조회</Tag> 를 누르면 거래처 목록이
            표시됩니다. 목록에서 거래처를 최대 {MAX_SELECTABLE_ACCOUNTS}개 선택한 뒤{' '}
            <Tag color="geekblue">선택 거래처 POS 조회</Tag> 로 POS매출을 조회하세요.
          </Text>
        </Card>
      )}

      <PosSalesDetailModal
        open={detailTarget != null}
        onClose={() => setDetailTarget(null)}
        customerId={detailTarget?.accountId ?? null}
        customerName={detailTarget?.accountName ?? null}
        startDate={posQuery?.startDate ?? dateRange[0].format('YYYY-MM-DD')}
        endDate={posQuery?.endDate ?? dateRange[1].format('YYYY-MM-DD')}
        productIds={posQuery?.productIds ?? []}
        category2={posQuery?.category2}
        category3={posQuery?.category3}
      />
    </div>
  );
}
