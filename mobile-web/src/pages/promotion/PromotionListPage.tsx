import { useState } from 'react';
import { Card, Input, List, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchPromotions } from '@/api/promotions';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

export default function PromotionListPage() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState('');

  const query = useQuery({
    queryKey: ['promotions', keyword],
    queryFn: () => fetchPromotions({ keyword: keyword || undefined, size: 30 }),
  });

  return (
    <>
      <DetailHeader title="행사매출" />
      <Input.Search
        placeholder="거래처/행사 검색"
        allowClear
        onSearch={setKeyword}
        style={{ marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.content.length === 0}
        emptyDescription="행사가 없습니다"
      >
        {(data) => (
          <List
            dataSource={data.content}
            split={false}
            renderItem={(item) => (
              <Card
                size="small"
                style={{ marginBottom: 10, cursor: 'pointer' }}
                styles={{ body: { padding: 14 } }}
                onClick={() => navigate(`/promotions/${item.id}`)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong ellipsis style={{ display: 'block' }}>
                      {item.accountName ?? '거래처 미지정'}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {item.promotionType ?? '-'}
                      {item.standLocation ? ` · ${item.standLocation}` : ''}
                    </Typography.Text>
                    <div style={{ fontSize: 12, color: '#999', marginTop: 4 }}>
                      {formatDate(item.startDate)} ~ {formatDate(item.endDate)}
                    </div>
                  </div>
                  <Tag color={item.isClosed ? 'default' : 'green'}>
                    {item.isClosed ? '마감' : '진행중'}
                  </Tag>
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
