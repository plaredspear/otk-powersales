import { Descriptions } from 'antd';
import type { PromotionDetail } from '@/api/promotion';

interface Props {
  promotion: PromotionDetail;
}

export default function PromotionAmountSection({ promotion }: Props) {
  return (
    <Descriptions column={2} bordered size="small">
      <Descriptions.Item label="목표금액">
        {promotion.targetAmount != null
          ? `${promotion.targetAmount.toLocaleString()}원`
          : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="실적금액(원)">
        {promotion.actualAmount != null
          ? `${promotion.actualAmount.toLocaleString()}원`
          : '-'}
      </Descriptions.Item>
    </Descriptions>
  );
}
