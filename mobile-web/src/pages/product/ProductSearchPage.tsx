import { useState } from 'react';
import { Card, Input, List, Tag, Typography, Button, Space, App as AntdApp } from 'antd';
import { BarcodeOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { searchProducts } from '@/api/products';
import { detectBarcodeFromFile, isBarcodeDetectorSupported } from '@/lib/device';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';

/**
 * 제품 검색 (레거시 product/search/search + result 통합).
 * 텍스트/바코드 검색. 바코드는 BarcodeDetector(지원 시 사진→디코드), 미지원 시 수동 입력.
 */
export default function ProductSearchPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const [query, setQuery] = useState('');
  const [type, setType] = useState<'text' | 'barcode'>('text');

  const search = useQuery({
    queryKey: ['product-search', query, type],
    queryFn: () => searchProducts(query, type),
    enabled: query.length > 0,
  });

  const onPickBarcodeImage = async (file: File | undefined) => {
    if (!file) return;
    const code = await detectBarcodeFromFile(file);
    if (code) {
      setType('barcode');
      setQuery(code);
    } else {
      message.warning('바코드를 인식하지 못했습니다. 직접 입력해 주세요.');
    }
  };

  return (
    <>
      <DetailHeader title="제품 검색" />
      <Space.Compact style={{ width: '100%', marginBottom: 12 }}>
        <Input
          placeholder="제품명 / 제품코드 / 바코드"
          allowClear
          value={query}
          onChange={(e) => {
            setType('text');
            setQuery(e.target.value);
          }}
          onPressEnter={() => search.refetch()}
        />
        <Button
          icon={<BarcodeOutlined />}
          onClick={() => document.getElementById('mw-barcode-file')?.click()}
        >
          스캔
        </Button>
      </Space.Compact>
      <input
        id="mw-barcode-file"
        type="file"
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
        onChange={(e) => onPickBarcodeImage(e.target.files?.[0])}
      />
      {!isBarcodeDetectorSupported() && (
        <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
          이 기기는 웹 바코드 인식을 지원하지 않습니다. 코드를 직접 입력하거나 Capacitor 앱에서
          스캔하세요.
        </Typography.Paragraph>
      )}

      {query.length === 0 ? (
        <Typography.Text type="secondary">검색어를 입력하세요.</Typography.Text>
      ) : (
        <QueryBoundary
          isLoading={search.isLoading}
          isError={search.isError}
          data={search.data}
          onRetry={search.refetch}
          isEmpty={(d) => d.content.length === 0}
          emptyDescription="검색 결과가 없습니다"
        >
          {(data) => (
            <List
              dataSource={data.content}
              split={false}
              renderItem={(p) => (
                <Card
                  size="small"
                  style={{ marginBottom: 10, cursor: 'pointer' }}
                  styles={{ body: { padding: 14 } }}
                  onClick={() => p.productCode && navigate(`/products/${p.productCode}`)}
                >
                  <Typography.Text strong ellipsis style={{ display: 'block' }}>
                    {p.productName ?? '-'}
                  </Typography.Text>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {p.productCode}
                  </Typography.Text>
                  <Space size={4} wrap style={{ marginTop: 4 }}>
                    {p.category1 && <Tag color="purple">{p.category1}</Tag>}
                    {p.storageCondition && <Tag color="blue">{p.storageCondition}</Tag>}
                  </Space>
                </Card>
              )}
            />
          )}
        </QueryBoundary>
      )}
    </>
  );
}
