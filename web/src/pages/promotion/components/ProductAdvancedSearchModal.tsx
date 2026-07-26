import { useEffect, useMemo, useState } from 'react';
import { Input, Modal, Select, Space, Spin, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import { useQuery } from '@tanstack/react-query';
import {
  fetchProductLookupFilterOptions,
  fetchProductsForPromotionLookup,
  type Product,
} from '@/api/product';
import { PRODUCT_STATUS_TAG } from '@/components/product/productStatus';
import ProductStatusInfoIcon from '@/components/product/ProductStatusInfoIcon';

const PAGE_SIZE = 20;

interface Props {
  open: boolean;
  onClose: () => void;
  onSelect: (product: Product) => void;
  /**
   * 드롭다운 빠른 검색에서 입력 중이던 키워드 — "더보기"/"고급 검색" 진입 시 이어받아 즉시 조회한다.
   * 2자 미만이면 빈 검색 상태로 열린다 (백엔드 keyword 최소 길이 정합).
   */
  initialKeyword?: string;
}

/**
 * 행사마스터 등록/수정 화면의 제품 고급 검색 모달.
 *
 * SF 레거시 행사마스터 대표제품 Enhanced Lookup(고급 검색) 동등 — 검색창 1개 + 다중 컬럼 결과 그리드 +
 * 라디오 단일 선택 + 선택 버튼. 백엔드 `/api/v1/admin/products/lookup` 을 재사용하므로 드롭다운 빠른
 * 검색과 동일한 검색 조건(제품명/제품코드/물류바코드 부분일치, 삭제 제외)이 그대로 적용된다.
 *
 * 드롭다운 빠른 검색은 첫 페이지 20건만 노출해 45건 규모의 키워드는 뒤쪽 제품에 도달할 수 없다.
 * 본 모달은 페이지 이동으로 전체 결과를 열람하게 하는 것이 목적이다.
 *
 * 필터 옵션은 `/lookup-filter-options`(promotion.READ 가드) 에서 모달 오픈 시에만 조회한다 —
 * `/categories` 는 product.READ 가드라 행사마스터 권한만 가진 사용자가 403 이 된다.
 */
export default function ProductAdvancedSearchModal({
  open,
  onClose,
  onSelect,
  initialKeyword,
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
  const [selectedId, setSelectedId] = useState<number | null>(null);

  // 모달 오픈 시 드롭다운 키워드를 이어받아 즉시 조회, 닫을 때 검색 상태 초기화.
  useEffect(() => {
    if (open) {
      const seed = initialKeyword?.trim() ?? '';
      setKeywordInput(seed);
      setSubmitted(seed.length >= 2 ? { keyword: seed } : null);
    } else {
      setKeywordInput('');
      setCategory1Input(undefined);
      setCategory2Input(undefined);
      setCategory3Input(undefined);
      setProductStatusInput(undefined);
      setSubmitted(null);
      setPage(0);
      setSelectedId(null);
    }
    // initialKeyword 는 오픈 시점 값만 사용 — 열려 있는 동안 부모의 입력 변화로 검색이 리셋되지 않게 한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 대/중/소분류 필터 드롭다운 옵션 — 모달을 열 때만 조회.
  const { data: filterOptions } = useQuery({
    queryKey: ['product-lookup-filter-options'],
    queryFn: fetchProductLookupFilterOptions,
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const categories = filterOptions?.categories;

  const category1Options = useMemo(
    () => categories?.map((c) => ({ value: c.category1, label: c.category1 })) ?? [],
    [categories],
  );

  const category2Options = useMemo(() => {
    if (!categories || !category1Input) return [];
    const node = categories.find((c) => c.category1 === category1Input);
    return node?.children.map((c) => ({ value: c.category2, label: c.category2 })) ?? [];
  }, [categories, category1Input]);

  const category3Options = useMemo(() => {
    if (!categories || !category1Input || !category2Input) return [];
    const node1 = categories.find((c) => c.category1 === category1Input);
    const node2 = node1?.children.find((c) => c.category2 === category2Input);
    return node2?.children.map((v) => ({ value: v, label: v })) ?? [];
  }, [categories, category1Input, category2Input]);

  const { data, isFetching } = useQuery({
    queryKey: ['product-advanced-search', submitted, page],
    queryFn: () =>
      fetchProductsForPromotionLookup({
        keyword: submitted?.keyword,
        category1: submitted?.category1,
        category2: submitted?.category2,
        category3: submitted?.category3,
        productStatus: submitted?.productStatus,
        page,
        size: PAGE_SIZE,
      }),
    // 검색 실행 시에만 조회 (SF 고급 검색과 동일 — 빈 검색 전체 노출 방지). keyword 2자 이상
    // 또는 분류/상태 필터 중 하나 이상 선택 시 검색 가능.
    enabled: open && submitted != null,
  });

  const rows = data?.content ?? [];
  const selectedProduct = rows.find((p) => p.id === selectedId) ?? null;

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
    // 검색 조건이 전혀 없으면(키워드 2자 미만 + 분류/상태 미선택) 검색하지 않는다.
    if (
      trimmed.length < 2 &&
      !category1Input &&
      !category2Input &&
      !category3Input &&
      !productStatusInput
    ) {
      return;
    }
    setSubmitted({
      keyword: trimmed.length >= 2 ? trimmed : undefined,
      category1: category1Input,
      category2: category2Input,
      category3: category3Input,
      productStatus: productStatusInput,
    });
    setPage(0);
    setSelectedId(null);
  };

  const handleConfirm = () => {
    if (selectedProduct) {
      onSelect(selectedProduct);
      onClose();
    }
  };

  const columns: ColumnsType<Product> = [
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
      okText="선택"
      cancelText="취소"
      onOk={handleConfirm}
      onCancel={onClose}
      okButtonProps={{ disabled: selectedId == null }}
      // 결과 그리드(컬럼 합계 + 라디오 열 ≈ 1270px)가 가로 스크롤 없이 들어가도록 넓히되,
      // 초대형 화면에서 화면을 꽉 채우지 않도록 상한(1280px) + 좌우 여백(64px)을 둬 모달 형태를 유지한다.
      width="min(1280px, calc(100vw - 64px))"
      destroyOnClose
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
        placeholder="제품명 / 제품코드 / 물류바코드 검색 (2자 이상)"
        allowClear
        enterButton="검색"
        value={keywordInput}
        onChange={(e) => setKeywordInput(e.target.value)}
        onSearch={handleSearch}
        style={{ width: '100%', marginBottom: 12 }}
      />
      {isFetching ? (
        <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
          <Spin />
        </div>
      ) : (
        <ResizableTable<Product>
          dataSource={rows}
          rowKey="id"
          columns={columns}
          size="small"
          scroll={{ x: 1270 }}
          locale={{
            emptyText: submitted ? '검색 결과가 없습니다' : '검색 조건을 입력해주세요',
          }}
          pagination={{
            current: page + 1,
            pageSize: PAGE_SIZE,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (p) => {
              setPage(p - 1);
              setSelectedId(null);
            },
          }}
          rowSelection={{
            type: 'radio',
            selectedRowKeys: selectedId != null ? [selectedId] : [],
            onChange: (keys) => setSelectedId(keys[0] as number),
          }}
          onRow={(record) => ({
            onClick: () => setSelectedId(record.id),
          })}
        />
      )}
    </Modal>
  );
}
