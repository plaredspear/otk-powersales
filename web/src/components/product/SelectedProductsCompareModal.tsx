import { Modal, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import ResizableTable from '@/components/common/ResizableTable';
import type { Product } from '@/api/product';

interface Props {
  open: boolean;
  onClose: () => void;
  products: Product[];
}

const STATUS_TAG: Record<string, string> = {
  판매중: 'green',
  단종: 'red',
  출고중지: 'red',
};

/**
 * UC-05: 선택제품 비교 보기 모달.
 *
 * 레거시 SelectProductViewPage 의 FieldSet 동적 컬럼 대신 명시 컬럼 채택.
 * 엑셀 다운로드는 ProductPage 의 [엑셀변환] 버튼이 별도로 담당.
 */
export default function SelectedProductsCompareModal({ open, onClose, products }: Props) {
  const columns: ColumnsType<Product> = [
    {
      title: '#',
      width: 50,
      align: 'center',
      render: (_: unknown, __: Product, idx: number) => idx + 1,
    },
    { title: '제품코드', dataIndex: 'productCode', width: 110, render: (v: string | null) => v ?? '-' },
    { title: '제품명', dataIndex: 'name', ellipsis: true, render: (v: string | null) => v ?? '-' },
    { title: '카테고리1', dataIndex: 'category1', width: 100, render: (v: string | null) => v ?? '-' },
    { title: '카테고리2', dataIndex: 'category2', width: 100, render: (v: string | null) => v ?? '-' },
    { title: '카테고리3', dataIndex: 'category3', width: 100, render: (v: string | null) => v ?? '-' },
    { title: '보관방법', dataIndex: 'storageCondition', width: 90, render: (v: string | null) => v ?? '-' },
    { title: '단위', dataIndex: 'unit', width: 60, align: 'center', render: (v: string | null) => v ?? '-' },
    { title: '출시일', dataIndex: 'launchDate', width: 110, render: (v: string | null) => v ?? '-' },
    {
      title: '표준가(원)',
      dataIndex: 'standardUnitPrice',
      width: 100,
      align: 'right',
      render: (v: number | null) => (v != null ? v.toLocaleString() : '-'),
    },
    {
      title: '상태',
      dataIndex: 'productStatus',
      width: 80,
      align: 'center',
      render: (v: string | null) => (v ? <Tag color={STATUS_TAG[v] ?? undefined}>{v}</Tag> : '-'),
    },
  ];

  return (
    <Modal
      title={`선택제품 보기 (${products.length}건)`}
      open={open}
      width={1100}
      onCancel={onClose}
      footer={null}
    >
      <ResizableTable
        rowKey="productCode"
        size="small"
        columns={columns}
        dataSource={products}
        pagination={false}
        locale={{ emptyText: '선택된 제품이 없습니다' }}
        scroll={{ x: 1000 }}
      />
    </Modal>
  );
}
