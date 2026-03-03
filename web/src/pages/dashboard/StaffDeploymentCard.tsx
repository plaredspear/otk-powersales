import { memo } from 'react';
import { Card, Col, Row, Typography } from 'antd';
import ReactECharts from 'echarts-for-react';
import type { StaffDeployment } from '@/api/dashboard';

const { Text } = Typography;

const WORK_TYPE_COLORS: Record<string, string> = {
  고정: '#5470c6',
  격고: '#91cc75',
  순회: '#fac858',
};

interface StaffDeploymentCardProps {
  data: StaffDeployment;
}

function StaffDeploymentCard({ data }: StaffDeploymentCardProps) {
  const accountTypePieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}명 ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        data: data.byAccountType.map((a) => ({
          name: a.accountType,
          value: a.count,
        })),
        label: { formatter: '{b}\n{c}명' },
      },
    ],
  };

  const workTypePieOption = {
    tooltip: { trigger: 'item', formatter: '{b}: {c}명 ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '45%'],
        data: data.byWorkType.map((w) => ({
          name: w.workType,
          value: w.count,
          itemStyle: { color: WORK_TYPE_COLORS[w.workType] },
        })),
        label: { formatter: '{b}\n{c}명' },
      },
    ],
  };

  const stackedBarOption = {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, data: ['고정', '격고', '순회'] },
    grid: { left: 80, right: 20, top: 10, bottom: 40 },
    xAxis: {
      type: 'category',
      data: data.byChannelAndWorkType.map((c) => c.channelName),
    },
    yAxis: { type: 'value', name: '명' },
    series: [
      {
        name: '고정',
        type: 'bar',
        stack: 'total',
        data: data.byChannelAndWorkType.map((c) => c.fixed),
        itemStyle: { color: WORK_TYPE_COLORS['고정'] },
      },
      {
        name: '격고',
        type: 'bar',
        stack: 'total',
        data: data.byChannelAndWorkType.map((c) => c.alternating),
        itemStyle: { color: WORK_TYPE_COLORS['격고'] },
      },
      {
        name: '순회',
        type: 'bar',
        stack: 'total',
        data: data.byChannelAndWorkType.map((c) => c.visiting),
        itemStyle: { color: WORK_TYPE_COLORS['순회'] },
      },
    ],
  };

  // Compare current vs previous month
  const prevMap = new Map(data.previousMonth.byWorkType.map((w) => [w.workType, w.count]));
  const comparison = data.byWorkType.map((w) => {
    const prev = prevMap.get(w.workType) ?? 0;
    const diff = w.count - prev;
    return { workType: w.workType, count: w.count, diff };
  });

  return (
    <Card title="여사원 투입현황" size="small">
      <Row gutter={24}>
        <Col xs={24} md={8}>
          <Text strong>거래처유형별</Text>
          <ReactECharts option={accountTypePieOption} style={{ height: 220 }} />
        </Col>
        <Col xs={24} md={8}>
          <Text strong>근무형태별</Text>
          <ReactECharts option={workTypePieOption} style={{ height: 220 }} />
        </Col>
        <Col xs={24} md={8}>
          <Text strong>유통별 × 근무형태</Text>
          <ReactECharts option={stackedBarOption} style={{ height: 220 }} />
        </Col>
      </Row>
      <div style={{ marginTop: 12, textAlign: 'center' }}>
        <Text type="secondary">
          당월 vs 전월:{' '}
          {comparison.map((c, i) => (
            <span key={c.workType}>
              {i > 0 && ' / '}
              {c.workType} {c.count}
              {c.diff !== 0 && (
                <span style={{ color: c.diff > 0 ? '#52c41a' : '#ff4d4f' }}>
                  ({c.diff > 0 ? '+' : ''}{c.diff})
                </span>
              )}
            </span>
          ))}
        </Text>
      </div>
    </Card>
  );
}

export default memo(StaffDeploymentCard);
