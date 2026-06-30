import { Typography } from 'antd';

const { Title, Text } = Typography;

/**
 * 개발자 도구 - 대시보드 (빈 페이지).
 *
 * 향후 개발자 도구 관련 지표/요약을 모아 보여줄 대시보드 자리. 현재는 골격만 둔다.
 */
export default function ToolsDashboardPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>대시보드</Title>
      <Text type="secondary">개발자 도구 대시보드입니다.</Text>
    </div>
  );
}
