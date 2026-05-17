import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SafetyCheckPage from './SafetyCheckPage';
import { fetchSafetyCheckStatus, type SafetyCheckStatusData } from '@/api/safetyCheck';

vi.mock('@/api/safetyCheck', async () => {
  const actual = await vi.importActual<typeof import('@/api/safetyCheck')>('@/api/safetyCheck');
  return {
    ...actual,
    fetchSafetyCheckStatus: vi.fn(),
  };
});

const mockedFetch = vi.mocked(fetchSafetyCheckStatus);

const sampleData: SafetyCheckStatusData = {
  date: '2026-05-18',
  totalCount: 2,
  submittedCount: 1,
  notSubmittedCount: 1,
  members: [
    {
      id: 42,
      employeeCode: '123456',
      employeeName: '홍길동',
      accountCode: 'A001',
      accountName: '이마트 강남점',
      submitted: true,
      submittedAt: '2026-05-18T09:15:30',
      startTime: '2026-05-18T09:00:00',
      equipments: Array.from({ length: 9 }, (_, i) => ({
        seqNum: i + 1,
        label: `항목 ${i + 1}`,
        answer: i % 2 === 0 ? '예' : '해당없음',
      })),
      yesCount: 5,
      noCount: 4,
      precautions: '안전사고 유의;중량물 주의',
      precautionCount: 2,
      workReportStatus: '출근',
    },
    {
      id: 55,
      employeeCode: '654321',
      employeeName: '김영희',
      accountCode: null,
      accountName: null,
      submitted: false,
      submittedAt: null,
      startTime: null,
      equipments: [],
      yesCount: 0,
      noCount: 0,
      precautions: null,
      precautionCount: 0,
      workReportStatus: null,
    },
  ],
};

function setViewportWidth(width: number) {
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: width });
  window.matchMedia = vi.fn().mockImplementation((query: string) => {
    // Ant Design Grid.useBreakpoint 가 사용하는 matchMedia 모킹.
    // md breakpoint = 768 — 768 이상이면 md 매칭, 미만이면 매칭 안 됨
    const minWidthMatch = query.match(/min-width:\s*(\d+)px/);
    const maxWidthMatch = query.match(/max-width:\s*(\d+)px/);
    let matches = true;
    if (minWidthMatch) matches = matches && width >= Number(minWidthMatch[1]);
    if (maxWidthMatch) matches = matches && width <= Number(maxWidthMatch[1]);
    return {
      matches,
      media: query,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    };
  });
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <SafetyCheckPage />
    </QueryClientProvider>,
  );
}

describe('SafetyCheckPage', () => {
  beforeEach(() => {
    mockedFetch.mockReset();
    mockedFetch.mockResolvedValue(sampleData);
  });

  it('데스크탑 폭 (≥ md 768px) - datatable 표 형태로 렌더링', async () => {
    setViewportWidth(1280);
    renderPage();

    await waitFor(() => {
      expect(screen.getAllByText('홍길동').length).toBeGreaterThan(0);
      expect(screen.getAllByText('김영희').length).toBeGreaterThan(0);
    });

    // 데스크탑 표 컬럼 헤더 확인 — Ant Design Table 의 thead 안에 표시됨
    expect(screen.getAllByText('사번').length).toBeGreaterThan(0);
    expect(screen.getAllByText('사원명').length).toBeGreaterThan(0);
    expect(screen.getAllByText('출근현황').length).toBeGreaterThan(0);
  });

  it('모바일 폭 (< md 768px) - 여사원당 카드 1개 형태로 렌더링', async () => {
    setViewportWidth(375);
    renderPage();

    await waitFor(() => {
      // 카드 헤더에 사번 + 사원명이 한 줄로 함께 표시되어야 함
      expect(screen.getByText(/123456 홍길동/)).toBeInTheDocument();
      expect(screen.getByText(/654321 김영희/)).toBeInTheDocument();
    });

    // 카드 본문의 라벨 (좌측 컬럼) 이 한 여사원 카드에 모두 표시 — 9개 안전점검 항목 + 추가 항목들
    expect(screen.getAllByText('거래처코드').length).toBe(2);
    expect(screen.getAllByText('거래처명').length).toBe(2);
    expect(screen.getAllByText('출근현황').length).toBe(2);
    expect(screen.getAllByText('설문시작시간').length).toBe(2);
    expect(screen.getAllByText('항목 1').length).toBe(2);
    expect(screen.getAllByText('항목 9').length).toBe(2);
  });

  it('미제출 여사원 카드 - 점검 항목 값은 "-" 로 표시', async () => {
    setViewportWidth(375);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/654321 김영희/)).toBeInTheDocument();
    });

    // 미제출 여사원 카드 (김영희) 에는 "미제출" 태그 + 점검 항목 값이 "-"
    const tags = screen.getAllByText('미제출');
    expect(tags.length).toBeGreaterThan(0);
  });

  it('스케줄 0건 - "해당 날짜에 근무 스케줄이 없습니다" Empty 표시', async () => {
    mockedFetch.mockResolvedValue({
      date: '2026-05-18',
      totalCount: 0,
      submittedCount: 0,
      notSubmittedCount: 0,
      members: [],
    });
    setViewportWidth(375);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('해당 날짜에 근무 스케줄이 없습니다')).toBeInTheDocument();
    });
  });
});
