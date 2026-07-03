import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { useAuthStore } from '@/stores/authStore';

function setAuth(opts: { isAuthenticated: boolean; passwordChangeRequired?: boolean }) {
  useAuthStore.setState({
    user: opts.isAuthenticated
      ? {
          id: 1,
          employeeCode: 'TEST-001',
          username: 'test@otoki.local',
          name: '테스트',
          orgName: null,
          role: null,
          profileName: null,
          isSalesSupport: false,
          costCenterCode: null,
          permissions: [],
        }
      : null,
    accessToken: opts.isAuthenticated ? 'token' : null,
    isAuthenticated: opts.isAuthenticated,
    passwordChangeRequired: opts.passwordChangeRequired ?? false,
  });
}

function renderWithRoute() {
  return render(
    <MemoryRouter
      initialEntries={['/protected']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/protected" element={<div>PROTECTED_CONTENT</div>} />
        </Route>
        <Route path="/login" element={<div>LOGIN_PAGE</div>} />
        <Route path="/change-password" element={<div>CHANGE_PASSWORD_PAGE</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    setAuth({ isAuthenticated: false });
  });

  it('미인증 시 로그인 페이지로 리다이렉트한다', () => {
    setAuth({ isAuthenticated: false });
    renderWithRoute();
    expect(screen.getByText('LOGIN_PAGE')).toBeInTheDocument();
    expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
  });

  it('인증 + 정상 상태면 보호된 콘텐츠를 렌더링한다', () => {
    setAuth({ isAuthenticated: true, passwordChangeRequired: false });
    renderWithRoute();
    expect(screen.getByText('PROTECTED_CONTENT')).toBeInTheDocument();
  });

  it('임시 비밀번호 상태(passwordChangeRequired=true)면 강제 변경 화면으로 리다이렉트한다', () => {
    setAuth({ isAuthenticated: true, passwordChangeRequired: true });
    renderWithRoute();
    expect(screen.getByText('CHANGE_PASSWORD_PAGE')).toBeInTheDocument();
    expect(screen.queryByText('PROTECTED_CONTENT')).not.toBeInTheDocument();
  });
});
