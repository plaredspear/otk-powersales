import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SapIntegrationPage from './SapIntegrationPage';

// 호출 이력/대기/테스트 탭은 stub — 본 테스트는 통합 페이지 탭 구조와 API 별 탭 분리를 검증한다.
vi.mock('../sap-inbound/SapInboundAuditsTab', () => ({
  default: () => <div data-testid="inbound-audits-tab">inbound-audits</div>,
}));
vi.mock('../sap-outbound/SapOutboundLogsTab', () => ({
  default: () => <div data-testid="outbound-logs-tab">outbound-logs</div>,
}));
vi.mock('../sap-outbound/SapOutboundOutboxTab', () => ({
  default: () => <div data-testid="outbound-outbox-tab">outbound-outbox</div>,
}));
vi.mock('../sap-outbound/SapOutboundTestTab', () => ({
  default: () => <div data-testid="outbound-test-tab">outbound-test</div>,
}));

const inboundCatalog = [
  {
    endpointPath: '/api/v1/sap/organization',
    koreanName: '조직 마스터 수신',
    requiredScope: 'sap.org.write',
    targetEntity: 'OrganizeMaster',
    controllerClass: 'SapOrganizationController',
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
    expect(screen.getAllByRole('tab', { name: '호출 이력' })).toHaveLength(2);
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
    expect(screen.getByRole('tab', { name: '전문행사조 마스터' })).toBeInTheDocument();
  });

  it('H3 - 진입 시 Inbound 호출 이력 탭이 기본 활성', () => {
    renderPage();
    expect(screen.getByTestId('inbound-audits-tab')).toBeInTheDocument();
  });

  it('H4 - Inbound API 탭 클릭 시 해당 API 상세(Endpoint/Scope/적재 대상)가 표시', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '조직 마스터 수신' }));

    expect(await screen.findByText('/api/v1/sap/organization')).toBeInTheDocument();
    expect(screen.getByText('sap.org.write')).toBeInTheDocument();
    expect(screen.getByText('OrganizeMaster')).toBeInTheDocument();
  });

  it('H5 - Outbound API 탭 클릭 시 Interface ID 와 트리거가 표시', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '전문행사조 마스터' }));

    expect(await screen.findByText('SD03300')).toBeInTheDocument();
    expect(screen.getByText('BATCH')).toBeInTheDocument();
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
