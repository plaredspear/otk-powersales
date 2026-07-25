import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeeInputCriteriaMasterListPage from './EmployeeInputCriteriaMasterListPage';
import type { EmployeeInputCriteriaMaster } from '@/api/employeeInputCriteriaMaster';
import { useAuthStore } from '@/stores/authStore';

const sampleItem: EmployeeInputCriteriaMaster = {
  id: 1,
  name: 'EIC-0001',
  categoryId: 10,
  categoryCode: '08',
  categoryName: '홀세일',
  typeOfWork1: '진열',
  startDate: '2023-01-01',
  endDate: null,
  confirmed: false,
  boundary: '30',
  fixed1PersonStandardAmount: '50000000',
  bifurcationHalfPersonStandard: '30000000',
  fixed1PersonMinAmountInRealmRange: null,
  bifurcationHalfPersonMinAmountInRealmRange: null,
  accountCategorizedCode: '08',
  validData: '유효',
};

/** 목록 mock 이 돌려줄 확정 여부 — 케이스별로 renderPage / renderConfirmedPage 가 토글한다. */
let listConfirmed = false;

vi.mock('@/hooks/employee-input-criteria-master/useEmployeeInputCriteriaMasters', () => ({
  useEmployeeInputCriteriaMasters: () => ({
    data: [{ ...sampleItem, confirmed: listConfirmed }],
    isLoading: false,
    refetch: vi.fn(),
    isFetching: false,
  }),
  useCreateEmployeeInputCriteriaMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUpdateEmployeeInputCriteriaMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useConfirmEmployeeInputCriteriaMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useBulkConfirmEmployeeInputCriteriaMasters: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteEmployeeInputCriteriaMaster: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

vi.mock('@/hooks/employee-input-criteria-master/useEmployeeInputCriteriaMasterFormMeta', () => ({
  useEmployeeInputCriteriaMasterFormMeta: () => ({
    data: {
      accountCategories: [{ value: 10, accountCode: '08', name: '홀세일' }],
      typeOfWork1Options: [{ value: '진열', name: '진열' }],
    },
  }),
}));

vi.mock('@/hooks/employee-input-criteria-master/useEmployeeInputCriteriaMasterListMeta', () => ({
  useEmployeeInputCriteriaMasterListMeta: () => ({
    data: {
      filters: [
        {
          key: 'status',
          type: 'SELECT',
          options: [
            { value: 'ALL', label: '전체' },
            { value: 'VALID', label: '유효' },
            { value: 'PLANNED', label: '예정' },
            { value: 'ENDED', label: '종료' },
          ],
        },
      ],
      defaults: { status: 'ALL' },
    },
  }),
}));

function setPermissions(permissions: string[], profileName: string | null = '5.영업사원') {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName,
      isSalesSupport: false,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderWith(confirmed: boolean) {
  listConfirmed = confirmed;
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <EmployeeInputCriteriaMasterListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

/** 미확정 레코드 1건이 목록에 있는 상태로 렌더. */
const renderPage = () => renderWith(false);

/** 확정된 레코드 1건이 목록에 있는 상태로 렌더. */
const renderConfirmedPage = () => renderWith(true);

/**
 * 등록/수정/확정/일괄확정/삭제 5개 쓰기 액션 모두 backend 가 EDIT 단일로 가드하므로
 * (AdminEmployeeInputCriteriaMasterController), UI 게이팅도 EDIT 단일 기준이다.
 * PPT마스터처럼 CREATE/DELETE 를 분리해 게이팅하면 실제 API 가드와 어긋난다.
 */
describe('EmployeeInputCriteriaMasterListPage 권한 게이팅', () => {
  describe('쓰기 권한 미보유 (READ only)', () => {
    beforeEach(() => {
      setPermissions(['employee_input_criteria_master:R']);
    });

    it('신규 등록 버튼은 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /신규 등록/ })).not.toBeInTheDocument();
    });

    it('일괄 확정 버튼은 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /일괄 확정/ })).not.toBeInTheDocument();
    });

    it('행 액션(수정/확정/삭제) 버튼은 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '확정' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    });

    it('관리 컬럼 헤더가 사라진다', () => {
      renderPage();
      expect(screen.queryAllByText('관리')).toHaveLength(0);
    });

    it('일괄 확정 전용 행 선택 체크박스가 사라진다', () => {
      renderPage();
      expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });

    it('목록 조회(READ) 자체는 유지된다', () => {
      renderPage();
      expect(screen.getByText('홀세일')).toBeInTheDocument();
      // 상태 필터(Radio.Group)는 READ 범위 — 게이팅 대상이 아니다.
      expect(screen.getByRole('radio', { name: '전체' })).toBeInTheDocument();
    });
  });

  describe('EDIT 만 보유 — 확정/삭제 권한 없음', () => {
    beforeEach(() => {
      setPermissions(['employee_input_criteria_master:R', 'employee_input_criteria_master:E']);
    });

    it('신규 등록 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /신규 등록/ })).toBeInTheDocument();
    });

    it('수정 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
    });

    it('삭제 버튼은 숨겨진다 (DELETE 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument();
    });

    it('확정 버튼은 숨겨진다 (확정=시스템 관리자 전용)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: '확정' })).not.toBeInTheDocument();
    });

    it('일괄 확정 버튼은 숨겨진다 (확정=시스템 관리자 전용)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /일괄 확정/ })).not.toBeInTheDocument();
    });

    it('일괄 확정 전용 행 선택 체크박스도 숨겨진다', () => {
      renderPage();
      expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });
  });

  describe('DELETE 만 보유 — 수정 권한 없음', () => {
    beforeEach(() => {
      setPermissions(['employee_input_criteria_master:R', 'employee_input_criteria_master:D']);
    });

    it('삭제 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument();
    });

    it('수정 버튼은 숨겨진다 (EDIT 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
    });

    it('신규 등록 버튼은 숨겨진다 (EDIT 미보유)', () => {
      renderPage();
      expect(screen.queryByRole('button', { name: /신규 등록/ })).not.toBeInTheDocument();
    });

    it('관리 컬럼은 유지된다 (삭제가 가능하므로)', () => {
      renderPage();
      expect(screen.getAllByText('관리').length).toBeGreaterThan(0);
    });
  });

  describe('시스템 관리자 — 확정 포함 전체 액션', () => {
    beforeEach(() => {
      // permissions 가 비어 있어도 시스템 관리자는 전체 통과 (usePermission).
      setPermissions([], '시스템 관리자');
    });

    it('등록/수정/삭제가 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /신규 등록/ })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '삭제' })).toBeInTheDocument();
    });

    it('확정 버튼이 노출된다', () => {
      renderPage();
      expect(screen.getByRole('button', { name: '확정' })).toBeInTheDocument();
    });

    it('일괄 확정 버튼이 노출된다 (선택 0건이라 disabled)', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /일괄 확정/ })).toBeDisabled();
    });

    it('행 선택 체크박스가 노출된다', () => {
      renderPage();
      expect(screen.queryAllByRole('checkbox').length).toBeGreaterThan(0);
    });
  });

  describe('확정된 레코드의 편집 제한', () => {
    it('EDIT 보유자에게는 수정 버튼이 「종료일 수정」으로 표시된다', () => {
      setPermissions(['employee_input_criteria_master:R', 'employee_input_criteria_master:E']);
      renderConfirmedPage();
      expect(screen.getByRole('button', { name: '종료일 수정' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: '수정' })).not.toBeInTheDocument();
    });

    it('시스템 관리자에게는 그대로 「수정」으로 표시된다 (제한 예외)', () => {
      setPermissions([], '시스템 관리자');
      renderConfirmedPage();
      expect(screen.getByRole('button', { name: '수정' })).toBeInTheDocument();
    });

    it('확정된 레코드에는 확정 버튼이 다시 노출되지 않는다', () => {
      setPermissions([], '시스템 관리자');
      renderConfirmedPage();
      expect(screen.queryByRole('button', { name: '확정' })).not.toBeInTheDocument();
    });
  });
});
