import { useMemo, useRef, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Row,
  Select,
  Space,
  Statistic,
  Tag,
  Typography,
  message,
} from 'antd';
import { PlusOutlined, SearchOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import {
  MAX_SELECTABLE_ACCOUNTS,
  POS_SALES_EXPORT_LIST_PATH,
  exportPosSalesListParams,
  fetchPosSalesList,
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
import { usePosSalesBranches } from '@/hooks/sales/usePosSalesBranches';
import type { Branch } from '@/api/team-schedule';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';
import PosSalesDetailModal from './PosSalesDetailModal';
import PosSalesAccountSelectModal, {
  type PosSalesSelectedAccount,
} from './PosSalesAccountSelectModal';

const { Text } = Typography;

// 조회 가능한 최대 기간(두 끝점 일수 차이) — 레거시 posmain.jsp daterangepicker maxSpan: { days: 31 }
// 정합. backend PosSalesAdminQueryService.MAX_RANGE_DAYS 정합.
const MAX_RANGE_DAYS = 31;

// 지점 목록 미로드(undefined) 시 기본값 — 모듈 상수로 identity 를 고정한다.
const EMPTY_BRANCHES: Branch[] = [];

/** POS 조회 조건 (선택 거래처 + 기간 + 제품/분류). */
interface PosQueryParams {
  startDate: string;
  endDate: string;
  accountIds: number[];
  productIds: number[];
  category2?: string;
  category3?: string;
}

/**
 * POS매출 — web admin 조회 페이지.
 *
 * 레거시 「POS매출 조회」(`/sales/posMain` → `posmain.jsp`, POS `live_pos_sales_dh`) 의 거래처별 확장.
 * 외부 POS DB 부하를 줄이기 위해 조회를 2단으로 분리한다:
 * - 거래처 선택: 「거래처 선택」 모달(`PosSalesAccountSelectModal`)에서 지점/거래처명/유통형태/거래처
 *   유형으로 메인 DB 거래처 목록만 조회(POS 미접촉)하고 최대 20개를 선택 → 메인에 칩으로 표시.
 * - POS 조회: 선택 거래처(+기간 최대 31일 + 제품/중분류/소분류)만 외부 POS DB 로 집계. 상단 합계
 *   카드는 선택 거래처 기준. 페이징 + 정렬 + 엑셀 export. row 클릭 시 제품별 상세 modal.
 *
 * 거래처 검색을 모달로 분리한 이유: 예전엔 메인 필터바의 [조회] 버튼이 "거래처 목록 조회"를 수행해
 * POS매출 조회로 오인되던 UX 혼란이 있었다. 메인에는 POS 조회 조건(기간/제품)과 선택 거래처 칩,
 * 그리고 최종 [POS 매출 조회] 버튼만 남긴다.
 *
 * 조건 옵션/제품 검색은 월매출(전산실적)과 동일 endpoint 재사용 (동일 권한 `monthly_sales_history`).
 */
export default function SalesQueryPage() {
  // POS매출 전용 지점 셀렉터 (monthly_sales_history 게이팅) — 조직 트리 스코프. 모달에 주입.
  const { data: branches = EMPTY_BRANCHES } = usePosSalesBranches();

  // 거래처 선택 (모달에서 확정). id + 칩 라벨 메타.
  const [selectedAccounts, setSelectedAccounts] = useState<PosSalesSelectedAccount[]>([]);
  const [accountModalOpen, setAccountModalOpen] = useState<boolean>(false);

  // POS 조회 조건 (버퍼)
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

  // 중·소분류 옵션 — 메인 DB distinct, 메타 데이터라 10분 캐시. 전산실적과 동일 queryKey 로 캐시 공유.
  const filterOptionsQuery = useQuery({
    queryKey: ['electronicSalesDashboard', 'filter-options'],
    queryFn: fetchFilterOptions,
    staleTime: 10 * 60 * 1000,
  });
  const filterOptions = filterOptionsQuery.data;
  const category2Options = useMemo(
    () => (filterOptions?.categories ?? []).map((c) => ({ value: c.category2, label: c.category2 })),
    [filterOptions],
  );
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

  const selectedAccountIds = useMemo(
    () => selectedAccounts.map((a) => a.accountId),
    [selectedAccounts],
  );

  // 모달 [선택 완료] — 칩만 갱신. 선택 거래처가 바뀌면 기존 POS 결과는 초기화(조건 불일치 방지).
  const handleAccountConfirm = (next: PosSalesSelectedAccount[]) => {
    setSelectedAccounts(next);
    setPosQuery(null);
  };

  // 칩 개별 제거.
  const handleRemoveAccount = (accountId: number) => {
    setSelectedAccounts((prev) => prev.filter((a) => a.accountId !== accountId));
    setPosQuery(null);
  };

  // POS 조회 — 선택 거래처 집계
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

  // POS 조회 — 선택 거래처 집계 실행
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

  // POS 결과 컬럼
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

  const noAccountSelected = selectedAccountIds.length === 0;

  return (
    <div style={{ padding: 16 }}>
      {/* ── POS 조회 조건 (기간 + 제품/분류) ── */}
      <Card size="small" style={{ marginBottom: 12 }}>
        <Space wrap align="end">
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
          <div>
            <span>중분류:</span>
            <div style={{ marginTop: 4 }}>
              <Select
                value={category2}
                onChange={(v) => {
                  setCategory2(v);
                  setCategory3(undefined); // 중분류 변경 시 종속 소분류 초기화
                }}
                options={category2Options}
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
                style={{ width: 420 }}
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

      {/* ── 조회 거래처 (모달 선택 + 칩) ── */}
      <Card size="small" style={{ marginBottom: 12 }}>
        <div
          style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}
        >
          <div style={{ flex: 1, minWidth: 0 }}>
            <Space align="center" style={{ marginBottom: selectedAccounts.length > 0 ? 8 : 0 }}>
              <Text strong>조회 거래처</Text>
              <Button icon={<PlusOutlined />} onClick={() => setAccountModalOpen(true)}>
                거래처 선택
              </Button>
              <Text type="secondary">
                선택 {selectedAccounts.length}/{MAX_SELECTABLE_ACCOUNTS}
              </Text>
            </Space>
            {selectedAccounts.length > 0 ? (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                {selectedAccounts.map((a) => (
                  <Tag
                    key={a.accountId}
                    closable
                    onClose={() => handleRemoveAccount(a.accountId)}
                    style={{ marginInlineEnd: 0 }}
                  >
                    {a.accountName ?? `#${a.accountId}`}
                  </Tag>
                ))}
              </div>
            ) : (
              <Text type="secondary">
                {' '}
                — [거래처 선택]으로 조회할 거래처를 먼저 선택하세요.
              </Text>
            )}
          </div>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearchPos}
            disabled={noAccountSelected || rangeExceedsMax}
            loading={listQuery.isFetching}
          >
            POS 매출 조회
          </Button>
        </div>
      </Card>

      {rangeExceedsMax && (
        <Alert
          type="warning"
          message={`조회 기간은 최대 ${MAX_RANGE_DAYS}일까지 가능합니다`}
          style={{ marginBottom: 16 }}
        />
      )}

      {/* ── POS 결과 액션 (새로고침 + 엑셀) ── */}
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

      {/* 조회 전 안내 — 아직 POS 조회하지 않은 초기 상태 */}
      {posQuery == null && (
        <Card size="small">
          <Text type="secondary">
            <Tag color="blue">거래처 선택</Tag> 으로 조회할 거래처를 최대 {MAX_SELECTABLE_ACCOUNTS}개
            고른 뒤, 기간·제품 조건을 확인하고 <Tag color="geekblue">POS 매출 조회</Tag> 를 누르면
            POS매출이 조회됩니다.
          </Text>
        </Card>
      )}

      <PosSalesAccountSelectModal
        open={accountModalOpen}
        onClose={() => setAccountModalOpen(false)}
        branches={branches}
        initialSelected={selectedAccounts}
        onConfirm={handleAccountConfirm}
      />

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
