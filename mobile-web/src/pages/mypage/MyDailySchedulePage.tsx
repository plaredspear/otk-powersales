import { Card, List, Progress, Tag, Typography, Space } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { fetchDailySchedule } from '@/api/schedule';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatDate } from '@/lib/format';

export default function MyDailySchedulePage() {
  const { date = '' } = useParams();

  const query = useQuery({
    queryKey: ['my-daily', date],
    queryFn: () => fetchDailySchedule(date),
    enabled: !!date,
  });

  return (
    <>
      <DetailHeader title="일별 현황" />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(d) => {
          const pct =
            d.reportProgress.total > 0
              ? Math.round((d.reportProgress.completed / d.reportProgress.total) * 100)
              : 0;
          return (
            <>
              <Card size="small" style={{ marginBottom: 12 }}>
                <Space style={{ justifyContent: 'space-between', width: '100%' }}>
                  <div>
                    <Typography.Text strong>{formatDate(d.date)}</Typography.Text>{' '}
                    <Typography.Text type="secondary">({d.dayOfWeek})</Typography.Text>
                    <div style={{ fontSize: 12, color: '#666' }}>{d.memberName}</div>
                  </div>
                  {d.workingType && <Tag color="blue">{d.workingType}</Tag>}
                </Space>
                <div style={{ marginTop: 10 }}>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {d.reportProgress.workType} 등록 {d.reportProgress.completed}/
                    {d.reportProgress.total}
                  </Typography.Text>
                  <Progress percent={pct} size="small" />
                </div>
              </Card>

              <List
                dataSource={d.accounts}
                split={false}
                locale={{ emptyText: '거래처 일정이 없습니다' }}
                renderItem={(acc) => (
                  <Card size="small" style={{ marginBottom: 10 }} styles={{ body: { padding: 14 } }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                      <div style={{ minWidth: 0 }}>
                        <Typography.Text strong ellipsis style={{ display: 'block' }}>
                          {acc.accountName}
                        </Typography.Text>
                        <Space size={4} wrap style={{ marginTop: 4 }}>
                          {[acc.workType1, acc.workType2, acc.workType3]
                            .filter(Boolean)
                            .map((w, i) => (
                              <Tag key={i}>{w}</Tag>
                            ))}
                        </Space>
                      </div>
                      <Tag color={acc.isRegistered ? 'green' : 'default'}>
                        {acc.isRegistered ? '등록완료' : '미등록'}
                      </Tag>
                    </div>
                  </Card>
                )}
              />
            </>
          );
        }}
      </QueryBoundary>
    </>
  );
}
