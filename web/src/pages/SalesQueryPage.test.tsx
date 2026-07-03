import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
import SalesQueryPage from './SalesQueryPage';
import * as posApi from '@/api/posSales';
import * as electronicApi from '@/api/electronicSalesDashboard';

vi.mock('@/api/posSales', async () => {
  const actual = await vi.importActual<typeof import('@/api/posSales')>('@/api/posSales');
  return {
    ...actual,
    fetchPosSalesList: vi.fn(),
    fetchPosSalesDetail: vi.fn(),
  };
});

// 조건 옵션/제품 검색은 전산실적 endpoint 재사용 — 해당 api 모듈을 mock.
vi.mock('@/api/electronicSalesDashboard', async () => {
  const actual = await vi.importActual<typeof import('@/api/electronicSalesDashboard')>(
    '@/api/electronicSalesDashboard',
  );
  return {
    ...actual,
    fetchFilterOptions: vi.fn(),
    fetchProductLookup: vi.fn(),
  };
});

// PeriodBranchFilterBar 의 지점 옵션 — 단일지점 사용자로 고정 (본인 지점 자동 선택).
vi.mock('@/hooks/team-schedule/useTeamScheduleBranches', () => ({
  useTeamScheduleBranches: () => ({
    data: [{ branchCode: 'B001', branchName: '원주1지점' }],
  }),
}));

const mockedList = vi.mocked(posApi.fetchPosSalesList);
const mockedFilterOptions = vi.mocked(electronicApi.fetchFilterOptions);
const mockedProductLookup = vi.mocked(electronicApi.fetchProductLookup);

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <SalesQueryPage />
    </QueryClientProvider>,
  );
}

const emptyListResponse: posApi.PosSalesDashboardListResponse = {
  startDate: dayjs().startOf('month').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
  totalSalesAmount: 0,
  totalSalesQuantity: 0,
  items: [],
  pageInfo: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
};

describe('SalesQueryPage (POS매출)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedFilterOptions.mockResolvedValue({
      distributionChannels: ['02 슈퍼'],
      accountTypes: ['6111 이마트'],
      categories: [{ category2: '면류', category3s: ['봉지면', '용기면'] }],
    });
    mockedProductLookup.mockResolvedValue([]);
    mockedList.mockResolvedValue(emptyListResponse);
  });

  it('초기 진입 시 조회기간 기본값이 당월 1일~오늘이고 자동 조회하지 않는다', () => {
    renderPage();
    const inputs = screen
      .getAllByRole('textbox')
      .filter((el) => (el as HTMLInputElement).value.match(/^\d{4}-\d{2}-\d{2}$/));
    expect((inputs[0] as HTMLInputElement).value).toBe(dayjs().startOf('month').format('YYYY-MM-DD'));
    expect((inputs[1] as HTMLInputElement).value).toBe(dayjs().format('YYYY-MM-DD'));
    expect(screen.getByText('조회 조건을 설정한 후 조회 버튼을 눌러주세요.')).toBeInTheDocument();
    expect(mockedList).not.toHaveBeenCalled();
  });

  it('조회 클릭 시 일 단위 기간 + 지점으로 조회하고 합계를 상단에 표시한다', async () => {
    mockedList.mockResolvedValue({
      ...emptyListResponse,
      totalSalesAmount: 1_234_567,
      totalSalesQuantity: 890,
      items: [
        {
          accountId: 1,
          accountName: '이마트 원주점',
          sapAccountCode: 'S001',
          branchCode: 'B001',
          branchName: '원주1지점',
          salesAmount: 1_234_567,
          salesQuantity: 890,
        },
      ],
      pageInfo: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));

    await waitFor(() => {
      expect(screen.getByText('이마트 원주점')).toBeInTheDocument();
      expect(screen.getByText('POS매출 금액 합계')).toBeInTheDocument();
      expect(screen.getByText('POS매출 수량 합계')).toBeInTheDocument();
    });
    expect(mockedList.mock.calls[0][0]).toMatchObject({
      startDate: dayjs().startOf('month').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
      costCenterCodes: ['B001'],
    });
  });

  it('유통형태/거래처유형/분류 옵션이 전산실적 filter-options API 재사용으로 로드된다', async () => {
    renderPage();
    await waitFor(() => expect(mockedFilterOptions).toHaveBeenCalled());
    expect(screen.getByText('유통형태:')).toBeInTheDocument();
    expect(screen.getByText('거래처유형:')).toBeInTheDocument();
    expect(screen.getByText('중분류:')).toBeInTheDocument();
    expect(screen.getByText('소분류:')).toBeInTheDocument();
    expect(screen.getByText('제품 (제품명/제품코드/바코드):')).toBeInTheDocument();
  });

  it('조회 기간이 31일을 초과하면 경고를 표시하고 조회 버튼이 비활성화된다', async () => {
    const user = userEvent.setup();
    renderPage();
    const inputs = screen
      .getAllByRole('textbox')
      .filter((el) => (el as HTMLInputElement).value.match(/^\d{4}-\d{2}-\d{2}$/));
    const startInput = inputs[0] as HTMLInputElement;
    // 시작일을 오늘로부터 60일 전으로 변경 → 31일 초과
    const sixtyDaysAgo = dayjs().subtract(60, 'day').format('YYYY-MM-DD');
    await user.click(startInput);
    await user.clear(startInput);
    await user.type(startInput, `${sixtyDaysAgo}{Enter}`);
    // antd picker 는 blur 시 입력값을 확정하는 경로도 있어 외부 클릭으로 닫는다.
    await user.click(document.body);

    await waitFor(() => {
      expect(screen.getByText('조회 기간은 최대 31일까지 가능합니다')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /조회/ })).toBeDisabled();
    expect(mockedList).not.toHaveBeenCalled();
  });
});
