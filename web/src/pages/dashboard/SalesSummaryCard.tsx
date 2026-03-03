import { memo } from 'react';
import { Card, Col, Row, Statistic, Typography } from 'antd';
import ReactECharts from 'echarts-for-react';
import type { SalesSummary } from '@/api/dashboard';

const { Title, Text } = Typography;

function formatAmount(amount: number): string {
  if (amount >= 100_000_000) {
    return `${(amount / 100_000_000).toFixed(1)}억`;
  }
  if (amount >= 10_000) {
    return `${Math.round(amount / 10_000).toLocaleString()}만`;
  }
  return amount.toLocaleString();
}

function getProgressColor(rate: number): string {
  if (rate < 50) return '#ff4d4f';
  if (rate < 80) return '#faad14';
  return '#52c41a';
}

interface SalesSummaryCardProps {
  data: SalesSummary;
}

function SalesSummaryCard({ data }: SalesSummaryCardProps) {
  const gaugeOption = {
    series: [
      {
        type: 'gauge',
        startAngle: 200,
        endAngle: -20,
        min: 0,
        max: 100,
        splitNumber: 10,
        axisLine: {
          lineStyle: {
            width: 20,
            color: [
              [0.5, '#ff4d4f'],
              [0.8, '#faad14'],
              [1, '#52c41a'],
            ],
          },
        },
        pointer: { length: '60%', width: 6 },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          fontSize: 24,
          fontWeight: 'bold',
          offsetCenter: [0, '40%'],
        },
        title: { show: false },
        data: [{ value: data.progressRate }],
        markLine: data.referenceProgressRate
          ? {
              silent: true,
              symbol: 'none',
              data: [{ yAxis: data.referenceProgressRate }],
            }
          : undefined,
      },
    ],
  };

  const barOption = {
    tooltip: {
      trigger: 'axis',
      formatter: (params: Array<{ name: string; value: number }>) => {
        const p = params[0];
        return `${p.name}: ${p.value}%`;
      },
    },
    grid: { left: 80, right: 40, top: 10, bottom: 30 },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' },
    },
    yAxis: {
      type: 'category',
      data: data.channelSales.map((c) => c.channelName),
      inverse: true,
    },
    series: [
      {
        type: 'bar',
        data: data.channelSales.map((c) => ({
          value: c.progressRate,
          itemStyle: { color: getProgressColor(c.progressRate) },
        })),
        barMaxWidth: 24,
        label: {
          show: true,
          position: 'right',
          formatter: '{c}%',
        },
      },
    ],
  };

  return (
    <Card title="매출현황" size="small">
      <Row gutter={24}>
        <Col xs={24} lg={10}>
          <div style={{ textAlign: 'center' }}>
            <ReactECharts option={gaugeOption} style={{ height: 220 }} />
            <Row gutter={16} justify="center">
              <Col>
                <Statistic title="목표" value={formatAmount(data.targetAmount)} />
              </Col>
              <Col>
                <Statistic title="실적" value={formatAmount(data.actualAmount)} />
              </Col>
            </Row>
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                기준진도율: {data.referenceProgressRate}% | 전년동기: {formatAmount(data.lastYearAmount)} (
                {data.lastYearRatio}%)
              </Text>
            </div>
          </div>
        </Col>
        <Col xs={24} lg={14}>
          <Title level={5} style={{ marginBottom: 8 }}>
            유통별 행사매출 진도율
          </Title>
          <ReactECharts option={barOption} style={{ height: 220 }} />
        </Col>
      </Row>
    </Card>
  );
}

export default memo(SalesSummaryCard);
