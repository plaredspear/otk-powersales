import { useState } from 'react';
import { Button, Card, DatePicker, List, Tag, Typography, Popconfirm, App as AntdApp } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import {
  fetchProductExpirationList,
  deleteProductExpiration,
  type ProductExpirationItem,
} from '@/api/productExpiration';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

const { RangePicker } = DatePicker;

function dDayTag(dDay: number, isExpired: boolean) {
  if (isExpired) return <Tag color="red">만료 (D+{Math.abs(dDay)})</Tag>;
  if (dDay <= 7) return <Tag color="orange">D-{dDay}</Tag>;
  return <Tag color="green">D-{dDay}</Tag>;
}

export default function ProductExpirationListPage() {
  const navigate = useNavigate();
  const { message } = AntdApp.useApp();
  const queryClient = useQueryClient();
  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs(), dayjs().add(3, 'month')]);

  const fromDate = range[0].format('YYYY-MM-DD');
  const toDate = range[1].format('YYYY-MM-DD');

  const query = useQuery({
    queryKey: ['product-expiration', fromDate, toDate],
    queryFn: () => fetchProductExpirationList({ fromDate, toDate }),
  });

  const deleteMutation = useMutation({
    mutationFn: (seq: number) => deleteProductExpiration(seq),
    onSuccess: () => {
      message.success('삭제되었습니다');
      queryClient.invalidateQueries({ queryKey: ['product-expiration'] });
    },
    onError: (e) => message.error(e instanceof Error ? e.message : '삭제에 실패했습니다'),
  });

  const goEdit = (item: ProductExpirationItem) =>
    navigate(`/product-expiration/${item.seq}/edit`, { state: item });

  return (
    <>
      <DetailHeader
        title="유통기한 관리"
        extra={
          <Button
            type="link"
            icon={<PlusOutlined />}
            onClick={() => navigate('/product-expiration/new')}
          >
            등록
          </Button>
        }
      />
      <RangePicker
        value={range}
        allowClear={false}
        onChange={(v) => v && v[0] && v[1] && setRange([v[0], v[1]])}
        style={{ width: '100%', marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
        isEmpty={(d) => d.length === 0}
        emptyDescription="해당 기간 유통기한 항목이 없습니다"
      >
        {(items) => (
          <List
            dataSource={items}
            split={false}
            renderItem={(item) => (
              <Card size="small" style={{ marginBottom: 10 }} styles={{ body: { padding: 14 } }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong ellipsis style={{ display: 'block' }}>
                      {item.productName}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {item.accountName}
                    </Typography.Text>
                    <div style={{ fontSize: 12, color: '#666', marginTop: 4 }}>
                      유통기한 {formatDate(item.expirationDate)}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    {dDayTag(item.dDay, item.isExpired)}
                    <div style={{ marginTop: 6 }}>
                      <Button type="text" size="small" icon={<EditOutlined />} onClick={() => goEdit(item)} />
                      <Popconfirm
                        title="삭제하시겠습니까?"
                        okText="삭제"
                        cancelText="취소"
                        onConfirm={() => deleteMutation.mutate(item.seq)}
                      >
                        <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                      </Popconfirm>
                    </div>
                  </div>
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
