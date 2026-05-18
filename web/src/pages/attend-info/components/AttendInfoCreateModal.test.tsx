import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AttendInfoCreateModal from './AttendInfoCreateModal';
import { createAttendInfo } from '@/api/attendInfo';

vi.mock('@/api/attendInfo', async () => {
  const actual = await vi.importActual<typeof import('@/api/attendInfo')>('@/api/attendInfo');
  return {
    ...actual,
    createAttendInfo: vi.fn(),
  };
});

const mockedCreate = vi.mocked(createAttendInfo);

function renderModal(onClose = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AttendInfoCreateModal open onClose={onClose} />
    </QueryClientProvider>,
  );
}

describe('AttendInfoCreateModal', () => {
  it('모달이 열리면 모든 필드와 보정 입력 안내가 노출된다', () => {
    renderModal();
    expect(
      screen.getByText('근무기간 조회 — 신규 등록 (SAP 미적재 / 오류 시 보정 입력용)'),
    ).toBeInTheDocument();
    expect(screen.getByLabelText('사원번호')).toBeInTheDocument();
    expect(screen.getByLabelText('근태유형')).toBeInTheDocument();
    expect(screen.getByLabelText('보정 입력 사유')).toBeInTheDocument();
  });

  it('사유 5자 미만이면 필드 검증으로 차단된다', async () => {
    const user = userEvent.setup();
    renderModal();
    await user.type(screen.getByLabelText('사원번호'), '20120253');
    await user.type(screen.getByLabelText('보정 입력 사유'), '짧음');
    await user.click(screen.getByRole('button', { name: /등록/ }));
    await waitFor(() => {
      expect(screen.getByText('사유는 최소 5자 이상이어야 합니다')).toBeInTheDocument();
    });
    expect(mockedCreate).not.toHaveBeenCalled();
  });

  it('근태유형 옵션이 6종 모두 노출된다', async () => {
    const user = userEvent.setup();
    renderModal();
    await user.click(screen.getByLabelText('근태유형'));
    expect(await screen.findByText('10 · 1년미만연차')).toBeInTheDocument();
    expect(await screen.findByText('14 · 연차')).toBeInTheDocument();
    expect(await screen.findByText('20 · 연중휴가')).toBeInTheDocument();
    expect(await screen.findByText('90 · 경조')).toBeInTheDocument();
    expect(await screen.findByText('120 · 생휴')).toBeInTheDocument();
    expect(await screen.findByText('133 · 연중&하기&하계')).toBeInTheDocument();
  });
});
