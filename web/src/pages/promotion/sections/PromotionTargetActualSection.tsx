import { Descriptions } from 'antd';
import type { PromotionDetail } from '@/api/promotion';

interface Props {
  promotion: PromotionDetail;
  editing?: boolean;
}

const fmtAmount = (v: number | null | undefined): string =>
  v == null ? '-' : v.toLocaleString();

/** 저장 시 다른 값으로부터 계산되는 읽기 전용 필드 안내 (SF "저장 시 이 필드가 계산됨" 동등). */
function CalculatedHint() {
  return (
    <div style={{ color: '#999', fontSize: 12, marginTop: 2 }}>저장 시 이 필드가 계산됨</div>
  );
}

/**
 * 목표/실적 섹션 — SF Promotion 상세의 목표금액/실적금액 동등.
 * 각각 DKRetail__TargetAmount__c / DKRetail__ActualAmount__c.
 */
export default function PromotionTargetActualSection({ promotion, editing = false }: Props) {
  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="목표금액">
        {fmtAmount(promotion.targetAmount)}
        {editing && <CalculatedHint />}
      </Descriptions.Item>
      <Descriptions.Item label="실적금액 (원)">
        {fmtAmount(promotion.actualAmount)}
        {editing && <CalculatedHint />}
      </Descriptions.Item>
    </Descriptions>
  );
}
