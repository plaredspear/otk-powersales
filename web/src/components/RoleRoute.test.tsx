import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RoleRoute from './RoleRoute';
import { useAuthStore } from '@/stores/authStore';
import type { UserRole } from '@/constants/userRole';

function TestProtected() {
  return <div>PROTECTED_CONTENT</div>;
}

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

function renderWithRoute() {
  return render(
    <MemoryRouter
      initialEntries={['/protected']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route element={<RoleRoute allowedRoles={['SYSTEM_ADMIN']} />}>
          <Route path="/protected" element={<TestProtected />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RoleRoute', () => {
  beforeEach(() => {
    setRole(null);
  });

  it('SYSTEM_ADMIN 호출자는 보호된 outlet 을 렌더링한다', () => {
    setRole('SYSTEM_ADMIN');
    renderWithRoute();
    expect(screen.getByText('PROTECTED_CONTENT')).toBeInTheDocument();
  });

  it('허용되지 않은 role 은 ForbiddenResult 를 표시한다', () => {
    setRole('WOMAN');
    renderWithRoute();
    expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
    expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
  });

  it('미인증(role=null) 은 ForbiddenResult 를 표시한다', () => {
    setRole(null);
    renderWithRoute();
    expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
  });
});
