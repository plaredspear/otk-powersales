import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { notification } from 'antd';
import { AxiosError, AxiosHeaders } from 'axios';
import DeviceResetModal from './DeviceResetModal';
import { resetEmployeeDevice } from '@/api/admin/employeeCredential';
import type { Employee } from '@/api/employee';

vi.mock('@/api/admin/employeeCredential', () => ({
  resetEmployeePassword: vi.fn(),
  resetEmployeeDevice: vi.fn(),
}));

const mockedResetDevice = vi.mocked(resetEmployeeDevice);

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
};

function renderModal() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const onClose = vi.fn();
  const utils = render(
    <QueryClientProvider client={client}>
      <DeviceResetModal employee={employee} open onClose={onClose} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose };
}

function makeAxiosError(status: number, code: string): AxiosError {
  return new AxiosError(
    'Request failed',
    String(status),
    undefined,
    null,
    {
      status,
      statusText: '',
      data: { success: false, error: { code, message: code } },
      headers: {},
      config: { headers: new AxiosHeaders() },
    },
  );
}

describe('DeviceResetModal (Spec #582 P2-W)', () => {
  beforeEach(() => {
    mockedResetDevice.mockReset();
  });

  it('모달에 사번/이름 + 안내 문구가 표시된다', () => {
    const { baseElement } = renderModal();
    expect(screen.getByText('단말 초기화 확인')).toBeInTheDocument();
    expect(baseElement).toHaveTextContent('100123');
    expect(baseElement).toHaveTextContent('홍길동');
    expect(
      screen.getByText(/처리 후 사원이 모바일 앱에 다시 로그인하면/),
    ).toBeInTheDocument();
  });

  it('초기화 실행 성공 - 토스트 표시 + onClose 호출', async () => {
    mockedResetDevice.mockResolvedValueOnce({
      employeeId: 12345,
      employeeCode: '100123',
      name: '홍길동',
      previousDeviceBound: true,
      resetAt: '2026-05-04T14:30:00',
    });
    const successSpy = vi.spyOn(notification, 'success').mockImplementation(() => undefined);

    const { onClose } = renderModal();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(mockedResetDevice).toHaveBeenCalledWith(12345);
    });
    await waitFor(() => {
      expect(successSpy).toHaveBeenCalled();
    });
    const arg = successSpy.mock.calls[0][0];
    expect(arg.message).toContain('단말이 초기화되었습니다');
    expect(onClose).toHaveBeenCalledTimes(1);

    successSpy.mockRestore();
  });

  it('403 응답 시 - "권한이 없습니다" 토스트 + onClose 미호출', async () => {
    mockedResetDevice.mockRejectedValueOnce(makeAxiosError(403, 'EMP_AUTH_FORBIDDEN'));
    const errorSpy = vi.spyOn(notification, 'error').mockImplementation(() => undefined);

    const { onClose } = renderModal();
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '초기화 실행' }));

    await waitFor(() => {
      expect(errorSpy).toHaveBeenCalled();
    });
    const arg = errorSpy.mock.calls[0][0];
    expect(arg.description).toContain('권한이 없습니다');
    expect(onClose).not.toHaveBeenCalled();

    errorSpy.mockRestore();
  });
});
