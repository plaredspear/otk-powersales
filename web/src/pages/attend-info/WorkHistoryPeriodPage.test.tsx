import { describe, it, expect, vi, beforeEach } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import dayjs from 'dayjs';
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

  it('조회 버튼이 없다 (여사원 선택이 곧 조회)', () => {
    renderPage();
    expect(screen.queryByRole('button', { name: /^조회$/ })).not.toBeInTheDocument();
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

    it('여사원을 선택하면 (조회 버튼 없이) 즉시 거래처별 집계가 표시된다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(screen.getByText('이마트 원주점')).toBeInTheDocument();
        expect(screen.getByText('(거래처 미지정)')).toBeInTheDocument();
        expect(screen.getByText('총 2개 거래처')).toBeInTheDocument();
      });
      expect(mockedAccounts.mock.calls[0][0]).toMatchObject({ employeeCode: '20230016' });
    });

    it('선택된 여사원을 다시 클릭하면 선택이 해제되고 미선택 안내로 복귀한다', async () => {
      renderPage();
      const memberItem = await screen.findByText('홍길동(20230016)');
      fireEvent.click(memberItem);
      await screen.findByText('총 2개 거래처');
      fireEvent.click(memberItem);
      await waitFor(() => {
        expect(screen.queryByText('총 2개 거래처')).not.toBeInTheDocument();
        expect(
          screen.getByText('좌측에서 여사원을 선택하면 거래처별 근무내역을 조회합니다.'),
        ).toBeInTheDocument();
      });
    });

    it('기간이 6개월을 초과하면 경고를 표시하고 (여사원 선택 상태여도) 조회하지 않는다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await screen.findByText('총 2개 거래처');

      // 시작 년월(월 캘린더)을 1년 전으로 입력 → 12개월 차이 → 6개월 초과.
      // 초기값이 채워진 월 DatePicker input(현재 YYYY-MM 표시)을 찾아 이전 연도로 교체.
      const currentYm = dayjs().format('YYYY-MM');
      const startYm = dayjs().subtract(1, 'year').format('YYYY-MM');
      // 시작/종료 둘 다 같은 값이므로 첫 번째(시작 년월)를 조작.
      const fromInput = screen.getAllByDisplayValue(currentYm)[0];
      mockedAccounts.mockClear();
      fireEvent.mouseDown(fromInput);
      fireEvent.change(fromInput, { target: { value: startYm } });
      fireEvent.keyDown(fromInput, { key: 'Enter', code: 'Enter' });

      await waitFor(() => {
        expect(screen.getByText('조회 기간은 최대 6개월까지 가능합니다')).toBeInTheDocument();
      });
      // rangeInvalid 이면 거래처별 조회를 억제한다.
      expect(mockedAccounts).not.toHaveBeenCalled();
    });
  });
});
