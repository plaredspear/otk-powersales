import { Result } from 'antd';

/**
 * 홈 대시보드 — 작업 중 플레이스홀더.
 *
 * 기존 대시보드 화면(매출현황 / 여사원 투입현황 / 기본 현황 탭)은 `DashboardPage.tsx` 에 보존되어 있으며,
 * 작업 완료 후 routes.tsx 의 `/` 라우트를 다시 `DashboardPage` 로 되돌리면 복원된다.
 */
export default function DashboardPlaceholderPage() {
  return <Result status="info" title="작업 중" subTitle="대시보드는 현재 작업 중입니다." />;
}
