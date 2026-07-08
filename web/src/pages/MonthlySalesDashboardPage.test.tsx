import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import MonthlySalesDashboardPage from './MonthlySalesDashboardPage';
import * as api from '@/api/monthlySalesDashboard';

vi.mock('@/api/monthlySalesDashboard', async () => {
  const actual = await vi.importActual<typeof import('@/api/monthlySalesDashboard')>(
    '@/api/monthlySalesDashboard',
  );
  return {
    ...actual,
    fetchSummary: vi.fn(),
    fetchList: vi.fn(),
    fetchDetail: vi.fn(),
  };
});

// 월별 추이 차트(ECharts)는 jsdom canvas 미지원으로 렌더 시 크래시하므로 stub 처리.
vi.mock('@/components/charts/MonthlyTrendChart', () => ({
  default: () => null,
}));

// 월 매출(물류배부) 지점 셀렉터 — 대시보드와 동일한 다중 지점 화이트리스트로 고정.
vi.mock('@/hooks/sales/useMonthlySalesBranches', () => ({
  useMonthlySalesBranches: () => ({
    data: [
      { branchCode: '5817', branchName: '강남1지점' },
      { branchCode: '5824', branchName: '강남2지점' },
      { branchCode: '5823', branchName: '강남3지점' },
    ],
  }),
}));

const mockedSummary = vi.mocked(api.fetchSummary);
const mockedList = vi.mocked(api.fetchList);

function renderPage(initialUrl: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialUrl]}>
        <MonthlySalesDashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const emptySummary: api.MonthlySalesDashboardSummary = {
  salesYear: 2026,
  salesMonth: 7,
  totalTargetAmount: 0,
  totalAchievedAmount: 0,
  overallAchievementRate: 0,
  referenceAchievementRate: 0,
  totalLastYearAchievedAmount: null,
  lastYearComparisonRatio: null,
  monthlyTrend: [],
};

const emptyList: api.MonthlySalesDashboardListResponse = {
  items: [],
  pageInfo: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
} as unknown as api.MonthlySalesDashboardListResponse;

describe('MonthlySalesDashboardPage — 대시보드 지점 전달', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedSummary.mockResolvedValue(emptySummary);
    mockedList.mockResolvedValue(emptyList);
  });

  it('URL branchCodes 로 진입하면 해당 지점들이 선택되고 자동 조회된다', async () => {
    renderPage('/sales/monthly?yearMonth=2026-07&deploymentFilter=deployed&branchCodes=5817,5824');

    // 전달된 조회월(2026-07) + 선택 지점으로 별도 조회 버튼 클릭 없이 자동 조회된다.
    await waitFor(() => {
      expect(mockedList).toHaveBeenCalled();
    });
    expect(mockedList.mock.calls[0][0]).toMatchObject({
      year: 2026,
      month: 7,
      costCenterCodes: ['5817', '5824'],
      deploymentFilter: 'deployed',
    });
  });

  it('URL branchCodes 없이 진입하면 자동 조회하지 않는다', async () => {
    renderPage('/sales/monthly?yearMonth=2026-07&deploymentFilter=deployed');

    // 지점 미선택이므로 셀렉터 placeholder 가 그대로 노출된다.
    expect(screen.getByText('지점 선택')).toBeInTheDocument();
    // 잠시 대기해도 자동 조회가 발생하지 않는다.
    await new Promise((r) => setTimeout(r, 50));
    expect(mockedList).not.toHaveBeenCalled();
  });
});
