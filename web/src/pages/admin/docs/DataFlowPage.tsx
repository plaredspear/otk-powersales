import { Alert, Card, Divider, Typography } from 'antd';
import Mermaid from '@/components/common/Mermaid';

const { Title, Paragraph, Text } = Typography;

/**
 * 데이터 흐름(DFD) — 외부 시스템 유입부터 화면 노출까지 주요 데이터 흐름을 Mermaid 로 시각화.
 * 시스템 관리자(profile)만 접근 (라우터 가드).
 *
 * 본 페이지의 다이어그램은 정적(하드코딩) 콘텐츠다. 흐름이 바뀌면 수동 갱신이 필요하다.
 * 최종 갱신: 2026-05-31, backend(orora/sales/sap/auth/schedule/order/batch) 코드 실측.
 */

// 1) 매출/실적 유입 흐름
const SALES_FLOW = `flowchart LR
  ORORA[(Orora MSSQL\\n월매출 view)]
  SAP[SAP / ERP]
  subgraph BE[Backend]
    GW[Orora 조회 게이트웨이\\norora 도메인]
    INBOUND[SAP inbound REST\\n/api/v1/sap/daily-sales-history]
    SVC[매출 서비스\\nsales 도메인]
    BATCH[월/전월 매출 갱신 배치\\nShedLock]
  end
  DB[(PostgreSQL\\ndaily_sales_history 등)]
  WEB[웹 대시보드]
  MOB[모바일 앱]

  SAP -->|일일 ERP 매출/배부액 push| INBOUND
  INBOUND -->|UPSERT| DB
  ORORA -->|실시간 조회| GW
  GW --> SVC
  DB --> SVC
  DB -->|집계 읽기| BATCH
  SVC -->|관리자 집계 조회| WEB
  SVC -->|영업사원 조회| MOB`;

// 2) 주문 발주 흐름 (Outbox)
const ORDER_FLOW = `flowchart LR
  MOB[모바일 앱\\n발주/취소]
  subgraph BE[Backend]
    ORD[주문 서비스\\norder 도메인]
    OUTBOX[(SAP Outbox\\n테이블)]
    OBATCH[Outbox 전송 배치]
  end
  SAP[SAP / ERP]

  MOB -->|주문 요청| ORD
  ORD -->|메시지 적재| OUTBOX
  OBATCH -->|미전송 읽기| OUTBOX
  OBATCH -->|재시도 가능 전송| SAP
  SAP -.->|결과/상태| OBATCH`;

// 3) 출근/진열 → SAP 흐름
const ATTENDANCE_FLOW = `flowchart LR
  MOB[모바일 앱\\n출근/진열 입력]
  subgraph BE[Backend]
    SCH[출근 서비스\\nschedule 도메인]
    DB[(PostgreSQL\\nattendance_log)]
    ABATCH[근무일정 SAP Outbound 배치\\n매일 01:00 KST]
    DBATCH[진열 SAP Outbound 배치\\n매일 01:00 KST]
  end
  SAP[SAP / ERP]

  MOB -->|출근/진열 입력| SCH
  SCH --> DB
  ABATCH -->|근무일정 집계| DB
  DBATCH -->|진열 마스터 집계| DB
  ABATCH -->|일괄 전송| SAP
  DBATCH -->|일괄 전송| SAP`;

// 4) 인증/권한 흐름
const AUTH_FLOW = `flowchart LR
  SF[Salesforce]
  subgraph BE[Backend]
    MIG[초기 적재\\nsfmigration 일회성]
    DB[(PostgreSQL\\nProfile / PermissionSet)]
    AUTH[인증 서비스\\nauth 도메인]
    CACHE[권한 캐시\\nAdminPermissionCache]
  end
  CLIENT[웹 / 모바일]

  SF -->|Profile/PermissionSet 적재| MIG
  MIG --> DB
  CLIENT -->|사번 + 비밀번호| AUTH
  AUTH -->|JWT 발급 \\n(role claim)| CLIENT
  CLIENT -->|API 호출 + JWT| CACHE
  CACHE -->|권한 lazy 조회| DB`;

interface FlowSection {
  key: string;
  title: string;
  chart: string;
  desc: React.ReactNode;
}

const SECTIONS: FlowSection[] = [
  {
    key: 'sales',
    title: '1. 매출 / 실적 유입',
    chart: SALES_FLOW,
    desc: (
      <>
        매출·실적의 원천은 두 외부 시스템이며 유입 방식이 다릅니다. <Text strong>SAP</Text> 는 일일 ERP
        매출·배부액을 backend 의 inbound REST 로 push 하여 <Text code>daily_sales_history</Text> 에
        UPSERT 됩니다. <Text strong>Orora</Text> 는 별도 MSSQL 의 월매출 view 를 조회 게이트웨이가{' '}
        <Text strong>실시간 직결 조회</Text>합니다(배치 적재 아님). 별도 배치(ShedLock)는 적재된 매출을
        읽어 월/전월 실적 지표를 갱신하는 역할입니다. 웹 관리자(집계)와 모바일 앱(개인 실적)이 이를
        조회합니다.
      </>
    ),
  },
  {
    key: 'order',
    title: '2. 주문 발주 (Outbox 패턴)',
    chart: ORDER_FLOW,
    desc: (
      <>
        영업사원이 모바일에서 발주/취소를 요청하면 backend 는 곧장 SAP 으로 보내지 않고{' '}
        <Text strong>Outbox 테이블에 메시지를 먼저 적재</Text>합니다. 별도 배치가 미전송 메시지를 읽어 SAP
        으로 전송하므로, 전송 실패 시에도 유실 없이 재시도할 수 있습니다.
      </>
    ),
  },
  {
    key: 'attendance',
    title: '3. 출근 / 진열 → SAP',
    chart: ATTENDANCE_FLOW,
    desc: (
      <>
        모바일에서 입력한 출근·진열 정보는 <Text code>attendance_log</Text> 에 저장된 뒤, 매일 새벽
        01:00 배치가 이를 집계해 SAP 으로 일괄 전송합니다. 실시간 전송이 아니라{' '}
        <Text strong>배치 기반 동기</Text>라는 점이 주문 흐름과 다릅니다. 일반 출근과 진열은 입력 진입점은
        공유하지만 <Text strong>전송 배치가 각각 분리</Text>되어 있습니다(출근/진열 집계 조건이 다름).
      </>
    ),
  },
  {
    key: 'auth',
    title: '4. 인증 / 권한',
    chart: AUTH_FLOW,
    desc: (
      <>
        권한 모델(Profile / PermissionSet) 의 원천은 <Text strong>Salesforce</Text> 이며, 전환 시점에{' '}
        <Text strong>일회성 마이그레이션</Text>으로 DB 에 적재됩니다(상시 동기 배치 아님 — 이후 권한 변경은
        관리자 화면에서 수행). 사용자가 사번+비밀번호로 로그인하면 인증 서비스는 <Text code>role</Text>{' '}
        claim 만 담은 JWT 를 발급합니다(권한 목록은 헤더 크기 제한으로 토큰에 싣지 않음). 이후 API 호출
        시 권한 캐시(<Text code>AdminPermissionCache</Text>)가 DB 에서 권한을 지연 조회해 접근을
        판정합니다.
      </>
    ),
  },
];

export default function DataFlowPage() {
  return (
    <Typography>
      <Title level={2}>데이터 흐름 (DFD)</Title>
      <Paragraph type="secondary">
        외부 시스템에서 데이터가 어떻게 들어와 가공·저장되고 화면까지 도달하는지, 주요 흐름을 다이어그램으로
        정리했습니다. "이 데이터는 어디서 와서 어디로 가는가" 를 이해하는 용도입니다.
      </Paragraph>

      <Alert
        type="info"
        showIcon
        message="이 다이어그램은 정적 안내입니다"
        description="흐름은 코드 변경에 따라 자동 갱신되지 않습니다. 연동/배치 구조가 바뀌면 본 페이지(DataFlowPage.tsx)의 Mermaid 정의도 함께 갱신해 주세요."
        style={{ margin: '12px 0 24px' }}
      />

      {SECTIONS.map((s) => (
        <div key={s.key}>
          <Title level={3}>{s.title}</Title>
          <Paragraph>{s.desc}</Paragraph>
          <Card size="small" style={{ marginBottom: 8, overflowX: 'auto' }}>
            <Mermaid chart={s.chart} />
          </Card>
          <Divider />
        </div>
      ))}

      <Paragraph type="secondary" style={{ fontSize: 12 }}>
        엔드포인트 단위의 계약은 API 카탈로그를, 도메인별 책임은 도메인/모듈 맵을 참고하세요.
      </Paragraph>
    </Typography>
  );
}
