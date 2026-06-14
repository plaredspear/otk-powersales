import { Descriptions, Divider, Typography } from 'antd';
import type { SapInboundCatalogItem } from '@/api/admin/sapIntegration';
import SapInboundAuditsTab from './SapInboundAuditsTab';

const { Title } = Typography;

/**
 * SAP Inbound API 1건의 상세 카드.
 *
 * 카탈로그 항목의 연동 정보(엔드포인트/스코프/적재 대상/컨트롤러/설명)를 하나의 "연동 정보"
 * 표로 표시하고, 이어서 해당 Endpoint 로 고정된 호출 이력을 인라인으로 보여준다.
 * 통합 페이지에서 인바운드 API 마다 개별 탭으로 나누어 본 컴포넌트를 렌더링한다.
 */
export default function SapInboundCatalogDetail({
  item,
}: {
  item: SapInboundCatalogItem;
}) {
  return (
    <>
      <Title level={5} style={{ marginBottom: 16 }}>
        연동 정보
      </Title>
      <Descriptions column={1} bordered size="middle">
        <Descriptions.Item label="Endpoint">
          <code>{item.endpointPath}</code>
        </Descriptions.Item>
        <Descriptions.Item label="Scope">
          <code>{item.requiredScope}</code>
        </Descriptions.Item>
        <Descriptions.Item label="적재 대상">{item.targetEntity}</Descriptions.Item>
        <Descriptions.Item label="컨트롤러">
          <code>{item.controllerClass}</code>
        </Descriptions.Item>
        <Descriptions.Item label="설명">{item.description}</Descriptions.Item>
      </Descriptions>

      <Divider />

      <Title level={5} style={{ marginBottom: 16 }}>
        호출 이력
      </Title>
      <SapInboundAuditsTab lockedEndpoint={item.endpointPath} />
    </>
  );
}
