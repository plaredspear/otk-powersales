import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PPTHistoryPage from './PPTHistoryPage';
import type { PPTHistory } from '@/api/pptMaster';

const sampleHistory: PPTHistory = {
  id: 1,
  employeeId: 100,
  employeeName: '백은경',
  employeeCode: 'EMP005',
  orgName: '서울지점',
  status: '재직',
  oldValue: '라면세일조',
  newValue: '카레행사조',
  changedAt: '2026-05-18T14:30:00',
};

const deletedEmployeeHistory: PPTHistory = {
  id: 2,
  employeeId: 999,
  employeeName: null,
  employeeCode: null,
  orgName: null,
  status: null,
  oldValue: null,
  newValue: '라면세일조',
  changedAt: '2026-05-17T09:00:00',
};

const mockHook = vi.fn();
vi.mock('@/hooks/promotion/usePPTHistories', () => ({
  usePPTHistories: (params: unknown) => mockHook(params),
}));

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PPTHistoryPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('PPTHistoryPage', () => {
  beforeEach(() => {
    mockHook.mockReset();
    mockHook.mockReturnValue({
      data: {
        content: [sampleHistory, deletedEmployeeHistory],
        totalElements: 2,
        totalPages: 1,
        number: 0,
        size: 20,
      },
      isLoading: false,
    });
  });

  it('초기 렌더 시 이력 행 2건 표시', () => {
    renderPage();
    expect(screen.getByText('백은경')).toBeInTheDocument();
    expect(screen.getByText('EMP005')).toBeInTheDocument();
    expect(screen.getByText('서울지점')).toBeInTheDocument();
    expect(screen.getByText('카레행사조')).toBeInTheDocument();
  });

  it('사원이 deleted 인 row 는 "-" 로 표시', () => {
    renderPage();
    const cells = screen.getAllByText('-');
    expect(cells.length).toBeGreaterThan(0);
  });

  it('[조회] 버튼 클릭 시 hook params 업데이트', () => {
    renderPage();
    const employeeNameInput = screen.getByPlaceholderText('사원명');
    fireEvent.change(employeeNameInput, { target: { value: '홍' } });
    fireEvent.click(screen.getByRole('button', { name: '조회' }));

    const lastCall = mockHook.mock.calls[mockHook.mock.calls.length - 1][0];
    expect(lastCall.employeeName).toBe('홍');
    expect(lastCall.page).toBe(0);
  });

  it('[초기화] 버튼 클릭 시 모든 필터 비워짐', () => {
    renderPage();
    const employeeNameInput = screen.getByPlaceholderText('사원명');
    fireEvent.change(employeeNameInput, { target: { value: '홍' } });
    fireEvent.click(screen.getByRole('button', { name: '초기화' }));
    expect((employeeNameInput as HTMLInputElement).value).toBe('');
  });

  it('빈 결과 시 Table empty 표시', () => {
    mockHook.mockReturnValue({
      data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 },
      isLoading: false,
    });
    renderPage();
    // scroll 테이블은 빈 상태 placeholder 를 본문 + sticky 로 중복 렌더하므로 getAllByText 로 검증
    expect(screen.getAllByText(/No data|데이터 없음/i).length).toBeGreaterThanOrEqual(1);
  });

  it('행 클릭 시 상세 모달 표시', () => {
    renderPage();
    const row = screen.getByText('백은경').closest('tr');
    expect(row).not.toBeNull();
    fireEvent.click(row!);
    expect(screen.getByText('전문행사조 이력 상세')).toBeInTheDocument();
  });
});
