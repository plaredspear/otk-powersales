import { Card, Descriptions } from 'antd';
import type { SapInboundCatalogItem } from '@/api/admin/sapIntegration';

/**
 * SAP Inbound API 1건의 상세 카드.
 *
 * 카탈로그 항목 하나(엔드포인트/한글명/스코프/적재 대상/컨트롤러/설명)를 키-값 표로 표시한다.
 * 통합 페이지에서 인바운드 API 마다 개별 탭으로 나누어 본 컴포넌트를 렌더링한다.
 */
export default function SapInboundCatalogDetail({
  item,
}: {
  item: SapInboundCatalogItem;
}) {
  return (
    <Card>
      <Descriptions column={1} bordered size="middle">
        <Descriptions.Item label="한글명">{item.koreanName}</Descriptions.Item>
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
    </Card>
  );
}
