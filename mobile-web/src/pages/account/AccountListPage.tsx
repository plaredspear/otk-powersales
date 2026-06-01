import { useState } from 'react';
import { Card, Input, List, Typography, Button } from 'antd';
import { PhoneOutlined, EnvironmentOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { fetchMyAccounts } from '@/api/accounts';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';

export default function AccountListPage() {
  const [keyword, setKeyword] = useState('');

  const query = useQuery({
    queryKey: ['my-accounts', keyword],
    queryFn: () => fetchMyAccounts(keyword || undefined),
  });

  return (
    <>
      <DetailHeader title="내 거래처" />
      <Input.Search
        placeholder="거래처명/코드 검색"
        allowClear
        onSearch={setKeyword}
        style={{ marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.stores.length === 0}
        emptyDescription="거래처가 없습니다"
      >
        {(data) => (
          <List
            dataSource={data.stores}
            split={false}
            renderItem={(store) => (
              <Card size="small" style={{ marginBottom: 10 }} styles={{ body: { padding: 14 } }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong style={{ display: 'block' }}>
                      {store.accountName}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {store.accountCode}
                      {store.representativeName ? ` · ${store.representativeName}` : ''}
                    </Typography.Text>
                    {store.address && (
                      <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                        <EnvironmentOutlined /> {store.address}
                        {store.addressDetail ? ` ${store.addressDetail}` : ''}
                      </div>
                    )}
                  </div>
                  {store.phoneNumber && (
                    <Button
                      type="primary"
                      ghost
                      shape="circle"
                      icon={<PhoneOutlined />}
                      href={`tel:${store.phoneNumber}`}
                    />
                  )}
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
