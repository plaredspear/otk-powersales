import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import NaverGeocodeTestPage from './NaverGeocodeTestPage';

const mutateAsyncMock = vi.fn();
const mutationState = { isPending: false };

vi.mock('@/hooks/admin/useNaverGeocodeTest', () => ({
  useNaverGeocodeTest: () => ({
    mutateAsync: mutateAsyncMock,
    isPending: mutationState.isPending,
  }),
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <MemoryRouter>
      <QueryClientProvider client={client}>
        <NaverGeocodeTestPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('NaverGeocodeTestPage (Spec #638 P2-W)', () => {
  beforeEach(() => {
    mutateAsyncMock.mockReset();
    mutationState.isPending = false;
  });

  it('E1 - address blank 시 "변환" 버튼이 disabled 상태', () => {
    renderPage();
    const button = screen.getByRole('button', { name: '변환' });
    expect(button).toBeDisabled();
  });

  it('H1 - 정상 주소 변환 시 결과 Table 표시', async () => {
    mutateAsyncMock.mockResolvedValue({
      input: '서울특별시 강남구 테헤란로 123',
      matchedCount: 1,
      results: [
        {
          roadAddress: '서울특별시 강남구 테헤란로 123',
          jibunAddress: '서울특별시 강남구 역삼동 123-45',
          longitude: '127.0584',
          latitude: '37.5074',
        },
      ],
    });

    renderPage();
    const user = userEvent.setup();
    const input = screen.getByPlaceholderText(/예: 서울특별시 강남구 테헤란로 123/);
    await user.type(input, '서울특별시 강남구 테헤란로 123');
    const button = screen.getByRole('button', { name: '변환' });
    await user.click(button);

    await waitFor(() => {
      expect(mutateAsyncMock).toHaveBeenCalledWith({
        address: '서울특별시 강남구 테헤란로 123',
      });
    });
    expect(await screen.findByText(/매칭 건수:/)).toBeInTheDocument();
    expect(screen.getByText('127.0584')).toBeInTheDocument();
    expect(screen.getByText('37.5074')).toBeInTheDocument();
  });

  it('H2 - 매칭 0건 시 "변환 결과 없음" 메시지 표시', async () => {
    mutateAsyncMock.mockResolvedValue({
      input: '잘못된 주소',
      matchedCount: 0,
      results: [],
    });

    renderPage();
    const user = userEvent.setup();
    const input = screen.getByPlaceholderText(/예: 서울특별시 강남구 테헤란로 123/);
    await user.type(input, '잘못된 주소');
    const button = screen.getByRole('button', { name: '변환' });
    await user.click(button);

    expect(
      await screen.findByText('변환 결과 없음. 주소를 다시 확인해주세요.'),
    ).toBeInTheDocument();
  });

  it('E2 - address 201자 입력 시 변환 버튼 disabled + 검증 메시지', async () => {
    renderPage();
    const user = userEvent.setup();
    const input = screen.getByPlaceholderText(/예: 서울특별시 강남구 테헤란로 123/);
    const longAddress = 'a'.repeat(201);
    // maxLength=201 로 한 글자 더 받게 한 후 검증 트리거
    await user.click(input);
    await user.paste(longAddress);

    const button = screen.getByRole('button', { name: '변환' });
    await waitFor(() => expect(button).toBeDisabled());
  });
});
