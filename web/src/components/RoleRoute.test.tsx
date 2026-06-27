import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import RoleRoute from './RoleRoute';
import { useAuthStore } from '@/stores/authStore';

function TestProtected() {
  return <div>PROTECTED_CONTENT</div>;
}

function setProfileName(profileName: string | null) {
  const initial = useAuthStore.getState();
  if (profileName === null) {
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
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      profileName,
      isSalesSupport: false,
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
        <Route element={<RoleRoute allowedProfileNames={['시스템 관리자']} />}>
          <Route path="/protected" element={<TestProtected />} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('RoleRoute', () => {
  beforeEach(() => {
    setProfileName(null);
  });

  it('시스템 관리자 호출자는 보호된 outlet 을 렌더링한다', () => {
    setProfileName('시스템 관리자');
    renderWithRoute();
    expect(screen.getByText('PROTECTED_CONTENT')).toBeInTheDocument();
  });

  it('허용되지 않은 profileName 은 ForbiddenResult 를 표시한다', () => {
    setProfileName('5.영업사원');
    renderWithRoute();
    expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
    expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
  });

  it('미인증(profileName=null) 은 ForbiddenResult 를 표시한다', () => {
    setProfileName(null);
    renderWithRoute();
    expect(screen.getByText('접근 권한 없음')).toBeInTheDocument();
  });
});
