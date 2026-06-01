import { Button, Card, List, Tag, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchMyOrderRequests } from '@/api/orders';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatWon } from '@/lib/format';

export default function OrderListPage() {
  const navigate = useNavigate();
  const query = useQuery({ queryKey: ['orders'], queryFn: () => fetchMyOrderRequests() });

  return (
    <>
      <DetailHeader
        title="주문 현황"
        extra={
          <Button type="link" icon={<PlusOutlined />} onClick={() => navigate('/orders/new')}>
            작성
          </Button>
        }
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.items.length === 0}
        emptyDescription="주문 내역이 없습니다"
      >
        {(data) => (
          <List
            dataSource={data.items}
            split={false}
            renderItem={(o) => (
              <Card
                size="small"
                style={{ marginBottom: 10, cursor: 'pointer' }}
                styles={{ body: { padding: 14 } }}
                onClick={() => navigate(`/orders/${o.id}`)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong ellipsis style={{ display: 'block' }}>
                      {o.clientName}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {o.orderRequestNumber}
                    </Typography.Text>
                    <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                      납품 {formatDate(o.deliveryDate)} · {formatWon(o.totalAmount)}
                    </div>
                  </div>
                  <Tag color={o.isClosed ? 'default' : 'blue'}>{o.orderRequestStatus}</Tag>
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
