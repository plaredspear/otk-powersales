import { useState } from 'react';
import { Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import SapInboundAuditsTab from '../sap-inbound/SapInboundAuditsTab';
import SapInboundCatalogDetail from '../sap-inbound/SapInboundCatalogDetail';
import SapOutboundLogsTab from '../sap-outbound/SapOutboundLogsTab';
import SapOutboundOutboxTab from '../sap-outbound/SapOutboundOutboxTab';
import SapOutboundCatalogDetail from '../sap-outbound/SapOutboundCatalogDetail';
import SapOutboundTestTab from '../sap-outbound/SapOutboundTestTab';
import { useSapInboundCatalog } from '@/hooks/admin/useSapInbound';
import {
  useSapOutboundCatalog,
  useSapOutboundOutboxPending,
} from '@/hooks/admin/useSapOutbound';

const { Title, Text } = Typography;

type TabItem = NonNullable<TabsProps['items']>[number];

/**
 * 왼쪽 세로 탭에서 Inbound / Outbound 방향을 구분하는 그룹 헤더 탭.
 *
 * antd Tabs 는 Menu 와 달리 `type: 'group'` 을 지원하지 않으므로, 선택 불가(`disabled`)한
 * 헤더 탭으로 시각적 섹션 구분을 표현한다. `children` 이 없어 본문은 렌더링되지 않는다.
 */
function groupHeader(key: string, label: string): TabItem {
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
 * (`tabPosition="left"`)으로 구성하며, 선택 불가 헤더 탭으로 Inbound / Outbound
 * 방향을 시각적으로 구분한다.
 *
 * API 카탈로그는 하나의 목록 표가 아니라 API(엔드포인트/인터페이스) 1건당 개별 탭으로
 * 나누어, 각 탭에서 해당 API 의 상세(스코프/적재 대상/sender 등)를 카드로 보여준다.
 * 탭 목록은 카탈로그 조회 결과로부터 동적으로 구성된다.
 *
 * - Inbound: 호출 이력 / (API 별 상세 탭 N개)
 * - Outbound: 호출 이력 / 대기 중(Outbox) / (API 별 상세 탭 N개) / 테스트
 */
export default function SapIntegrationPage() {
  const [activeKey, setActiveKey] = useState('inbound-audits');

  const inboundCatalogQuery = useSapInboundCatalog();
  const outboundCatalogQuery = useSapOutboundCatalog();

  // 대기 큐 건수를 탭 라벨에 표기. TanStack Query 캐시 공유로 Outbox 탭과 중복 호출 비용 없음.
  const outboxQuery = useSapOutboundOutboxPending(1, 20);
  const outboxCount = outboxQuery.data?.totalCount ?? 0;

  // 카탈로그 각 항목을 API 별 개별 탭으로 변환 (key 는 식별자로 유일하게).
  const inboundApiTabs: TabItem[] = (inboundCatalogQuery.data ?? []).map((item) => ({
    key: `inbound-api:${item.endpointPath}`,
    label: item.koreanName,
    children: <SapInboundCatalogDetail item={item} />,
  }));

  const outboundApiTabs: TabItem[] = (outboundCatalogQuery.data ?? []).map((item) => ({
    key: `outbound-api:${item.interfaceId}`,
    label: item.koreanName,
    children: <SapOutboundCatalogDetail item={item} />,
  }));

  const tabItems: TabItem[] = [
    groupHeader('inbound-header', 'Inbound'),
    {
      key: 'inbound-audits',
      label: '호출 이력',
      children: <SapInboundAuditsTab />,
    },
    ...inboundApiTabs,
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
    ...outboundApiTabs,
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
