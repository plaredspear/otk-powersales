import { useMemo, useState } from 'react';
import { Alert, Button, Input, Select, Space, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useLocation, useNavigate } from 'react-router-dom';
import { useListQueryParams } from '@/hooks/common/useListQueryParams';
import { useProducts, useProductCategories } from '@/hooks/product/useProducts';
import { downloadProductsExcel, type Product } from '@/api/product';
import { useProductInventorySearchStore } from '@/stores/productInventorySearchStore';
import InventorySearchModal from '@/components/product/InventorySearchModal';
import SelectedProductsCompareModal from '@/components/product/SelectedProductsCompareModal';
import ResizableTable from '@/components/common/ResizableTable';
import RefreshButton from '@/components/common/RefreshButton';

const STATUS_TAG: Record<string, string> = {
  판매중: 'green',
  단종: 'red',
  출고중지: 'red',
};

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '판매중', label: '판매중' },
  { value: '단종', label: '단종' },
];

const PAGE_SIZE = 20;
const INVENTORY_SEARCH_MAX = 50;

// SF ShelfLifeFull__c formula 동등 — 유통기한 단위 한글 접미사 변환 (월→개월, 일자→일, 연도→년)
function shelfLifeUnitLabel(unit: string | null): string {
  switch (unit) {
    case '월':
      return '개월';
    case '일자':
      return '일';
    case '연도':
      return '년';
    default:
      return unit ?? '';
  }
}

export default function ProductPage() {
  const navigate = useNavigate();
  const location = useLocation();
  // 상세 진입 시 현재 목록의 query string 을 state 로 넘겨, 상세의 "목록으로" 버튼이 직전 조건으로 복귀하게 한다.
  const goToDetail = (code: string) =>
    navigate(`/product/${encodeURIComponent(code)}`, { state: { listSearch: location.search } });
  // page/필터를 URL query string 에 보관 — 상세 진입 후 뒤로가기/재진입 시 직전 조건 복원.
  const { page, setPage, filters, setFilters } = useListQueryParams({
    defaultFilters: { keyword: '', category1: '', category2: '', category3: '', productStatus: '' },
  });
  const { keyword, category1, category2, category3, productStatus } = filters;
  // 조회 조건 버퍼 — "조회" 버튼 / Enter 시점에만 URL 필터로 일괄 반영 (필터 변경만으로 조회하지 않음)
  const [keywordInput, setKeywordInput] = useState(keyword);
  const [category1Input, setCategory1Input] = useState(category1);
  const [category2Input, setCategory2Input] = useState(category2);
  const [category3Input, setCategory3Input] = useState(category3);
  const [productStatusInput, setProductStatusInput] = useState(productStatus);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [inventoryModalOpen, setInventoryModalOpen] = useState(false);
  const [compareModalOpen, setCompareModalOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const setInventoryTargets = useProductInventorySearchStore((s) => s.setTargets);

  const { data, isLoading, isError, error, refetch, isFetching } = useProducts({
    keyword: keyword || undefined,
    category1: category1 || undefined,
    category2: category2 || undefined,
    category3: category3 || undefined,
    productStatus: productStatus || undefined,
    page,
    size: PAGE_SIZE,
  });

  const { data: categories } = useProductCategories();

  const category1Options = categories
    ? categories.map((c) => ({ value: c.category1, label: c.category1 }))
    : [];

  const category2Options = useMemo(() => {
    if (!categories || !category1Input) return [];
    const node = categories.find((c) => c.category1 === category1Input);
    if (!node) return [];
    return node.children.map((c) => ({ value: c.category2, label: c.category2 }));
  }, [categories, category1Input]);

  const category3Options = useMemo(() => {
    if (!categories || !category1Input || !category2Input) return [];
    const node1 = categories.find((c) => c.category1 === category1Input);
    if (!node1) return [];
    const node2 = node1.children.find((c) => c.category2 === category2Input);
    if (!node2) return [];
    return node2.children.map((v) => ({ value: v, label: v }));
  }, [categories, category1Input, category2Input]);

  const handleCategory1Change = (val: string) => {
    setCategory1Input(val);
    setCategory2Input('');
    setCategory3Input('');
  };

  const handleCategory2Change = (val: string) => {
    setCategory2Input(val);
    setCategory3Input('');
  };

  const handleSearch = () => {
    setFilters({
      keyword: keywordInput,
      category1: category1Input,
      category2: category2Input,
      category3: category3Input,
      productStatus: productStatusInput,
    });
  };

  const selectedProducts = useMemo(() => {
    if (!data?.content) return [];
    const keySet = new Set(selectedRowKeys);
    return data.content.filter((p) => keySet.has(p.productCode ?? ''));
  }, [data?.content, selectedRowKeys]);

  const handleOpenInventory = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('선택하신 제품이 없습니다. 조회 할 제품을 선택해주세요.');
      return;
    }
    if (selectedRowKeys.length > INVENTORY_SEARCH_MAX) {
      message.warning(`최대 ${INVENTORY_SEARCH_MAX}건까지만 조회가 가능합니다. 조회 할 제품을 줄여주세요.`);
      return;
    }
    setInventoryTargets(selectedProducts.map((p) => ({
      productCode: p.productCode ?? '',
      name: p.name,
      category1: p.category1,
      category2: p.category2,
      unit: p.unit,
    })));
    setInventoryModalOpen(true);
  };

  const handleOpenCompare = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('선택된 제품이 없습니다.');
      return;
    }
    setCompareModalOpen(true);
  };

  const handleDownloadExcel = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('선택된 제품이 없습니다.');
      return;
    }
    setDownloading(true);
    try {
      const blob = await downloadProductsExcel(selectedRowKeys.map(String));
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = '선택제품.xlsx';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      message.error((err as Error)?.message || '엑셀 다운로드에 실패했습니다');
    } finally {
      setDownloading(false);
    }
  };

  const columns: ColumnsType<Product> = [
    {
      title: '제품코드',
      dataIndex: 'productCode',
      width: 110,
      render: (val: string | null) =>
        val ? (
          <a
            onClick={(e) => {
              e.stopPropagation();
              goToDetail(val);
            }}
          >
            {val}
          </a>
        ) : (
          '-'
        ),
    },
    {
      title: '제품명',
      dataIndex: 'name',
      width: 200,
      ellipsis: true,
      render: (val: string | null, record) =>
        val ? (
          <a
            onClick={(e) => {
              e.stopPropagation();
              if (record.productCode) {
                goToDetail(record.productCode);
              }
            }}
          >
            {val}
          </a>
        ) : (
          '-'
        ),
    },
    { title: '대분류', dataIndex: 'category1', width: 100, render: (val: string | null) => val ?? '-' },
    { title: '중분류', dataIndex: 'category2', width: 100, render: (val: string | null) => val ?? '-' },
    { title: '소분류', dataIndex: 'category3', width: 100, render: (val: string | null) => val ?? '-' },
    {
      title: '보관방법',
      dataIndex: 'storageCondition',
      width: 100,
      render: (val: string | null) => val ?? '-',
    },
    { title: '단위', dataIndex: 'unit', width: 60, align: 'center', render: (val: string | null) => val ?? '-' },
    {
      title: '출시일',
      dataIndex: 'launchDate',
      width: 110,
      render: (val: string | null) => val ?? '-',
    },
    {
      title: '표준출고가(원)',
      dataIndex: 'standardUnitPrice',
      width: 100,
      align: 'right',
      render: (val: number | null) => (val != null ? val.toLocaleString() : '-'),
    },
    {
      title: '부가세',
      dataIndex: 'superTax',
      width: 80,
      align: 'right',
      render: (val: number | null) => (val != null ? `₩${val.toLocaleString()}` : '-'),
    },
    {
      title: '유통기한',
      width: 110,
      render: (_: unknown, record) =>
        record.shelfLife ? `${record.shelfLife}${shelfLifeUnitLabel(record.shelfLifeUnit)}` : '-',
    },
    {
      title: '제품상태',
      dataIndex: 'productStatus',
      width: 80,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={STATUS_TAG[val] ?? undefined}>{val}</Tag> : '-',
    },
    {
      title: '증정/시식 구분',
      dataIndex: 'tasteGift',
      width: 80,
      align: 'center',
      render: (val: string | null) => {
        if (val === '1') return '전용';
        if (val === '2') return '범용';
        return val ?? '-';
      },
    },
    {
      title: '최종 수정 일자',
      dataIndex: 'lastModifiedAt',
      width: 160,
      render: (val: string | null) => (val ? val.replace('T', ' ').slice(0, 19) : '-'),
    },
  ];

  if (isError) {
    return (
      <div style={{ padding: 24 }}>
        <Alert
          type="error"
          message="제품 목록을 불러오지 못했습니다"
          description={(error as Error)?.message}
          action={<Button onClick={() => refetch()}>재시도</Button>}
        />
      </div>
    );
  }

  return (
    <div style={{ padding: 16 }}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
        <Select
          style={{ width: 140 }}
          value={category1Input}
          options={[{ value: '', label: '카테고리1 전체' }, ...category1Options]}
          onChange={handleCategory1Change}
        />
        <Select
          style={{ width: 140 }}
          value={category2Input}
          options={[{ value: '', label: '카테고리2 전체' }, ...category2Options]}
          disabled={!category1Input}
          onChange={handleCategory2Change}
        />
        <Select
          style={{ width: 140 }}
          value={category3Input}
          options={[{ value: '', label: '카테고리3 전체' }, ...category3Options]}
          disabled={!category2Input}
          onChange={setCategory3Input}
        />
        <Select
          style={{ width: 140 }}
          value={productStatusInput}
          options={STATUS_OPTIONS}
          onChange={setProductStatusInput}
        />
        <Input
          placeholder="제품코드/제품명/바코드 검색"
          allowClear
          value={keywordInput}
          style={{ width: 280 }}
          onChange={(e) => setKeywordInput(e.target.value)}
          onPressEnter={handleSearch}
        />
        <Button type="primary" onClick={handleSearch}>
          조회
        </Button>
      </div>

      <Space style={{ marginBottom: 12 }}>
        <RefreshButton onRefresh={refetch} refreshing={isFetching} />
        <Button
          type="primary"
          disabled={selectedRowKeys.length === 0}
          onClick={handleOpenInventory}
        >
          재고조회 ({selectedRowKeys.length})
        </Button>
        <Button
          disabled={selectedRowKeys.length === 0}
          onClick={handleOpenCompare}
        >
          선택제품 보기 ({selectedRowKeys.length})
        </Button>
        <Button
          disabled={selectedRowKeys.length === 0}
          loading={downloading}
          onClick={handleDownloadExcel}
        >
          엑셀변환
        </Button>
      </Space>

      <ResizableTable
        rowKey="productCode"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        locale={{ emptyText: '검색 결과가 없습니다' }}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
          preserveSelectedRowKeys: true,
        }}
        pagination={{
          current: page + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
        scroll={{ x: 1500 }}
      />

      <InventorySearchModal
        open={inventoryModalOpen}
        onClose={() => setInventoryModalOpen(false)}
      />

      <SelectedProductsCompareModal
        open={compareModalOpen}
        onClose={() => setCompareModalOpen(false)}
        products={selectedProducts}
      />
    </div>
  );
}
