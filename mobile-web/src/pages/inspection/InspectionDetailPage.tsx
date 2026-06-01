import { Card, Descriptions, Image, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchInspectionDetail } from '@/api/inspections';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatDateTime, formatNumber } from '@/lib/format';

export default function InspectionDetailPage() {
  const { id } = useParams();
  const inspectionId = Number(id);

  const query = useQuery({
    queryKey: ['inspection', inspectionId],
    queryFn: () => fetchInspectionDetail(inspectionId),
    enabled: Number.isFinite(inspectionId) && inspectionId > 0,
  });

  return (
    <>
      <DetailHeader title="점검 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(d) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 8 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {d.accountName}
              </Typography.Title>
              <Tag color="blue">{d.fieldType}</Tag>
            </Space>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="테마">{d.themeName}</Descriptions.Item>
              <Descriptions.Item label="점검일">{formatDate(d.inspectionDate)}</Descriptions.Item>
              {d.productName && <Descriptions.Item label="제품">{d.productName}</Descriptions.Item>}
              {d.description && <Descriptions.Item label="내용">{d.description}</Descriptions.Item>}
              {d.competitorName && (
                <Descriptions.Item label="경쟁사">{d.competitorName}</Descriptions.Item>
              )}
              {d.competitorActivity && (
                <Descriptions.Item label="경쟁사 활동">{d.competitorActivity}</Descriptions.Item>
              )}
              {d.competitorProductName && (
                <Descriptions.Item label="경쟁사 제품">
                  {d.competitorProductName}
                  {d.competitorProductPrice != null ? ` (${formatNumber(d.competitorProductPrice)}원)` : ''}
                </Descriptions.Item>
              )}
              {d.competitorSalesQuantity != null && (
                <Descriptions.Item label="경쟁사 판매량">
                  {formatNumber(d.competitorSalesQuantity)}
                </Descriptions.Item>
              )}
              <Descriptions.Item label="등록일시">{formatDateTime(d.createdAt)}</Descriptions.Item>
            </Descriptions>
            {d.photos?.length > 0 && (
              <Image.PreviewGroup>
                <Space wrap style={{ marginTop: 12 }}>
                  {d.photos.map((p) => (
                    <Image key={p.id} src={p.url} width={100} height={100} style={{ objectFit: 'cover', borderRadius: 8 }} />
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
