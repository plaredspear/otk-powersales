import {
  Alert,
  Anchor,
  Card,
  Col,
  Descriptions,
  Divider,
  Row,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';

const { Title, Paragraph, Text } = Typography;

/**
 * 시스템 개요 — 시스템 관리자 / 개발자가 본 시스템의 전체 구성을 한눈에 파악하기 위한 안내 페이지.
 * 시스템 관리자(profile)만 접근 (라우터 가드).
 *
 * 본 페이지의 서술은 정적(하드코딩) 콘텐츠다. 코드가 크게 바뀌면 수동 갱신이 필요하다.
 * 작성 시점 기준: backend(Spring Boot 4 / Kotlin) · web(React 19) · mobile(Flutter) 코드 실측.
 */

// ───────── 외부 연동 시스템 ─────────
interface IntegrationRow {
  key: string;
  system: string;
  direction: string;
  purpose: string;
  status: 'active' | 'planned';
}

const INTEGRATIONS: IntegrationRow[] = [
  {
    key: 'sap',
    system: 'SAP (ERP / 알라딘)',
    direction: '양방향',
    purpose:
      '주문·취소·출근·진열·여신 정보를 SAP 으로 전송(Outbound)하고, 마스터 데이터를 수신(Inbound). 전송 신뢰성을 위해 Outbox 패턴 사용.',
    status: 'active',
  },
  {
    key: 'orora',
    system: 'Orora 영업시스템',
    direction: '수신',
    purpose: '일/월 매출 실적 등 영업 데이터를 REST/외부 DB(MSSQL) 로 조회하여 적재.',
    status: 'active',
  },
  {
    key: 'sf',
    system: 'Salesforce',
    direction: '수신 + 인증',
    purpose: '권한 모델(Profile / PermissionSet) 의 원천(SoT). SF 로부터 권한 메타를 동기화. 레거시 데이터 마이그레이션 경로.',
    status: 'active',
  },
  {
    key: 'naver',
    system: 'Naver Geocode',
    direction: '호출',
    purpose: '거래처 주소를 위도·경도 좌표로 변환(지오코딩). 배치 + 관리자 도구로 호출.',
    status: 'active',
  },
  {
    key: 'aws',
    system: 'AWS (S3 / Secrets Manager)',
    direction: '호출',
    purpose: '파일 저장(S3) 및 운영 비밀값 관리(Secrets Manager).',
    status: 'active',
  },
  {
    key: 'fcm',
    system: 'Firebase / FCM 푸시',
    direction: '발신',
    purpose: '모바일 푸시 알림. 현재 메시지 엔티티만 존재하고 발송 클라이언트는 미연동 — 추후 예정.',
    status: 'planned',
  },
];

const INTEGRATION_COLUMNS: ColumnsType<IntegrationRow> = [
  { title: '시스템', dataIndex: 'system', key: 'system', width: 200 },
  {
    title: '방향',
    dataIndex: 'direction',
    key: 'direction',
    width: 110,
    render: (v: string) => <Tag>{v}</Tag>,
  },
  { title: '역할', dataIndex: 'purpose', key: 'purpose' },
  {
    title: '상태',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    render: (v: IntegrationRow['status']) =>
      v === 'active' ? <Tag color="green">연동중</Tag> : <Tag color="orange">예정</Tag>,
  },
];

// ───────── 기술 스택 ─────────
interface StackRow {
  layer: string;
  items: string;
}

const STACK_BACKEND: StackRow[] = [
  { layer: '프레임워크', items: 'Spring Boot 4 · Kotlin' },
  { layer: '영속/조회', items: 'JPA(Hibernate) · QueryDSL · Flyway(마이그레이션)' },
  { layer: 'DB / 캐시', items: 'PostgreSQL(주) · Redis(토큰·캐시) · MSSQL(Orora 외부)' },
  { layer: '인증/보안', items: 'JWT(jjwt) · BCrypt' },
  { layer: '배치', items: '@Scheduled · ShedLock(다중 인스턴스 중복 방지)' },
  { layer: '기타', items: 'AWS SDK(S3/Secrets) · Caffeine 캐시 · Apache POI(Excel) · springdoc(OpenAPI)' },
];

const STACK_WEB: StackRow[] = [
  { layer: '프레임워크', items: 'React 19 · TypeScript · Vite' },
  { layer: 'UI', items: 'Ant Design · Pro Layout · ECharts · FullCalendar' },
  { layer: '상태/통신', items: 'TanStack Query(서버 상태) · Zustand(클라 상태) · Axios' },
  { layer: '라우팅', items: 'React Router' },
];

const STACK_MOBILE: StackRow[] = [
  { layer: '프레임워크', items: 'Flutter · Dart' },
  { layer: '상태/통신', items: 'Riverpod · Dio + Retrofit' },
  { layer: '저장/보안', items: 'Hive(로컬) · flutter_secure_storage(토큰)' },
  { layer: '디바이스', items: 'geolocator(GPS) · permission_handler · fl_chart' },
];

const STACK_COLUMNS: ColumnsType<StackRow> = [
  { title: '영역', dataIndex: 'layer', key: 'layer', width: 120 },
  { title: '기술', dataIndex: 'items', key: 'items' },
];

// ───────── 핵심 개념 ─────────
const CONCEPTS: { term: string; desc: string }[] = [
  {
    term: '두 종류의 클라이언트',
    desc: 'Web(관리자 대시보드) 과 Mobile(영업 여사원 앱) 이 같은 Backend API 를 공유한다. 대상이 다르므로 권한·노출 화면이 갈린다.',
  },
  {
    term: 'SF 기반 권한 모델',
    desc: 'Salesforce 의 Profile / PermissionSet 개념을 그대로 차용. 사용자는 Profile 1개 + PermissionSet 다수로 권한이 결정된다.',
  },
  {
    term: '시스템 권한(System Permission)',
    desc: 'VIEW_ALL_DATA / MODIFY_ALL_DATA 등 광역 권한. 엔티티별 CRUD 권한과 별개로, 운영 도구·전체 조회 접근을 통제한다.',
  },
  {
    term: 'SAP Outbox 패턴',
    desc: 'SAP 으로 보낼 메시지를 DB(Outbox) 에 먼저 적재하고, 배치가 이를 읽어 재시도 가능하게 전송한다. 전송 실패 시 유실 방지.',
  },
];

export default function SystemOverviewPage() {
  return (
    <Row gutter={24} wrap={false}>
      <Col flex="auto" style={{ minWidth: 0 }}>
        <Typography>
          <Title level={2}>시스템 개요</Title>
          <Paragraph type="secondary">
            오뚜기 파워세일즈 — B2B 영업 실적 조회 및 목표 관리 시스템. 본 페이지는 시스템 관리자와 개발자가
            전체 구성을 빠르게 파악하도록 돕는 안내서입니다.
          </Paragraph>

          <Alert
            type="info"
            showIcon
            message="이 문서는 정적 안내입니다"
            description="아래 내용은 코드 변경에 따라 자동 갱신되지 않습니다. 큰 구조 변경 시 본 페이지(SystemOverviewPage.tsx)도 함께 갱신해 주세요."
            style={{ margin: '16px 0 24px' }}
          />

          <Divider />

          <Title level={3} id="purpose">
            무엇을 하는 시스템인가
          </Title>
          <Paragraph>
            영업 현장의 여사원이 모바일 앱으로 매출 실적·근무·일정·점검을 조회/입력하고, 본사 관리자가 웹
            대시보드로 이를 관리·집계·운영합니다. 매출/목표 데이터는 외부 시스템(SAP·Orora)에서 들어오고,
            본 시스템은 이를 가공·저장하여 두 클라이언트에 제공합니다.
          </Paragraph>

          <Title level={3} id="architecture">
            전체 구성
          </Title>
          <Paragraph>
            <Text strong>클라이언트 2종 → Backend API → 데이터 저장소 → 외부 연동</Text> 의 계층 구조입니다.
          </Paragraph>
          <Card size="small" style={{ marginBottom: 24, background: '#fafafa' }}>
            <pre style={{ margin: 0, fontFamily: 'monospace', fontSize: 12, lineHeight: 1.7, overflowX: 'auto' }}>
{`  ┌─────────────────┐     ┌─────────────────────┐
  │  Web (관리자)    │     │  Mobile (영업 여사원) │
  │  React 19        │     │  Flutter             │
  └────────┬────────┘     └──────────┬──────────┘
           │   JWT 인증 / REST API     │
           └────────────┬─────────────┘
                        ▼
            ┌───────────────────────┐
            │  Backend (Spring Boot) │
            │  도메인별 서비스 + 배치  │
            └───┬───────────────┬───┘
                ▼               ▼
        ┌──────────────┐  ┌──────────────────────────┐
        │ PostgreSQL   │  │  외부 연동                 │
        │ Redis        │  │  SAP · Orora · SF · Naver  │
        └──────────────┘  └──────────────────────────┘`}
            </pre>
          </Card>

          <Title level={3} id="concepts">
            먼저 알아야 할 핵심 개념
          </Title>
          <Descriptions bordered column={1} size="small" style={{ marginBottom: 24 }}>
            {CONCEPTS.map((c) => (
              <Descriptions.Item key={c.term} label={c.term}>
                {c.desc}
              </Descriptions.Item>
            ))}
          </Descriptions>

          <Title level={3} id="integrations">
            외부 연동 시스템
          </Title>
          <Paragraph type="secondary">
            매출/목표/마스터 데이터는 대부분 외부에서 유입됩니다. 각 연동의 방향과 역할은 다음과 같습니다.
          </Paragraph>
          <Table
            rowKey="key"
            columns={INTEGRATION_COLUMNS}
            dataSource={INTEGRATIONS}
            pagination={false}
            size="small"
            style={{ marginBottom: 24 }}
          />

          <Title level={3} id="stack">
            기술 스택
          </Title>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} lg={8}>
              <Card size="small" title="Backend">
                <Table rowKey="layer" columns={STACK_COLUMNS} dataSource={STACK_BACKEND} pagination={false} size="small" showHeader={false} />
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card size="small" title="Web (관리자)">
                <Table rowKey="layer" columns={STACK_COLUMNS} dataSource={STACK_WEB} pagination={false} size="small" showHeader={false} />
              </Card>
            </Col>
            <Col xs={24} lg={8}>
              <Card size="small" title="Mobile (영업)">
                <Table rowKey="layer" columns={STACK_COLUMNS} dataSource={STACK_MOBILE} pagination={false} size="small" showHeader={false} />
              </Card>
            </Col>
          </Row>

          <Title level={3} id="auth">
            인증과 권한
          </Title>
          <Paragraph>
            <Text strong>로그인</Text>: 사번 + 비밀번호(BCrypt 검증) → JWT 발급. Access Token 으로 API 를
            호출하고, 만료 시 Refresh Token 으로 자동 재발급합니다. Refresh Token 은 회전(rotation) 방식으로
            세션 탈취에 대응합니다.
          </Paragraph>
          <Paragraph>
            <Text strong>권한</Text>: Salesforce 모델을 차용해 사용자마다 <Text code>Profile 1개</Text> +{' '}
            <Text code>PermissionSet 다수</Text> 로 권한이 합산됩니다. 권한은 엔티티별 CRUD
            (<Text code>employee:R</Text> 형식) 와 시스템 권한(<Text code>SYSTEM:VIEW_ALL_DATA</Text>) 으로
            나뉩니다. 자세한 내용은 <Text strong>시스템 → 권한 사용 가이드</Text> 페이지를 참고하세요.
          </Paragraph>

          <Title level={3} id="batch">
            배치 / 스케줄
          </Title>
          <Paragraph>
            출근·진열·주문의 SAP 전송, 매출 수익 집계, 약관 순환, 거래처 지오코딩 등 주기 작업이 배치로
            동작합니다. 다중 인스턴스 환경에서 같은 배치가 중복 실행되지 않도록 ShedLock 으로 잠금을
            겁니다. 실행 이력은 <Text strong>운영 도구 → 스케줄 잡 실행 이력</Text> 에서 확인할 수 있습니다.
          </Paragraph>

          <Divider />
          <Paragraph type="secondary" style={{ fontSize: 12 }}>
            도메인별 상세(어떤 기능이 어느 코드에 있는지), API 카탈로그, 데이터 흐름도(DFD)는 후속 페이지로
            추가될 예정입니다.
          </Paragraph>
        </Typography>
      </Col>
      <Col flex="200px" className="system-docs-anchor-col">
        <Anchor
          offsetTop={80}
          items={[
            { key: 'purpose', href: '#purpose', title: '무엇을 하는가' },
            { key: 'architecture', href: '#architecture', title: '전체 구성' },
            { key: 'concepts', href: '#concepts', title: '핵심 개념' },
            { key: 'integrations', href: '#integrations', title: '외부 연동' },
            { key: 'stack', href: '#stack', title: '기술 스택' },
            { key: 'auth', href: '#auth', title: '인증과 권한' },
            { key: 'batch', href: '#batch', title: '배치 / 스케줄' },
          ]}
        />
      </Col>
    </Row>
  );
}
