import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import WorkHistoryPeriodPage from './WorkHistoryPeriodPage';
import * as api from '@/api/attendInfo';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey } from '@/hooks/usePermission';

vi.mock('@/api/attendInfo', async () => {
  const actual = await vi.importActual<typeof import('@/api/attendInfo')>('@/api/attendInfo');
  return {
    ...actual,
    fetchAttendInfoBranches: vi.fn().mockResolvedValue([]),
    fetchAttendInfoMembers: vi.fn().mockResolvedValue([]),
    fetchWorkHistoryEmployeeAccounts: vi.fn(),
  };
});

const mockedBranches = vi.mocked(api.fetchAttendInfoBranches);
const mockedMembers = vi.mocked(api.fetchAttendInfoMembers);
const mockedAccounts = vi.mocked(api.fetchWorkHistoryEmployeeAccounts);

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <WorkHistoryPeriodPage />
    </QueryClientProvider>,
  );
}

describe('WorkHistoryPeriodPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedBranches.mockResolvedValue([]);
    mockedMembers.mockResolvedValue([]);
    useAuthStore.setState({
      user: {
        id: 1,
        employeeCode: 'admin',
        username: 'admin',
        name: 'admin',
        orgName: null,
        role: null,
        isSalesSupport: false,
        costCenterCode: null,
        permissions: [entityPermissionKey('attend_info', 'READ')],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
  });

  it('초기 진입 시 필터 영역과 미선택 안내 문구가 렌더된다', () => {
    renderPage();
    expect(screen.getByText('시작 년월:')).toBeInTheDocument();
    expect(screen.getByText('종료 년월:')).toBeInTheDocument();
    expect(
      screen.getByText('좌측에서 여사원을 선택하면 거래처별 근무내역을 조회합니다.'),
    ).toBeInTheDocument();
  });

  it('여사원 미선택 상태에서는 조회 버튼이 비활성화된다', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /조회/ })).toBeDisabled();
  });

  describe('여사원 선택 → 거래처별 집계', () => {
    const member = {
      employeeId: 10,
      employeeCode: '20230016',
      name: '홍길동',
      orgName: '원주1지점',
      jikwee: 'OSPM',
      status: '재직',
    };

    beforeEach(() => {
      mockedBranches.mockResolvedValue([{ branchCode: 'B001', branchName: '원주1지점' }]);
      mockedMembers.mockResolvedValue([member]);
      mockedAccounts.mockResolvedValue({
        fromYearMonth: '2026-06',
        toYearMonth: '2026-06',
        employeeCode: '20230016',
        employeeName: '홍길동',
        totalCount: 2,
        items: [
          {
            accountName: '이마트 원주점',
            accountExternalKey: 'A0001',
            accountBranchName: '원주1지점',
            distributionChannelLabel: '02 대형마트(3대)',
            abcTypeLabel: '6111 이마트',
            totalWorkingDays: 3,
            displayDays: 2,
            eventDays: 1,
            workDays: 3,
            annualLeaveDays: 0,
            altHolidayDays: 0,
            totalInputCount: 2,
            equivalentWorkingDays: '2.5000',
            monthlyStats: [],
          },
          {
            accountName: null,
            accountExternalKey: null,
            accountBranchName: null,
            distributionChannelLabel: null,
            abcTypeLabel: null,
            totalWorkingDays: 1,
            displayDays: 0,
            eventDays: 0,
            workDays: 0,
            annualLeaveDays: 1,
            altHolidayDays: 0,
            totalInputCount: 0,
            equivalentWorkingDays: '0.0000',
            monthlyStats: [],
          },
        ],
      });
    });

    it('좌측 패널에 여사원 목록이 렌더된다', async () => {
      renderPage();
      expect(await screen.findByText('홍길동(20230016)')).toBeInTheDocument();
    });

    it('여사원을 선택하면 조회 버튼이 활성화된다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /조회/ })).not.toBeDisabled();
      });
    });

    it('여사원 선택 후에도 조회 버튼을 누르기 전에는 조회하지 않고 안내를 표시한다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(
          screen.getByText('조회 버튼을 눌러 선택한 여사원의 거래처별 근무내역을 조회하세요.'),
        ).toBeInTheDocument();
      });
      expect(mockedAccounts).not.toHaveBeenCalled();
    });

    it('여사원 선택 후 조회 버튼을 누르면 거래처별 집계가 표시된다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /조회/ })).not.toBeDisabled();
      });
      fireEvent.click(screen.getByRole('button', { name: /조회/ }));
      await waitFor(() => {
        expect(screen.getByText('이마트 원주점')).toBeInTheDocument();
        expect(screen.getByText('(거래처 미지정)')).toBeInTheDocument();
        expect(screen.getByText('총 2개 거래처')).toBeInTheDocument();
      });
      expect(mockedAccounts.mock.calls[0][0]).toMatchObject({ employeeCode: '20230016' });
    });

    it('전체 목록으로 버튼 클릭 시 선택이 해제되고 조회 결과가 사라진다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /조회/ })).not.toBeDisabled();
      });
      fireEvent.click(screen.getByRole('button', { name: /조회/ }));
      const backBtn = await screen.findByRole('button', { name: /전체 목록으로/ });
      fireEvent.click(backBtn);
      await waitFor(() => {
        expect(screen.queryByText('총 2개 거래처')).not.toBeInTheDocument();
        expect(
          screen.getByText('좌측에서 여사원을 선택하면 거래처별 근무내역을 조회합니다.'),
        ).toBeInTheDocument();
      });
    });

    it('선택된 여사원을 다시 클릭하면 선택이 해제된다', async () => {
      renderPage();
      const memberItem = await screen.findByText('홍길동(20230016)');
      fireEvent.click(memberItem);
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /조회/ })).not.toBeDisabled();
      });
      fireEvent.click(memberItem);
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /조회/ })).toBeDisabled();
      });
    });
  });

  it('조회 기간이 6개월을 초과하면 경고를 표시하고 조회 버튼이 비활성화된다', async () => {
    renderPage();
    // 시작/종료 년월 입력은 [시작년, 시작월, 종료년, 종료월] 순의 spinbutton.
    const spinButtons = screen.getAllByRole('spinbutton');
    const fromYearInput = spinButtons[0];
    const current = Number((fromYearInput as HTMLInputElement).value);
    fireEvent.change(fromYearInput, { target: { value: String(current - 1) } });

    expect(screen.getByText('조회 기간은 최대 6개월까지 가능합니다')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /조회/ })).toBeDisabled();
  });
});
