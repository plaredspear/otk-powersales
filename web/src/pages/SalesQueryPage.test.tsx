import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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
    { accountId: 1, accountName: '이마트 원주점', sapAccountCode: 'S001', distributionChannel: '01 대형마트(3대)', accountType: '6111 이마트', branchCode: 'B001', branchName: '원주1지점' },
    { accountId: 2, accountName: '홈플러스 원주점', sapAccountCode: 'S002', distributionChannel: '01 대형마트(3대)', accountType: '6112 홈플러스', branchCode: 'B001', branchName: '원주1지점' },
  ],
};

/**
 * 거래처 선택 모달 열기 — 거래처 검색 조건(지점/유통형태/거래처유형/거래처명)은 모두 이 모달 안에 있다.
 * 메인 화면에는 POS 조회 조건(기간/제품/분류)과 선택 거래처 칩만 남아 있다.
 */
async function openAccountModal(): Promise<HTMLElement> {
  fireEvent.click(screen.getByRole('button', { name: /거래처 선택/ }));
  return await screen.findByRole('dialog');
}

/** 모달에서 거래처 목록을 조회한다 (외부 POS DB 미접촉). */
async function searchAccounts(dialog: HTMLElement): Promise<void> {
  fireEvent.click(within(dialog).getByRole('button', { name: /거래처 검색/ }));
  await waitFor(() => expect(mockedAccounts).toHaveBeenCalled());
}

/** antd Select 열기 — userEvent.click 보다 훨씬 빠르다 (mousedown 에 반응). */
function openSelect(combobox: HTMLElement): void {
  fireEvent.mouseDown(combobox);
}

/** 열린 드롭다운에서 옵션 선택 (옵션은 body 포털에 렌더된다). */
async function pickOption(label: string): Promise<void> {
  fireEvent.click(await screen.findByTitle(label));
}

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
      // 유통형태 옵션 = 거래처유형마스터 {코드, "{코드} {이름}"} — 종속 매핑 key 도 코드.
      distributionChannels: [
        { code: '01', label: '01 대형마트(3대)' },
        { code: '06', label: '06 슈퍼' },
      ],
      accountTypes: ['6111 이마트', '6112 홈플러스', '6200 일반슈퍼'],
      categories: [{ category2: '면류', category3s: ['봉지면', '용기면'] }],
      dependentAccountTypes: {
        '01': ['6111 이마트', '6112 홈플러스'],
        '06': ['6200 일반슈퍼'],
      },
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

  it('모달에서 거래처 검색 시 POS 미접촉으로 거래처 목록만 조회한다', async () => {
    renderPage();
    const dialog = await openAccountModal();
    await searchAccounts(dialog);

    await waitFor(() => {
      expect(within(dialog).getByText('이마트 원주점')).toBeInTheDocument();
      expect(within(dialog).getByText('홈플러스 원주점')).toBeInTheDocument();
    });
    // 목록에 유통형태·거래처유형 컬럼 값이 표시된다 (유통형태는 거래처유형마스터 "{코드} {이름}").
    expect(within(dialog).getByText('6111 이마트')).toBeInTheDocument();
    expect(within(dialog).getByText('6112 홈플러스')).toBeInTheDocument();
    expect(within(dialog).getAllByText('01 대형마트(3대)').length).toBeGreaterThan(0);
    expect(mockedAccounts.mock.calls[0][0]).toMatchObject({ costCenterCodes: ['B001'] });
    // 거래처를 확정하지 않았으므로 POS 조회는 아직 발생하지 않음
    expect(mockedList).not.toHaveBeenCalled();
  });

  it('모달에서 거래처 선택 완료 후 POS 매출 조회 시 선택 거래처(accountIds)로 집계하고 합계를 표시한다', async () => {
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
    const dialog = await openAccountModal();
    await searchAccounts(dialog);
    await within(dialog).findByText('이마트 원주점');

    // 행 아무 곳이나 클릭하면 선택/해제 (체크박스 조준 불필요) → [선택 완료] 로 확정.
    fireEvent.click(within(dialog).getByText('이마트 원주점'));
    fireEvent.click(within(dialog).getByRole('button', { name: /선택 완료/ }));

    // 확정된 거래처는 메인에 칩으로 남고 [POS 매출 조회] 가 활성화된다.
    // (모달은 닫힘 애니메이션 동안 DOM 에 남아 있어 제거 대신 버튼 활성화를 기다린다.)
    await waitFor(() => expect(screen.getByRole('button', { name: /POS 매출 조회/ })).toBeEnabled());
    expect(screen.getAllByText('이마트 원주점').length).toBeGreaterThan(0);

    fireEvent.click(screen.getByRole('button', { name: /POS 매출 조회/ }));

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

  it('유통형태/거래처유형은 거래처 선택 모달, 기간/중·소분류/제품 필터는 메인 화면에 있다', async () => {
    renderPage();
    await waitFor(() => expect(mockedFilterOptions).toHaveBeenCalled());

    // POS 조회 조건은 메인에 상시 노출.
    expect(screen.getByText('조회기간:')).toBeInTheDocument();
    expect(screen.getByText('중분류:')).toBeInTheDocument();
    expect(screen.getByText('소분류:')).toBeInTheDocument();
    expect(screen.getByText('제품 (제품명/제품코드/바코드):')).toBeInTheDocument();
    // 거래처 검색 조건은 모달을 열기 전에는 미노출.
    expect(screen.queryByText('유통형태:')).not.toBeInTheDocument();
    expect(screen.queryByText('거래처유형:')).not.toBeInTheDocument();

    const dialog = await openAccountModal();
    expect(within(dialog).getByText('유통형태:')).toBeInTheDocument();
    expect(within(dialog).getByText('거래처유형:')).toBeInTheDocument();
  });

  it('유통형태 선택 시 거래처유형 옵션이 해당 유통형태의 종속 목록으로 좁혀진다', async () => {
    renderPage();
    const dialog = await openAccountModal();
    await waitFor(() => expect(mockedFilterOptions).toHaveBeenCalled());

    // 단일지점 사용자라 지점은 Tag 로 렌더 → 모달의 combobox 는 [유통형태, 거래처유형] 순.
    const comboboxes = within(dialog).getAllByRole('combobox');
    openSelect(comboboxes[0]);
    await pickOption('01 대형마트(3대)');

    // 거래처유형 콤보박스를 열면 대형마트 종속 목록만(이마트/홈플러스) 노출되고 일반슈퍼는 빠진다.
    openSelect(comboboxes[1]);
    await waitFor(() => {
      expect(screen.getByTitle('6111 이마트')).toBeInTheDocument();
      expect(screen.getByTitle('6112 홈플러스')).toBeInTheDocument();
      expect(screen.queryByTitle('6200 일반슈퍼')).not.toBeInTheDocument();
    });
  });

  it('유통형태 변경으로 종속 목록에서 사라진 거래처유형 선택값은 자동 정리된다', async () => {
    renderPage();
    const dialog = await openAccountModal();
    await waitFor(() => expect(mockedFilterOptions).toHaveBeenCalled());

    const comboboxes = within(dialog).getAllByRole('combobox');
    // 유통형태 '06 슈퍼' 선택 → 거래처유형 '6200 일반슈퍼' 선택 (종속 허용).
    openSelect(comboboxes[0]);
    await pickOption('06 슈퍼');
    openSelect(comboboxes[1]);
    await pickOption('6200 일반슈퍼');

    // 유통형태를 '01 대형마트(3대)' 로 교체 (06 슈퍼 해제 후 대형마트 선택).
    openSelect(comboboxes[0]);
    await pickOption('06 슈퍼'); // 토글 해제
    await pickOption('01 대형마트(3대)');

    // 거래처유형 옵션이 대형마트 종속(이마트/홈플러스)으로 좁혀지고, 선택했던 '6200 일반슈퍼' 는
    // 종속 목록에 없어 선택값이 정리된다 → 거래처 검색 조건에 일반슈퍼가 반영되지 않음.
    await searchAccounts(dialog);
    const lastAccountsCall = mockedAccounts.mock.calls[mockedAccounts.mock.calls.length - 1];
    expect(lastAccountsCall[0]).toMatchObject({
      // 전송값은 라벨이 아니라 거래처유형마스터 코드.
      distributionChannels: ['01'],
      accountTypes: [],
    });
    // 다중선택 Select 를 5회 조작하는 테스트라 기본 10s 로는 병렬 실행에서 flaky 하다.
  }, 30_000);

  it('조회 기간이 31일을 초과하면 경고를 표시하고 POS 매출 조회 버튼이 비활성화된다', async () => {
    // delay: null — 기본 delay 는 모달+다중선택 조작이 많은 본 파일에서 전체 스위트 병렬 실행 시
    // 테스트 타임아웃을 넘긴다 (동작 검증에 delay 는 불필요).
    const user = userEvent.setup({ delay: null });
    renderPage();
    const dialog = await openAccountModal();
    await searchAccounts(dialog);
    await within(dialog).findByText('이마트 원주점');

    // 거래처 확정 — 기간 위반이 아니면 [POS 매출 조회] 가 활성화되는 상태를 만든다.
    fireEvent.click(within(dialog).getByText('이마트 원주점'));
    fireEvent.click(within(dialog).getByRole('button', { name: /선택 완료/ }));
    await waitFor(() => expect(screen.getByRole('button', { name: /POS 매출 조회/ })).toBeEnabled());

    const inputs = screen
      .getAllByRole('textbox')
      .filter((el) => (el as HTMLInputElement).value.match(/^\d{4}-\d{2}-\d{2}$/));
    const startInput = inputs[0] as HTMLInputElement;
    const sixtyDaysAgo = dayjs().subtract(60, 'day').format('YYYY-MM-DD');
    // DatePicker 는 controlled input 이라 fireEvent.change 로는 값이 반영되지 않는다 — 실제 타이핑 필요.
    await user.click(startInput);
    await user.clear(startInput);
    await user.type(startInput, `${sixtyDaysAgo}{Enter}`);
    await user.click(document.body);

    await waitFor(() => {
      expect(screen.getByText('조회 기간은 최대 31일까지 가능합니다')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /POS 매출 조회/ })).toBeDisabled();
    expect(mockedList).not.toHaveBeenCalled();
    // 날짜 타이핑 + 모달 조작이 겹쳐 기본 10s 안에 못 끝나는 경우가 있어 여유를 준다 (병렬 실행 flaky 방지).
  }, 30_000);
});
