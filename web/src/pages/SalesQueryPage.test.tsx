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
    fetchPosSalesAccounts: vi.fn(),
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
vi.mock('@/hooks/sales/usePosSalesBranches', () => ({
  usePosSalesBranches: () => ({
    data: [{ branchCode: 'B001', branchName: '원주1지점' }],
  }),
}));

const mockedAccounts = vi.mocked(posApi.fetchPosSalesAccounts);
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

const accountsResponse: posApi.PosSalesAccountListResponse = {
  totalElements: 2,
  items: [
    { accountId: 1, accountName: '이마트 원주점', sapAccountCode: 'S001', branchCode: 'B001', branchName: '원주1지점' },
    { accountId: 2, accountName: '홈플러스 원주점', sapAccountCode: 'S002', branchCode: 'B001', branchName: '원주1지점' },
  ],
};

const emptyListResponse: posApi.PosSalesDashboardListResponse = {
  startDate: dayjs().startOf('month').format('YYYY-MM-DD'),
  endDate: dayjs().format('YYYY-MM-DD'),
  totalSalesAmount: 0,
  totalSalesQuantity: 0,
  items: [],
  pageInfo: { page: 0, size: 20, totalElements: 0, totalPages: 0 },
};

describe('SalesQueryPage (POS매출 2단 조회)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedFilterOptions.mockResolvedValue({
      distributionChannels: ['02 슈퍼'],
      accountTypes: ['6111 이마트'],
      categories: [{ category2: '면류', category3s: ['봉지면', '용기면'] }],
    });
    mockedProductLookup.mockResolvedValue([]);
    mockedAccounts.mockResolvedValue(accountsResponse);
    mockedList.mockResolvedValue(emptyListResponse);
  });

  it('초기 진입 시 조회기간 기본값이 당월 1일~오늘이고 어떤 조회도 하지 않는다', () => {
    renderPage();
    const inputs = screen
      .getAllByRole('textbox')
      .filter((el) => (el as HTMLInputElement).value.match(/^\d{4}-\d{2}-\d{2}$/));
    expect((inputs[0] as HTMLInputElement).value).toBe(dayjs().startOf('month').format('YYYY-MM-DD'));
    expect((inputs[1] as HTMLInputElement).value).toBe(dayjs().format('YYYY-MM-DD'));
    expect(mockedAccounts).not.toHaveBeenCalled();
    expect(mockedList).not.toHaveBeenCalled();
  });

  it('1단 조회 클릭 시 POS 미접촉으로 거래처 목록만 조회한다', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /search조회$/ }));

    await waitFor(() => {
      expect(screen.getByText('이마트 원주점')).toBeInTheDocument();
      expect(screen.getByText('홈플러스 원주점')).toBeInTheDocument();
    });
    expect(mockedAccounts.mock.calls[0][0]).toMatchObject({ costCenterCodes: ['B001'] });
    // 거래처를 선택하지 않았으므로 POS 조회는 아직 발생하지 않음
    expect(mockedList).not.toHaveBeenCalled();
  });

  it('거래처 선택 후 POS 조회 클릭 시 선택 거래처(accountIds)로 집계하고 합계를 표시한다', async () => {
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
    fireEvent.click(screen.getByRole('button', { name: /search조회$/ }));
    await screen.findByText('이마트 원주점');

    // 첫 거래처(이마트 원주점) 행 클릭으로 선택 (체크박스 조준 불필요)
    fireEvent.click(screen.getByText('이마트 원주점'));

    fireEvent.click(screen.getByRole('button', { name: /선택 거래처 POS 조회/ }));

    await waitFor(() => {
      expect(screen.getByText('POS매출 금액 합계')).toBeInTheDocument();
      expect(screen.getByText('POS매출 수량 합계')).toBeInTheDocument();
    });
    expect(mockedList.mock.calls[0][0]).toMatchObject({
      startDate: dayjs().startOf('month').format('YYYY-MM-DD'),
      endDate: dayjs().format('YYYY-MM-DD'),
      accountIds: [1],
    });
  });

  it('유통형태/거래처유형은 1단, 중·소분류/제품 필터는 1단 조회 후 노출된다', async () => {
    renderPage();
    await waitFor(() => expect(mockedFilterOptions).toHaveBeenCalled());
    // 1단 조건 (항상 노출)
    expect(screen.getByText('유통형태:')).toBeInTheDocument();
    expect(screen.getByText('거래처유형:')).toBeInTheDocument();
    // 2단 필터는 거래처 조회 전에는 미노출
    expect(screen.queryByText('중분류:')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /search조회$/ }));
    await screen.findByText('이마트 원주점');

    expect(screen.getByText('중분류:')).toBeInTheDocument();
    expect(screen.getByText('소분류:')).toBeInTheDocument();
    expect(screen.getByText('제품 (제품명/제품코드/바코드):')).toBeInTheDocument();
  });

  it('조회 기간이 31일을 초과하면 경고를 표시하고 POS 조회 버튼이 비활성화된다', async () => {
    const user = userEvent.setup();
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /search조회$/ }));
    await screen.findByText('이마트 원주점');

    // 거래처 선택 (행 클릭)
    fireEvent.click(screen.getByText('이마트 원주점'));

    const inputs = screen
      .getAllByRole('textbox')
      .filter((el) => (el as HTMLInputElement).value.match(/^\d{4}-\d{2}-\d{2}$/));
    const startInput = inputs[0] as HTMLInputElement;
    const sixtyDaysAgo = dayjs().subtract(60, 'day').format('YYYY-MM-DD');
    await user.click(startInput);
    await user.clear(startInput);
    await user.type(startInput, `${sixtyDaysAgo}{Enter}`);
    await user.click(document.body);

    await waitFor(() => {
      expect(screen.getByText('조회 기간은 최대 31일까지 가능합니다')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /선택 거래처 POS 조회/ })).toBeDisabled();
    expect(mockedList).not.toHaveBeenCalled();
  });
});
