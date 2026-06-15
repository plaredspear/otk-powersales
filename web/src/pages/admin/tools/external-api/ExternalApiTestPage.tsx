import { useState } from 'react';
import { Alert, Space, Tabs, Typography } from 'antd';
import type { TabsProps } from 'antd';
import NaverGeocodeTab from './NaverGeocodeTab';
import ClaimRegistTab from './ClaimRegistTab';
import ClaimStatusUpdateTab from './ClaimStatusUpdateTab';
import LogisticsClaimRegistTab from './LogisticsClaimRegistTab';
import LogisticsClaimStatusUpdateTab from './LogisticsClaimStatusUpdateTab';
import SalesProgressRateMasterSyncTab from './SalesProgressRateMasterSyncTab';
import IntegrationInfoDescriptions from './IntegrationInfoDescriptions';

const { Title, Text } = Typography;

/**
 * 외부 API 테스트 통합 페이지 (개발자 도구).
 *
 * backend 에 연동된 외부(third-party) API 요청 중 SAP 이외의 연동을 한 곳에서 탭으로
 * 구분해 테스트 호출한다. 스케줄 잡 실행 이력 화면과 동일하게 왼쪽 세로 탭
 * (`tabPosition="left"`)으로 인터페이스를 구분하며, 각 탭 상단에 해당 API 가 어떤 외부
 * 시스템과 무슨 처리를 주고받는지에 대한 자연어 설명 Alert 를 노출한다.
 * Naver Geocode + SF 클레임/물류 클레임 등록이 각각 개별 탭이 되며, 각 탭에서
 * 폼 입력 → 미리보기 → 실송신까지 직접 수행할 수 있다.
 *
 * SAP outbound 인터페이스의 테스트 송신과 호출 이력 조회는 본 페이지가 아니라 "SAP 연동"
 * 페이지(`/admin/tools/sap-integration`)로 일원화되어 있다. SAP 테스트는 그곳의 "테스트"
 * 탭에서 수행하고, 실송신 결과 로그는 같은 페이지의 "호출 이력" / "대기 중 (Outbox)" 탭에서
 * 확인한다.
 * 새 외부(비-SAP) API 가 추가되면 본 탭 배열에 항목 1개를 추가하고 `API_DESCRIPTIONS` 에
 * 자연어 설명을 함께 등록한다.
 */

/** 탭 key → 해당 외부 API 가 어떤 외부 시스템과 무슨 처리를 수행하는지에 대한 자연어 설명. */
const API_DESCRIPTIONS: Record<string, string> = {
  'naver-geocode':
    '입력한 도로명/지번 주소를 Naver Cloud Platform 의 Maps Geocode API 로 전송하여 위도·경도 좌표로 변환합니다. backend 가 받은 응답을 가공 없이 raw JSON 그대로 노출하므로 좌표 변환 정확도와 API 키 동작을 직접 확인할 수 있습니다. 거래처 좌표 보강 배치(account-naver-geocode-batch)가 내부적으로 호출하는 것과 동일한 경로입니다. 조회 전용이라 DB 변경은 발생하지 않습니다.',
  'claim-regist':
    '입력한 파라미터로 클레임 등록 apiMap 을 구성하여 Salesforce Apex REST `/services/apexrest/mobile/ClaimRegist` 로 직접 POST 합니다. 레거시 Heroku FieldTalkController 가 클레임 등록 시 호출하던 것과 동일한 SF endpoint 이며, 운영 클레임 등록(dual-write)과 달리 본 테스트는 신규 DB(claim 테이블)에는 저장하지 않고 SF 로만 전송합니다. SF 가 돌려준 RESULT_CODE/RESULT_MSG 와 전송 payload 미리보기를 그대로 노출합니다. 이미지 3종은 모두 선택이며, 미첨부 시 빈 Buffer 로 전송됩니다.',
  'claim-status-update':
    '기준 일자(MOD_DT, YYYYMMDD) 하나를 Salesforce Apex REST `/services/apexrest/mobile/IF_SendClaimToPWS` 로 POST 하면, SF 가 해당 일자 기준으로 변경된 클레임 마스터 목록(제품코드/거래처/상태/조치 등 32개 필드)을 응답하는 SF → PWS 방향의 조회 인터페이스입니다("알라딘 클레임 마스터 API" 문서 정합). 클레임 등록과 동일한 OAuth2(Bearer) + 401 재시도 경로로 호출하며, SF 응답을 결과 테이블 + raw JSON 그대로 노출합니다. 조회 전용이라 신규 DB 에는 저장하지 않습니다.',
  'logistics-claim-status-update':
    'SF 에 등록된 물류 클레임(제안)의 처리 상태(접수/처리중/완료/반려 등)를 업데이트하는 전송 테스트 탭입니다. UI 레이아웃은 "SF 클레임 상태 업데이트" 탭과 동일한 형태로 구성되어 있으며, 상세 파라미터 계약과 실제 SF 전송 로직은 추후 반영될 예정입니다. 현재 단계에서는 폼 레이아웃만 제공하며 백엔드/SF 호출은 수행하지 않습니다.',
  'sales-progress-rate-master-sync':
    '거래처목표등록마스터(SalesProgressRateMaster__c)를 Salesforce 에서 조회하여 신규 DB 로 갱신하는 주기 동기화 배치(매시 정각)를 즉시 1회 수동 실행합니다. 배치와 동일하게 ExternalKey(연+월+거래처코드) 기준으로 upsert 하며(없으면 INSERT, 있으면 목표/실적/진도율/지점 컬럼 UPDATE), 삭제는 반영하지 않습니다. 실행 즉시 실제 DB 에 반영되며 조회/추가/갱신/건너뜀 통계를 표시합니다. SF fetch 통신부가 아직 미구현(TODO)인 동안에는 조회 0건의 no-op 으로 동작합니다.',
  'logistics-claim-regist':
    '모바일 물류 클레임 등록(제안하기 > 물류 클레임) 입력 정보를 토대로 Salesforce Apex REST `IF_REST_MOBILE_ProposalRegist` 전송 payload(apiMap) 미리보기를 구성합니다. SF 전송 API 정보가 아직 확보되지 않은 단계라 실제 SF POST 는 수행하지 않고, 레거시 Input 클래스 key 셋(Category/ProductCode/accountCode/EmployeeCode/Title/Description/CarNumber/claimList/logclaimDate/S3Image* 등) 정합의 apiMap 을 JSON 으로만 노출합니다. 추후 SF endpoint/계약 정보를 받으면 실제 전송 호출을 추가할 예정입니다. 사진은 최대 2장이며 모두 선택입니다.',
};

/** 탭 상단 자연어 설명 Alert. key 로 설명을 찾아 표시하며, 미등록 key 는 폴백 문구. */
function ApiDescriptionAlert({
  apiKey,
  title,
}: {
  apiKey: string;
  title: string;
}) {
  return (
    <Alert
      type="info"
      showIcon
      style={{ marginBottom: 16 }}
      message={<Text strong>{title}</Text>}
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
    key: 'claim-status-update',
    label: 'SF 클레임 상태 업데이트',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="claim-status-update"
          title="SF IF_SendClaimToPWS — 클레임 마스터 조회"
        />
        <IntegrationInfoDescriptions apiKey="claim-status-update" />
        <Alert
          type="info"
          showIcon
          message="이 탭은 SF 에서 클레임 마스터를 조회합니다 (조회 전용 — DB 변경 없음)."
          description="'SF 조회' 버튼은 입력한 기준 일자(MOD_DT)로 SF Apex REST IF_SendClaimToPWS 로 호출하여 변경 클레임 마스터 목록을 받아옵니다. 신규 DB 에는 저장하지 않습니다. SYSTEM_ADMIN 권한 필요."
        />
        <ClaimStatusUpdateTab />
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
  {
    key: 'logistics-claim-status-update',
    label: 'SF 물류 클레임 상태 업데이트',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="logistics-claim-status-update"
          title="SF 물류 클레임 상태 업데이트 — 상태 변경 전송"
        />
        <Alert
          type="info"
          showIcon
          message="이 탭은 아직 SF 로 전송하지 않습니다 (UI 레이아웃 전용)."
          description="상세 파라미터 계약과 실제 SF 전송 로직은 추후 반영될 예정입니다. 현재는 'SF 클레임 상태 업데이트' 탭과 동일한 형태의 폼 레이아웃만 제공합니다. SYSTEM_ADMIN 권한 필요."
        />
        <LogisticsClaimStatusUpdateTab />
      </Space>
    ),
  },
  {
    key: 'sales-progress-rate-master-sync',
    label: 'SF 거래처목표등록마스터 동기화',
    children: (
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <ApiDescriptionAlert
          apiKey="sales-progress-rate-master-sync"
          title="SF 거래처목표등록마스터 동기화 — SF 조회 후 DB 갱신"
        />
        <Alert
          type="warning"
          showIcon
          message="이 탭은 실제 DB 에 동기화 결과를 반영합니다."
          description="'지금 동기화 실행' 버튼은 주기 배치와 동일한 SF 조회 → ExternalKey upsert 경로를 즉시 실행하여 신규 DB(sales_progress_rate_master)에 반영합니다. SF fetch 통신부가 아직 미구현(TODO)인 동안에는 조회 0건의 no-op 으로 동작합니다. SYSTEM_ADMIN 권한 필요."
        />
        <SalesProgressRateMasterSyncTab />
      </Space>
    ),
  },
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
