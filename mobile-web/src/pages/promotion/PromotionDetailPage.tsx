import { Button, Card, Descriptions, List, Tag, Typography, Space } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchPromotionDetail } from '@/api/promotions';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatWon } from '@/lib/format';

export default function PromotionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const promotionId = Number(id);

  const query = useQuery({
    queryKey: ['promotion', promotionId],
    queryFn: () => fetchPromotionDetail(promotionId),
    enabled: Number.isFinite(promotionId) && promotionId > 0,
  });

  return (
    <>
      <DetailHeader title="행사 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(p) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 8 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {p.accountName ?? '거래처 미지정'}
              </Typography.Title>
              <Tag color={p.isClosed ? 'default' : 'green'}>{p.isClosed ? '마감' : '진행중'}</Tag>
            </Space>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="행사번호">{p.promotionNumber}</Descriptions.Item>
              <Descriptions.Item label="행사유형">{p.promotionType ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="기간">
                {formatDate(p.startDate)} ~ {formatDate(p.endDate)}
              </Descriptions.Item>
              <Descriptions.Item label="진열위치">{p.standLocation ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="대표제품">{p.primaryProductName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="기타제품">{p.otherProduct ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="제품유형">{p.productType ?? '-'}</Descriptions.Item>
              {p.message && <Descriptions.Item label="메시지">{p.message}</Descriptions.Item>}
              {p.remark && <Descriptions.Item label="비고">{p.remark}</Descriptions.Item>}
            </Descriptions>
            {p.employees?.length > 0 && (
              <>
                <Typography.Title level={5} style={{ marginTop: 16 }}>
                  투입 여사원 ({p.employees.length})
                </Typography.Title>
                <List
                  dataSource={p.employees}
                  renderItem={(emp) => (
                    <List.Item
                      actions={
                        emp.isMine
                          ? [
                              <Button
                                key="ds"
                                type="link"
                                onClick={() => navigate(`/daily-sales/${emp.id}`)}
                              >
                                {emp.isClosed ? '일매출 보기' : '일매출 등록'}
                              </Button>,
                            ]
                          : undefined
                      }
                    >
                      <List.Item.Meta
                        title={
                          <span>
                            {emp.employeeName ?? '-'}{' '}
                            {emp.isMine && <Tag color="blue">본인</Tag>}
                            {emp.isClosed && <Tag color="green">마감</Tag>}
                          </span>
                        }
                        description={
                          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                            실적 {formatWon(emp.actualAmount)}
                          </Typography.Text>
                        }
                      />
                    </List.Item>
                  )}
                />
              </>
            )}
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
