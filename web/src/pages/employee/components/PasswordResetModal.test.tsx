import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { notification } from 'antd';
import { AxiosError, AxiosHeaders } from 'axios';
import PasswordResetModal from './PasswordResetModal';
import { resetEmployeePassword } from '@/api/admin/employeeCredential';
import type { Employee } from '@/api/employee';

vi.mock('@/api/admin/employeeCredential', () => ({
  resetEmployeePassword: vi.fn(),
  resetEmployeeDevice: vi.fn(),
}));

const mockedResetPassword = vi.mocked(resetEmployeePassword);

const employee: Employee = {
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
  jikjong: null,
  workEmail: null,
  phone: null,
  age: null,
  yearsOfService: null,
};

function renderModal(isFemale = false) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const onClose = vi.fn();
  const utils = render(
    <QueryClientProvider client={client}>
      <PasswordResetModal employee={employee} open onClose={onClose} isFemale={isFemale} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose };
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

describe('PasswordResetModal (Spec #582 P2-W)', () => {
  beforeEach(() => {
    mockedResetPassword.mockReset();
  });

  it('모달에 사번/이름 + 임시 비밀번호 강조 박스("1234") + 복사 버튼이 표시된다', () => {
    const { baseElement } = renderModal();
    expect(screen.getByText('비밀번호 초기화 확인')).toBeInTheDocument();
    expect(baseElement).toHaveTextContent('100123');
    expect(baseElement).toHaveTextContent('홍길동');
    expect(screen.getByText('임시 비밀번호')).toBeInTheDocument();
    expect(screen.getByText('1234')).toBeInTheDocument();
    expect(screen.getByText('사원 본인에게 별도 전달해 주세요.')).toBeInTheDocument();
  });

  it('초기화 실행 성공 - 토스트 duration 10초 + 본문에 "1234" 명시 + onClose 호출', async () => {
    mockedResetPassword.mockResolvedValueOnce({
      employeeId: 12345,
      employeeCode: '100123',
      name: '홍길동',
      temporaryPasswordIssued: true,
      passwordChangeRequired: true,
      resetAt: '2026-05-04T14:30:00',
    });
    const successSpy = vi.spyOn(notification, 'success').mockImplementation(() => undefined);

    const { onClose } = renderModal();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(mockedResetPassword).toHaveBeenCalledWith(12345, false);
    });
    await waitFor(() => {
      expect(successSpy).toHaveBeenCalled();
    });
    const arg = successSpy.mock.calls[0][0];
    expect(arg.message).toContain('비밀번호가 초기화되었습니다');
    expect(arg.description).toContain("'1234'");
    expect(arg.duration).toBe(10);
    expect(onClose).toHaveBeenCalledTimes(1);

    successSpy.mockRestore();
  });

  it('여사원 현황 진입(isFemale) 시 female_employee 엔드포인트로 호출된다', async () => {
    mockedResetPassword.mockResolvedValueOnce({
      employeeId: 12345,
      employeeCode: '100123',
      name: '홍길동',
      temporaryPasswordIssued: true,
      passwordChangeRequired: true,
      resetAt: '2026-05-04T14:30:00',
    });
    const successSpy = vi.spyOn(notification, 'success').mockImplementation(() => undefined);

    renderModal(true);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(mockedResetPassword).toHaveBeenCalledWith(12345, true);
    });

    successSpy.mockRestore();
  });

  it('400 EMP_LOGIN_INACTIVE 응답 시 - 안내 메시지 토스트 + onClose 미호출', async () => {
    mockedResetPassword.mockRejectedValueOnce(
      makeAxiosError(400, 'EMP_LOGIN_INACTIVE', '앱 로그인이 비활성화된 사원입니다'),
    );
    const errorSpy = vi.spyOn(notification, 'error').mockImplementation(() => undefined);

    const { onClose } = renderModal();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const arg = errorSpy.mock.calls[0][0];
    expect(arg.description).toContain('앱 로그인이 비활성화');
    expect(onClose).not.toHaveBeenCalled();

    errorSpy.mockRestore();
  });

  it('404 EMP_NOT_FOUND 응답 시 - "사원 정보를 찾을 수 없습니다" 토스트', async () => {
    mockedResetPassword.mockRejectedValueOnce(
      makeAxiosError(404, 'EMP_NOT_FOUND', '사원을 찾을 수 없습니다'),
    );
    const errorSpy = vi.spyOn(notification, 'error').mockImplementation(() => undefined);

    renderModal();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const arg = errorSpy.mock.calls[0][0];
    expect(arg.description).toContain('사원 정보를 찾을 수 없습니다');

    errorSpy.mockRestore();
  });
});
