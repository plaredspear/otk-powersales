import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import PermissionRoute from './PermissionRoute';
import { useAuthStore } from '@/stores/authStore';
import { SALES_SUPPORT_TEAM2_COST_CENTER_CODE } from '@/hooks/usePermission';

function TestProtected() {
  return <div>PROTECTED_CONTENT</div>;
}

function setUser(params: {
  permissions?: string[];
  costCenterCode?: string | null;
  profileName?: string | null;
} | null) {
  if (params === null) {
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
    return;
  }
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName: params.profileName ?? '5.영업사원',
      isSalesSupport: false,
      costCenterCode: params.costCenterCode ?? null,
      permissions: params.permissions ?? [],
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderWithRoute(props: {
  entity?: string;
  operation?: 'READ' | 'CREATE' | 'EDIT' | 'DELETE';
  deniedCostCenterCodes?: string[];
}) {
  return render(
    <MemoryRouter
      initialEntries={['/protected']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route element={<PermissionRoute {...props} />}>
          <Route path="/protected" element={<TestProtected />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('PermissionRoute', () => {
  beforeEach(() => {
    setUser(null);
  });

  it('entity 권한 보유 사용자는 보호된 outlet 을 렌더링한다', () => {
    setUser({ permissions: ['display_work_schedule:R'] });
    renderWithRoute({ entity: 'display_work_schedule', operation: 'READ' });
    expect(screen.getByText('PROTECTED_CONTENT')).toBeInTheDocument();
  });

  it('entity 권한 없는 사용자는 ForbiddenResult 를 표시한다', () => {
    setUser({ permissions: [] });
    renderWithRoute({ entity: 'display_work_schedule', operation: 'READ' });
    expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
    expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
  });

  describe('deniedCostCenterCodes (deny-list)', () => {
    it('권한이 있어도 차단 costCenterCode(영업지원2팀) 이면 ForbiddenResult', () => {
      setUser({
        permissions: ['display_work_schedule:R'],
        costCenterCode: SALES_SUPPORT_TEAM2_COST_CENTER_CODE,
      });
      renderWithRoute({
        entity: 'display_work_schedule',
        operation: 'READ',
        deniedCostCenterCodes: [SALES_SUPPORT_TEAM2_COST_CENTER_CODE],
      });
      expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
      expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
    });

    it('차단 목록에 없는 costCenterCode 는 권한 충족 시 통과', () => {
      setUser({ permissions: ['display_work_schedule:R'], costCenterCode: '5457' });
      renderWithRoute({
        entity: 'display_work_schedule',
        operation: 'READ',
        deniedCostCenterCodes: [SALES_SUPPORT_TEAM2_COST_CENTER_CODE],
      });
      expect(screen.getByText('PROTECTED_CONTENT')).toBeInTheDocument();
    });

    it('시스템 관리자는 entity 권한 없이도 통과하되, 차단 costCenterCode 면 여전히 ForbiddenResult', () => {
      // 시스템 관리자라도 costCenterCode=4889 면 deny-list 가 우선 (조직 기준 차단)
      setUser({
        permissions: [],
        profileName: '시스템 관리자',
        costCenterCode: SALES_SUPPORT_TEAM2_COST_CENTER_CODE,
      });
      renderWithRoute({
        entity: 'display_work_schedule',
        operation: 'READ',
        deniedCostCenterCodes: [SALES_SUPPORT_TEAM2_COST_CENTER_CODE],
      });
      expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
    });
  });
});
