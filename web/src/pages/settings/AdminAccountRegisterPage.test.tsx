import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { Modal } from 'antd';
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

  it('정상 렌더링 — 사번 prefix(ADMIN-) Space.Compact 표시 및 안내 문구 표시 + 사번 입력란 autoFocus', () => {
    renderPage();
    expect(screen.getByText('시스템 관리자 등록')).toBeInTheDocument();
    // prefix 는 disabled Input 의 value 로 표시되므로 getByDisplayValue 로 검증
    // (이전 addonBefore 는 textNode 였으나 Space.Compact 패턴은 disabled Input value)
    expect(screen.getByDisplayValue('ADMIN-')).toBeInTheDocument();
    expect(
      screen.getByText(/SAP 인바운드로 동기화되지 않는 시스템 관리자 계정/),
    ).toBeInTheDocument();
    expect(screen.getByText(/시스템관리자.*권한을 가지며/)).toBeInTheDocument();
    // 사번 본문 입력란이 마운트 직후 포커스를 보유한다 (581-P2 §5.2)
    expect(screen.getByLabelText('사번')).toHaveFocus();
  });

  it('role 안내 문구 명시 — [시스템관리자] 권한 고정 + 권한 선택 UI 부재', () => {
    renderPage();
    expect(
      screen.getByText(/\[시스템관리자\] 권한을 가지며/),
    ).toBeInTheDocument();
    // role 선택 셀렉트가 페이지에 존재하지 않음을 단언 (백엔드 고정값)
    expect(screen.queryByLabelText(/권한/)).not.toBeInTheDocument();
    expect(screen.queryByRole('combobox', { name: /권한/ })).not.toBeInTheDocument();
  });

  // userEvent.type 다회 입력 (사번/이름/비밀번호/확인) 누적으로 전체 테스트 실행 시 기본 5초
  // timeout 초과. 명시적 10초 부여 (#643 P2-W AccountUpdateModal/AccountCreateModal W1 동일 패턴).
  it('등록 중 로딩 상태 — mutation 진행 중 등록 버튼이 loading 표시', { timeout: 10000 }, async () => {
    let resolveRegister: ((value: unknown) => void) | undefined;
    mockedRegister.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveRegister = resolve as (value: unknown) => void;
        }),
    );

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');
    await user.type(screen.getByLabelText('비밀번호'), 'Admin@2026!');
    await user.type(screen.getByLabelText('비밀번호 확인'), 'Admin@2026!');
    await user.click(screen.getByRole('button', { name: '등록하기' }));

    // Ant Design Button 의 loading=true 는 ant-btn-loading 클래스 + 내부 spinner 아이콘으로 표현된다.
    await waitFor(() => {
      const button = screen.getByRole('button', { name: /등록하기/ });
      expect(button).toHaveClass('ant-btn-loading');
    });

    // mutation resolve 후 정리 (테스트 누수 방지)
    resolveRegister?.({
      employeeId: 1,
      employeeCode: 'ADMIN-001',
      name: '홍길동',
      role: 'SYSTEM_ADMIN',
      origin: 'MANUAL',
      appLoginActive: false,
      passwordChangeRequired: true,
      createdAt: '2026-05-04T00:00:00',
    });
    await waitFor(() => {
      expect(navigateMock).toHaveBeenCalledWith('/settings/permissions/employees');
    });
  });

  it('페이지 이탈 확인 모달 — 입력 후 취소 클릭 시 Modal.confirm 호출 + 이탈 선택 시 navigate', async () => {
    // antd Modal.confirm 은 ReactDOM portal 로 마운트되어 React 19 환경에서 RTL 쿼리가 불안정하므로
    // 호출 자체와 onOk 콜백 동작을 spy 로 검증한다.
    const confirmSpy = vi.spyOn(Modal, 'confirm').mockImplementation(() => {
      return { destroy: vi.fn(), update: vi.fn() };
    });

    const user = userEvent.setup();
    renderPage();

    await user.type(screen.getByLabelText('사번'), '001');
    await user.type(screen.getByLabelText('이름'), '홍길동');

    await user.click(screen.getByRole('button', { name: '취소' }));

    // Modal.confirm 가 호출되었고, 모달의 헤더/버튼/onOk 가 스펙 §3.2 시나리오 #12 에 부합하는지 단언
    await waitFor(() => {
      expect(confirmSpy).toHaveBeenCalledTimes(1);
    });
    const config = confirmSpy.mock.calls[0][0];
    expect(config.title).toBe('입력 중인 내용이 있습니다');
    expect(config.okText).toBe('이탈');
    expect(config.cancelText).toBe('머무르기');

    // 사용자가 '이탈' 을 선택했다고 가정하고 onOk 를 직접 호출 → 목록으로 navigate
    await config.onOk?.();
    expect(navigateMock).toHaveBeenCalledWith('/settings/permissions/employees');

    confirmSpy.mockRestore();
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
      await screen.findByText(/영문\/숫자\/하이픈\/언더스코어 1~30자만 가능합니다/),
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
