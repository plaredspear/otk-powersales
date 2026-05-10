import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { notification } from 'antd';
import AccountDeleteAction from './AccountDeleteAction';
import { deleteAdminAccount, type Account } from '@/api/account';

vi.mock('@/api/account', async () => {
  const actual = await vi.importActual<typeof import('@/api/account')>('@/api/account');
  return {
    ...actual,
    deleteAdminAccount: vi.fn(),
  };
});

const mockedDelete = vi.mocked(deleteAdminAccount);

const nativeAccount: Account = {
  id: 1234,
  externalKey: null,
  name: '(신규) 강남점',
  abcType: null,
  branchCode: null,
  branchName: '강남지점',
  employeeCode: '100123',
  address1: null,
  phone: null,
  accountStatusName: '활성',
};

const sapAccount: Account = {
  ...nativeAccount,
  id: 5678,
  externalKey: 'SAP-12345',
  name: '이마트 본점',
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

function renderAction(account: Account) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = vi.spyOn(client, 'invalidateQueries');
  const utils = render(
    <QueryClientProvider client={client}>
      <AccountDeleteAction account={account} />
    </QueryClientProvider>,
  );
  return { ...utils, client, invalidateSpy };
}

describe('AccountDeleteAction (Spec #642 P2-W)', () => {
  beforeEach(() => {
    mockedDelete.mockReset();
  });

  it('W1 row 삭제 클릭 → Modal.confirm 표시 → 삭제 클릭 → mutation 호출', async () => {
    mockedDelete.mockResolvedValue();
    const successSpy = vi.spyOn(notification, 'success');
    renderAction(nativeAccount);

    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    // confirm modal 'OK' 버튼 (okText '삭제')
    const confirmButtons = await screen.findAllByRole('button', { name: '삭제' });
    // 첫 버튼은 row action, 두 번째는 modal 의 OK
    await userEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => expect(mockedDelete).toHaveBeenCalledWith(1234));
    await waitFor(() => expect(successSpy).toHaveBeenCalled());
    expect(successSpy.mock.calls[0][0].message).toBe('거래처가 삭제되었습니다.');
    successSpy.mockRestore();
  });

  it('W2 row 삭제 클릭 → 취소 → mutation 미호출', async () => {
    renderAction(nativeAccount);

    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    const cancelBtn = await screen.findByRole('button', { name: '취소' });
    await userEvent.click(cancelBtn);

    // 취소 클릭 후에도 mutation 미호출만 확인 (Modal DOM 잔존은 antd 동작)
    expect(mockedDelete).not.toHaveBeenCalled();
  });

  it('W3 SAP 동기 거래처 — 삭제 버튼 disabled', () => {
    renderAction(sapAccount);
    const btn = screen.getByRole('button', { name: '삭제' });
    expect(btn).toBeDisabled();
  });

  it('W5 Backend 409 ACCOUNT_DELETE_BLOCKED_SAP_SYNCED → notification.error 메시지 정합', async () => {
    mockedDelete.mockRejectedValue(
      makeAxiosError(409, 'ACCOUNT_DELETE_BLOCKED_SAP_SYNCED', '거래처 코드가 있는 거래처는 삭제할 수 없습니다.'),
    );
    const errorSpy = vi.spyOn(notification, 'error');
    renderAction(nativeAccount);

    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    const confirmButtons = await screen.findAllByRole('button', { name: '삭제' });
    await userEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => expect(errorSpy).toHaveBeenCalled());
    expect(errorSpy.mock.calls[0][0].description).toBe(
      '거래처 코드가 있는 거래처는 삭제할 수 없습니다.',
    );
    errorSpy.mockRestore();
  });

  it('W6 Backend 404 ACCOUNT_NOT_FOUND → notification.error + invalidate 호출', async () => {
    mockedDelete.mockRejectedValue(
      makeAxiosError(404, 'ACCOUNT_NOT_FOUND', '거래처를 찾을 수 없습니다.'),
    );
    const errorSpy = vi.spyOn(notification, 'error');
    const { invalidateSpy } = renderAction(nativeAccount);

    await userEvent.click(screen.getByRole('button', { name: '삭제' }));
    const confirmButtons = await screen.findAllByRole('button', { name: '삭제' });
    await userEvent.click(confirmButtons[confirmButtons.length - 1]);

    await waitFor(() => expect(errorSpy).toHaveBeenCalled());
    // 다른 관리자 선삭제 회복용 invalidate 가 추가로 호출되었는지 확인 — onSuccess 의 invalidate 와 별도
    await waitFor(() =>
      expect(
        invalidateSpy.mock.calls.some(
          (call) => Array.isArray(call[0]?.queryKey) && call[0].queryKey[0] === 'admin' && call[0].queryKey[1] === 'accounts',
        ),
      ).toBe(true),
    );
    errorSpy.mockRestore();
  });
});
