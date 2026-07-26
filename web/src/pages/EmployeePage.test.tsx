import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeePage from './EmployeePage';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey, systemPermissionKey } from '@/hooks/usePermission';
import type { Employee } from '@/api/employee';

// 여사원 목록 hook 은 본 테스트와 무관하므로 고정 데이터를 반환하도록 mock 한다.
vi.mock('@/hooks/employee/useEmployees', () => ({
  useFemaleEmployees: () => ({
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

// 조회 조건 로드(`/meta`) hook — 지점 옵션 개수에 따라 Select/Tag 분기를 검증하므로 테스트별로 교체한다.
const listMetaResult = vi.hoisted(() => ({ current: null as unknown }));
vi.mock('@/hooks/employee/useFemaleEmployeeListMeta', () => ({
  useFemaleEmployeeListMeta: () => ({ data: listMetaResult.current }),
}));

/** 서버 `/meta` 응답 형태의 stub. branches 를 넘기면 costCenterCode SELECT 필터로 조립된다. */
function listMeta(branches: { value: string; label: string }[]) {
  return {
    filters: [
      {
        key: 'status',
        type: 'SELECT' as const,
        options: [
          { value: '재직', label: '재직' },
          { value: '휴직', label: '휴직' },
          { value: '퇴직', label: '퇴직' },
        ],
      },
      {
        key: 'workType1',
        type: 'SELECT' as const,
        options: [
          { value: '진열', label: '진열' },
          { value: '행사', label: '행사' },
        ],
      },
      { key: 'workType3', type: 'SELECT' as const, options: [{ value: '고정', label: '고정' }] },
      {
        key: 'professionalPromotionTeam',
        type: 'SELECT' as const,
        options: [
          { value: '행사조 전체', label: '행사조 전체' },
          { value: '일반', label: '일반' },
        ],
      },
      { key: 'keyword', type: 'TEXT' as const, options: null },
      { key: 'costCenterCode', type: 'SELECT' as const, options: branches },
    ],
    defaults: { pageSize: 20, sort: 'name,ASC' },
  };
}

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
  jikjong: 'OSPM',
  workEmail: 'hong@otoki.com',
  phone: '01012345678',
  age: '47살',
  yearsOfService: '6년',
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
      isSalesSupport: false,
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
    listMetaResult.current = listMeta([{ value: 'A001', label: '서울1지점' }]);
  });

  it('MANAGE_USERS 권한 미보유 - 계정 관리 컬럼이 렌더링되지 않음', () => {
    setPermissions([entityPermissionKey('female_employee', 'READ')]);
    renderPage();
    expect(screen.queryByText('계정 관리')).not.toBeInTheDocument();
    expect(screen.queryAllByRole('button', { name: '단말 초기화' })).toHaveLength(0);
    expect(screen.queryAllByRole('button', { name: '비밀번호 초기화' })).toHaveLength(0);
  });

  it('MANAGE_USERS 권한 보유 - active 사원은 버튼 활성, 비활성 사원은 disabled', () => {
    setPermissions([entityPermissionKey('female_employee', 'READ'), systemPermissionKey('MANAGE_USERS')]);
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

// 여사원 현황은 조회 전용 화면 — 사원 등록은 기준정보 > 사원(`/settings/employees`) 에서만 관리한다.
describe('EmployeePage 조회 전용 (사원 등록 경로 없음)', () => {
  const REGISTER_BUTTON = '+ 신규 사원 등록';

  beforeEach(() => {
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
    listMetaResult.current = listMeta([{ value: 'A001', label: '서울1지점' }]);
  });

  it('female_employee READ 만 보유 - 등록 버튼 미노출', () => {
    setPermissions([entityPermissionKey('female_employee', 'READ')]);
    renderPage();
    expect(screen.queryByRole('button', { name: REGISTER_BUTTON })).not.toBeInTheDocument();
  });

  it('female_employee CREATE/EDIT 를 모두 보유해도 등록 버튼 미노출 (화면 자체가 조회 전용)', () => {
    setPermissions([
      entityPermissionKey('female_employee', 'READ'),
      entityPermissionKey('female_employee', 'CREATE'),
      entityPermissionKey('female_employee', 'EDIT'),
    ]);
    renderPage();
    expect(screen.queryByRole('button', { name: REGISTER_BUTTON })).not.toBeInTheDocument();
  });
});

describe('EmployeePage 조회 조건 로드 (/meta 단일 응답)', () => {
  beforeEach(() => {
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
    setPermissions([entityPermissionKey('female_employee', 'READ')]);
    // 테스트 간 mock 누수 방지 — 각 테스트가 필요 시 덮어쓴다.
    listMetaResult.current = listMeta([{ value: 'A001', label: '서울1지점' }]);
  });

  it('단일 지점(조장 등) - 지점 Select 대신 고정 Tag 로 지점명 표시', () => {
    listMetaResult.current = listMeta([{ value: 'A001', label: '서울1지점' }]);
    renderPage();

    expect(screen.getByText('지점: 서울1지점')).toBeInTheDocument();
    expect(screen.queryByText('지점 (전체)')).not.toBeInTheDocument();
  });

  it('다중 지점(전사 권한자) - 지점 Select 노출, 고정 Tag 미표시', () => {
    listMetaResult.current = listMeta([
      { value: 'A001', label: '서울1지점' },
      { value: 'A002', label: '서울2지점' },
    ]);
    renderPage();

    expect(screen.getByText('지점 (전체)')).toBeInTheDocument();
    expect(screen.queryByText('지점: 서울1지점')).not.toBeInTheDocument();
  });

  it("meta 옵션 앞에 '전체' 선택지가 붙어 기본 선택으로 표시됨", () => {
    listMetaResult.current = listMeta([{ value: 'A001', label: '서울1지점' }]);
    renderPage();

    // 각 셀렉터의 기본값('' = 전체) 라벨이 표시된다 — 옵션 본문은 meta 응답이 단일 출처.
    expect(screen.getByText('상태 전체')).toBeInTheDocument();
    expect(screen.getByText('근무형태 전체')).toBeInTheDocument();
    expect(screen.getByText('세부 전체')).toBeInTheDocument();
    // 전문행사조 셀렉터의 빈 값('') 기본 라벨은 '전체'(일반 포함 완전 전체) — '일반 제외'는 meta 의 '행사조 전체' 옵션.
    expect(screen.getByText('전체')).toBeInTheDocument();
  });

  it('meta 미수신(초기 렌더) - 지점 셀렉터/Tag 없이도 렌더 실패하지 않음', () => {
    listMetaResult.current = undefined;
    renderPage();

    expect(screen.queryByText('지점 (전체)')).not.toBeInTheDocument();
    // '전체' 선택지는 화면이 붙이므로 meta 부재에도 표시된다.
    expect(screen.getByText('상태 전체')).toBeInTheDocument();
  });
});
