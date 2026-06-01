import { useState } from 'react';
import { Button, Card, DatePicker, List, Tag, Typography } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import { fetchInspections } from '@/api/inspections';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

const { RangePicker } = DatePicker;

export default function InspectionListPage() {
  const navigate = useNavigate();
  const [range, setRange] = useState<[Dayjs, Dayjs]>([dayjs().startOf('month'), dayjs()]);
  const fromDate = range[0].format('YYYY-MM-DD');
  const toDate = range[1].format('YYYY-MM-DD');

  const query = useQuery({
    queryKey: ['inspections', fromDate, toDate],
    queryFn: () => fetchInspections({ fromDate, toDate }),
  });

  return (
    <>
      <DetailHeader
        title="현장 점검"
        extra={
          <Button type="link" icon={<PlusOutlined />} onClick={() => navigate('/inspections/new')}>
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
        emptyDescription="점검 내역이 없습니다"
      >
        {(items) => (
          <List
            dataSource={items}
            split={false}
            renderItem={(it) => (
              <Card
                size="small"
                style={{ marginBottom: 10, cursor: 'pointer' }}
                styles={{ body: { padding: 14 } }}
                onClick={() => navigate(`/inspections/${it.id}`)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <div style={{ minWidth: 0 }}>
                    <Typography.Text strong ellipsis style={{ display: 'block' }}>
                      {it.accountName}
                    </Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                      {formatDate(it.inspectionDate)}
                    </Typography.Text>
                  </div>
                  <Tag color="blue">{it.fieldType}</Tag>
                </div>
              </Card>
            )}
          />
        )}
      </QueryBoundary>
    </>
  );
}
