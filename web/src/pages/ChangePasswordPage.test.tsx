import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ChangePasswordPage from './ChangePasswordPage';
import { useAuthStore } from '@/stores/authStore';
import { changePassword, ChangePasswordError } from '@/api/auth';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

// changePassword 만 mock 하고 ChangePasswordError 는 실제 클래스를 그대로 노출한다
// (화면이 instanceof + sessionExpired 플래그로 세션 만료를 판별하므로).
vi.mock('@/api/auth', async () => {
  const actual = await vi.importActual<typeof import('@/api/auth')>('@/api/auth');
  return { ...actual, changePassword: vi.fn() };
});

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

  it('강제 변경 화면에 새 비밀번호/확인 입력과 정책 체크리스트가 표시된다', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: '비밀번호 변경' })).toBeInTheDocument();
    expect(screen.getByText(/임시 비밀번호로 로그인하셨습니다/)).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호')).toBeInTheDocument();
    expect(screen.getByLabelText('새 비밀번호 확인')).toBeInTheDocument();
    // 실시간 정책 체크리스트 규칙 표시
    expect(screen.getByText('8자 이상')).toBeInTheDocument();
    expect(
      screen.getByText('영문 대/소문자·숫자·특수문자 중 3종 이상 조합'),
    ).toBeInTheDocument();
  });

  it('입력에 따라 체크리스트 valid 상태가 실시간 반영된다', async () => {
    const user = userEvent.setup();
    renderPage();

    const lengthColor = () =>
      (screen.getByText('8자 이상').closest('li') as HTMLElement).style.color;
    const typesColor = () =>
      (
        screen
          .getByText('영문 대/소문자·숫자·특수문자 중 3종 이상 조합')
          .closest('li') as HTMLElement
      ).style.color;

    // 2종·8자 → 길이 녹색, 종류 빨강
    await user.type(screen.getByLabelText('새 비밀번호'), 'abcd1234');
    await waitFor(() => {
      expect(lengthColor()).toBe('rgb(82, 196, 26)');
      expect(typesColor()).toBe('rgb(255, 77, 79)');
    });

    // 특수문자 추가로 3종 충족 → 종류도 녹색
    await user.clear(screen.getByLabelText('새 비밀번호'));
    await user.type(screen.getByLabelText('새 비밀번호'), 'Abcd123!');
    await waitFor(() => {
      expect(lengthColor()).toBe('rgb(82, 196, 26)');
      expect(typesColor()).toBe('rgb(82, 196, 26)');
    });
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

    await user.type(screen.getByLabelText('새 비밀번호'), 'Newpw123!');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'Newpw123!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(mockedChangePassword).toHaveBeenCalledWith({ newPassword: 'Newpw123!' });
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

    await user.type(screen.getByLabelText('새 비밀번호'), 'Newpw123!');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'Different1!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText('비밀번호가 일치하지 않습니다')).toBeInTheDocument();
    });
    expect(mockedChangePassword).not.toHaveBeenCalled();
  });

  it('정책 위반(2종 조합) 시 종류 조합 에러 표시 + API 미호출', async () => {
    const user = userEvent.setup();
    renderPage();

    // abcd1234: 8자지만 소문자+숫자 2종 → 3종 미만 위반
    await user.type(screen.getByLabelText('새 비밀번호'), 'abcd1234');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'abcd1234');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(
        screen.getByText('영문 대/소문자·숫자·특수문자 중 3종 이상을 조합해주세요'),
      ).toBeInTheDocument();
    });
    expect(mockedChangePassword).not.toHaveBeenCalled();
  });

  it('정책 위반(7자) 시 길이 에러 표시 + API 미호출', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('새 비밀번호'), 'Abc12!x');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'Abc12!x');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText('비밀번호는 8자 이상이어야 합니다')).toBeInTheDocument();
    });
    expect(mockedChangePassword).not.toHaveBeenCalled();
  });

  it('강제 상태가 아니면(false) 대시보드로 리다이렉트(화면 미표시)', () => {
    setAuth(false);
    renderPage();
    expect(screen.queryByText('비밀번호 변경')).not.toBeInTheDocument();
  });

  it('세션 만료(401) 응답 시 로그아웃 후 /login 으로 탈출한다 (데드락 방지)', async () => {
    localStorage.setItem('accessToken', 'stale');
    localStorage.setItem('refreshToken', 'stale-refresh');
    mockedChangePassword.mockRejectedValueOnce(
      new ChangePasswordError('토큰이 만료되었습니다', true),
    );
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('새 비밀번호'), 'Newpw123!');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'Newpw123!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });
    // logout() 이 세션과 강제 플래그를 정리해 라우터 가드 루프를 끊는다.
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().passwordChangeRequired).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('세션 만료가 아닌 일반 실패는 에러만 표시하고 화면을 유지한다', async () => {
    mockedChangePassword.mockRejectedValueOnce(
      new ChangePasswordError('비밀번호 변경에 실패했습니다', false),
    );
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('새 비밀번호'), 'Newpw123!');
    await user.type(screen.getByLabelText('새 비밀번호 확인'), 'Newpw123!');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText('비밀번호 변경에 실패했습니다')).toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalledWith('/login', { replace: true });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
  });

  it('다시 로그인 버튼: 로그아웃 후 /login 으로 이동한다', async () => {
    localStorage.setItem('accessToken', 'stale');
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole('button', { name: '다시 로그인' }));

    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().passwordChangeRequired).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
  });
});
