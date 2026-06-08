import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SapIntegrationPage from './SapIntegrationPage';

// 자식 탭 컴포넌트는 stub 으로 대체 — 본 테스트는 통합 페이지의 탭 구조/레이아웃만 검증한다.
vi.mock('../sap-inbound/SapInboundAuditsTab', () => ({
  default: () => <div data-testid="inbound-audits-tab">inbound-audits</div>,
}));
vi.mock('../sap-inbound/SapInboundCatalogTab', () => ({
  default: () => <div data-testid="inbound-catalog-tab">inbound-catalog</div>,
}));
vi.mock('../sap-outbound/SapOutboundLogsTab', () => ({
  default: () => <div data-testid="outbound-logs-tab">outbound-logs</div>,
}));
vi.mock('../sap-outbound/SapOutboundOutboxTab', () => ({
  default: () => <div data-testid="outbound-outbox-tab">outbound-outbox</div>,
}));
vi.mock('../sap-outbound/SapOutboundCatalogTab', () => ({
  default: () => <div data-testid="outbound-catalog-tab">outbound-catalog</div>,
}));
vi.mock('../sap-outbound/SapOutboundTestTab', () => ({
  default: () => <div data-testid="outbound-test-tab">outbound-test</div>,
}));

const outboxState = { totalCount: 0 };
vi.mock('@/hooks/admin/useSapOutbound', () => ({
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

  it('H1 - Inbound/Outbound 방향별 그룹 헤더와 6개 실제 탭이 모두 노출', () => {
    renderPage();

    // 방향 그룹 헤더 (disabled 탭)
    expect(screen.getByText('Inbound')).toBeInTheDocument();
    expect(screen.getByText('Outbound')).toBeInTheDocument();

    // 실제 탭 라벨 (호출 이력/API 목록은 양 방향에 중복되므로 개수로 검증)
    expect(screen.getAllByRole('tab', { name: '호출 이력' })).toHaveLength(2);
    expect(screen.getAllByRole('tab', { name: 'API 목록' })).toHaveLength(2);
    expect(screen.getByRole('tab', { name: /대기 중 \(Outbox\)/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '테스트' })).toBeInTheDocument();
  });

  it('H2 - 진입 시 Inbound 호출 이력 탭이 기본 활성', () => {
    renderPage();
    expect(screen.getByTestId('inbound-audits-tab')).toBeInTheDocument();
  });

  it('H3 - Outbound 테스트 탭으로 전환 시 해당 콘텐츠 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '테스트' }));
    expect(await screen.findByTestId('outbound-test-tab')).toBeInTheDocument();
  });

  it('H4 - 대기 큐 건수가 있으면 Outbox 탭 라벨에 건수 표기', () => {
    outboxState.totalCount = 7;
    renderPage();
    expect(
      screen.getByRole('tab', { name: '대기 중 (Outbox) · 7' }),
    ).toBeInTheDocument();
  });

  it('H5 - 그룹 헤더 탭은 클릭 불가(disabled)', () => {
    renderPage();
    const inboundHeader = screen.getByText('Inbound').closest('[role="tab"]');
    expect(inboundHeader).toHaveAttribute('aria-disabled', 'true');
  });
});
