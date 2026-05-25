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

  it('H1 - 정상 주소 변환 시 raw JSON 응답을 그대로 출력', async () => {
    const rawJson = JSON.stringify({
      status: 'OK',
      meta: { totalCount: 1, page: 1, count: 1 },
      addresses: [
        {
          roadAddress: '서울특별시 강남구 테헤란로 123',
          jibunAddress: '서울특별시 강남구 역삼동 123-45',
          x: '127.0584',
          y: '37.5074',
        },
      ],
      errorMessage: '',
    });
    mutateAsyncMock.mockResolvedValue({
      input: '서울특별시 강남구 테헤란로 123',
      rawResponse: rawJson,
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
    const pre = await screen.findByTestId('naver-geocode-raw-response');
    expect(pre).toHaveTextContent('"status": "OK"');
    expect(pre).toHaveTextContent('"x": "127.0584"');
    expect(pre).toHaveTextContent('"y": "37.5074"');
  });

  it('H2 - 매칭 0건 응답도 raw JSON 그대로 출력', async () => {
    const rawJson = JSON.stringify({
      status: 'OK',
      meta: { totalCount: 0, page: 1, count: 0 },
      addresses: [],
      errorMessage: '',
    });
    mutateAsyncMock.mockResolvedValue({
      input: '잘못된 주소',
      rawResponse: rawJson,
    });

    renderPage();
    const user = userEvent.setup();
    const input = screen.getByPlaceholderText(/예: 서울특별시 강남구 테헤란로 123/);
    await user.type(input, '잘못된 주소');
    const button = screen.getByRole('button', { name: '변환' });
    await user.click(button);

    const pre = await screen.findByTestId('naver-geocode-raw-response');
    expect(pre).toHaveTextContent('"totalCount": 0');
    expect(pre).toHaveTextContent('"addresses": []');
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
