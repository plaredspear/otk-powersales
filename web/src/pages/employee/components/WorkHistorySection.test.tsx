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
    accountName: '거래처A',
    accountExternalKey: 'A001',
    isClockIn: true,
  },
  {
    id: 2,
    workingDate: '2026-05-19',
    workingType: '연차',
    workingCategory1: null,
    workingCategory3: null,
    workingCategory4: null,
    accountName: null,
    accountExternalKey: null,
    isClockIn: false,
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
    expect(screen.getByText('출근')).toBeInTheDocument();
    expect(screen.getByText('미출근')).toBeInTheDocument();
  });
});
