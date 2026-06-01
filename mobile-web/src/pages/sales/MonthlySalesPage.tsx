import { useState } from 'react';
import { Card, Col, DatePicker, Progress, Row, Statistic, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import dayjs, { type Dayjs } from 'dayjs';
import ReactECharts from 'echarts-for-react';
import { fetchMonthlySales, type MonthlySales } from '@/api/sales';
import DetailHeader from '@/components/DetailHeader';
import { QueryBoundary } from '@/components/PageStates';
import { formatWon } from '@/lib/format';

function CategoryChart({ data }: { data: MonthlySales }) {
  const option = {
    grid: { left: 8, right: 16, top: 24, bottom: 24, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: { data: ['목표', '실적'], bottom: 0 },
    xAxis: { type: 'category', data: data.categorySales.map((c) => c.category) },
    yAxis: { type: 'value', axisLabel: { formatter: (v: number) => `${(v / 10000).toFixed(0)}만` } },
    series: [
      { name: '목표', type: 'bar', data: data.categorySales.map((c) => c.targetAmount), itemStyle: { color: '#d9d9d9' } },
      { name: '실적', type: 'bar', data: data.categorySales.map((c) => c.achievedAmount), itemStyle: { color: '#1677ff' } },
    ],
  };
  return <ReactECharts option={option} style={{ height: 240 }} notMerge lazyUpdate />;
}

export default function MonthlySalesPage() {
  const [month, setMonth] = useState<Dayjs>(dayjs());
  const yearMonth = month.format('YYYYMM');

  const query = useQuery({
    queryKey: ['monthly-sales', yearMonth],
    queryFn: () => fetchMonthlySales({ yearMonth }),
  });

  return (
    <div>
      <DetailHeader title="월매출 현황" />
      <DatePicker
        picker="month"
        value={month}
        allowClear={false}
        onChange={(v) => v && setMonth(v)}
        style={{ width: '100%', marginBottom: 12 }}
      />
      <QueryBoundary
        isLoading={query.isLoading}
        isError={query.isError}
        data={query.data}
        onRetry={query.refetch}
      >
        {(data) => (
          <>
            <Card style={{ marginBottom: 12 }} styles={{ body: { padding: 16 } }}>
              <Typography.Text type="secondary">{data.customerName}</Typography.Text>
              <Row gutter={16} style={{ marginTop: 8 }}>
                <Col span={12}>
                  <Statistic title="목표" value={data.targetAmount} formatter={(v) => formatWon(Number(v))} />
                </Col>
                <Col span={12}>
                  <Statistic title="실적" value={data.achievedAmount} formatter={(v) => formatWon(Number(v))} />
                </Col>
              </Row>
              <Progress
                percent={Math.round(data.achievementRate * 100) / 100}
                status="active"
                style={{ marginTop: 12 }}
              />
            </Card>

            <Card title="제품유형별 매출" style={{ marginBottom: 12 }} styles={{ body: { padding: 12 } }}>
              {data.categorySales.length > 0 ? (
                <CategoryChart data={data} />
              ) : (
                <Typography.Text type="secondary">데이터 없음</Typography.Text>
              )}
            </Card>

            <Card title="전년 동월 비교" styles={{ body: { padding: 16 } }}>
              <Row gutter={16}>
                <Col span={12}>
                  <Statistic
                    title="당해년도"
                    value={data.yearComparison.currentYear}
                    formatter={(v) => formatWon(Number(v))}
                  />
                </Col>
                <Col span={12}>
                  <Statistic
                    title="전년"
                    value={data.yearComparison.previousYear}
                    formatter={(v) => formatWon(Number(v))}
                  />
                </Col>
              </Row>
            </Card>
          </>
        )}
      </QueryBoundary>
    </div>
  );
}
