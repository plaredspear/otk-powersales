import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ChangePasswordPage from './ChangePasswordPage';
import { useAuthStore } from '@/stores/authStore';
import { changePassword } from '@/api/auth';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@/api/auth', () => ({
  changePassword: vi.fn(),
}));

const mockedChangePassword = vi.mocked(changePassword);

function setAuth(passwordChangeRequired: boolean, isAuthenticated = true) {
  useAuthStore.setState({
    user: isAuthenticated
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
    accessToken: isAuthenticated ? 'token' : null,
    isAuthenticated,
    passwordChangeRequired,
  });
}

function renderPage() {
  return render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <ChangePasswordPage />
    </MemoryRouter>,
  );
}

describe('ChangePasswordPage', () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    mockedChangePassword.mockReset();
    localStorage.clear();
    setAuth(true);
  });

  it('강제 변경 화면에 새 비밀번호/확인 입력과 안내 문구가 표시된다', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: '비밀번호 변경' })).toBeInTheDocument();
    expect(screen.getByText(/임시 비밀번호로 로그인하셨습니다/)).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호')).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호 확인')).toBeInTheDocument();
  });

  it('변경 성공 - 새 토큰 저장 + 강제 플래그 해제 + 대시보드 이동', async () => {
    mockedChangePassword.mockResolvedValueOnce({
      passwordChangeRequired: false,
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
      expiresIn: 3600,
    });
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('새 비밀번호'), 'newpw123');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'newpw123');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(mockedChangePassword).toHaveBeenCalledWith({ newPassword: 'newpw123' });
    });
    await waitFor(() => {
      expect(localStorage.getItem('accessToken')).toBe('new-access');
    });
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh');
    expect(useAuthStore.getState().passwordChangeRequired).toBe(false);
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });
  });

  it('비밀번호 불일치 시 API 를 호출하지 않는다', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('새 비밀번호'), 'newpw123');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'different');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText('비밀번호가 일치하지 않습니다')).toBeInTheDocument();
    });
    expect(mockedChangePassword).not.toHaveBeenCalled();
  });

  it('강제 상태가 아니면(false) 대시보드로 리다이렉트(화면 미표시)', () => {
    setAuth(false);
    renderPage();
    expect(screen.queryByText('비밀번호 변경')).not.toBeInTheDocument();
  });
});
