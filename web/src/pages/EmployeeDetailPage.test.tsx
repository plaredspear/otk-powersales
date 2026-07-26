import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeeDetailPage from './EmployeeDetailPage';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey, systemPermissionKey } from '@/hooks/usePermission';
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
  appVersionName: null,
  appVersionCode: null,
  appPlatform: null,
  appVersionSeenAt: null,
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

/**
 * @param isFemale 여사원 현황(`/female-employee/...`) 진입 여부. 화면이 진입 맥락을
 *   pathname 으로 판별하므로 라우트를 함께 바꿔 렌더한다.
 */
function renderPage(isFemale = false) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const base = isFemale ? '/female-employee' : '/employee';
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`${base}/100`]}>
        <Routes>
          <Route path={`${base}/:employeeId`} element={<EmployeeDetailPage />} />
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
        permissions: [
          entityPermissionKey('employee', 'READ'),
          entityPermissionKey('employee', 'EDIT'),
          systemPermissionKey('MANAGE_USERS'),
        ],
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

  it('설정 사원 상세 진입 -> 「권한 변경」 버튼 노출', () => {
    renderPage();
    expect(screen.getByRole('button', { name: '권한 변경' })).toBeInTheDocument();
  });

  it('여사원 현황 진입 -> 「권한 변경」 버튼 미노출 (호출 API 가 employee:EDIT 가드)', () => {
    useAuthStore.setState({
      user: {
        employeeId: 1,
        employeeCode: 'LEADER-001',
        name: '조장',
        // 여사원 권한만 보유 — employee:EDIT 없음
        permissions: [
          entityPermissionKey('female_employee', 'READ'),
          entityPermissionKey('female_employee', 'EDIT'),
        ],
      } as never,
    });

    renderPage(true);

    expect(screen.queryByRole('button', { name: '권한 변경' })).not.toBeInTheDocument();
    // 나머지 액션은 그대로 노출된다 (권한 변경만 제외).
    expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
  });
});
