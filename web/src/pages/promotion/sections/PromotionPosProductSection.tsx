import { useState } from 'react';
import { Button, Dropdown, Popconfirm, Space, Table, Typography, message } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { Link } from 'react-router-dom';
import { usePromotionPosProducts } from '@/hooks/promotion/usePromotionPosProducts';
import { useDeletePromotionPosProduct } from '@/hooks/promotion/usePromotionPosProductMutation';
import type { PromotionPosProduct } from '@/api/promotionPosProduct';
import PromotionPosProductFormModal from '../components/PromotionPosProductFormModal';

const { Title } = Typography;

interface Props {
  promotionId: number;
}

// SF Promotion 상세의 "상세 POS품목 (N)" Related List 동등 (DKRetail__PromotionProduct__c).
export default function PromotionPosProductSection({ promotionId }: Props) {
  const { data, isLoading } = usePromotionPosProducts(promotionId);
  const deleteMutation = useDeletePromotionPosProduct(promotionId);
  const rows = data ?? [];

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<PromotionPosProduct | null>(null);

  const openCreate = () => {
    setEditing(null);
    setModalOpen(true);
  };
  const openEdit = (row: PromotionPosProduct) => {
    setEditing(row);
    setModalOpen(true);
  };
  const handleDelete = async (row: PromotionPosProduct) => {
    try {
      await deleteMutation.mutateAsync(row.id);
      message.success('상세 POS품목을 삭제했습니다');
    } catch (e) {
      message.error(e instanceof Error ? e.message : '삭제에 실패했습니다');
    }
  };

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
    {
      title: '',
      key: 'actions',
      width: 80,
      align: 'center',
      render: (_, r) => (
        <Dropdown
          menu={{
            items: [
              { key: 'edit', label: '편집', onClick: () => openEdit(r) },
              {
                key: 'delete',
                label: (
                  <Popconfirm
                    title="삭제하시겠습니까?"
                    okText="삭제"
                    cancelText="취소"
                    onConfirm={() => handleDelete(r)}
                  >
                    <span>삭제</span>
                  </Popconfirm>
                ),
              },
            ],
          }}
          trigger={['click']}
        >
          <Button size="small" icon={<DownOutlined />} />
        </Dropdown>
      ),
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
        <Button onClick={openCreate}>새로 만들기</Button>
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

      <PromotionPosProductFormModal
        promotionId={promotionId}
        open={modalOpen}
        editing={editing}
        onClose={() => setModalOpen(false)}
      />
    </div>
  );
}
