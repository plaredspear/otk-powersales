import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OroraDailySalesPage from './OroraDailySalesPage';
import * as api from '@/api/dailySalesHistory';

// 기준정보 > ORORA 일매출 — 목록 조회 API 만 stub (거래처 고급 검색 모달은 열지 않는 시나리오).
vi.mock('@/api/dailySalesHistory', async () => {
  const actual = await vi.importActual<typeof import('@/api/dailySalesHistory')>(
    '@/api/dailySalesHistory',
  );
  return { ...actual, fetchDailySalesHistories: vi.fn() };
});

const mockedFetch = vi.mocked(api.fetchDailySalesHistories);

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <OroraDailySalesPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('OroraDailySalesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('진입 시에는 조회 전 안내만 표시하고 API 를 호출하지 않는다', () => {
    renderPage();

    expect(
      screen.getByText('거래처와 매출발생년월을 선택한 후 조회 버튼을 눌러주세요.'),
    ).toBeInTheDocument();
    expect(mockedFetch).not.toHaveBeenCalled();
  });

  it('거래처코드 없이 조회하면 API 를 호출하지 않는다 (필수 조건)', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: '조회' }));

    await waitFor(() => expect(mockedFetch).not.toHaveBeenCalled());
  });

  it('거래처코드 + 매출월로 조회하면 일별 행과 금액 합계를 표시한다', async () => {
    mockedFetch.mockResolvedValue({
      salesMonth: '202607',
      sapAccountCode: '1000000',
      accountName: 'GS25 역삼점',
      branchName: '강남지점',
      content: [
        {
          id: 1,
          salesDate: '20260731',
          sapAccountCode: '1000000',
          externalKey: '100000020260731',
          erpSalesAmount: 1000,
          erpDistributionAmount: 200,
          ledgerAmount: null,
          createdAt: '2026-07-31T11:00:00',
          updatedAt: '2026-07-31T11:00:00',
        },
      ],
      totalErpSalesAmount: 1000,
      totalErpDistributionAmount: 200,
      totalLedgerAmount: 0,
      lastMaterializedAt: '2026-07-31T11:05:00',
    });

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('거래처코드'), '1000000');
    await user.click(screen.getByRole('button', { name: '조회' }));

    await waitFor(() =>
      expect(mockedFetch).toHaveBeenCalledWith(
        expect.objectContaining({ accountCode: '1000000' }),
      ),
    );
    expect(await screen.findByText('2026-07-31')).toBeInTheDocument();
    expect(screen.getByText('100000020260731')).toBeInTheDocument();
    expect(screen.getByText(/GS25 역삼점/)).toBeInTheDocument();

    // 금액 합계 — 상단 요약(Statistic) + 테이블 하단 Summary 행 양쪽에 표시.
    expect(screen.getByText('전산매출실적 합계')).toBeInTheDocument();
    expect(screen.getByText('물류배부매출실적 합계')).toBeInTheDocument();
    expect(screen.getAllByText('1,000').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('200').length).toBeGreaterThanOrEqual(2);

    // 마지막 적재 시각 — 조회한 매출월 기준 (조회 결과 행의 최신 적재일시).
    expect(
      screen.getByText(/마지막 적재 시각: 2026-07-31 11:05 \(2026년 07월 기준\)/),
    ).toBeInTheDocument();
  });
});
