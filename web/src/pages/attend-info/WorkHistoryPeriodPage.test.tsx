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
    fetchWorkHistoryPeriodSummary: vi.fn(),
    fetchWorkHistoryPeriodSummaryExport: vi.fn().mockResolvedValue(undefined),
    fetchWorkHistoryEmployeeAccounts: vi.fn(),
  };
});

const mockedSummary = vi.mocked(api.fetchWorkHistoryPeriodSummary);
const mockedExport = vi.mocked(api.fetchWorkHistoryPeriodSummaryExport);
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
    // 기본값: 지점 없음(빈 배열) — 자동 조회 미트리거. 단일/다중 지점 케이스는 테스트별 재설정.
    mockedBranches.mockResolvedValue([]);
    mockedMembers.mockResolvedValue([]);
    mockedExport.mockResolvedValue(undefined);
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

  it('초기 진입 시 필터 영역과 안내 문구가 렌더된다', () => {
    renderPage();
    expect(screen.getByText('시작 년월:')).toBeInTheDocument();
    expect(screen.getByText('종료 년월:')).toBeInTheDocument();
    expect(screen.getByText('조회 조건을 설정한 후 조회 버튼을 눌러주세요.')).toBeInTheDocument();
    expect(mockedSummary).not.toHaveBeenCalled();
  });

  it('지점이 하나뿐인 사용자는 진입 시 본인 지점으로 자동 조회한다', async () => {
    mockedBranches.mockResolvedValue([{ branchCode: 'B001', branchName: '원주1지점' }]);
    mockedSummary.mockResolvedValue({
      fromYearMonth: '2026-06',
      toYearMonth: '2026-06',
      totalCount: 0,
      items: [],
    });
    renderPage();
    await waitFor(() => {
      expect(mockedSummary).toHaveBeenCalled();
    });
    // 자동 조회 시 단일 지점 코드가 전달된다.
    expect(mockedSummary.mock.calls[0][0]).toMatchObject({ costCenterCodes: ['B001'] });
  });

  it('지점이 여러 개인 사용자는 진입 시 자동 조회하지 않는다', async () => {
    mockedBranches.mockResolvedValue([
      { branchCode: 'B001', branchName: '원주1지점' },
      { branchCode: 'B002', branchName: '원주2지점' },
    ]);
    renderPage();
    // 지점 목록 로드 후에도 자동 조회가 트리거되지 않음을 확인.
    await screen.findByText('조회 조건을 설정한 후 조회 버튼을 눌러주세요.');
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
          monthlyBreakdown: [],
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

  it('월별 분해가 있는 행은 펼치면 각 월 통계가 표시된다', async () => {
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
          monthlyBreakdown: [
            {
              yearMonth: '2026-05',
              totalWorkingDays: 5,
              workingAccountCount: 2,
              displayDays: 3,
              eventDays: 2,
              workDays: 5,
              annualLeaveDays: 0,
              altHolidayDays: 0,
            },
            {
              yearMonth: '2026-06',
              totalWorkingDays: 7,
              workingAccountCount: 3,
              displayDays: 5,
              eventDays: 2,
              workDays: 5,
              annualLeaveDays: 1,
              altHolidayDays: 1,
            },
          ],
        },
      ],
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));
    // 집계 행이 먼저 렌더된 뒤, 행(이름 셀)을 클릭하면 월별 통계가 펼쳐진다.
    const nameCell = await screen.findByText('홍길동');
    fireEvent.click(nameCell);
    await waitFor(() => {
      expect(screen.getByText('2026-05')).toBeInTheDocument();
      expect(screen.getByText('2026-06')).toBeInTheDocument();
    });
    // 년월(직위 컬럼) 셀은 흰색 배경 (회색 미적용).
    const ymCell = screen.getByText('2026-05').closest('td') as HTMLElement;
    expect(ymCell).not.toHaveStyle({ backgroundColor: '#fafafa' });
    // 같은 월별 행의 식별부(소속지점 자리, 행의 첫 셀)는 회색 배경.
    const firstCellOfRow = ymCell.parentElement?.querySelector('td');
    expect(firstCellOfRow).toHaveStyle({ backgroundColor: '#fafafa' });
  });

  it('단일 월 조회(월별 분해 없음) 행은 클릭해도 펼쳐지지 않는다', async () => {
    mockedSummary.mockResolvedValue({
      fromYearMonth: '2026-05',
      toYearMonth: '2026-05',
      totalCount: 1,
      items: [
        {
          orgName: '강남지점',
          employeeCode: '20230016',
          employeeName: '홍길동',
          title: '사원',
          totalWorkingDays: 5,
          workingAccountCount: 2,
          displayDays: 3,
          eventDays: 2,
          workDays: 5,
          annualLeaveDays: 0,
          altHolidayDays: 0,
          monthlyBreakdown: [],
        },
      ],
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));
    const nameCell = await screen.findByText('홍길동');
    fireEvent.click(nameCell);
    // 월별 분해가 없으므로 클릭해도 월별(yyyy-MM) 행이 추가되지 않는다.
    expect(screen.queryByText('2026-05')).not.toBeInTheDocument();
  });

  it('조회 결과가 있으면 엑셀 다운로드 버튼이 활성화되고 클릭 시 export API 를 호출한다', async () => {
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
          monthlyBreakdown: [],
        },
      ],
    });
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /조회/ }));
    const exportBtn = await screen.findByRole('button', { name: /엑셀 다운로드/ });
    await waitFor(() => expect(exportBtn).not.toBeDisabled());
    fireEvent.click(exportBtn);
    await waitFor(() => expect(mockedExport).toHaveBeenCalled());
  });

  it('조회 전에는 엑셀 다운로드 버튼이 비활성화된다', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /엑셀 다운로드/ })).toBeDisabled();
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
      expect(screen.getByText('조회 결과가 없습니다.')).toBeInTheDocument();
    });
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
      mockedSummary.mockResolvedValue({
        fromYearMonth: '2026-06',
        toYearMonth: '2026-06',
        totalCount: 0,
        items: [],
      });
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
            totalWorkingDays: 3,
            displayDays: 2,
            eventDays: 1,
            workDays: 3,
            annualLeaveDays: 0,
            altHolidayDays: 0,
          },
          {
            accountName: null,
            accountExternalKey: null,
            totalWorkingDays: 1,
            displayDays: 0,
            eventDays: 0,
            workDays: 0,
            annualLeaveDays: 1,
            altHolidayDays: 0,
          },
        ],
      });
    });

    it('좌측 패널에 여사원 목록이 렌더된다', async () => {
      renderPage();
      expect(await screen.findByText('홍길동(20230016)')).toBeInTheDocument();
    });

    it('여사원 클릭 시 선택 기간의 거래처별 집계가 표시된다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      await waitFor(() => {
        expect(screen.getByText('이마트 원주점')).toBeInTheDocument();
        expect(screen.getByText('(거래처 미지정)')).toBeInTheDocument();
        expect(screen.getByText('총 2개 거래처')).toBeInTheDocument();
      });
      // 화면의 시작/종료 년월 입력값이 그대로 전달된다 (사번 포함).
      expect(mockedAccounts.mock.calls[0][0]).toMatchObject({ employeeCode: '20230016' });
    });

    it('전체 목록으로 버튼 클릭 시 전체 집계 목록으로 복귀한다', async () => {
      renderPage();
      fireEvent.click(await screen.findByText('홍길동(20230016)'));
      const backBtn = await screen.findByRole('button', { name: /전체 목록으로/ });
      fireEvent.click(backBtn);
      await waitFor(() => {
        expect(screen.queryByText('총 2개 거래처')).not.toBeInTheDocument();
      });
    });

    it('선택된 여사원을 다시 클릭하면 선택이 해제된다', async () => {
      renderPage();
      const memberItem = await screen.findByText('홍길동(20230016)');
      fireEvent.click(memberItem);
      await screen.findByText('총 2개 거래처');
      fireEvent.click(memberItem);
      await waitFor(() => {
        expect(screen.queryByText('총 2개 거래처')).not.toBeInTheDocument();
      });
    });
  });

  it('조회 기간이 6개월을 초과하면 경고를 표시하고 조회 버튼이 비활성화된다', async () => {
    renderPage();
    // 시작/종료 년월 입력은 [시작년, 시작월, 종료년, 종료월] 순의 spinbutton.
    // 시작 년도를 1년 낮춰 12개월 차이를 만들어 6개월 초과 상태로 만든다.
    const spinButtons = screen.getAllByRole('spinbutton');
    const fromYearInput = spinButtons[0];
    const current = Number((fromYearInput as HTMLInputElement).value);
    fireEvent.change(fromYearInput, { target: { value: String(current - 1) } });

    expect(screen.getByText('조회 기간은 최대 6개월까지 가능합니다')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /조회/ })).toBeDisabled();
    expect(mockedSummary).not.toHaveBeenCalled();
  });
});
