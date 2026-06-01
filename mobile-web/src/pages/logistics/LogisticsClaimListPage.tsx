import { Card, List, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchLogisticsClaims } from '@/api/suggestions';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

export default function LogisticsClaimListPage() {
  const navigate = useNavigate();

  const query = useQuery({
    queryKey: ['logistics-claims'],
    queryFn: () => fetchLogisticsClaims(),
  });

  return (
    <>
      <DetailHeader title="물류클레임" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.length === 0}
        emptyDescription="물류클레임이 없습니다"
      >
        {(items) => (
          <List
            dataSource={items}
            split={false}
            renderItem={(item) => (
              <Card
                size="small"
                style={{ marginBottom: 10, cursor: 'pointer' }}
                styles={{ body: { padding: 14 } }}
                onClick={() => navigate(`/logistics-claims/${item.id}`)}
              >
                <div style={{ display: 'flex', gap: 6, marginBottom: 4 }}>
                  <Tag color="cyan">{item.categoryName}</Tag>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {item.proposalNumber}
                  </Typography.Text>
                </div>
                <Typography.Text strong ellipsis style={{ display: 'block' }}>
                  {item.title}
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {formatDate(item.createdAt)}
                </Typography.Text>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
