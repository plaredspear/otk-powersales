import { useEffect, useState } from 'react';
import { Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import SapInboundCatalogDetail from '../sap-inbound/SapInboundCatalogDetail';
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
 * 나누어, 각 탭에서 해당 API 의 상세(스코프/적재 대상/sender 등) + 그 API 의 호출 이력을
 * 함께 보여준다. 탭 목록은 카탈로그 조회 결과로부터 동적으로 구성된다.
 *
 * 호출 이력은 별도 탭으로 분리하지 않고 각 API 상세 탭 안에 해당 API 로 고정된 형태로 표시한다.
 *
 * - Inbound: (API 별 상세 + 호출 이력 탭 N개)
 * - Outbound: (API 별 상세 + 호출 이력 탭 N개) / 대기 중(Outbox) / 테스트
 */
export default function SapIntegrationPage() {
  const [activeKey, setActiveKey] = useState<string | undefined>(undefined);

  const inboundCatalogQuery = useSapInboundCatalog();
  const outboundCatalogQuery = useSapOutboundCatalog();

  // 대기 큐 건수를 탭 라벨에 표기. TanStack Query 캐시 공유로 Outbox 탭과 중복 호출 비용 없음.
  const outboxQuery = useSapOutboundOutboxPending(1, 20);
  const outboxCount = outboxQuery.data?.totalCount ?? 0;

  // 카탈로그 각 항목을 API 별 개별 탭으로 변환 (key 는 식별자로 유일하게).
  // 각 탭은 API 상세 + 그 API 로 고정된 호출 이력을 함께 렌더링한다.
  const inboundApiTabs: TabItem[] = (inboundCatalogQuery.data ?? []).map((item) => ({
    key: `inbound-api:${item.endpointPath}`,
    label: item.koreanName,
    children: <SapInboundCatalogDetail item={item} />,
  }));

  // Outbound 탭 라벨에는 한글명 아래 SAP interfaceId 를 회색 보조 텍스트로 표기한다.
  const outboundApiTabs: TabItem[] = (outboundCatalogQuery.data ?? []).map((item) => ({
    key: `outbound-api:${item.interfaceId}`,
    label: (
      <div style={{ lineHeight: 1.4, textAlign: 'left' }}>
        <div>{item.koreanName}</div>
        <Text type="secondary" style={{ fontSize: 13 }}>
          ({item.interfaceId})
        </Text>
      </div>
    ),
    children: <SapOutboundCatalogDetail item={item} />,
  }));

  const tabItems: TabItem[] = [
    groupHeader('inbound-header', 'Inbound'),
    ...inboundApiTabs,
    groupHeader('outbound-header', 'Outbound'),
    ...outboundApiTabs,
    {
      key: 'outbound-outbox',
      label: `대기 중 (Outbox)${outboxCount ? ` · ${outboxCount}` : ''}`,
      children: <SapOutboundOutboxTab />,
    },
    {
      key: 'outbound-test',
      label: '테스트',
      children: <SapOutboundTestTab />,
    },
  ];

  // 카탈로그 로드 전에는 선택 가능한 API 탭이 없으므로, 첫 선택 가능 탭으로 기본 활성화.
  // (그룹 헤더는 disabled 이므로 제외)
  const firstSelectableKey = tabItems.find((tab) => !tab.disabled)?.key;
  useEffect(() => {
    if (activeKey === undefined && firstSelectableKey) {
      setActiveKey(firstSelectableKey);
    }
  }, [activeKey, firstSelectableKey]);

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
        activeKey={activeKey ?? firstSelectableKey}
        onChange={setActiveKey}
        items={tabItems}
      />
    </div>
  );
}
