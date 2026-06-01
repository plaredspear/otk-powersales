import { useState } from 'react';
import { Input, List, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchNotices } from '@/api/notices';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

export default function NoticeListPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const query = useQuery({
    queryKey: ['notices', search],
    queryFn: () => fetchNotices({ search: search || undefined, page: 1, size: 30 }),
  });

  return (
    <div>
      <Input.Search
        placeholder="제목 검색"
        allowClear
        onSearch={setSearch}
        style={{ marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.content.length === 0}
        emptyDescription="공지사항이 없습니다"
      >
        {(data) => (
          <List
            style={{ background: '#fff', borderRadius: 12, padding: '0 12px' }}
            dataSource={data.content}
            renderItem={(item) => (
              <List.Item
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/notices/${item.id}`)}
              >
                <div style={{ width: '100%' }}>
                  <div style={{ display: 'flex', gap: 6, marginBottom: 4 }}>
                    <Tag color="blue">{item.categoryName}</Tag>
                    {item.branch && <Tag>{item.branch}</Tag>}
                  </div>
                  <Typography.Text strong ellipsis style={{ display: 'block' }}>
                    {item.title}
                  </Typography.Text>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {item.authorName ?? '-'} · {formatDate(item.createdAt)}
                  </Typography.Text>
                </div>
              </List.Item>
            )}
          />
        )}
      </QueryBoundary>
    </div>
  );
}
