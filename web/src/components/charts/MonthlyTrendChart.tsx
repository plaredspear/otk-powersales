import { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import type { MonthlyTrendPoint } from '@/api/monthlySalesDashboard';

interface MonthlyTrendChartProps {
  data: MonthlyTrendPoint[];
  height?: number;
}

/**
 * 월별 추이 라인 차트 — 목표 / 실적 / 전년 동월 3 series.
 *
 * X축은 `YYYY-MM`, Y축은 원 (만원 단위 표시는 tooltip 에서 처리).
 * data 가 비어있으면 빈 차트만 표시 (echarts 가 빈 series 처리).
 */
export default function MonthlyTrendChart({ data, height = 300 }: MonthlyTrendChartProps) {
  const option = useMemo(() => {
    const categories = data.map((p) => `${p.salesYear}-${String(p.salesMonth).padStart(2, '0')}`);
    return {
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v: number | null) => {
          if (v == null) return '-';
          return `${(v / 10000).toLocaleString()}만원`;
        },
      },
      legend: { data: ['목표', '실적', '전년 동월'] },
      grid: { left: 60, right: 20, top: 40, bottom: 30 },
      xAxis: { type: 'category', data: categories },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (v: number) => `${(v / 10000).toLocaleString()}만`,
        },
      },
      series: [
        {
          name: '목표',
          type: 'line',
          smooth: true,
          itemStyle: { color: '#1677ff' },
          data: data.map((p) => p.targetAmount),
        },
        {
          name: '실적',
          type: 'line',
          smooth: true,
          itemStyle: { color: '#52c41a' },
          data: data.map((p) => p.achievedAmount),
        },
        {
          name: '전년 동월',
          type: 'line',
          smooth: true,
          itemStyle: { color: '#8c8c8c' },
          data: data.map((p) => p.lastYearAchievedAmount),
        },
      ],
    };
  }, [data]);

  return <ReactECharts option={option} style={{ height, width: '100%' }} notMerge />;
}
