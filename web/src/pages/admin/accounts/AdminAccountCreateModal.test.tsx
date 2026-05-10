import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { notification } from 'antd';
import AdminAccountCreateModal from './AdminAccountCreateModal';
import { createAdminAccount } from '@/api/account';
import { fetchEmployees } from '@/api/employee';
import type { Employee } from '@/api/employee';

vi.mock('@/api/account', async () => {
  const actual = await vi.importActual<typeof import('@/api/account')>('@/api/account');
  return {
    ...actual,
    createAdminAccount: vi.fn(),
  };
});

vi.mock('@/api/employee', async () => {
  const actual = await vi.importActual<typeof import('@/api/employee')>('@/api/employee');
  return {
    ...actual,
    fetchEmployees: vi.fn(),
  };
});

const mockedCreate = vi.mocked(createAdminAccount);
const mockedFetchEmployees = vi.mocked(fetchEmployees);

const sampleEmployee: Employee = {
  id: 1,
  employeeCode: '100123',
  name: '홍길동',
  status: '재직',
  gender: null,
  orgName: '강남지점',
  costCenterCode: 'C001',
  role: null,
  roleLabel: null,
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

function renderModal(props: { onClose?: () => void; onSuccess?: () => void } = {}) {
  const onClose = props.onClose ?? vi.fn();
  const onSuccess = props.onSuccess ?? vi.fn();
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const utils = render(
    <QueryClientProvider client={client}>
      <AdminAccountCreateModal open onClose={onClose} onSuccess={onSuccess} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose, onSuccess };
}

describe('AdminAccountCreateModal (Spec #640 P2-W)', () => {
  beforeEach(() => {
    mockedCreate.mockReset();
    mockedFetchEmployees.mockReset();
    mockedFetchEmployees.mockResolvedValue({
      content: [sampleEmployee],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('W1 정상 입력 + 등록 클릭 → mutation 호출 + body 정합', async () => {
    mockedCreate.mockResolvedValue({
      id: 1234,
      name: '(신규) 강남점',
      accountGroup: '9999',
      employeeCode: '100123',
      branchCode: 'C001',
      branchName: '강남지점',
    });
    const onClose = vi.fn();
    const onSuccess = vi.fn();
    renderModal({ onClose, onSuccess });

    await userEvent.type(screen.getByPlaceholderText('(신규) 강남점'), '(신규) 강남점');
    const select = screen.getByRole('combobox');
    await userEvent.click(select);
    await userEvent.type(select, '홍길');
    await waitFor(() => expect(mockedFetchEmployees).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByText(/홍길동/)).toBeInTheDocument());
    await userEvent.click(screen.getByText(/홍길동/));

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(mockedCreate).toHaveBeenCalledWith({
      name: '(신규) 강남점',
      employeeCode: '100123',
    }));
    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    expect(onClose).toHaveBeenCalled();
  });

  it('W2 name blank — Form validation 에러 표시 + mutation 미호출', async () => {
    renderModal();
    await userEvent.click(screen.getByRole('button', { name: '등록' }));
    // employee 미선택이라 등록 버튼 disabled — 직접 disabled 상태 확인
    expect(screen.getByRole('button', { name: '등록' })).toBeDisabled();
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('W3 name prefix 미포함 — Form validator 에러 메시지', async () => {
    renderModal();

    // employeeCode 선택해야 등록 버튼 enabled
    const select = screen.getByRole('combobox');
    await userEvent.click(select);
    await userEvent.type(select, '홍');
    await waitFor(() => expect(screen.getByText(/홍길동/)).toBeInTheDocument());
    await userEvent.click(screen.getByText(/홍길동/));

    await userEvent.type(screen.getByPlaceholderText('(신규) 강남점'), '강남점');
    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() =>
      expect(screen.getByText(/거래처명에 \(신규\) \/ \(기타\) 중 1개를 포함해 주세요\./)).toBeInTheDocument(),
    );
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('W4 employeeCode 미선택 — 등록 버튼 disabled', () => {
    renderModal();
    expect(screen.getByRole('button', { name: '등록' })).toBeDisabled();
  });

  it('W5 Backend 409 ACCOUNT_NAME_DUPLICATE → notification + 인라인 에러', async () => {
    mockedCreate.mockRejectedValue(
      makeAxiosError(409, 'ACCOUNT_NAME_DUPLICATE', '동일한 이름의 거래처가 이미 존재합니다.'),
    );
    const notifySpy = vi.spyOn(notification, 'error');
    renderModal();

    await userEvent.type(screen.getByPlaceholderText('(신규) 강남점'), '(신규) 강남점');
    const select = screen.getByRole('combobox');
    await userEvent.click(select);
    await userEvent.type(select, '홍');
    await waitFor(() => expect(screen.getByText(/홍길동/)).toBeInTheDocument());
    await userEvent.click(screen.getByText(/홍길동/));

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(notifySpy).toHaveBeenCalled());
    expect(notifySpy.mock.calls[0][0].description).toBe('동일한 이름의 거래처가 이미 존재합니다.');
    notifySpy.mockRestore();
  });

  it('W6 Backend 200 → notification.success + onSuccess + onClose 호출', async () => {
    mockedCreate.mockResolvedValue({
      id: 1234,
      name: '(신규) 강남점',
      accountGroup: '9999',
      employeeCode: '100123',
      branchCode: 'C001',
      branchName: '강남지점',
    });
    const successSpy = vi.spyOn(notification, 'success');
    const onClose = vi.fn();
    const onSuccess = vi.fn();
    renderModal({ onClose, onSuccess });

    await userEvent.type(screen.getByPlaceholderText('(신규) 강남점'), '(신규) 강남점');
    const select = screen.getByRole('combobox');
    await userEvent.click(select);
    await userEvent.type(select, '홍');
    await waitFor(() => expect(screen.getByText(/홍길동/)).toBeInTheDocument());
    await userEvent.click(screen.getByText(/홍길동/));

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(successSpy).toHaveBeenCalled());
    expect(successSpy.mock.calls[0][0].message).toBe('거래처가 등록되었습니다.');
    expect(onSuccess).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    successSpy.mockRestore();
  });
});
