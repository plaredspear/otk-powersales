import { App, Descriptions, Divider, Space, Switch, Tag, Typography } from 'antd';
import type { SapInboundCatalogItem } from '@/api/admin/sapIntegration';
import { useSetSapInboundEnabled } from '@/hooks/admin/useSapInbound';
import SapInboundAuditsTab from './SapInboundAuditsTab';

const { Title, Text } = Typography;

/**
 * SAP Inbound API 1건의 상세 카드.
 *
 * 카탈로그 항목의 연동 정보(엔드포인트/스코프/적재 대상/컨트롤러/설명)를 하나의 "연동 정보"
 * 표로 표시하고, 이어서 해당 Endpoint 로 고정된 호출 이력을 인라인으로 보여준다.
 * 통합 페이지에서 인바운드 API 마다 개별 탭으로 나누어 본 컴포넌트를 렌더링한다.
 *
 * 상단의 활성/비활성 스위치로 해당 endpoint 의 적재 처리를 토글한다.
 * - 활성: 기존과 동일하게 적재 처리.
 * - 비활성: 요청을 수신하면 적재 처리를 생략하고 정상(OK) 응답만 반환한다.
 */
export default function SapInboundCatalogDetail({
  item,
}: {
  item: SapInboundCatalogItem;
}) {
  const { message } = App.useApp();
  const toggleMutation = useSetSapInboundEnabled();

  const handleToggle = (checked: boolean) => {
    toggleMutation.mutate(
      { endpointPath: item.endpointPath, enabled: checked },
      {
        onSuccess: () => {
          message.success(`'${item.koreanName}' 처리를 ${checked ? '활성화' : '비활성화'}했습니다.`);
        },
        onError: (err: unknown) => {
          message.error(err instanceof Error ? err.message : '처리 상태 변경에 실패했습니다.');
        },
      },
    );
  };

  return (
    <>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Title level={5} style={{ margin: 0 }}>
          연동 정보
        </Title>
        <Space>
          <Text type="secondary">처리</Text>
          <Switch
            checked={item.enabled}
            loading={toggleMutation.isPending}
            onChange={handleToggle}
            checkedChildren="활성"
            unCheckedChildren="비활성"
          />
          {!item.enabled && (
            <Tag color="warning" style={{ marginInlineEnd: 0 }}>
              수신 시 적재 생략 (정상 응답)
            </Tag>
          )}
        </Space>
      </Space>

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
