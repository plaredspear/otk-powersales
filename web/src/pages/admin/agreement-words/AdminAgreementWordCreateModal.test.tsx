import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AxiosError, AxiosHeaders } from 'axios';
import { notification } from 'antd';
import dayjs from 'dayjs';
import AdminAgreementWordCreateModal from './AdminAgreementWordCreateModal';
import { createAgreementWord } from '@/api/agreementWord';

vi.mock('@/api/agreementWord', async () => {
  const actual = await vi.importActual<typeof import('@/api/agreementWord')>('@/api/agreementWord');
  return {
    ...actual,
    createAgreementWord: vi.fn(),
    fetchActiveAgreementWord: vi.fn(),
  };
});

const mockedCreate = vi.mocked(createAgreementWord);

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
      <AdminAgreementWordCreateModal open onClose={onClose} onSuccess={onSuccess} />
    </QueryClientProvider>,
  );
  return { ...utils, onClose, onSuccess, client };
}

async function fillFutureDate(label = '다음 시행 일자') {
  // antd DatePicker — placeholder 가 한국어인 'Select date' (또는 default 영어). 실제 input 은 Form.Item label 로 접근.
  const futureDate = dayjs().add(7, 'day').format('YYYY-MM-DD');
  const datePickerInput = document.querySelector(
    `input[id$="afterActiveDate"]`,
  ) as HTMLInputElement;
  expect(datePickerInput).toBeTruthy();
  await userEvent.click(datePickerInput);
  await userEvent.type(datePickerInput, futureDate);
  // dropdown 닫기 (Enter)
  await userEvent.keyboard('{Enter}');
  // antd 비동기 update 대기
  await act(async () => {});
  // label 변수는 향후 다국어 정합성 체크 시 사용 예정 (현재 테스트는 input id 기반)
  void label;
}

describe('AdminAgreementWordCreateModal (Spec #658 P2-W)', () => {
  beforeEach(() => {
    mockedCreate.mockReset();
  });

  it('W4 정상 입력 + 등록 클릭 → mutation 호출 + body 정합 + 성공 알림 + Modal 닫힘', async () => {
    mockedCreate.mockResolvedValue({
      agreementWordId: 1234,
      name: 'AGR-2026-001',
      afterActiveDate: '2026-12-01',
      active: false,
      activeDate: null,
      createdAt: '2026-05-11T10:00:00',
    });
    const successSpy = vi.spyOn(notification, 'success');
    const onClose = vi.fn();
    const onSuccess = vi.fn();
    renderModal({ onClose, onSuccess });

    await userEvent.type(screen.getByPlaceholderText('예: AGR-2026-001'), 'AGR-2026-001');
    await fillFutureDate();
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    await userEvent.type(textarea, '약관 본문입니다');

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(mockedCreate).toHaveBeenCalled());
    const callArg = mockedCreate.mock.calls[0][0];
    expect(callArg.name).toBe('AGR-2026-001');
    expect(callArg.contents).toBe('약관 본문입니다');
    expect(callArg.afterActiveDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    await waitFor(() => expect(successSpy).toHaveBeenCalled());
    expect(successSpy.mock.calls[0][0].message).toBe('약관이 등록되었습니다.');
    expect(onSuccess).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
    successSpy.mockRestore();
  });

  it('W5 name 누락 → Form validation 에러 + mutation 미호출', async () => {
    renderModal();
    // afterActiveDate / contents 채워도 name 비어 있으면 검증 실패
    await fillFutureDate();
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    await userEvent.type(textarea, '약관 본문');

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => {
      expect(screen.getByText(/약관 이름은 필수입니다/)).toBeInTheDocument();
    });
    expect(mockedCreate).not.toHaveBeenCalled();
    await act(async () => {});
  });

  it('W6 name 80자 초과 입력 차단 (maxLength prop)', () => {
    renderModal();
    const input = screen.getByPlaceholderText('예: AGR-2026-001') as HTMLInputElement;
    expect(input.maxLength).toBe(80);
  });

  it('W7 contents 8000자 초과 입력 차단 (maxLength prop)', () => {
    renderModal();
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    expect(textarea.maxLength).toBe(8000);
  });

  it('W8 afterActiveDate 미입력 → Form validation 에러', async () => {
    renderModal();
    await userEvent.type(screen.getByPlaceholderText('예: AGR-2026-001'), 'AGR-2026-001');
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    await userEvent.type(textarea, '약관 본문');

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => {
      expect(screen.getByText(/다음 시행 일자는 필수입니다/)).toBeInTheDocument();
    });
    expect(mockedCreate).not.toHaveBeenCalled();
    await act(async () => {});
  });

  it('W9 Backend 400 응답 → notification.error', async () => {
    mockedCreate.mockRejectedValue(
      makeAxiosError(400, 'INVALID_REQUEST', '약관 이름은 필수입니다'),
    );
    const errorSpy = vi.spyOn(notification, 'error');
    renderModal();

    await userEvent.type(screen.getByPlaceholderText('예: AGR-2026-001'), 'AGR-2026-001');
    await fillFutureDate();
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    await userEvent.type(textarea, '약관 본문');

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(errorSpy).toHaveBeenCalled());
    expect(errorSpy.mock.calls[0][0].description).toBe('약관 이름은 필수입니다');
    errorSpy.mockRestore();
  });

  it('W9-bis Backend 403 응답 → notification.error 권한 안내', async () => {
    mockedCreate.mockRejectedValue(
      new AxiosError('Forbidden', '403', undefined, null, {
        status: 403,
        statusText: '',
        data: '',
        headers: {},
        config: { headers: new AxiosHeaders() },
      }),
    );
    const errorSpy = vi.spyOn(notification, 'error');
    renderModal();

    await userEvent.type(screen.getByPlaceholderText('예: AGR-2026-001'), 'AGR-2026-001');
    await fillFutureDate();
    const textarea = document.querySelector('textarea') as HTMLTextAreaElement;
    await userEvent.type(textarea, '약관 본문');

    await userEvent.click(screen.getByRole('button', { name: '등록' }));

    await waitFor(() => expect(errorSpy).toHaveBeenCalled());
    expect(errorSpy.mock.calls[0][0].description).toBe('등록 권한이 없습니다.');
    errorSpy.mockRestore();
  });
});
