import { Card, Divider, List, Tag, Typography, Button } from 'antd';
import { DownloadOutlined, FileOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchEducationDetail, type EducationAttachment } from '@/api/education';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDateTime } from '@/lib/format';
import { sanitizeHtml } from '@/lib/html';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function AttachmentItem({ file }: { file: EducationAttachment }) {
  return (
    <List.Item
      actions={[
        <Button
          key="dl"
          type="link"
          icon={<DownloadOutlined />}
          href={file.fileUrl}
          target="_blank"
          rel="noopener noreferrer"
        >
          받기
        </Button>,
      ]}
    >
      <List.Item.Meta
        avatar={<FileOutlined style={{ fontSize: 20, color: '#1677ff' }} />}
        title={file.fileName}
        description={formatFileSize(file.fileSize)}
      />
    </List.Item>
  );
}

export default function EducationDetailPage() {
  const { id = '' } = useParams();

  const query = useQuery({
    queryKey: ['education-detail', id],
    queryFn: () => fetchEducationDetail(id),
    enabled: !!id,
  });

  return (
    <>
      <DetailHeader title="교육자료" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(edu) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Tag color="green">{edu.categoryName}</Tag>
            <Typography.Title level={4} style={{ margin: '10px 0 4px' }}>
              {edu.title}
            </Typography.Title>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {formatDateTime(edu.createdAt)}
            </Typography.Text>
            <Divider style={{ margin: '12px 0' }} />
            <div
              className="mw-rich-content"
              dangerouslySetInnerHTML={{ __html: sanitizeHtml(edu.content) }}
            />
            {edu.attachments?.length > 0 && (
              <>
                <Divider orientation="left" style={{ marginTop: 16 }}>
                  첨부파일
                </Divider>
                <List
                  dataSource={edu.attachments}
                  renderItem={(file) => <AttachmentItem file={file} />}
                />
              </>
            )}
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
