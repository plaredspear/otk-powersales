import { useState } from 'react';
import { Input, List, Typography } from 'antd';
import { RightOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchEducationPosts, EDUCATION_CATEGORIES } from '@/api/education';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

export default function EducationListPage() {
  const { category = '' } = useParams();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const categoryName =
    EDUCATION_CATEGORIES.find((c) => c.code === category)?.name ?? '교육자료';

  const query = useQuery({
    queryKey: ['education', category, search],
    queryFn: () => fetchEducationPosts({ category, search: search || undefined, size: 30 }),
    enabled: !!category,
  });

  return (
    <>
      <DetailHeader title={categoryName} />
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
        emptyDescription="교육 자료가 없습니다"
      >
        {(data) => (
          <List
            style={{ background: '#fff', borderRadius: 12, padding: '0 12px' }}
            dataSource={data.content}
            renderItem={(item) => (
              <List.Item
                style={{ cursor: 'pointer' }}
                onClick={() => navigate(`/education/${item.id}`)}
                extra={<RightOutlined style={{ color: '#bfbfbf' }} />}
              >
                <List.Item.Meta
                  title={item.title}
                  description={
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {formatDate(item.createdAt)}
                    </Typography.Text>
                  }
                />
              </List.Item>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
