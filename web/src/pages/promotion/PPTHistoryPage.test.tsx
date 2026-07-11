import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import PPTHistoryPage from './PPTHistoryPage';
import type { PPTHistory } from '@/api/pptMaster';

const sampleHistory: PPTHistory = {
  id: 1,
  name: 'PH0014972',
  employeeId: 100,
  employeeName: '백은경',
  employeeCode: 'EMP005',
  orgName: '서울지점',
  oldValue: '라면세일조',
  newValue: '카레세일조',
  changedAt: '2026-05-18T14:30:00',
  accountId: 55,
  accountCode: 'SAP001',
  accountName: '이마트 강남점',
};

const deletedEmployeeHistory: PPTHistory = {
  id: 2,
  name: 'PH0010771',
  employeeId: 999,
  employeeName: null,
  employeeCode: null,
  orgName: null,
  oldValue: null,
  newValue: '라면세일조',
  changedAt: '2026-05-17T09:00:00',
  // masterId 없는 이력(알라딘 마이그레이션분) — 거래처 3필드 모두 null.
  accountId: null,
  accountCode: null,
  accountName: null,
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
    expect(screen.getByText('PH0014972')).toBeInTheDocument();
    expect(screen.getByText('백은경')).toBeInTheDocument();
    expect(screen.getByText('EMP005')).toBeInTheDocument();
    expect(screen.getByText('서울지점')).toBeInTheDocument();
    expect(screen.getByText('카레세일조')).toBeInTheDocument();
    // 원인 마스터(masterId) 거래처가 행에 표시된다.
    expect(screen.getByText('이마트 강남점')).toBeInTheDocument();
    expect(screen.getByText('SAP001')).toBeInTheDocument();
  });

  it('사원이 deleted 인 row 는 "-" 로 표시', () => {
    renderPage();
    const cells = screen.getAllByText('-');
    expect(cells.length).toBeGreaterThan(0);
  });

  it('사원명은 사원 상세 링크, 거래처명은 거래처 상세 링크로 렌더', () => {
    renderPage();
    // 사원명 링크 — female_employee 권한 없으면 /employee prefix (테스트 미인증 기본).
    const employeeLink = screen.getByRole('link', { name: '백은경' });
    expect(employeeLink).toHaveAttribute('href', '/employee/100');
    // 거래처명 링크 — accountId 기반.
    const accountLink = screen.getByRole('link', { name: '이마트 강남점' });
    expect(accountLink).toHaveAttribute('href', '/account/55');
  });

  it('거래처가 없는 이력(알라딘 마이그레이션분)은 "알라딘" 으로 표기', () => {
    renderPage();
    // accountId/accountName 이 모두 null 인 두 번째 행은 '-' 가 아니라 '알라딘' 으로 출처를 표기.
    expect(screen.getByText('알라딘')).toBeInTheDocument();
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
    expect(screen.getAllByText('조회 결과가 없습니다.').length).toBeGreaterThanOrEqual(1);
  });

  it('행 클릭 시 상세 모달 표시', () => {
    renderPage();
    const row = screen.getByText('백은경').closest('tr');
    expect(row).not.toBeNull();
    fireEvent.click(row!);
    expect(screen.getByText('전문행사조 이력 상세')).toBeInTheDocument();
  });
});
