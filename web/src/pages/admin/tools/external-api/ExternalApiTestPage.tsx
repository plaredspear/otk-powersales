import { useState } from 'react';
import { Alert, Space, Tabs, Tag, Typography } from 'antd';
import type { TabsProps } from 'antd';
import NaverGeocodeTab from './NaverGeocodeTab';
import ClaimRegistTab from './ClaimRegistTab';
import LogisticsClaimRegistTab from './LogisticsClaimRegistTab';
import IntegrationInfoDescriptions from './IntegrationInfoDescriptions';
import SapOutboundSenderCard from '../sap-outbound/SapOutboundSenderCard';
import {
  SENDER_CONFIGS,
  TRIGGER_TAG_COLOR,
} from '../sap-outbound/sapOutboundSenderConfigs';

const { Title, Text } = Typography;

/**
 * 외부 API 테스트 통합 페이지 (개발자 도구).
 *
 * backend 에 연동된 외부(third-party) API 요청을 한 곳에서 탭으로 구분해 테스트 호출한다.
 * 스케줄 잡 실행 이력 화면과 동일하게 왼쪽 세로 탭(`tabPosition="left"`)으로 인터페이스를
 * 구분하며, 각 탭 상단에 해당 API 가 어떤 외부 시스템과 무슨 처리를 주고받는지에 대한
 * 자연어 설명 Alert 를 노출한다.
 * Naver Geocode 1개 + SAP outbound 7개 인터페이스가 각각 개별 탭이 되며, 각 탭에서
 * 폼 입력 → 미리보기 → 실송신까지 직접 수행할 수 있다.
 * 새 외부 API 가 추가되면 본 탭 배열(또는 `SENDER_CONFIGS`)에 항목 1개를 추가하고
 * `API_DESCRIPTIONS` 에 자연어 설명을 함께 등록한다.
 */

/** 탭 key → 해당 외부 API 가 어떤 외부 시스템과 무슨 처리를 수행하는지에 대한 자연어 설명. */
const API_DESCRIPTIONS: Record<string, string> = {
  'naver-geocode':
    '입력한 도로명/지번 주소를 Naver Cloud Platform 의 Maps Geocode API 로 전송하여 위도·경도 좌표로 변환합니다. backend 가 받은 응답을 가공 없이 raw JSON 그대로 노출하므로 좌표 변환 정확도와 API 키 동작을 직접 확인할 수 있습니다. 거래처 좌표 보강 배치(account-naver-geocode-batch)가 내부적으로 호출하는 것과 동일한 경로입니다. 조회 전용이라 DB 변경은 발생하지 않습니다.',
  'claim-regist':
    '입력한 파라미터로 클레임 등록 apiMap 을 구성하여 Salesforce Apex REST `/services/apexrest/mobile/ClaimRegist` 로 직접 POST 합니다. 레거시 Heroku FieldTalkController 가 클레임 등록 시 호출하던 것과 동일한 SF endpoint 이며, 운영 클레임 등록(dual-write)과 달리 본 테스트는 신규 DB(claim 테이블)에는 저장하지 않고 SF 로만 전송합니다. SF 가 돌려준 RESULT_CODE/RESULT_MSG 와 전송 payload 미리보기를 그대로 노출합니다. 이미지 3종은 모두 선택이며, 미첨부 시 빈 Buffer 로 전송됩니다.',
  'logistics-claim-regist':
    '모바일 물류 클레임 등록(제안하기 > 물류 클레임) 입력 정보를 토대로 Salesforce Apex REST `IF_REST_MOBILE_ProposalRegist` 전송 payload(apiMap) 미리보기를 구성합니다. SF 전송 API 정보가 아직 확보되지 않은 단계라 실제 SF POST 는 수행하지 않고, 레거시 Input 클래스 key 셋(Category/ProductCode/accountCode/EmployeeCode/Title/Description/CarNumber/claimList/logclaimDate/S3Image* 등) 정합의 apiMap 을 JSON 으로만 노출합니다. 추후 SF endpoint/계약 정보를 받으면 실제 전송 호출을 추가할 예정입니다. 사진은 최대 2장이며 모두 선택입니다.',
  'loan-inquiry':
    'SAP 거래처 코드(account.external_key)로 해당 거래처 1건의 여신 한도를 SAP 에서 동기(실시간) 조회합니다. 호출 즉시 실제 SAP 시스템으로 요청이 나가며 응답을 그대로 돌려받습니다. 조회 전용이라 신규 DB 상태 변경은 없고, sap_outbound_log 에 호출 흔적만 남습니다.',
  'order-request-detail':
    '주문 번호(order_request_number)로 해당 주문 1건의 상세 라인(품목·수량·금액 등)을 SAP 에서 동기 조회합니다. 호출 즉시 실제 SAP 로 요청이 나가며, 조회 전용이라 DB 상태 변경 없이 응답만 확인합니다.',
  'order-request-cancel':
    'orderRequestId 와 취소 대상 orderProductIds 로 SAP 에 주문 요청 취소를 송신합니다. orderProductIds 를 비우면 아직 취소되지 않은 라인 전체가 대상이 됩니다. 호출 즉시 실제 SAP 로 취소 요청이 전송되지만, 본 테스트는 송신만 수행하며 신규 시스템의 주문 상태는 바꾸지 않습니다.',
  'order-request-register':
    'orderRequestId 로 주문 요청 등록을 sap_outbox 큐에 INSERT 만 합니다. 본 호출 시점에는 SAP 로 직접 송신하지 않으며, 트랜잭셔널 아웃박스 패턴에 따라 SapOutboxWorker 배치가 곧 큐를 폴링하여 실제 SAP 송신을 비동기로 수행합니다. 실송신 시 sap_outbox 행이 새로 적재됩니다.',
  attendance:
    '지정한 날짜의 일반 출근(여사원 일정, TeamMemberSchedule) 데이터 중 한 페이지(page-size 행)를 SAP 으로 송신합니다. 매일 도는 근태 SAP 전송 배치(attendance-sap-batch)의 페이지 단위 처리를 수동으로 1페이지만 재현하는 도구이며, 실송신 시 sap_outbound_log 에 적재됩니다.',
  'display-master':
    '지정한 날짜의 진열사원 일정 마스터(TeamMemberMasterSchedule) 한 페이지를 SAP 으로 송신합니다. 진열 업무 실적을 SAP 에 반영하는 배치(display-master-sap-batch)의 페이지 단위 처리를 수동으로 재현하며, 실송신 시 sap_outbound_log 에 적재됩니다.',
  'ppt-master':
    '기준일(targetDate, 기본 = 오늘)이 속한 월의 활성 전문행사조(PPT) 마스터 첫 페이지를 SAP 으로 송신합니다. 전문 판촉팀 마스터 정보를 SAP 에 전달하는 처리이며, pageSize 로 한 번에 보낼 행 수를 조절할 수 있습니다.',
};

/** 탭 상단 자연어 설명 Alert. key 로 설명을 찾아 표시하며, 미등록 key 는 폴백 문구. */
function ApiDescriptionAlert({
  apiKey,
  title,
  triggerTag,
}: {
  apiKey: string;
  title: string;
  triggerTag?: keyof typeof TRIGGER_TAG_COLOR;
}) {
  return (
    <Alert
      type="info"
      showIcon
      style={{ marginBottom: 16 }}
      message={
        <Space size={8} wrap>
          <Text strong>{title}</Text>
          {triggerTag && (
            <Tag color={TRIGGER_TAG_COLOR[triggerTag]}>{triggerTag}</Tag>
          )}
        </Space>
      }
      description={API_DESCRIPTIONS[apiKey] ?? '등록된 설명이 없습니다.'}
    />
  );
}

const TAB_ITEMS: NonNullable<TabsProps['items']> = [
  {
    key: 'naver-geocode',
    label: 'Naver Geocode',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="naver-geocode"
          title="Naver Geocode — 주소 → 좌표 변환"
        />
        <IntegrationInfoDescriptions apiKey="naver-geocode" />
        <NaverGeocodeTab />
      </Space>
    ),
  },
  {
    key: 'claim-regist',
    label: 'SF 클레임 등록',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="claim-regist"
          title="SF ClaimRegist — 클레임 등록 전송"
        />
        <IntegrationInfoDescriptions apiKey="claim-regist" />
        <Alert
          type="warning"
          showIcon
          message="이 탭은 실제 Salesforce 로 클레임을 INSERT 합니다."
          description="'SF 전송' 버튼은 현재 환경의 SF Apex REST 로 호출이 전송되어 SF 에 클레임 row 가 생성됩니다 (테스트 잡데이터 주의). 신규 DB(claim 테이블)에는 저장하지 않습니다. SYSTEM_ADMIN 권한 필요."
        />
        <ClaimRegistTab />
      </Space>
    ),
  },
  {
    key: 'logistics-claim-regist',
    label: 'SF 물류 클레임 등록',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="logistics-claim-regist"
          title="SF ProposalRegist — 물류 클레임 등록 전송"
        />
        <Alert
          type="info"
          showIcon
          message="이 탭은 아직 SF 로 전송하지 않습니다 (payload 미리보기 전용)."
          description="SF 전송 API 정보가 확보되기 전 단계로, 입력 정보로 구성한 전송 payload(apiMap) 미리보기만 제공합니다. 실제 SF 호출은 추후 추가됩니다. SYSTEM_ADMIN 권한 필요."
        />
        <LogisticsClaimRegistTab />
      </Space>
    ),
  },
  ...SENDER_CONFIGS.map((config) => ({
    key: config.kind,
    label: config.tabLabel,
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey={config.kind}
          title={config.title}
          triggerTag={config.triggerTag}
        />
        <IntegrationInfoDescriptions apiKey={config.kind} />
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
  const [activeKey, setActiveKey] = useState('naver-geocode');

  return (
    <div style={{ padding: 24 }}>
      <Title level={3} style={{ marginBottom: 4 }}>
        외부 API 테스트
      </Title>
      <Text type="secondary">
        backend 에 연동된 외부 API 요청을 왼쪽 탭으로 구분해 직접 호출하고 응답을 확인합니다.
      </Text>

      <Tabs
        tabPosition="left"
        style={{ marginTop: 24 }}
        activeKey={activeKey}
        onChange={setActiveKey}
        items={TAB_ITEMS}
      />
    </div>
  );
}
