import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CategorySchedulePage from './CategorySchedulePage';
import * as api from '@/api/monthlyIntegration';

vi.mock('@/api/monthlyIntegration', async () => {
  const actual = await vi.importActual<typeof import('@/api/monthlyIntegration')>(
    '@/api/monthlyIntegration',
  );
  return {
    ...actual,
    fetchCategorySchedule: vi.fn(),
    fetchCategoryExport: vi.fn(),
  };
});

// PeriodBranchFilterBar 의 지점 옵션 — 단일지점 사용자로 고정 (본인 지점 자동 선택 → 자동 조회 트리거).
vi.mock('@/hooks/team-schedule/useTeamScheduleBranches', () => ({
  useTeamScheduleBranches: () => ({
    data: [{ branchCode: 'B001', branchName: '강북4지점' }],
  }),
}));

const mockedFetch = vi.mocked(api.fetchCategorySchedule);

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <CategorySchedulePage />
    </QueryClientProvider>,
  );
}

describe('CategorySchedulePage 소수점 표시 (SF CategorySearchByTeamMemberController 정합)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // SF cls:147-149 는 총계 3열에만 `.setScale(1, HALF_UP)` 를 적용하고,
  // 진열/행사 12열은 `getCount` 의 scale 3 누적값(cls:258,261)을 Aura 에서 포맷 없이 그대로 출력한다.
  it('총계 3열은 소수 1자리 고정, 진열/행사 12열은 최대 3자리 + 후행 0 제거로 표시한다', async () => {
    mockedFetch.mockResolvedValue({
      year: 2026,
      month: 6,
      items: [
        {
          branchName: '강북4지점',
          // 총계 3열 — 백엔드가 이미 scale 1 로 내려주는 값
          currentMonthTotal: 42.8,
          previousMonthTotal: 43.9,
          totalChange: -1.1,
          // 진열 6열 — scale 3 원값 (후행 0 은 화면에서 제거되어야 함)
          displayFixed: 26,
          displayAlternate: 2.056,
          displayPatrol: 4.657,
          currentMonthDisplayTotal: 32.713,
          previousMonthDisplayTotal: 33.166,
          displayChange: -0.453,
          // 행사 5열
          eventAmbient: 3.123,
          eventFrozenChilled: 7,
          currentMonthEventTotal: 10.123,
          previousMonthEventTotal: 10.777,
          eventChange: -0.654,
        },
      ],
    });

    renderPage();

    await waitFor(() => expect(mockedFetch).toHaveBeenCalled());

    // 총계 3열 — 소수 1자리 고정
    await screen.findByText('42.8');
    expect(screen.getByText('43.9')).toBeInTheDocument();
    expect(screen.getByText('-1.1')).toBeInTheDocument();

    // 진열/행사 — 3자리 원값 그대로
    expect(screen.getByText('2.056')).toBeInTheDocument();
    expect(screen.getByText('4.657')).toBeInTheDocument();
    expect(screen.getByText('32.713')).toBeInTheDocument();
    expect(screen.getByText('33.166')).toBeInTheDocument();
    expect(screen.getByText('-0.453')).toBeInTheDocument();
    expect(screen.getByText('3.123')).toBeInTheDocument();
    expect(screen.getByText('10.123')).toBeInTheDocument();
    expect(screen.getByText('10.777')).toBeInTheDocument();
    expect(screen.getByText('-0.654')).toBeInTheDocument();

    // 정수값은 후행 0 없이 (SF Aura 의 Decimal → JSON number → 문자열 변환 동등)
    expect(screen.getByText('26')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.queryByText('26.000')).not.toBeInTheDocument();
    expect(screen.queryByText('7.000')).not.toBeInTheDocument();
    // 1자리로 절삭되어 보이던 회귀 형태가 남아있지 않은지 확인
    expect(screen.queryByText('32.7')).not.toBeInTheDocument();
    expect(screen.queryByText('2.1')).not.toBeInTheDocument();
  });

  // SF setNull() (cls:341-363) — 당월/전월 합계 모두 0 인 지점은 행 유지 + 수치 전부 빈 칸
  it('수치가 null 인 지점 행은 지점명만 표시하고 수치는 빈 칸으로 둔다', async () => {
    mockedFetch.mockResolvedValue({
      year: 2026,
      month: 6,
      items: [
        {
          branchName: '강북5지점',
          currentMonthTotal: null,
          previousMonthTotal: null,
          totalChange: null,
          displayFixed: null,
          displayAlternate: null,
          displayPatrol: null,
          currentMonthDisplayTotal: null,
          previousMonthDisplayTotal: null,
          displayChange: null,
          eventAmbient: null,
          eventFrozenChilled: null,
          currentMonthEventTotal: null,
          previousMonthEventTotal: null,
          eventChange: null,
        },
      ],
    });

    renderPage();

    await screen.findByText('강북5지점');
    expect(screen.queryByText('0')).not.toBeInTheDocument();
    expect(screen.queryByText('0.0')).not.toBeInTheDocument();
  });
});
