import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import WorkHistorySection from './WorkHistorySection';
import type { EmployeeWorkHistoryItem } from '@/api/employee';

const items: EmployeeWorkHistoryItem[] = [
  {
    id: 1,
    workingDate: '2026-05-20',
    workingType: '근무',
    workingCategory1: '진열',
    workingCategory3: '고정',
    workingCategory4: null,
    professionalPromotionTeam: null,
    accountName: '거래처A',
    accountExternalKey: 'A001',
    accountType: '체인',
    abcType: 'A급',
    abcTypeCode: '01',
    abcTypeLabel: '01 A급',
    accountStatusCode: '10',
    distributionChannelLabel: '10 체인',
    isClockIn: true,
    refAccountName: null,
    costCenterCode: null,
    secondWorkType: null,
    startTime: null,
    completeTime: null,
  },
  {
    id: 2,
    workingDate: '2026-05-19',
    workingType: '연차',
    workingCategory1: null,
    workingCategory3: null,
    workingCategory4: null,
    professionalPromotionTeam: null,
    accountName: null,
    accountExternalKey: null,
    accountType: null,
    abcType: null,
    abcTypeCode: null,
    abcTypeLabel: null,
    accountStatusCode: null,
    distributionChannelLabel: null,
    isClockIn: false,
    refAccountName: null,
    costCenterCode: null,
    secondWorkType: null,
    startTime: null,
    completeTime: null,
  },
];

vi.mock('@/hooks/employee/useEmployeeWorkHistory', () => ({
  useEmployeeWorkHistory: () => ({
    data: { items },
    isLoading: false,
    isError: false,
    error: null,
  }),
}));

function renderSection() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <WorkHistorySection employeeId={100} />
    </QueryClientProvider>,
  );
}

describe('WorkHistorySection', () => {
  it('근무이력 카드 + 항목 표시', () => {
    renderSection();
    expect(screen.getByText('근무이력 (최근 10건)')).toBeInTheDocument();
    expect(screen.getByText('2026-05-20')).toBeInTheDocument();
    expect(screen.getByText('근무')).toBeInTheDocument();
    expect(screen.getByText('거래처A (A001)')).toBeInTheDocument();
    // '출근' 은 컬럼 헤더 + 출근 Tag 양쪽에 노출되므로 헤더(columnheader) 를 제외한 Tag 셀만 검증
    const clockInTags = screen
      .getAllByText('출근')
      .filter((el) => el.closest('th') === null);
    expect(clockInTags.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('미출근')).toBeInTheDocument();
  });
});
