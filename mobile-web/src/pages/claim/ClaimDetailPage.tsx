import { Card, Descriptions, Image, Space, Tag, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchClaimDetail } from '@/api/claims';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate, formatDateTime, formatWon, formatNumber } from '@/lib/format';

export default function ClaimDetailPage() {
  const { id } = useParams();
  const claimId = Number(id);

  const query = useQuery({
    queryKey: ['claim', claimId],
    queryFn: () => fetchClaimDetail(claimId),
    enabled: Number.isFinite(claimId) && claimId > 0,
  });

  return (
    <>
      <DetailHeader title="클레임 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(c) => (
          <Card styles={{ body: { padding: 16 } }}>
            <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 8 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                {c.productName ?? '제품 미지정'}
              </Typography.Title>
              <Tag color="blue">{c.statusLabel}</Tag>
            </Space>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="거래처">{c.accountName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="제품코드">{c.productCode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="클레임 종류">
                {[c.categoryLabel, c.subcategoryLabel].filter(Boolean).join(' / ') || '-'}
              </Descriptions.Item>
              {c.dateTypeLabel && (
                <Descriptions.Item label={c.dateTypeLabel}>{formatDate(c.date)}</Descriptions.Item>
              )}
              <Descriptions.Item label="불량 수량">{formatNumber(c.defectQuantity)}</Descriptions.Item>
              <Descriptions.Item label="구매 금액">{formatWon(c.purchaseAmount)}</Descriptions.Item>
              <Descriptions.Item label="구매 방법">{c.purchaseMethodName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="요청 사항">{c.requestTypeName ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="불량 내용">{c.defectDescription ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="등록일시">{formatDateTime(c.createdAt)}</Descriptions.Item>
            </Descriptions>
            {c.photos?.length > 0 && (
              <Image.PreviewGroup>
                <Space wrap style={{ marginTop: 12 }}>
                  {c.photos.map((photo) => (
                    <Image
                      key={photo.photoId}
                      src={photo.url}
                      width={100}
                      height={100}
                      style={{ objectFit: 'cover', borderRadius: 8 }}
                    />
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
