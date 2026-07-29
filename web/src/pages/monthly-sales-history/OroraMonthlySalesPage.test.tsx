import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OroraMonthlySalesPage from './OroraMonthlySalesPage';
import * as api from '@/api/monthlySalesHistory';

// 기준정보 > ORORA 월매출 — 목록 조회 API 만 stub (거래처 고급 검색 모달은 열지 않는 시나리오).
vi.mock('@/api/monthlySalesHistory', async () => {
  const actual = await vi.importActual<typeof import('@/api/monthlySalesHistory')>(
    '@/api/monthlySalesHistory',
  );
  return { ...actual, fetchMonthlySalesHistories: vi.fn() };
});

const mockedFetch = vi.mocked(api.fetchMonthlySalesHistories);

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <OroraMonthlySalesPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('OroraMonthlySalesPage', () => {
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

  it('거래처코드 + 매출년월로 조회하면 온도대별 금액과 합계를 표시한다', async () => {
    mockedFetch.mockResolvedValue({
      salesMonth: '202607',
      sapAccountCode: '1000000',
      accountName: 'GS25 역삼점',
      branchName: '강남지점',
      content: [
        {
          id: 1,
          salesYear: '2026',
          salesMonth: '07',
          sapAccountCode: '1000000',
          externalKey: '1000000202607',
          abcClosingAmount1: 5000,
          abcClosingAmount2: 3000,
          abcClosingAmount3: 2000,
          abcClosingAmount4: 2000,
          abcClosingSumAmount: 12000,
          shipClosingAmount1: 1000,
          shipClosingAmount2: 900,
          shipClosingAmount3: 800,
          shipClosingAmount4: 700,
          shipClosingSumAmount: 3400,
          isDeleted: false,
          createdAt: '2026-08-09T11:30:00',
          updatedAt: '2026-08-09T11:30:00',
        },
      ],
      totalAbcClosingAmount: 12000,
      totalShipClosingAmount: 3400,
      lastMaterializedAt: '2026-08-09T11:35:00',
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
    expect(await screen.findByText('2026년 07월')).toBeInTheDocument();
    expect(screen.getByText('1000000202607')).toBeInTheDocument();
    expect(screen.getByText(/GS25 역삼점/)).toBeInTheDocument();

    // 온도대별 금액 컬럼 (전산/물류 각 4종) — 그룹 헤더 아래 상온/라면/... 이 각 그룹에 1개씩.
    // ResizableTable 이 헤더 셀을 측정용으로 중복 렌더링하므로 개수 대신 존재만 확인한다.
    expect(screen.getByText('전산마감실적 (원)')).toBeInTheDocument();
    expect(screen.getByText('물류마감실적 (원)')).toBeInTheDocument();
    expect(screen.getAllByText('상온').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('냉장냉동').length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText('5,000')).toBeInTheDocument();
    expect(screen.getByText('700')).toBeInTheDocument();

    // 금액 합계 — 상단 요약(Statistic). 값은 적재된 합계 컬럼 기준.
    expect(screen.getByText('전산마감실적 합계')).toBeInTheDocument();
    expect(screen.getByText('물류마감실적 합계')).toBeInTheDocument();
    // 셀(12,000) + Statistic(12,000) 양쪽에 표시.
    expect(screen.getAllByText('12,000').length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText('3,400').length).toBeGreaterThanOrEqual(2);

    // 마지막 적재 시각 — 조회한 매출년월 기준 (조회 결과 행의 최신 적재일시).
    expect(
      screen.getByText(/마지막 적재 시각: 2026-08-09 11:35 \(2026년 07월 기준\)/),
    ).toBeInTheDocument();

    // 삭제 행이 없으면 합계 모수 안내를 띄우지 않는다.
    expect(screen.queryByText(/합계에서는 제외되었습니다/)).not.toBeInTheDocument();
  });

  it('soft-delete 행은 목록에 삭제됨으로 표시하고 합계 제외 안내를 띄운다', async () => {
    const row = {
      id: 1,
      salesYear: '2026',
      salesMonth: '07',
      sapAccountCode: '1000000',
      externalKey: '1000000202607',
      abcClosingAmount1: null,
      abcClosingAmount2: null,
      abcClosingAmount3: null,
      abcClosingAmount4: null,
      abcClosingSumAmount: 7777,
      shipClosingAmount1: null,
      shipClosingAmount2: null,
      shipClosingAmount3: null,
      shipClosingAmount4: null,
      shipClosingSumAmount: 999,
      isDeleted: true,
      createdAt: '2026-08-09T11:30:00',
      updatedAt: '2026-08-09T11:30:00',
    };
    mockedFetch.mockResolvedValue({
      salesMonth: '202607',
      sapAccountCode: '1000000',
      accountName: 'GS25 역삼점',
      branchName: '강남지점',
      content: [row],
      // 서버가 삭제 행을 합계에서 제외하므로 합계는 0.
      totalAbcClosingAmount: 0,
      totalShipClosingAmount: 0,
      lastMaterializedAt: '2026-08-09T11:30:00',
    });

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByPlaceholderText('거래처코드'), '1000000');
    await user.click(screen.getByRole('button', { name: '조회' }));

    expect(await screen.findByText('삭제됨')).toBeInTheDocument();
    expect(
      screen.getByText(/삭제된 행 1건은 목록에만 표시되고 위 합계에서는 제외되었습니다/),
    ).toBeInTheDocument();
  });
});
