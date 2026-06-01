import { Card, Divider, Image, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchNoticeDetail } from '@/api/notices';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDateTime } from '@/lib/format';
import { sanitizeHtml } from '@/lib/html';

export default function NoticeDetailPage() {
  const { id } = useParams();
  const noticeId = Number(id);

  const query = useQuery({
    queryKey: ['notice', noticeId],
    queryFn: () => fetchNoticeDetail(noticeId),
    enabled: Number.isFinite(noticeId) && noticeId > 0,
  });

  return (
    <>
      <DetailHeader title="공지사항" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(notice) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Space size={6} wrap>
              <Tag color="blue">{notice.categoryName}</Tag>
              {notice.branch && <Tag>{notice.branch}</Tag>}
            </Space>
            <Typography.Title level={4} style={{ margin: '10px 0 4px' }}>
              {notice.title}
            </Typography.Title>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {formatDateTime(notice.createdAt)}
            </Typography.Text>
            <Divider style={{ margin: '12px 0' }} />
            <div
              className="mw-rich-content"
              dangerouslySetInnerHTML={{ __html: sanitizeHtml(notice.content) }}
            />
            {notice.images?.length > 0 && (
              <Image.PreviewGroup>
                <Space direction="vertical" style={{ width: '100%', marginTop: 12 }}>
                  {notice.images.map((img) => (
                    <Image key={img.id} src={img.url} style={{ borderRadius: 8 }} />
                  ))}
                </Space>
              </Image.PreviewGroup>
            )}
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
