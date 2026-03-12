import { useMemo, useState } from 'react';
import { Alert, Button, Input, Select, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useProducts, useProductCategories } from '@/hooks/product/useProducts';
import type { Product } from '@/api/product';

const STATUS_TAG: Record<string, string> = {
  판매중: 'green',
  단종: 'red',
};

const STATUS_OPTIONS = [
  { value: '', label: '상태 전체' },
  { value: '판매중', label: '판매중' },
  { value: '단종', label: '단종' },
];

const PAGE_SIZE = 20;

export default function ProductPage() {
  const [keyword, setKeyword] = useState<string | undefined>();
  const [category1, setCategory1] = useState<string | undefined>();
  const [category2, setCategory2] = useState<string | undefined>();
  const [category3, setCategory3] = useState<string | undefined>();
  const [productStatus, setProductStatus] = useState<string | undefined>();
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch } = useProducts({
    keyword,
    category1,
    category2,
    category3,
    productStatus,
    page,
    size: PAGE_SIZE,
  });

  const { data: categories } = useProductCategories();

  const category1Options = useMemo(() => {
    if (!categories) return [];
    return categories.map((c) => ({ value: c.category1, label: c.category1 }));
  }, [categories]);

  const category2Options = useMemo(() => {
    if (!categories || !category1) return [];
    const node = categories.find((c) => c.category1 === category1);
    if (!node) return [];
    return node.children.map((c) => ({ value: c.category2, label: c.category2 }));
  }, [categories, category1]);

  const category3Options = useMemo(() => {
    if (!categories || !category1 || !category2) return [];
    const node1 = categories.find((c) => c.category1 === category1);
    if (!node1) return [];
    const node2 = node1.children.find((c) => c.category2 === category2);
    if (!node2) return [];
    return node2.children.map((v) => ({ value: v, label: v }));
  }, [categories, category1, category2]);

  const handleCategory1Change = (val: string) => {
    setCategory1(val || undefined);
    setCategory2(undefined);
    setCategory3(undefined);
    setPage(0);
  };

  const handleCategory2Change = (val: string) => {
    setCategory2(val || undefined);
    setCategory3(undefined);
    setPage(0);
  };

  const columns: ColumnsType<Product> = [
    { title: '제품코드', dataIndex: 'productCode', width: 110, render: (val: string | null) => val ?? '-' },
    { title: '제품명', dataIndex: 'name', width: 200, ellipsis: true, render: (val: string | null) => val ?? '-' },
    { title: '카테고리1', dataIndex: 'category1', width: 100, render: (val: string | null) => val ?? '-' },
    { title: '카테고리2', dataIndex: 'category2', width: 100, render: (val: string | null) => val ?? '-' },
    { title: '카테고리3', dataIndex: 'category3', width: 100, render: (val: string | null) => val ?? '-' },
    {
      title: '표준가격',
      dataIndex: 'standardPrice',
      width: 100,
      align: 'right',
      render: (val: number | null) => (val != null ? val.toLocaleString() : '-'),
    },
    { title: '단위', dataIndex: 'unit', width: 60, align: 'center', render: (val: string | null) => val ?? '-' },
    {
      title: '상태',
      dataIndex: 'productStatus',
      width: 80,
      align: 'center',
      render: (val: string | null) =>
        val ? <Tag color={STATUS_TAG[val] ?? undefined}>{val}</Tag> : '-',
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
          value={category1 ?? ''}
          options={[{ value: '', label: '카테고리1 전체' }, ...category1Options]}
          onChange={handleCategory1Change}
        />
        <Select
          style={{ width: 140 }}
          value={category2 ?? ''}
          options={[{ value: '', label: '카테고리2 전체' }, ...category2Options]}
          disabled={!category1}
          onChange={handleCategory2Change}
        />
        <Select
          style={{ width: 140 }}
          value={category3 ?? ''}
          options={[{ value: '', label: '카테고리3 전체' }, ...category3Options]}
          disabled={!category2}
          onChange={(val) => { setCategory3(val || undefined); setPage(0); }}
        />
        <Select
          style={{ width: 140 }}
          value={productStatus ?? ''}
          options={STATUS_OPTIONS}
          onChange={(val) => { setProductStatus(val || undefined); setPage(0); }}
        />
        <Input.Search
          placeholder="제품코드/제품명/바코드 검색"
          allowClear
          style={{ width: 280 }}
          onSearch={(val) => { setKeyword(val || undefined); setPage(0); }}
        />
      </div>

      <Table
        rowKey="productCode"
        columns={columns}
        dataSource={data?.content}
        loading={isLoading}
        locale={{ emptyText: '검색 결과가 없습니다' }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          total: data?.totalElements ?? 0,
          pageSize: PAGE_SIZE,
          showSizeChanger: false,
          showTotal: (total) => `총 ${total}건`,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </div>
  );
}
