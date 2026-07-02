import { useMemo, useRef, useState } from 'react';
import { Alert, Card, Col, DatePicker, Input, Row, Select, Statistic, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  EXPORT_LIST_PATH,
  exportListParams,
  fetchFilterOptions,
  fetchList,
  fetchProductLookup,
  type ElectronicSalesDashboardListItem,
  type ElectronicSalesProductLookupItem,
} from '@/api/electronicSalesDashboard';
import { useExcelDownload } from '@/hooks/common/useExcelDownload';
import { EXCEL_EXPORT_MAX_ROWS } from '@/lib/excelDownload';
import { buildListPagination } from '@/lib/listPagination';
import { listTableLocale } from '@/lib/listTableLocale';
import PeriodBranchFilterBar from '@/components/common/PeriodBranchFilterBar';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import ElectronicSalesDashboardDetailModal from './ElectronicSalesDashboardDetailModal';

const { Text } = Typography;

// 조회 가능한 최대 기간(개월, 시작일 포함 기준). backend ElectronicSalesAdminQueryService.MAX_RANGE_MONTHS 정합.
const MAX_RANGE_MONTHS = 3;

interface QueryParams {
  startDate: string;
  endDate: string;
  codes: string[];
  customerKeyword?: string;
  distributionChannels: string[];
  accountTypes: string[];
  productIds: number[];
  category2?: string;
  category3?: string;
}

/**
 * 월 매출(전산실적) — web admin 대시보드 페이지.
 *
 * 레거시 「월 매출 조회(전산)」(`/sales/abcMain` → `abcmain.jsp`, POS `live_tot_sales_dh`) 동등.
 * 기간은 일 단위(기본: 당월 1일~오늘 — 레거시 daterangepicker 기본값 정합, 최대 3개월).
 * 유통형태/거래처유형(메인 DB Account) + 제품/중분류/소분류(메인 DB Product → POS 바코드 IN) 필터.
 * 권한 범위 거래처 N건의 기간 전산매출 합계 명세 + 상단 전체 합계 + 페이징 + 정렬 + 엑셀 export.
 * row 클릭 시 제품별 상세 modal (목록과 동일 필터 반영).
 */
export default function ElectronicSalesDashboardPage() {
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()]);
  const [selectedCodes, setSelectedCodes] = useState<string[]>([]);
  const [customerKeyword, setCustomerKeyword] = useState<string>('');
  const [distributionChannels, setDistributionChannels] = useState<string[]>([]);
  const [accountTypes, setAccountTypes] = useState<string[]>([]);
  const [selectedProducts, setSelectedProducts] = useState<ElectronicSalesProductLookupItem[]>([]);
  const [category2, setCategory2] = useState<string | undefined>(undefined);
  const [category3, setCategory3] = useState<string | undefined>(undefined);
  const [queryParams, setQueryParams] = useState<QueryParams | null>(null);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(20);
  const [sort, setSort] = useState<string | undefined>(undefined);
  const [detailTarget, setDetailTarget] = useState<ElectronicSalesDashboardListItem | null>(null);

  // 시작일 기준 3개월(포함) 초과 여부 — 초과 시 경고 + 조회 차단 (외부 POS DB 스캔 보호).
  const rangeExceedsMax = dateRange[1].isAfter(
    dateRange[0].add(MAX_RANGE_MONTHS, 'month').subtract(1, 'day'),
    'day',
  );

  // 유통형태/거래처유형/중·소분류 옵션 — 메인 DB distinct, 메타 데이터라 10분 캐시.
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

  const listQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'list', queryParams, page, pageSize, sort],
    queryFn: () => {
      const p = queryParams!;
      return fetchList({
        startDate: p.startDate,
        endDate: p.endDate,
        costCenterCodes: p.codes,
        customerKeyword: p.customerKeyword,
        distributionChannels: p.distributionChannels,
        accountTypes: p.accountTypes,
        productIds: p.productIds,
        category2: p.category2,
        category3: p.category3,
        page,
        size: pageSize,
        sort,
      });
    },
    enabled: queryParams != null,
    placeholderData: (prev) => prev,
  });

  const handleSearch = () => {
    if (selectedCodes.length === 0) {
      message.warning('지점은 필수항목입니다.');
      return;
    }
    if (rangeExceedsMax) return;
    setPage(0);
    setQueryParams({
      startDate: dateRange[0].format('YYYY-MM-DD'),
      endDate: dateRange[1].format('YYYY-MM-DD'),
      codes: selectedCodes,
      customerKeyword: customerKeyword.trim() || undefined,
      distributionChannels,
      accountTypes,
      productIds: selectedProducts.map((p) => p.productId),
      category2,
      category3,
    });
  };

  const list = listQuery.data;

  const { run: runExport, downloading: exporting } = useExcelDownload();

  const handleExport = () => {
    if (!queryParams) return;
    runExport(
      EXPORT_LIST_PATH,
      `electronic-sales-${queryParams.startDate}-${queryParams.endDate}.xlsx`,
      {
        params: exportListParams({
          startDate: queryParams.startDate,
          endDate: queryParams.endDate,
          costCenterCodes: queryParams.codes,
          customerKeyword: queryParams.customerKeyword,
          distributionChannels: queryParams.distributionChannels,
          accountTypes: queryParams.accountTypes,
          productIds: queryParams.productIds,
          category2: queryParams.category2,
          category3: queryParams.category3,
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

  const columns: ColumnsType<ElectronicSalesDashboardListItem> = useMemo(
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
        title: '전산매출 금액',
        dataIndex: 'salesAmount',
        width: 150,
        sorter: true,
        align: 'right',
        render: (v) => formatWon(v),
      },
      {
        title: '전산매출 수량',
        dataIndex: 'salesQuantity',
        width: 130,
        sorter: true,
        align: 'right',
        render: (v) => formatQty(v),
      },
    ],
    [],
  );

  const filterSummary = queryParams
    ? [
        `${queryParams.startDate} ~ ${queryParams.endDate}`,
        `${queryParams.codes.length}개 지점`,
        queryParams.customerKeyword && `거래처: ${queryParams.customerKeyword}`,
        queryParams.distributionChannels.length > 0 &&
          `유통형태 ${queryParams.distributionChannels.length}개`,
        queryParams.accountTypes.length > 0 && `거래처유형 ${queryParams.accountTypes.length}개`,
        queryParams.productIds.length > 0 && `제품 ${queryParams.productIds.length}개`,
        queryParams.category2 && `중분류: ${queryParams.category2}`,
        queryParams.category3 && `소분류: ${queryParams.category3}`,
      ]
        .filter(Boolean)
        .join(' · ')
    : '';

  return (
    <div style={{ padding: 16 }}>
      <PeriodBranchFilterBar
        selectedCodes={selectedCodes}
        onCodesChange={setSelectedCodes}
        onSearch={handleSearch}
        onExport={handleExport}
        exportDisabled={!list || list.items.length === 0}
        exportLoading={exporting}
        searchLoading={listQuery.isLoading}
        searchDisabled={rangeExceedsMax}
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
            <div>
              <span>거래처 검색:</span>
              <div style={{ marginTop: 4 }}>
                <Input
                  placeholder="거래처명 부분 일치"
                  value={customerKeyword}
                  onChange={(e) => setCustomerKeyword(e.target.value)}
                  onPressEnter={handleSearch}
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
          message={`조회 기간은 최대 ${MAX_RANGE_MONTHS}개월까지 가능합니다`}
          style={{ marginBottom: 16 }}
        />
      )}

      {queryParams != null && (
        <div
          style={{
            marginBottom: 8,
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <Text type="secondary">{filterSummary}</Text>
          <RefreshButton onRefresh={() => listQuery.refetch()} refreshing={listQuery.isFetching} />
        </div>
      )}

      {/* 최종 합계 — 조회 결과 전체(페이징 무관)의 전산매출 금액/수량 (요구 5) */}
      {queryParams != null && list && (
        <Card size="small" style={{ marginBottom: 12 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Statistic
                title="전산매출 금액 합계"
                value={list.totalSalesAmount.toLocaleString()}
                suffix="원"
              />
            </Col>
            <Col span={12}>
              <Statistic title="전산매출 수량 합계" value={list.totalSalesQuantity.toLocaleString()} />
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

      <ResizableTable
        rowKey={(r) => r.accountId}
        size="small"
        columns={columns}
        dataSource={list?.items ?? []}
        loading={queryParams != null && listQuery.isLoading}
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
        // scroll 미지정 → ResizableTable 이 컬럼 width 합을 x 로 자동 계산하여
        // tableLayout: fixed 로 전환, 리사이즈/ellipsis 가 동작한다 (기존 'max-content' 는 폭 고정을 막음).
        locale={listTableLocale({ searched: queryParams != null })}
      />

      <ElectronicSalesDashboardDetailModal
        open={detailTarget != null}
        onClose={() => setDetailTarget(null)}
        customerId={detailTarget?.accountId ?? null}
        customerName={detailTarget?.accountName ?? null}
        startDate={queryParams?.startDate ?? dateRange[0].format('YYYY-MM-DD')}
        endDate={queryParams?.endDate ?? dateRange[1].format('YYYY-MM-DD')}
        productIds={queryParams?.productIds ?? []}
        category2={queryParams?.category2}
        category3={queryParams?.category3}
      />
    </div>
  );
}
