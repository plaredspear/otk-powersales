import { useState } from 'react';
import { Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import SapInboundAuditsTab from '../sap-inbound/SapInboundAuditsTab';
import SapInboundCatalogTab from '../sap-inbound/SapInboundCatalogTab';
import SapOutboundLogsTab from '../sap-outbound/SapOutboundLogsTab';
import SapOutboundOutboxTab from '../sap-outbound/SapOutboundOutboxTab';
import SapOutboundCatalogTab from '../sap-outbound/SapOutboundCatalogTab';
import SapOutboundTestTab from '../sap-outbound/SapOutboundTestTab';
import { useSapOutboundOutboxPending } from '@/hooks/admin/useSapOutbound';

const { Title, Text } = Typography;

/**
 * 왼쪽 세로 탭에서 Inbound / Outbound 방향을 구분하는 그룹 헤더 탭.
 *
 * antd Tabs 는 Menu 와 달리 `type: 'group'` 을 지원하지 않으므로, 선택 불가(`disabled`)한
 * 헤더 탭으로 시각적 섹션 구분을 표현한다. `children` 이 없어 본문은 렌더링되지 않는다.
 */
function groupHeader(
  key: string,
  label: string,
): NonNullable<TabsProps['items']>[number] {
  return {
    key,
    disabled: true,
    label: (
      <Text type="secondary" strong style={{ fontSize: 12, letterSpacing: 1 }}>
        {label}
      </Text>
    ),
  };
}

/**
 * SAP 연동 통합 페이지 (개발자 도구).
 *
 * 기존 분리되어 있던 'SAP Inbound' / 'SAP Outbound' 두 페이지를 하나로 통합한다.
 * 스케줄 잡 실행 이력 화면(`ScheduledJobsPage`)과 동일하게 왼쪽 세로 탭
 * (`tabPosition="left"`)으로 구성하며, antd Tabs 의 `type: 'group'` 으로
 * Inbound / Outbound 방향을 시각적으로 구분한다.
 *
 * - Inbound: 호출 이력 / API 목록
 * - Outbound: 호출 이력 / 대기 중(Outbox) / API 목록 / 테스트
 */
export default function SapIntegrationPage() {
  const [activeKey, setActiveKey] = useState('inbound-audits');

  // 대기 큐 건수를 탭 라벨에 표기. TanStack Query 캐시 공유로 Outbox 탭과 중복 호출 비용 없음.
  const outboxQuery = useSapOutboundOutboxPending(1, 20);
  const outboxCount = outboxQuery.data?.totalCount ?? 0;

  const tabItems: NonNullable<TabsProps['items']> = [
    groupHeader('inbound-header', 'Inbound'),
    {
      key: 'inbound-audits',
      label: '호출 이력',
      children: <SapInboundAuditsTab />,
    },
    {
      key: 'inbound-catalog',
      label: 'API 목록',
      children: <SapInboundCatalogTab />,
    },
    groupHeader('outbound-header', 'Outbound'),
    {
      key: 'outbound-logs',
      label: '호출 이력',
      children: <SapOutboundLogsTab />,
    },
    {
      key: 'outbound-outbox',
      label: `대기 중 (Outbox)${outboxCount ? ` · ${outboxCount}` : ''}`,
      children: <SapOutboundOutboxTab />,
    },
    {
      key: 'outbound-catalog',
      label: 'API 목록',
      children: <SapOutboundCatalogTab />,
    },
    {
      key: 'outbound-test',
      label: '테스트',
      children: <SapOutboundTestTab />,
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>SAP 연동</Title>
      <Text type="secondary">
        SAP ↔ backend 의 인바운드 / 아웃바운드 연동을 한 곳에서 조회합니다. 인바운드는 SAP 에서
        backend 로 들어오는 호출, 아웃바운드는 backend 에서 SAP REST Adapter 로 나가는 호출입니다.
      </Text>

      <Tabs
        tabPosition="left"
        style={{ marginTop: 24 }}
        activeKey={activeKey}
        onChange={setActiveKey}
        items={tabItems}
      />
    </div>
  );
}
