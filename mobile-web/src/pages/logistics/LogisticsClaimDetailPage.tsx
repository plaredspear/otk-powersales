import { Card, Descriptions, Divider, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchSuggestionDetail } from '@/api/suggestions';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatDateTime } from '@/lib/format';

export default function LogisticsClaimDetailPage() {
  const { id } = useParams();
  const suggestionId = Number(id);

  const query = useQuery({
    queryKey: ['logistics-claim', suggestionId],
    queryFn: () => fetchSuggestionDetail(suggestionId),
    enabled: Number.isFinite(suggestionId) && suggestionId > 0,
  });

  return (
    <>
      <DetailHeader title="물류클레임 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(s) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Tag color="cyan">{s.categoryName}</Tag>
            <Typography.Title level={4} style={{ margin: '10px 0 4px' }}>
              {s.title}
            </Typography.Title>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {s.proposalNumber} · {formatDateTime(s.createdAt)}
            </Typography.Text>
            <Divider style={{ margin: '12px 0' }} />
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
              {s.content}
            </Typography.Paragraph>
            <Descriptions column={1} size="small" bordered>
              {s.claimType && <Descriptions.Item label="클레임 유형">{s.claimType}</Descriptions.Item>}
              {s.claimDate && (
                <Descriptions.Item label="클레임 일자">{formatDate(s.claimDate)}</Descriptions.Item>
              )}
              {s.carNumber && <Descriptions.Item label="차량번호">{s.carNumber}</Descriptions.Item>}
              {s.receptionLogisticsCenter && (
                <Descriptions.Item label="접수 물류센터">
                  {s.receptionLogisticsCenter}
                </Descriptions.Item>
              )}
              {s.responsibleLogisticsCenter && (
                <Descriptions.Item label="담당 물류센터">
                  {s.responsibleLogisticsCenter}
                </Descriptions.Item>
              )}
              {s.logisticsResponsibility && (
                <Descriptions.Item label="물류 귀책">{s.logisticsResponsibility}</Descriptions.Item>
              )}
              {s.actionStatus && (
                <Descriptions.Item label="조치 상태">{s.actionStatus}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
