import { Card, Col, Row, Statistic } from 'antd';
import ReactECharts from 'echarts-for-react';
import type { BasicStats } from '@/api/dashboard';

interface BasicStatsCardProps {
  data: BasicStats;
}

function BasicStatsCard({ data }: BasicStatsCardProps) {
  const ageBarOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.byAgeGroup.map((a) => a.ageGroup),
    },
    yAxis: { type: 'value', name: '명' },
    series: [
      {
        type: 'bar',
        data: data.byAgeGroup.map((a) => a.count),
        barMaxWidth: 32,
        itemStyle: { color: '#5470c6' },
        label: { show: true, position: 'top' },
      },
    ],
  };

  const workTypeBarOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 10, bottom: 30 },
    xAxis: {
      type: 'category',
      data: ['고정', '격고', '순회'],
    },
    yAxis: { type: 'value', name: '명' },
    series: [
      {
        type: 'bar',
        data: [data.byWorkType.fixed, data.byWorkType.alternating, data.byWorkType.visiting],
        barMaxWidth: 32,
        itemStyle: { color: '#91cc75' },
        label: { show: true, position: 'top' },
      },
    ],
  };

  return (
    <Card title="기본 현황" size="small">
      <Row gutter={24}>
        <Col xs={12} md={6}>
          <Card size="small" title="판촉/OSC" variant="borderless">
            <Row gutter={16}>
              <Col span={12}>
                <Statistic title="판촉직" value={data.staffType.promotion} suffix="명" />
              </Col>
              <Col span={12}>
                <Statistic title="OSC직" value={data.staffType.osc} suffix="명" />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card size="small" title="총원" variant="borderless">
            <Row gutter={16}>
              <Col span={12}>
                <Statistic title="재직" value={data.totalByPosition.active} suffix="명" />
              </Col>
              <Col span={12}>
                <Statistic title="휴직" value={data.totalByPosition.onLeave} suffix="명" />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card size="small" title="연령별" variant="borderless">
            <ReactECharts option={ageBarOption} style={{ height: 160 }} />
          </Card>
        </Col>
        <Col xs={24} md={6}>
          <Card size="small" title="근무형태별" variant="borderless">
            <ReactECharts option={workTypeBarOption} style={{ height: 160 }} />
          </Card>
        </Col>
      </Row>
    </Card>
  );
}

export default BasicStatsCard;
