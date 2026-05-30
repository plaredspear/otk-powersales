import { useMemo } from 'react';
import ReactECharts from 'echarts-for-react';
import type { PromotionTargetActualChartItem } from '@/api/promotionTargetActualReport';

interface Props {
  data: PromotionTargetActualChartItem[];
  height?: number;
}

/**
 * 지점별 행사실적 구성 현황 — 행사명별 실적금액 구성비 도넛 차트 (Spec #845, SF Report new_report_AtQ 차트 재현).
 *
 * data 가 비어있으면 빈 차트만 표시.
 */
export default function PromotionActualDonutChart({ data, height = 320 }: Props) {
  const option = useMemo(() => {
    return {
      title: {
        text: '지점별 행사실적 구성 현황',
        left: 'center',
        textStyle: { fontSize: 14 },
      },
      tooltip: {
        trigger: 'item',
        formatter: (p: { name: string; value: number; percent: number }) =>
          `${p.name}<br/>${p.value.toLocaleString()}원 (${p.percent}%)`,
      },
      legend: { orient: 'vertical', right: 0, top: 'middle', type: 'scroll' },
      series: [
        {
          name: '실적금액',
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['40%', '55%'],
          avoidLabelOverlap: true,
          label: { show: false },
          data: data.map((d) => ({
            name: d.promotionName ?? '(미지정)',
            value: d.actualAmount,
          })),
        },
      ],
    };
  }, [data]);

  return <ReactECharts option={option} style={{ height, width: '100%' }} notMerge />;
}
