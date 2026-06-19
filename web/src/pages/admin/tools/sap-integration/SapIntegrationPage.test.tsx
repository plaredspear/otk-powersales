import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SapIntegrationPage from './SapIntegrationPage';

// 호출 이력/대기/테스트 탭은 stub — 본 테스트는 통합 페이지 탭 구조와 API 별 탭 분리를 검증한다.
// 호출 이력 탭은 별도 탭이 아니라 각 API 상세 카드 안에 인라인으로 렌더되므로,
// stub 이 lockedEndpoint/lockedInterfaceId prop 을 받아 해당 API 로 고정되었음을 노출한다.
vi.mock('../sap-inbound/SapInboundAuditsTab', () => ({
  default: ({ lockedEndpoint }: { lockedEndpoint?: string }) => (
    <div data-testid="inbound-audits-tab">inbound-audits:{lockedEndpoint ?? 'all'}</div>
  ),
}));
vi.mock('../sap-outbound/SapOutboundLogsTab', () => ({
  default: ({ lockedInterfaceId }: { lockedInterfaceId?: string }) => (
    <div data-testid="outbound-logs-tab">outbound-logs:{lockedInterfaceId ?? 'all'}</div>
  ),
}));
vi.mock('../sap-outbound/SapOutboundOutboxTab', () => ({
  default: () => <div data-testid="outbound-outbox-tab">outbound-outbox</div>,
}));
vi.mock('../sap-outbound/SapOutboundTestTab', () => ({
  default: () => <div data-testid="outbound-test-tab">outbound-test</div>,
}));
// Outbound 카탈로그 상세에 인라인되는 외부 연동 정보 조회 hook / 테스트 송신 카드를 stub —
// 본 테스트는 탭 구조와 각 API 상세에 해당 섹션이 인라인됨을 검증한다.
vi.mock('@/api/admin/externalApiIntegrationInfo', () => ({
  useExternalApiIntegrationInfo: (apiKey: string) => ({
    info: {
      key: apiKey,
      externalSystem: `external-system:${apiKey}`,
      endpoint: `https://sap.example.com/${apiKey}`,
      httpMethod: 'POST',
      authType: 'HTTP Basic',
      note: `integration-note:${apiKey}`,
    },
    isLoading: false,
    isError: false,
  }),
}));
vi.mock('../sap-outbound/SapOutboundSenderCard', () => ({
  default: ({ config }: { config: { kind: string } }) => (
    <div data-testid="sender-card">sender-card:{config.kind}</div>
  ),
}));

const inboundCatalog = [
  {
    endpointPath: '/api/v1/sap/organization',
    koreanName: '조직 마스터 수신',
    requiredScope: 'sap.org.write',
    targetEntity: 'Organization',
    controllerClass: 'SapOrganizationMasterController',
    description: '조직 마스터를 수신한다.',
  },
  {
    endpointPath: '/api/v1/sap/employee',
    koreanName: '사원 마스터 수신',
    requiredScope: 'sap.employee.write',
    targetEntity: 'Employee',
    controllerClass: 'SapEmployeeController',
    description: '사원 마스터를 수신한다.',
  },
];

const outboundCatalog = [
  {
    interfaceId: 'SD03300',
    koreanName: '전문행사조 마스터',
    triggerType: 'BATCH' as const,
    senderClass: 'com.otoki.PptMasterSender',
    description: '전문행사조 마스터를 송신한다.',
  },
];

const outboxState = { totalCount: 0 };

vi.mock('@/hooks/admin/useSapInbound', () => ({
  useSapInboundCatalog: () => ({ data: inboundCatalog }),
}));
vi.mock('@/hooks/admin/useSapOutbound', () => ({
  useSapOutboundCatalog: () => ({ data: outboundCatalog }),
  useSapOutboundOutboxPending: () => ({ data: { totalCount: outboxState.totalCount } }),
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <SapIntegrationPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('SapIntegrationPage (SAP 연동 통합 페이지)', () => {
  beforeEach(() => {
    outboxState.totalCount = 0;
  });

  it('H1 - Inbound/Outbound 방향별 그룹 헤더와 고정 탭이 노출', () => {
    renderPage();

    expect(screen.getByText('Inbound')).toBeInTheDocument();
    expect(screen.getByText('Outbound')).toBeInTheDocument();
    // 호출 이력은 별도 탭이 아니라 각 API 상세 안에 인라인 표시되므로 '호출 이력' 탭은 없다.
    expect(screen.queryByRole('tab', { name: '호출 이력' })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /대기 중 \(Outbox\)/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '테스트' })).toBeInTheDocument();
  });

  it('H2 - API 목록 대신 카탈로그의 각 API 가 한글명 라벨의 개별 탭으로 노출', () => {
    renderPage();

    // 단일 'API 목록' 탭은 더 이상 존재하지 않음
    expect(screen.queryByRole('tab', { name: 'API 목록' })).not.toBeInTheDocument();

    // 카탈로그 항목마다 한글명 탭
    expect(screen.getByRole('tab', { name: '조직 마스터 수신' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '사원 마스터 수신' })).toBeInTheDocument();
    // Outbound 탭은 한글명 + 회색 SAP interfaceId 가 함께 렌더되므로 부분 매칭
    expect(screen.getByRole('tab', { name: /전문행사조 마스터/ })).toBeInTheDocument();
  });

  it('H3 - 진입 시 첫 Inbound API 탭이 기본 활성이고 그 API 의 상세가 표시', () => {
    renderPage();
    // 첫 inbound API(조직 마스터 수신) 상세가 기본 노출
    expect(screen.getByText('/api/v1/sap/organization')).toBeInTheDocument();
  });

  it('H4 - Inbound API 탭 클릭 시 해당 API 상세 + 그 API 로 고정된 호출 이력이 인라인 표시', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '조직 마스터 수신' }));

    expect(await screen.findByText('/api/v1/sap/organization')).toBeInTheDocument();
    expect(screen.getByText('sap.org.write')).toBeInTheDocument();
    expect(screen.getByText('Organization')).toBeInTheDocument();
    // 호출 이력이 해당 endpoint 로 고정되어 인라인 렌더
    expect(
      screen.getByText('inbound-audits:/api/v1/sap/organization'),
    ).toBeInTheDocument();
  });

  it('H5 - Outbound API 탭 클릭 시 트리거/Sender + 연동 정보 + 테스트 송신 + 호출 이력이 인라인 표시', async () => {
    renderPage();
    const user = userEvent.setup();
    // 탭 라벨에 한글명 + 회색 SAP interfaceId 가 함께 렌더되므로 부분 매칭으로 선택
    await user.click(screen.getByRole('tab', { name: /전문행사조 마스터/ }));

    // 카탈로그 고유 메타(트리거)와 외부 연동 정보가 하나의 표로 통합 표시
    expect(await screen.findByText('BATCH')).toBeInTheDocument();
    // 외부 연동 정보가 해당 인터페이스의 kind 로 같은 표에 인라인 렌더 (SD03300 → ppt-master)
    expect(screen.getByText('external-system:ppt-master')).toBeInTheDocument();
    expect(screen.getByText('https://sap.example.com/ppt-master')).toBeInTheDocument();
    // 테스트 송신 카드가 해당 인터페이스로 인라인 렌더
    expect(screen.getByText('sender-card:ppt-master')).toBeInTheDocument();
    // 호출 이력이 해당 interfaceId 로 고정되어 인라인 렌더
    expect(screen.getByText('outbound-logs:SD03300')).toBeInTheDocument();
  });

  it('H6 - 대기 큐 건수가 있으면 Outbox 탭 라벨에 건수 표기', () => {
    outboxState.totalCount = 7;
    renderPage();
    expect(
      screen.getByRole('tab', { name: '대기 중 (Outbox) · 7' }),
    ).toBeInTheDocument();
  });

  it('H7 - 그룹 헤더 탭은 클릭 불가(disabled)', () => {
    renderPage();
    const inboundHeader = screen.getByText('Inbound').closest('[role="tab"]');
    expect(inboundHeader).toHaveAttribute('aria-disabled', 'true');
  });
});
