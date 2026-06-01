import { Card, Descriptions, List, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchClientOrderDetail } from '@/api/orders';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatWon } from '@/lib/format';

/** 거래처별 출하 주문 상세 (레거시 order/client/view). */
export default function ClientOrderDetailPage() {
  const { sapOrderNumber = '' } = useParams();

  const query = useQuery({
    queryKey: ['client-order', sapOrderNumber],
    queryFn: () => fetchClientOrderDetail(sapOrderNumber),
    enabled: !!sapOrderNumber,
  });

  return (
    <>
      <DetailHeader title="거래처 주문 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(o) => (
          <>
            <Card size="small" style={{ marginBottom: 12 }}>
              <Descriptions column={1} size="small">
                <Descriptions.Item label="거래처">{o.sapAccountName ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="주문번호">{o.sapOrderNumber}</Descriptions.Item>
                <Descriptions.Item label="주문일">{formatDate(o.orderDate)}</Descriptions.Item>
                <Descriptions.Item label="납품일">{formatDate(o.deliveryDate)}</Descriptions.Item>
                <Descriptions.Item label="승인금액">{formatWon(o.totalApprovedAmount)}</Descriptions.Item>
                <Descriptions.Item label="마감시각">{o.clientDeadlineTime ?? '-'}</Descriptions.Item>
              </Descriptions>
            </Card>
            <Card size="small" title={`출하 품목 (${o.orderedItemCount})`}>
              <List
                dataSource={o.orderedItems}
                renderItem={(it) => (
                  <List.Item>
                    <List.Item.Meta
                      title={it.productName ?? it.productCode}
                      description={
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {it.deliveredQuantity}
                        </Typography.Text>
                      }
                    />
                    <Tag>{it.deliveryStatus}</Tag>
                  </List.Item>
                )}
              />
            </Card>
          </>
        )}
      </QueryBoundary>
    </>
  );
}
