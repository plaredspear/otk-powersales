import { Card, Descriptions, Divider, Tag, Typography } from 'antd';
import type {
  OutboundTriggerType,
  SapOutboundCatalogItem,
} from '@/api/admin/sapIntegration';
import SapOutboundLogsTab from './SapOutboundLogsTab';

const { Title } = Typography;

const TRIGGER_TAG_COLOR: Record<OutboundTriggerType, string> = {
  BATCH: 'blue',
  REALTIME: 'green',
  OUTBOX: 'purple',
};

/**
 * SAP Outbound API 1건의 상세 카드.
 *
 * 카탈로그 항목 하나(인터페이스 ID/한글명/트리거 유형/sender 클래스/설명)를 키-값 표로 표시하고,
 * 이어서 해당 Interface 로 고정된 호출 이력을 인라인으로 보여준다.
 * 통합 페이지에서 아웃바운드 API 마다 개별 탭으로 나누어 본 컴포넌트를 렌더링한다.
 */
export default function SapOutboundCatalogDetail({
  item,
}: {
  item: SapOutboundCatalogItem;
}) {
  return (
    <>
      <Card>
        <Descriptions column={1} bordered size="middle">
          <Descriptions.Item label="한글명">{item.koreanName}</Descriptions.Item>
          <Descriptions.Item label="Interface ID">
            <code>{item.interfaceId}</code>
          </Descriptions.Item>
          <Descriptions.Item label="트리거">
            <Tag color={TRIGGER_TAG_COLOR[item.triggerType]}>{item.triggerType}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Sender Class">
            <code>{item.senderClass}</code>
          </Descriptions.Item>
          <Descriptions.Item label="설명">{item.description}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Divider />

      <Title level={5} style={{ marginBottom: 16 }}>
        호출 이력
      </Title>
      <SapOutboundLogsTab lockedInterfaceId={item.interfaceId} />
    </>
  );
}
