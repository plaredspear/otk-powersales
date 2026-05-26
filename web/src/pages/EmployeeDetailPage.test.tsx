import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeeDetailPage from './EmployeeDetailPage';
import { useAuthStore } from '@/stores/authStore';
import type { EmployeeDetail } from '@/api/employee';

const sapEmployee: EmployeeDetail = {
  id: 100,
  employeeCode: '100100',
  name: 'SAP사원',
  gender: '여',
  status: '재직',
  birthDate: '1990-01-01',
  startDate: '2020-01-01',
  endDate: null,
  appointmentDate: null,
  origin: 'SAP',
  costCenterCode: 'A001',
  orgName: '영업1팀',
  locationCode: null,
  workArea: null,
  jobCode: '판촉직',
  jikjong: null,
  jikwee: '사원',
  jikchak: '진열',
  jikgub: null,
  workType: null,
  ordDetailNode: null,
  phone: '010-0000-0000',
  homePhone: '010-0000-0000',
  workPhone: null,
  officePhone: null,
  workEmail: 'sap@example.com',
  email: null,
  role: '여사원',
  appLoginActive: true,
  lockingFlag: false,
  professionalPromotionTeam: null,
  agreementFlag: true,
  crmWorkType: null,
  crmWorkStartDate: null,
  totalAnnualLeave: null,
  usedAnnualLeave: null,
};

vi.mock('@/hooks/employee/useEmployeeWorkHistory', () => ({
  useEmployeeWorkHistory: () => ({
    data: { items: [] },
    isLoading: false,
    isError: false,
    error: null,
  }),
}));

vi.mock('@/hooks/employee/useEmployee', () => ({
  useEmployee: () => ({
    data: sapEmployee,
    isLoading: false,
    isError: false,
    error: null,
    refetch: vi.fn(),
  }),
  useUpdateEmployee: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useManualRegisterEmployee: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
}));

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/employee/100']}>
        <Routes>
          <Route path="/employee/:employeeId" element={<EmployeeDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('EmployeeDetailPage', () => {
  beforeEach(() => {
    useAuthStore.setState({
      user: {
        employeeId: 1,
        employeeCode: 'ADMIN-001',
        name: '관리자',
        permissions: ['EMPLOYEE_READ', 'EMPLOYEE_WRITE', 'EMPLOYEE_RESET_CREDENTIALS'],
      } as never,
    });
  });

  it('6개 그룹 카드 + 사원 기본 정보 렌더링', () => {
    renderPage();
    expect(screen.getByText('인사 정보')).toBeInTheDocument();
    expect(screen.getByText('조직 정보')).toBeInTheDocument();
    expect(screen.getByText('직무 정보')).toBeInTheDocument();
    expect(screen.getByText('연락처')).toBeInTheDocument();
    expect(screen.getByText('앱 설정')).toBeInTheDocument();
    expect(screen.getByText('근무 정보')).toBeInTheDocument();
    expect(screen.getByText('100100')).toBeInTheDocument();
    expect(screen.getByText('SAP사원')).toBeInTheDocument();
  });

  it('SAP origin 사원 -> 수정 버튼 비활성화', () => {
    renderPage();
    const editButton = screen.getByRole('button', { name: '수정' });
    expect(editButton).toBeDisabled();
  });

  it('SAP origin 표시 태그', () => {
    renderPage();
    expect(screen.getByText('SAP 인입')).toBeInTheDocument();
  });
});
