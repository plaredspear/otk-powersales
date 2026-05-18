import { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';

interface YearComparisonBarChartProps {
  leftLabel: string;
  leftValue: number;
  rightLabel: string;
  rightValue: number;
  unitSuffix?: string;
  height?: number;
}

/**
 * 전년 대비 2 막대 비교 차트 (현재 vs 전년 동월 / 평균).
 *
 * 단순 막대 2개. 단위 suffix 는 백만원 / 만원 등 호출부 결정.
 */
export default function YearComparisonBarChart({
  leftLabel,
  leftValue,
  rightLabel,
  rightValue,
  unitSuffix = '',
  height = 220,
}: YearComparisonBarChartProps) {
  const option = useMemo(
    () => ({
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v: number | null) => (v == null ? '-' : `${v.toLocaleString()}${unitSuffix}`),
      },
      grid: { left: 60, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: [leftLabel, rightLabel] },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          barWidth: 60,
          itemStyle: {
            color: (params: { dataIndex: number }) => (params.dataIndex === 0 ? '#1677ff' : '#8c8c8c'),
          },
          label: {
            show: true,
            position: 'top',
            formatter: (params: { value: number }) =>
              `${params.value.toLocaleString()}${unitSuffix}`,
          },
          data: [leftValue, rightValue],
        },
      ],
    }),
    [leftLabel, leftValue, rightLabel, rightValue, unitSuffix],
  );

  return <ReactECharts option={option} style={{ height, width: '100%' }} notMerge />;
}
