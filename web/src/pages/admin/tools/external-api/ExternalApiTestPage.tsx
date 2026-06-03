import { Alert, Space, Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import NaverGeocodeTab from './NaverGeocodeTab';
import SapOutboundSenderCard from '../sap-outbound/SapOutboundSenderCard';
import { SENDER_CONFIGS } from '../sap-outbound/sapOutboundSenderConfigs';

const { Title, Text } = Typography;

/**
 * 외부 API 테스트 통합 페이지 (개발자 도구).
 *
 * backend 에 연동된 외부(third-party) API 요청을 한 곳에서 탭으로 구분해 테스트 호출한다.
 * Naver Geocode 1개 + SAP outbound 7개 인터페이스가 각각 개별 탭이 되며, 각 탭에서
 * 폼 입력 → 미리보기 → 실송신까지 직접 수행할 수 있다.
 * 새 외부 API 가 추가되면 본 탭 배열(또는 `SENDER_CONFIGS`)에 항목 1개를 추가하면 된다.
 */
const TAB_ITEMS: NonNullable<TabsProps['items']> = [
  {
    key: 'naver-geocode',
    label: 'Naver Geocode',
    children: <NaverGeocodeTab />,
  },
  ...SENDER_CONFIGS.map((config) => ({
    key: config.kind,
    label: config.tabLabel,
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert
          type="warning"
          showIcon
          message="이 탭은 실제 SAP 시스템으로 송신을 트리거합니다."
          description="'실송신' 버튼은 현재 환경의 SAP REST Adapter 로 호출이 전송됩니다 (sap_outbound_log / sap_outbox 적재됨). 페이로드 형식 확인만 필요하면 '미리보기' 만 사용하세요. SYSTEM_ADMIN 권한 필요."
        />
        {/* key 로 탭 전환 시 카드 state 를 초기화 */}
        <SapOutboundSenderCard key={config.kind} config={config} />
      </Space>
    ),
  })),
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
