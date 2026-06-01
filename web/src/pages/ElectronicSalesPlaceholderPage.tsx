import { Result } from 'antd';

/**
 * 월 매출(전산실적) — web admin 미구현 플레이스홀더.
 *
 * Backend 전산매출(`live_tot_sales_dh`) 조회는 모바일(`/api/v1/mobile/sales/electronic`) 에만
 * 존재. web admin 화면은 후속 구현 대상.
 */
export default function ElectronicSalesPlaceholderPage() {
  return <Result status="info" title="준비중" subTitle="이 기능은 준비 중입니다." />;
}
