import { Button, Card, Descriptions, List, Popconfirm, Tag, Typography, App as AntdApp } from 'antd';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchOrderRequestDetail, cancelOrderRequest } from '@/api/orders';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatWon, formatNumber } from '@/lib/format';

export default function OrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['order', orderId],
    queryFn: () => fetchOrderRequestDetail(orderId),
    enabled: Number.isFinite(orderId) && orderId > 0,
  });

  const cancelMutation = useMutation({
    mutationFn: () => cancelOrderRequest(orderId),
    onSuccess: () => {
      message.success('주문이 취소되었습니다');
      queryClient.invalidateQueries({ queryKey: ['order', orderId] });
      queryClient.invalidateQueries({ queryKey: ['orders'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '취소에 실패했습니다'),
  });

  return (
    <>
      <DetailHeader title="주문 상세" />
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
                <Descriptions.Item label="거래처">{o.clientName}</Descriptions.Item>
                <Descriptions.Item label="주문번호">{o.orderRequestNumber}</Descriptions.Item>
                <Descriptions.Item label="주문일">{formatDate(o.orderDate)}</Descriptions.Item>
                <Descriptions.Item label="납품일">{formatDate(o.deliveryDate)}</Descriptions.Item>
                <Descriptions.Item label="주문금액">{formatWon(o.totalAmount)}</Descriptions.Item>
                <Descriptions.Item label="승인금액">{formatWon(o.totalApprovedAmount)}</Descriptions.Item>
                <Descriptions.Item label="상태">
                  <Tag color={o.isClosed ? 'default' : 'blue'}>{o.orderRequestStatus}</Tag>
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Card size="small" title={`주문 품목 (${o.orderedItemCount})`} style={{ marginBottom: 12 }}>
              <List
                dataSource={o.orderedItems}
                renderItem={(it) => (
                  <List.Item>
                    <List.Item.Meta
                      title={
                        <span style={it.isCancelled ? { textDecoration: 'line-through', color: '#bbb' } : undefined}>
                          {it.productName ?? it.productCode}
                        </span>
                      }
                      description={
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {formatNumber(it.totalQuantityBoxes)} BOX / {formatNumber(it.totalQuantityPieces)} EA
                        </Typography.Text>
                      }
                    />
                    {it.isCancelled && <Tag color="red">취소</Tag>}
                  </List.Item>
                )}
              />
            </Card>

            {!o.isClosed && (
              <Popconfirm
                title="주문을 취소하시겠습니까?"
                okText="주문 취소"
                cancelText="닫기"
                onConfirm={() => cancelMutation.mutate()}
              >
                <Button danger block loading={cancelMutation.isPending}>
                  주문 취소
                </Button>
              </Popconfirm>
            )}
            <Button type="link" block onClick={() => navigate('/orders')} style={{ marginTop: 8 }}>
              목록으로
            </Button>
          </>
        )}
      </QueryBoundary>
    </>
  );
}
