import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import AdminAccountRegisterPage from './AdminAccountRegisterPage';
import { registerAdminAccount } from '@/api/admin/registerAdminAccount';

vi.mock('@/api/admin/registerAdminAccount', () => ({
  registerAdminAccount: vi.fn(),
}));

const navigateMock = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigateMock,
  };
});

const mockedRegister = vi.mocked(registerAdminAccount);

function createQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

function renderPage() {
  const client = createQueryClient();
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <AdminAccountRegisterPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

function makeAxiosError(status: number, code: string, message: string): AxiosError {
  return new AxiosError(
    'Request failed',
    String(status),
    undefined,
    null,
    {
      status,
      statusText: '',
      data: { success: false, error: { code, message } },
      headers: {},
      config: { headers: new AxiosHeaders() },
    },
  );
}

describe('AdminAccountRegisterPage', () => {
  beforeEach(() => {
    mockedRegister.mockReset();
    navigateMock.mockReset();
  });

  it('정상 렌더링 — 사번 prefix(ADMIN-) addonBefore 표시 및 안내 문구 표시', () => {
    renderPage();
    expect(screen.getByText('시스템 관리자 등록')).toBeInTheDocument();
    expect(screen.getByText('ADMIN-')).toBeInTheDocument();
    expect(
      screen.getByText(/SAP 인바운드로 동기화되지 않는 시스템 관리자 계정/),
    ).toBeInTheDocument();
    expect(screen.getByText(/시스템관리자.*권한을 가지며/)).toBeInTheDocument();
  });

  it('정상 제출 — 사번이 ADMIN-{본문} 으로 합쳐져 API 호출, 성공 후 목록으로 navigate', async () => {
    mockedRegister.mockResolvedValueOnce({
      employeeId: 12345,
      employeeCode: 'ADMIN-001',
      name: '홍길동',
      role: 'SYSTEM_ADMIN',
      origin: 'MANUAL',
      appLoginActive: false,
      passwordChangeRequired: true,
      createdAt: '2026-05-03T14:30:00',
    });

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');
    await user.type(screen.getByLabelText('비밀번호'), 'Admin@2026!');
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Admin@2026!');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    await waitFor(() => {
      expect(mockedRegister).toHaveBeenCalledTimes(1);
    });

    const payload = mockedRegister.mock.calls[0][0];
    expect(payload.employeeCode).toBe('ADMIN-001');
    expect(payload.name).toBe('홍길동');
    expect(payload.password).toBe('Admin@2026!');
    expect(payload.passwordConfirm).toBe('Admin@2026!');
    // 빈 부가 정보 필드는 null 로 변환
    expect(payload.workEmail).toBeNull();
    expect(payload.workPhone).toBeNull();
    expect(payload.orgName).toBeNull();
    expect(payload.costCenterCode).toBeNull();
    // role 은 송신하지 않는다 (백엔드 고정 SYSTEM_ADMIN)
    expect(payload).not.toHaveProperty('role');

    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith('/settings/permissions/employees');
    });
  });

  it('사번 형식 위반 — 한글 입력 시 인라인 에러 메시지 표시', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '홍길동');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    expect(
      await screen.findByText(/영문\/숫자\/하이픈\/언더스코어만 사용/),
    ).toBeInTheDocument();
    expect(mockedRegister).not.toHaveBeenCalled();
  });

  it('비밀번호 정책 — 7자 입력 시 인라인 에러 표시', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('비밀번호'), 'Ab1!def');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    expect(
      await screen.findByText('비밀번호는 8자 이상 64자 이하여야 합니다'),
    ).toBeInTheDocument();
  });

  it('비밀번호 정책 — 영문만 8자 입력 시 인라인 에러 표시', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('비밀번호'), 'abcdefgh');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    expect(
      await screen.findByText('영문, 숫자, 특수문자 중 2종 이상을 조합해주세요'),
    ).toBeInTheDocument();
  });

  it('비밀번호 불일치 — passwordConfirm 필드에 인라인 에러 표시', async () => {
    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');
    await user.type(screen.getByLabelText('비밀번호'), 'Admin@2026!');
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Other@2026!');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    expect(await screen.findByText('비밀번호가 일치하지 않습니다')).toBeInTheDocument();
    expect(mockedRegister).not.toHaveBeenCalled();
  });

  it('서버 응답 EMPLOYEE_CODE_DUPLICATED — 사번 필드에 인라인 에러 표시', async () => {
    mockedRegister.mockRejectedValueOnce(
      makeAxiosError(409, 'EMPLOYEE_CODE_DUPLICATED', '이미 사용 중인 사번입니다'),
    );

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');
    await user.type(screen.getByLabelText('비밀번호'), 'Admin@2026!');
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Admin@2026!');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    expect(await screen.findByText('이미 사용 중인 사번입니다')).toBeInTheDocument();
    expect(navigateMock).not.toHaveBeenCalledWith('/settings/permissions/employees');
  });

  it('서버 응답 403 FORBIDDEN — 토스트 후 이전 페이지로 이동', async () => {
    mockedRegister.mockRejectedValueOnce(
      makeAxiosError(403, 'FORBIDDEN', '권한이 없습니다'),
    );

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');
    await user.type(screen.getByLabelText('비밀번호'), 'Admin@2026!');
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Admin@2026!');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith(-1);
    });
  });
});
