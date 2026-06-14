import { Alert, Card, Descriptions, Divider, Tag, Typography } from 'antd';
import type {
  OutboundTriggerType,
  SapOutboundCatalogItem,
} from '@/api/admin/sapIntegration';
import IntegrationInfoDescriptions from '../external-api/IntegrationInfoDescriptions';
import SapOutboundLogsTab from './SapOutboundLogsTab';
import SapOutboundSenderCard from './SapOutboundSenderCard';
import { SENDER_CONFIGS } from './sapOutboundSenderConfigs';

const { Title } = Typography;

const TRIGGER_TAG_COLOR: Record<OutboundTriggerType, string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

/**
 * SAP Outbound API 1건의 상세 카드.
 *
 * 카탈로그 항목의 고유 메타(트리거/Sender Class/설명)와 연동 정보(endpoint/method/인증)를
 * 하나의 "연동 정보" 섹션으로 통합해 표시하고, 이어서 테스트 송신(미리보기/실송신) + 호출
 * 이력을 인라인으로 보여준다.
 * 통합 페이지에서 아웃바운드 API 마다 개별 탭으로 나누어 본 컴포넌트를 렌더링한다.
 *
 * 연동 정보 표 / 테스트 송신 폼은 `item.interfaceId` 로 `SENDER_CONFIGS` 를 역조회해
 * 렌더링한다. 매칭되는 설정이 없으면(테스트 미지원 인터페이스) 송신 섹션은 생략한다.
 */
export default function SapOutboundCatalogDetail({
  item,
}: {
  item: SapOutboundCatalogItem;
}) {
  const senderConfig = SENDER_CONFIGS.find((c) => c.interfaceId === item.interfaceId);

  return (
    <>
      <Title level={5} style={{ marginBottom: 16 }}>
        연동 정보
      </Title>
      <Card style={{ marginBottom: 16 }}>
        <Descriptions column={1} bordered size="middle">
          <Descriptions.Item label="트리거">
            <Tag color={TRIGGER_TAG_COLOR[item.triggerType]}>{item.triggerType}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Sender Class">
            <code>{item.senderClass}</code>
          </Descriptions.Item>
          <Descriptions.Item label="설명">{item.description}</Descriptions.Item>
        </Descriptions>
      </Card>
      {senderConfig ? (
        <IntegrationInfoDescriptions apiKey={senderConfig.kind} />
      ) : (
        <Alert
          type="info"
          showIcon
          message="이 인터페이스의 외부 연동 정보 / 테스트 송신은 제공되지 않습니다."
        />
      )}

      {senderConfig && (
        <>
          <Divider />
          <Title level={5} style={{ marginBottom: 16 }}>
            테스트 송신
          </Title>
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
            message="이 섹션은 실제 SAP 시스템으로 송신을 트리거합니다."
            description="'실송신' 버튼은 현재 환경의 SAP REST Adapter 로 호출이 전송됩니다 (sap_outbound_log / sap_outbox 적재됨). 페이로드 형식 확인만 필요하면 '미리보기' 만 사용하세요. SYSTEM_ADMIN 권한 필요."
          />
          {/* key 로 인터페이스 전환(탭 이동) 시 카드 state 를 초기화 */}
          <SapOutboundSenderCard key={senderConfig.kind} config={senderConfig} />
        </>
      )}

      <Divider />

      <Title level={5} style={{ marginBottom: 16 }}>
        호출 이력
      </Title>
      <SapOutboundLogsTab lockedInterfaceId={item.interfaceId} />
    </>
  );
}
