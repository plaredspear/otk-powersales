import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeePermissionPage from './EmployeePermissionPage';
import { useAuthStore } from '@/stores/authStore';
import type { UserRole } from '@/constants/userRole';

// Spec #581 P2-W §4: 관리자 등록 진입점 버튼 가시성 검증.
//
// `useEmployees` 훅은 본 검증과 무관하므로 빈 페이지로 mock 한다.
vi.mock('@/hooks/employee/useEmployees', () => ({
  useEmployees: () => ({
    data: {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
    },
    isLoading: false,
  }),
}));

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

function setRole(role: UserRole | null) {
  const initial = useAuthStore.getState();
  if (role === null) {
    useAuthStore.setState({
      user: null,
      accessToken: initial.accessToken,
      isAuthenticated: false,
    });
    return;
  }
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      name: '테스트',
      orgName: null,
      role,
      roleLabel: null,
      costCenterCode: null,
      permissions: [],
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
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <EmployeePermissionPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('EmployeePermissionPage 관리자 등록 버튼 가시성 (Spec #581 P2-W §4)', () => {
  beforeEach(() => {
    setRole(null);
    navigateMock.mockReset();
  });

  it('SYSTEM_ADMIN 호출자에게는 "관리자 등록" 버튼이 표시된다', () => {
    setRole('SYSTEM_ADMIN');
    renderPage();
    expect(
      screen.getByRole('button', { name: /관리자 등록/ }),
    ).toBeInTheDocument();
  });

  it('SYSTEM_ADMIN 이 아닌 호출자에게는 "관리자 등록" 버튼이 표시되지 않는다', () => {
    setRole('WOMAN');
    renderPage();
    expect(
      screen.queryByRole('button', { name: /관리자 등록/ }),
    ).not.toBeInTheDocument();
  });
});
