import { Card, Descriptions, Image, Tag, Typography, Space } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchProductDetail } from '@/api/products';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatWon } from '@/lib/format';

export default function ProductDetailPage() {
  const { productCode = '' } = useParams();

  const query = useQuery({
    queryKey: ['product', productCode],
    queryFn: () => fetchProductDetail(productCode),
    enabled: !!productCode,
  });

  return (
    <>
      <DetailHeader title="제품 상세" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(p) => (
          <Card styles={{ body: { padding: 16 } }}>
            {p.imgRefPathFront && (
              <Image.PreviewGroup>
                <Space style={{ width: '100%', justifyContent: 'center', marginBottom: 12 }}>
                  <Image src={p.imgRefPathFront} height={180} style={{ borderRadius: 8 }} />
                  {p.imgRefPathBack && (
                    <Image src={p.imgRefPathBack} height={180} style={{ borderRadius: 8 }} />
                  )}
                </Space>
              </Image.PreviewGroup>
            )}
            <Typography.Title level={4} style={{ marginBottom: 4 }}>
              {p.name ?? '-'}
            </Typography.Title>
            <Space size={6} wrap style={{ marginBottom: 12 }}>
              {p.category1 && <Tag color="purple">{p.category1}</Tag>}
              {p.category2 && <Tag>{p.category2}</Tag>}
              {p.storageCondition && <Tag color="blue">{p.storageCondition}</Tag>}
            </Space>
            <Descriptions column={1} size="small" bordered>
              <Descriptions.Item label="제품코드">{p.productCode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="바코드">{p.barcode ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="규격/단위">{p.unit ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="표준단가">{formatWon(p.standardUnitPrice)}</Descriptions.Item>
              <Descriptions.Item label="유통기한">
                {p.shelfLife ? `${p.shelfLife}${p.shelfLifeUnit ?? ''}` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="출시일">{p.launchDate ?? '-'}</Descriptions.Item>
              {p.allergen && <Descriptions.Item label="알레르기">{p.allergen}</Descriptions.Item>}
              {p.sellingPoint && (
                <Descriptions.Item label="셀링포인트">{p.sellingPoint}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>
        )}
      </QueryBoundary>
    </>
  );
}
