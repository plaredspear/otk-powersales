import { useEffect, useState } from 'react';
import { Input, Modal, Select, Space, Tag, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useQuery } from '@tanstack/react-query';
import ResizableTable from '@/components/common/ResizableTable';
import { listTableLocale } from '@/lib/listTableLocale';
import {
  fetchProductAdvancedSearch,
  fetchProductLookupFilterOptions,
  PRODUCT_ADVANCED_MIN_KEYWORD_LENGTH as MIN_KEYWORD,
  type ElectronicSalesProductAdvancedItem,
} from '@/api/electronicSalesDashboard';
import { PRODUCT_STATUS_TAG } from '@/components/product/productStatus';
import ProductStatusInfoIcon from '@/components/product/ProductStatusInfoIcon';

const PAGE_SIZE = 20;

/**
 * 선택 1건의 보관 형태.
 *
 * 모달을 열 때 부모의 선택 id 를 이어받는데, 그 시점에는 제품 메타(제품명 등)를 모른다.
 * 해당 제품이 검색 결과에 등장하면 메타를 채운다 — 그 전까지는 `kind: 'id'` 로 남는다.
 * 도메인 필드(name 등)의 null 여부로 보강 상태를 판별하면 제품명이 실제로 null 인 행에서
 * 오판하므로, 판별 축을 값이 아니라 형태로 둔다.
 */
type SelectedEntry =
  | { kind: 'id'; id: number }
  | { kind: 'full'; id: number; item: ElectronicSalesProductAdvancedItem };

export interface Props {
  open: boolean;
  onClose: () => void;
  /**
   * 「선택」 확정 시 호출 — 체크된 제품 전체를 넘긴다 (기존 선택을 대체하는 시맨틱).
   *
   * 모달은 오픈 시 부모의 선택을 체크 상태로 복원하므로, 확정 결과가 곧 최종 선택이다.
   * 전부 해제하고 확정하면 빈 배열이 오며, 이는 "선택 없음"(= 전체 제품 조회) 을 뜻한다.
   *
   * 메타를 끝내 확보하지 못한 항목(다른 검색 조건으로 넘어가 결과에 다시 등장하지 않은 경우)은
   * `item` 이 없는 상태로 전달되므로, 부모는 자신이 이미 가진 메타를 우선 사용해야 한다.
   */
  onSelect: (picked: { id: number; item?: ElectronicSalesProductAdvancedItem }[]) => void;
  /**
   * 드롭다운 빠른 검색에서 입력 중이던 키워드 — 「고급 검색」 진입 시 이어받아 즉시 조회한다.
   * [PRODUCT_ADVANCED_MIN_KEYWORD_LENGTH] 미만이면 빈 검색 상태로 열린다.
   */
  initialKeyword?: string;
  /** 모달을 열 때 이미 선택돼 있는 제품 id — 체크 상태를 복원해 재선택/해제를 가능하게 한다. */
  initialSelectedIds?: number[];
}

/**
 * POS 매출 / 월 매출(전산실적) 조회 조건의 제품 고급 검색 모달.
 *
 * 행사마스터 대표제품 고급 검색(`pages/promotion/components/ProductAdvancedSearchModal`) 과 동일한
 * 구성(필터 + 검색창 + 결과 그리드 + 선택 버튼) 이나, 두 가지가 다르다:
 *  1. 다중 선택 (checkbox) — 조회 조건의 제품은 복수 지정이 가능하다.
 *  2. 전용 endpoint (`/sales/electronic/product-lookup/advanced`, monthly_sales_history 가드) —
 *     행사마스터판은 promotion.READ 가드라 본 화면 권한만 가진 사용자가 403 이 된다.
 *
 * 검색 대상은 드롭다운 빠른 검색과 동일한 소비자 바코드 보유 제품 한정이다. 바코드가 없는 제품은
 * POS `UPC_CD IN` 필터에 쓸 수 없어 선택해도 항상 매출 0 건이므로 결과에서 제외된다.
 *
 * 페이지를 이동해도 체크 상태를 유지한다 — 서버 페이징이라 다른 페이지 행은 dataSource 에 없으므로
 * 선택을 [SelectedEntry] 맵에 따로 누적한다.
 */
export default function SalesProductAdvancedSearchModal({
  open,
  onClose,
  onSelect,
  initialKeyword,
  initialSelectedIds,
}: Props) {
  // 입력 중 필터 (검색 버튼 누르기 전 임시값).
  const [keywordInput, setKeywordInput] = useState('');
  const [category1Input, setCategory1Input] = useState<string | undefined>(undefined);
  const [category2Input, setCategory2Input] = useState<string | undefined>(undefined);
  const [category3Input, setCategory3Input] = useState<string | undefined>(undefined);
  const [productStatusInput, setProductStatusInput] = useState<string | undefined>(undefined);
  // 검색 실행 시점의 확정 필터 (쿼리 파라미터).
  const [submitted, setSubmitted] = useState<{
    keyword?: string;
    category1?: string;
    category2?: string;
    category3?: string;
    productStatus?: string;
  } | null>(null);
  const [page, setPage] = useState(0);
  // 페이지를 넘나들며 누적되는 선택 — id → 항목. 현재 페이지에 없는 행의 메타도 보존한다.
  const [selectedMap, setSelectedMap] = useState<Map<number, SelectedEntry>>(new Map());

  // 모달 오픈 시 드롭다운 키워드/기존 선택을 이어받아 즉시 조회, 닫을 때 검색 상태 초기화.
  useEffect(() => {
    if (open) {
      const seed = initialKeyword?.trim() ?? '';
      setKeywordInput(seed);
      setSubmitted(seed.length >= MIN_KEYWORD ? { keyword: seed } : null);
      setSelectedMap(
        new Map((initialSelectedIds ?? []).map((id) => [id, { kind: 'id' as const, id }])),
      );
    } else {
      setKeywordInput('');
      setCategory1Input(undefined);
      setCategory2Input(undefined);
      setCategory3Input(undefined);
      setProductStatusInput(undefined);
      setSubmitted(null);
      setPage(0);
      setSelectedMap(new Map());
    }
    // initialKeyword / initialSelectedIds 는 오픈 시점 값만 사용 — 열려 있는 동안 부모의 상태
    // 변화로 검색이나 체크가 리셋되지 않게 한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 대/중/소분류 + 상태 필터 드롭다운 옵션 — 모달을 열 때만 조회.
  const { data: filterOptions } = useQuery({
    queryKey: ['electronicSalesDashboard', 'product-lookup-filter-options'],
    queryFn: fetchProductLookupFilterOptions,
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const categories = filterOptions?.categories;

  const category1Options = categories?.map((c) => ({ value: c.category1, label: c.category1 })) ?? [];

  const category2Options = (() => {
    if (!categories || !category1Input) return [];
    const node = categories.find((c) => c.category1 === category1Input);
    return node?.children.map((c) => ({ value: c.category2, label: c.category2 })) ?? [];
  })();

  const category3Options = (() => {
    if (!categories || !category1Input || !category2Input) return [];
    const node1 = categories.find((c) => c.category1 === category1Input);
    const node2 = node1?.children.find((c) => c.category2 === category2Input);
    return node2?.children.map((v) => ({ value: v, label: v })) ?? [];
  })();

  const { data, isFetching } = useQuery({
    queryKey: ['electronicSalesDashboard', 'product-advanced-search', submitted, page],
    queryFn: () =>
      fetchProductAdvancedSearch({
        keyword: submitted?.keyword,
        category1: submitted?.category1,
        category2: submitted?.category2,
        category3: submitted?.category3,
        productStatus: submitted?.productStatus,
        page,
        size: PAGE_SIZE,
      }),
    // 검색 실행 시에만 조회 (빈 검색 전체 노출 방지). keyword 2자 이상 또는 분류/상태 필터
    // 중 하나 이상 선택 시 검색 가능.
    enabled: open && submitted != null,
  });

  const rows = data?.content ?? [];

  const handleCategory1Change = (val: string | undefined) => {
    setCategory1Input(val);
    setCategory2Input(undefined);
    setCategory3Input(undefined);
  };

  const handleCategory2Change = (val: string | undefined) => {
    setCategory2Input(val);
    setCategory3Input(undefined);
  };

  const handleSearch = () => {
    const trimmed = keywordInput.trim();
    // 검색 조건이 전혀 없으면(키워드 최소 길이 미만 + 분류/상태 미선택) 검색하지 않는다.
    if (
      trimmed.length < MIN_KEYWORD &&
      !category1Input &&
      !category2Input &&
      !category3Input &&
      !productStatusInput
    ) {
      return;
    }
    setSubmitted({
      keyword: trimmed.length >= MIN_KEYWORD ? trimmed : undefined,
      category1: category1Input,
      category2: category2Input,
      category3: category3Input,
      productStatus: productStatusInput,
    });
    setPage(0);
    // 검색 조건을 바꿔도 이미 고른 제품은 유지한다 (여러 번 검색해 누적 선택하는 흐름).
  };

  const handleConfirm = () => {
    // 현재 페이지 행으로 마지막 메타 보강 — 오픈 시 id 만 복원된 항목이 이 페이지에 있으면 채운다.
    const byId = new Map(rows.map((r) => [r.id, r]));
    onSelect(
      [...selectedMap.values()].map((entry) => ({
        id: entry.id,
        item: entry.kind === 'full' ? entry.item : byId.get(entry.id),
      })),
    );
    onClose();
  };

  const columns: ColumnsType<ElectronicSalesProductAdvancedItem> = [
    { title: '제품코드', dataIndex: 'productCode', key: 'productCode', width: 110, fixed: 'left' },
    { title: '제품명', dataIndex: 'name', key: 'name', width: 300, fixed: 'left' },
    { title: '대분류', dataIndex: 'category1', key: 'category1', width: 110 },
    { title: '중분류', dataIndex: 'category2', key: 'category2', width: 110 },
    { title: '소분류', dataIndex: 'category3', key: 'category3', width: 110 },
    {
      title: (
        <span>
          상태 <ProductStatusInfoIcon />
        </span>
      ),
      dataIndex: 'productStatus',
      key: 'productStatus',
      width: 100,
      align: 'center',
      render: (value: string | null) =>
        value ? <Tag color={PRODUCT_STATUS_TAG[value] ?? undefined}>{value}</Tag> : '-',
    },
    { title: '보관방법', dataIndex: 'storageCondition', key: 'storageCondition', width: 90 },
    { title: '단위', dataIndex: 'unit', key: 'unit', width: 70 },
    { title: '출시일', dataIndex: 'launchDate', key: 'launchDate', width: 110 },
    {
      title: '표준출고가',
      dataIndex: 'standardUnitPrice',
      key: 'standardUnitPrice',
      width: 110,
      align: 'right',
      render: (value: number | null) => (value != null ? value.toLocaleString() : '-'),
    },
  ];

  return (
    <Modal
      open={open}
      title="제품 고급 검색"
      okText={selectedMap.size > 0 ? `선택 (${selectedMap.size})` : '선택'}
      cancelText="취소"
      onOk={handleConfirm}
      onCancel={onClose}
      // 0건 확정을 막지 않는다 — 전부 해제한 뒤 확정하는 것이 "제품 조건 해제(전체 조회)" 의도다.
      // 여기서 비활성화하면 사용자가 모달에서 한 해제 작업이 통째로 버려진다.
      width="min(1280px, calc(100vw - 64px))"
      destroyOnHidden
    >
      <Space style={{ marginBottom: 12 }} wrap>
        <Select
          placeholder="대분류"
          allowClear
          style={{ width: 160 }}
          value={category1Input}
          onChange={handleCategory1Change}
          options={category1Options}
        />
        <Select
          placeholder="중분류"
          allowClear
          disabled={!category1Input}
          style={{ width: 160 }}
          value={category2Input}
          onChange={handleCategory2Change}
          options={category2Options}
        />
        <Select
          placeholder="소분류"
          allowClear
          disabled={!category2Input}
          style={{ width: 160 }}
          value={category3Input}
          onChange={setCategory3Input}
          options={category3Options}
        />
        <Select
          placeholder="상태"
          allowClear
          style={{ width: 140 }}
          value={productStatusInput}
          onChange={setProductStatusInput}
          options={filterOptions?.productStatuses.map((v) => ({ value: v, label: v })) ?? []}
        />
      </Space>
      <Input.Search
        placeholder={`제품명 / 제품코드 / 바코드 검색 (${MIN_KEYWORD}자 이상)`}
        allowClear
        enterButton="조회"
        value={keywordInput}
        onChange={(e) => setKeywordInput(e.target.value)}
        onSearch={handleSearch}
        style={{ width: '100%', marginBottom: 8 }}
      />
      <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12, fontSize: 12 }}>
        선택 {selectedMap.size}건 — 검색 조건을 바꿔가며 여러 번 담을 수 있습니다. 매출 집계 기준인
        바코드가 등록된 제품만 검색됩니다.
      </Typography.Text>
      <ResizableTable<ElectronicSalesProductAdvancedItem>
        dataSource={rows}
        rowKey="id"
        columns={columns}
        loading={isFetching}
        scroll={{ x: 1270 }}
        locale={listTableLocale({
          searched: submitted != null,
          beforeSearchText: '검색 조건을 입력해주세요.',
        })}
        pagination={{
          current: page + 1,
          pageSize: PAGE_SIZE,
          total: data?.totalElements ?? 0,
          showSizeChanger: false,
          // 페이지를 넘겨도 선택은 유지 — selectedMap 을 건드리지 않는다.
          onChange: (p) => setPage(p - 1),
        }}
        rowSelection={{
          type: 'checkbox',
          selectedRowKeys: [...selectedMap.keys()],
          // 현재 페이지 행에 대한 체크/해제만 반영 — 다른 페이지 선택은 그대로 둔다.
          onSelect: (record, selected) => {
            setSelectedMap((prev) => {
              const next = new Map(prev);
              if (selected) next.set(record.id, { kind: 'full', id: record.id, item: record });
              else next.delete(record.id);
              return next;
            });
          },
          // 헤더 전체선택 — 현재 페이지 행만 대상.
          onSelectAll: (selected, _selectedRows, changeRows) => {
            setSelectedMap((prev) => {
              const next = new Map(prev);
              changeRows.forEach((row) => {
                if (selected) next.set(row.id, { kind: 'full', id: row.id, item: row });
                else next.delete(row.id);
              });
              return next;
            });
          },
        }}
      />
    </Modal>
  );
}
