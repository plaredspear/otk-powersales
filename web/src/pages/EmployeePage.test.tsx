import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeePage from './EmployeePage';
import { useAuthStore } from '@/stores/authStore';
import type { Employee } from '@/api/employee';

// 여사원 목록 hook 은 본 테스트와 무관하므로 고정 데이터를 반환하도록 mock 한다.
vi.mock('@/hooks/employee/useEmployees', () => ({
  useWomanEmployees: () => ({
    data: {
      content: [activeEmployee, inactiveEmployee],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    },
    isLoading: false,
    isError: false,
    refetch: vi.fn(),
  }),
}));

const activeEmployee: Employee = {
  id: 12345,
  employeeCode: '100123',
  name: '홍길동',
  status: '재직',
  gender: '남',
  orgName: '영업1팀',
  costCenterCode: 'A001',
  role: '여사원',
  startDate: null,
  endDate: null,
  appLoginActive: true,
  workPhone: null,
  jikchak: null,
  jikwee: null,
  jikgub: null,
  jobCode: null,
  appointmentDate: null,
  ordDetailNode: null,
};

const inactiveEmployee: Employee = {
  ...activeEmployee,
  id: 12346,
  employeeCode: '100124',
  name: '김철수',
  appLoginActive: false,
};

function setPermissions(permissions: string[]) {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <QueryClientProvider client={client}>
        <EmployeePage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('EmployeePage 계정 관리 컬럼 (Spec #582 P2-W)', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
  });

  it('EMPLOYEE_RESET_CREDENTIALS 권한 미보유 - 계정 관리 컬럼이 렌더링되지 않음', () => {
    setPermissions(['EMPLOYEE_READ']);
    renderPage();
    expect(screen.queryByText('계정 관리')).not.toBeInTheDocument();
    expect(screen.queryAllByRole('button', { name: '단말 초기화' })).toHaveLength(0);
    expect(screen.queryAllByRole('button', { name: '비밀번호 초기화' })).toHaveLength(0);
  });

  it('EMPLOYEE_RESET_CREDENTIALS 권한 보유 - active 사원은 버튼 활성, 비활성 사원은 disabled', () => {
    setPermissions(['EMPLOYEE_READ', 'EMPLOYEE_RESET_CREDENTIALS']);
    renderPage();

    const deviceButtons = screen.getAllByRole('button', { name: '단말 초기화' });
    const passwordButtons = screen.getAllByRole('button', { name: '비밀번호 초기화' });
    expect(deviceButtons).toHaveLength(2);
    expect(passwordButtons).toHaveLength(2);

    // 첫 번째 행(활성 사원) - 활성
    expect(deviceButtons[0]).toBeEnabled();
    expect(passwordButtons[0]).toBeEnabled();

    // 두 번째 행(비활성 사원) - disabled
    expect(deviceButtons[1]).toBeDisabled();
    expect(passwordButtons[1]).toBeDisabled();
  });
});
