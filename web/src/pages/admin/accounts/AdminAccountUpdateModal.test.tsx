import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { notification } from 'antd';
import AdminAccountUpdateModal from './AdminAccountUpdateModal';
import { updateAdminAccount, type Account, type AdminAccountUpdateResponseData } from '@/api/account';
import { fetchEmployees } from '@/api/employee';
import type { Employee } from '@/api/employee';

vi.mock('@/api/account', async () => {
  const actual = await vi.importActual<typeof import('@/api/account')>('@/api/account');
  return {
    ...actual,
    updateAdminAccount: vi.fn(),
  };
});

vi.mock('@/api/employee', async () => {
  const actual = await vi.importActual<typeof import('@/api/employee')>('@/api/employee');
  return {
    ...actual,
    fetchEmployees: vi.fn(),
  };
});

const mockedUpdate = vi.mocked(updateAdminAccount);
const mockedFetchEmployees = vi.mocked(fetchEmployees);

const sampleAccount: Account = {
  id: 1234,
  externalKey: null,
  name: '(신규) 강남점',
  abcType: 'A',
  branchCode: 'C001',
  branchName: '강남지점',
  employeeCode: '100123',
  address1: '서울특별시 강남구',
  phone: '02-0000-0000',
  accountStatusName: '활성',
};

const sampleEmployee: Employee = {
  id: 2,
  employeeCode: '200456',
  name: '이몽룡',
  status: '재직',
  gender: null,
  orgName: '서초지점',
  costCenterCode: 'C002',
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

const baseResponse: AdminAccountUpdateResponseData = {
  id: 1234,
  name: '(신규) 강남점 신호 수정',
  accountGroup: '9999',
  employeeCode: '100123',
  branchCode: 'C001',
  branchName: '강남지점',
  address1: '서울특별시 강남구 테헤란로 100',
  address2: null,
  zipCode: null,
  phone: '02-0000-0000',
  mobilePhone: null,
  representative: null,
  email: null,
  fax: null,
  website: null,
  industry: null,
  description: null,
  businessNumber: null,
  businessLicenseNumber: null,
  businessType: null,
  businessCategory: null,
  abcType: 'A',
  abcTypeCode: null,
  accountType: null,
  accountStatusName: '활성',
  accountStatusCode: null,
  accountNumber: null,
  site: null,
  accountSource: null,
  mapCoordinate: null,
  parentSfid: null,
  rating: null,
  ownership: null,
  freezerInstalled: null,
  freezerType: null,
  firstInstalled: null,
  orderEndTime: null,
  closingTime1: null,
  closingTime2: null,
  closingTime3: null,
  remainingCredit: null,
  totalCredit: null,
  annualRevenue: null,
  numberOfEmployees: null,
  consignmentAcc: null,
  distribution: null,
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

function renderModal(
  props: { account?: Account | null; onClose?: () => void; onSuccess?: () => void } = {},
) {
  const onClose = props.onClose ?? vi.fn();
  const onSuccess = props.onSuccess ?? vi.fn();
  const account = props.account === undefined ? sampleAccount : props.account;
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const utils = render(
    <QueryClientProvider client={client}>
      <AdminAccountUpdateModal open account={account} onClose={onClose} onSuccess={onSuccess} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose, onSuccess };
}

describe('AdminAccountUpdateModal (Spec #643 P2-W)', () => {
  beforeEach(() => {
    mockedUpdate.mockReset();
    mockedFetchEmployees.mockReset();
    mockedFetchEmployees.mockResolvedValue({
      content: [sampleEmployee],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
  });

  // userEvent.type 한글 입력 + Form validation + mutation 처리 등이 누적되어 전체 테스트 실행 시
  // 기본 5초 timeout 을 초과하는 케이스가 있어 명시적 10초 부여 (기존 #640 W1 동일 패턴 — 같은 정정).
  it('W1 변경된 필드만 PUT body 에 포함 — name + phone 변경', { timeout: 10000 }, async () => {
    mockedUpdate.mockResolvedValue(baseResponse);
    const onClose = vi.fn();
    const onSuccess = vi.fn();
    renderModal({ onClose, onSuccess });

    // name 변경
    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '(신규) 강남점 신호 수정');

    // phone 변경 — '연락처' 그룹의 첫 번째 input (전화)
    const phoneLabel = screen.getByText('전화');
    const phoneInput = phoneLabel.parentElement?.parentElement?.querySelector('input');
    expect(phoneInput).toBeTruthy();
    if (phoneInput) {
      await userEvent.clear(phoneInput);
      await userEvent.type(phoneInput, '02-1234-5678');
    }

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => expect(mockedUpdate).toHaveBeenCalled());
    const [calledId, calledPayload] = mockedUpdate.mock.calls[0];
    expect(calledId).toBe(1234);
    expect(calledPayload.name).toBe('(신규) 강남점 신호 수정');
    expect(calledPayload.phone).toBe('02-1234-5678');
    // 변경 안 한 필드 (address1/branchCode 등) 는 PUT body 미포함
    expect(calledPayload.address1).toBeUndefined();
    expect(calledPayload.branchCode).toBeUndefined();
    await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    expect(onClose).toHaveBeenCalled();
  });

  it('W2 변경 없음 + 저장 클릭 → PUT skip + 모달 close (notification.info)', async () => {
    const infoSpy = vi.spyOn(notification, 'info');
    const onClose = vi.fn();
    renderModal({ onClose });

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    expect(mockedUpdate).not.toHaveBeenCalled();
    expect(infoSpy).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    infoSpy.mockRestore();
  });

  it('W3 name 을 빈 값으로 변경 → Form validation 에러 + mutation 미호출', async () => {
    renderModal();

    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => {
      expect(screen.getByText('거래처명을 입력해 주세요.')).toBeInTheDocument();
      expect(mockedUpdate).not.toHaveBeenCalled();
    });
    // antd Form validate reject 후 ItemHolder 의 후속 state update (errors map) flush 대기 —
    // 이 act flush 가 없으면 테스트 종료 시점에 unwrapped state update 경고 발생.
    await act(async () => {});
  });

  it('W4 name prefix 미포함 → Form validator 에러 메시지', async () => {
    renderModal();

    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '강남점');

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => {
      expect(screen.getByText(/거래처명에 \(신규\) \/ \(기타\) 중 1개를 포함해 주세요\./)).toBeInTheDocument();
      expect(mockedUpdate).not.toHaveBeenCalled();
    });
    // antd Form validate reject 후 ItemHolder state update flush 대기 (W3 동일)
    await act(async () => {});
  });

  it('W6 Backend 409 ACCOUNT_NAME_DUPLICATE → notification + 인라인 에러', async () => {
    mockedUpdate.mockRejectedValue(
      makeAxiosError(409, 'ACCOUNT_NAME_DUPLICATE', '동일한 이름의 거래처가 이미 존재합니다.'),
    );
    const notifySpy = vi.spyOn(notification, 'error');
    renderModal();

    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '(신규) 다른지점');

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => expect(notifySpy).toHaveBeenCalled());
    expect(notifySpy.mock.calls[0][0].description).toBe('동일한 이름의 거래처가 이미 존재합니다.');
    notifySpy.mockRestore();
  });

  it('W7 Backend 200 → notification.success + onSuccess + onClose 호출', async () => {
    mockedUpdate.mockResolvedValue(baseResponse);
    const successSpy = vi.spyOn(notification, 'success');
    const onClose = vi.fn();
    const onSuccess = vi.fn();
    renderModal({ onClose, onSuccess });

    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '(신규) 강남점 신호 수정');

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => expect(successSpy).toHaveBeenCalled());
    expect(successSpy.mock.calls[0][0].message).toBe('거래처가 수정되었습니다.');
    expect(onSuccess).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    successSpy.mockRestore();
  });

  it('W8 Backend 404 ACCOUNT_NOT_FOUND → notification.error', async () => {
    mockedUpdate.mockRejectedValue(
      makeAxiosError(404, 'ACCOUNT_NOT_FOUND', '거래처를 찾을 수 없습니다: 1234'),
    );
    const notifySpy = vi.spyOn(notification, 'error');
    renderModal();

    const nameInput = screen.getByPlaceholderText('(신규) 강남점');
    await userEvent.clear(nameInput);
    await userEvent.type(nameInput, '(신규) 새 이름');

    await userEvent.click(screen.getByRole('button', { name: '저장' }));

    await waitFor(() => expect(notifySpy).toHaveBeenCalled());
    expect(notifySpy.mock.calls[0][0].message).toBe('거래처 수정 실패');
    notifySpy.mockRestore();
  });
});
