import { Space, Table, Typography } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import { usePromotionPosProducts } from '@/hooks/promotion/usePromotionPosProducts';
import type { PromotionPosProduct } from '@/api/promotionPosProduct';

const { Title } = Typography;

interface Props {
  promotionId: number;
}

// SF Promotion 상세의 "상세 POS품목 (N)" Related List 동등 (DKRetail__PromotionProduct__c).
export default function PromotionPosProductSection({ promotionId }: Props) {
  const { data, isLoading } = usePromotionPosProducts(promotionId);
  const rows = data ?? [];

  const columns: ColumnsType<PromotionPosProduct> = [
    { title: 'Name', dataIndex: 'name', key: 'name', width: 160 },
    {
      title: '제품',
      key: 'product',
      render: (_, r) => {
        const name = r.productName ?? '-';
        if (!r.productCode) return name;
        return (
          <Link to={`/product/${encodeURIComponent(r.productCode)}`}>
            {name}
          </Link>
        );
      },
    },
    {
      title: '금액',
      dataIndex: 'price',
      key: 'price',
      align: 'right',
      width: 160,
      render: (v: number | null) => (v != null ? `${v.toLocaleString()}원` : '-'),
    },
  ];

  return (
    <div style={{ marginTop: 32 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <Space>
          <Title level={5} style={{ margin: 0 }}>
            상세 POS품목
          </Title>
        </Space>
      </div>
      <Table<PromotionPosProduct>
        columns={columns}
        dataSource={rows}
        rowKey="id"
        size="small"
        pagination={false}
        loading={isLoading}
        locale={{ emptyText: '등록된 상세 POS품목이 없습니다' }}
        footer={() => `총 ${rows.length}건`}
      />
    </div>
  );
}
