import { Descriptions } from 'antd';
import type { PromotionDetail } from '@/api/promotion';

interface Props {
  promotion: PromotionDetail;
}

const fmtAmount = (v: number | null | undefined): string =>
  v == null ? '-' : v.toLocaleString();

/**
 * 목표/실적 섹션 — SF Promotion 상세의 목표금액/실적금액 동등.
 * 각각 DKRetail__TargetAmount__c / DKRetail__ActualAmount__c.
 */
export default function PromotionTargetActualSection({ promotion }: Props) {
  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="목표금액">{fmtAmount(promotion.targetAmount)}</Descriptions.Item>
      <Descriptions.Item label="실적금액 (원)">{fmtAmount(promotion.actualAmount)}</Descriptions.Item>
    </Descriptions>
  );
}
