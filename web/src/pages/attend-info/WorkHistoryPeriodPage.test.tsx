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
    fetchWorkHistoryPeriodSummary: vi.fn(),
  };
});

const mockedSummary = vi.mocked(api.fetchWorkHistoryPeriodSummary);

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
    useAuthStore.setState({
      user: {
        id: 1,
        employeeCode: 'admin',
        username: 'admin',
        name: 'admin',
        orgName: null,
        role: null,
        costCenterCode: null,
        permissions: [entityPermissionKey('attend_info', 'READ')],
      },
      accessToken: 'test-token',
      isAuthenticated: true,
    });
  });

  it('초기 진입 시 필터 영역과 안내 문구가 렌더된다', () => {
    renderPage();
    expect(screen.getByText('시작 년월:')).toBeInTheDocument();
    expect(screen.getByText('종료 년월:')).toBeInTheDocument();
    expect(screen.getByText('조회 조건을 설정하고 조회 버튼을 눌러주세요')).toBeInTheDocument();
    expect(mockedSummary).not.toHaveBeenCalled();
  });

  it('조회 버튼 클릭 시 집계 행이 표시된다', async () => {
    mockedSummary.mockResolvedValue({
      fromYearMonth: '2026-05',
      toYearMonth: '2026-06',
      totalCount: 1,
      items: [
        {
          orgName: '강남지점',
          employeeCode: '20230016',
          employeeName: '홍길동',
          title: '사원',
          totalWorkingDays: 12,
          workingAccountCount: 3,
          displayDays: 8,
          eventDays: 4,
          workDays: 10,
          annualLeaveDays: 1,
          altHolidayDays: 1,
        },
      ],
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));
    await waitFor(() => {
      expect(screen.getByText('강남지점')).toBeInTheDocument();
      expect(screen.getByText('20230016')).toBeInTheDocument();
      expect(screen.getByText('홍길동')).toBeInTheDocument();
      expect(screen.getByText('총 1명')).toBeInTheDocument();
    });
    expect(mockedSummary).toHaveBeenCalled();
  });

  it('조회 결과가 없으면 빈 메시지를 표시한다', async () => {
    mockedSummary.mockResolvedValue({
      fromYearMonth: '2026-05',
      toYearMonth: '2026-05',
      totalCount: 0,
      items: [],
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));
    await waitFor(() => {
      expect(screen.getByText('조회 결과가 없습니다')).toBeInTheDocument();
    });
  });
});
