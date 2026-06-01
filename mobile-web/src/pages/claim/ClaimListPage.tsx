import { List, Tag, Typography, Card, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchClaims } from '@/api/claims';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

function statusColor(status: string): string {
  switch (status) {
    case 'COMPLETED':
    case 'APPROVED':
      return 'green';
    case 'REJECTED':
    case 'CANCELED':
      return 'red';
    default:
      return 'blue';
  }
}

export default function ClaimListPage() {
  const navigate = useNavigate();

  const query = useQuery({
    queryKey: ['claims'],
    queryFn: () => fetchClaims(),
  });

  return (
    <>
      <Button
        type="primary"
        block
        icon={<PlusOutlined />}
        style={{ marginBottom: 12 }}
        onClick={() => navigate('/claims/new')}
      >
        클레임 등록
      </Button>
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.length === 0}
        emptyDescription="등록된 클레임이 없습니다"
      >
      {(claims) => (
        <List
          dataSource={claims}
          split={false}
          renderItem={(claim) => (
            <Card
              size="small"
              style={{ marginBottom: 10, cursor: 'pointer' }}
              styles={{ body: { padding: 14 } }}
              onClick={() => navigate(`/claims/${claim.claimId}`)}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                <div style={{ minWidth: 0 }}>
                  <Typography.Text strong ellipsis style={{ display: 'block' }}>
                    {claim.productName ?? '제품 미지정'}
                  </Typography.Text>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {claim.accountName ?? '-'} · {claim.categoryLabel ?? '-'}
                  </Typography.Text>
                  <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                    {formatDate(claim.createdAt)}
                  </div>
                </div>
                <Tag color={statusColor(claim.status)}>{claim.statusLabel}</Tag>
              </div>
            </Card>
          )}
        />
      )}
      </QueryBoundary>
    </>
  );
}
