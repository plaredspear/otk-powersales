import { Space, Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import NaverGeocodeTab from './NaverGeocodeTab';
import SapOutboundLinkTab from './SapOutboundLinkTab';

const { Title, Text } = Typography;

/**
 * 외부 API 테스트 통합 페이지 (개발자 도구).
 *
 * backend 에 연동된 외부(third-party) API 요청을 한 곳에서 탭으로 구분해 테스트 호출한다.
 * 새 외부 API 가 추가되면 본 `TAB_ITEMS` 배열에 탭 1개를 추가하면 된다.
 */
const TAB_ITEMS: NonNullable<TabsProps['items']> = [
  {
    key: 'naver-geocode',
    label: 'Naver Geocode',
    children: <NaverGeocodeTab />,
  },
  {
    key: 'sap-outbound',
    label: 'SAP Outbound',
    children: <SapOutboundLinkTab />,
  },
];

export default function ExternalApiTestPage() {
  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 4 }}>
        외부 API 테스트
      </Title>
      <Text type="secondary">
        backend 에 연동된 외부 API 요청을 탭으로 구분해 직접 호출하고 응답을 확인합니다.
      </Text>

      <Space direction="vertical" size="large" style={{ width: '100%', marginTop: 24 }}>
        <Tabs defaultActiveKey="naver-geocode" items={TAB_ITEMS} />
      </Space>
    </div>
  );
}
