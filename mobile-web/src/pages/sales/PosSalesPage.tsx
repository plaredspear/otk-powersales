import { Result, Typography } from 'antd';
import DetailHeader from '@/components/DetailHeader';

/**
 * POS 매출 조회 (레거시 promotion/month/posmain).
 *
 * ⚠️ 보류: POS매출은 현재 **mobile 엔드포인트가 없다**(backend `AdminPosSalesController`,
 * `/api/v1/admin/sales/pos` 만 존재). 또한 데이터 소스(POS DB `live_pos_sales_dh`, 제품/SKU 단위)
 * 의 모바일 노출은 데이터 플랫폼/권한 결정 선결 과제. 화면 골격만 두고 연동은 T1/PO 결정 후.
 */
export default function PosSalesPage() {
  return (
    <>
      <DetailHeader title="POS 매출" />
      <Result
        status="info"
        title="준비 중"
        subTitle={
          <Typography.Paragraph type="secondary" style={{ maxWidth: 360, margin: '0 auto' }}>
            POS 매출은 모바일 전용 API와 데이터 소스(제품/SKU 단위) 연동이 확정되지 않았습니다.
            데이터 플랫폼 결정 후 거래처 레벨로 제공될 예정입니다.
          </Typography.Paragraph>
        }
      />
    </>
  );
}
