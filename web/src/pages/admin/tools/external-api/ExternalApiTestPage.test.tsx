import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import ExternalApiTestPage from './ExternalApiTestPage';

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
        <ExternalApiTestPage />
      </QueryClientProvider>
    </MemoryRouter>,
  );
}

describe('ExternalApiTestPage (외부 API 테스트 통합 페이지)', () => {
  beforeEach(() => {
    mutateAsyncMock.mockReset();
    mutationState.isPending = false;
  });

  it('H1 - 페이지 진입 시 Naver Geocode / SAP Outbound 탭이 모두 노출', () => {
    renderPage();
    expect(screen.getByRole('tab', { name: 'Naver Geocode' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'SAP Outbound' })).toBeInTheDocument();
  });

  it('H2 - 기본 탭(Naver)에서 주소 변환 시 raw JSON 응답을 출력', async () => {
    const rawJson = JSON.stringify({
      status: 'OK',
      addresses: [{ x: '127.0584', y: '37.5074' }],
    });
    mutateAsyncMock.mockResolvedValue({
      input: '서울특별시 강남구 테헤란로 123',
      rawResponse: rawJson,
    });

    renderPage();
    const user = userEvent.setup();
    const input = screen.getByPlaceholderText(/예: 서울특별시 강남구 테헤란로 123/);
    await user.type(input, '서울특별시 강남구 테헤란로 123');
    await user.click(screen.getByRole('button', { name: '변환' }));

    await waitFor(() => {
      expect(mutateAsyncMock).toHaveBeenCalledWith({
        address: '서울특별시 강남구 테헤란로 123',
      });
    });
    const pre = await screen.findByTestId('naver-geocode-raw-response');
    expect(pre).toHaveTextContent('"status": "OK"');
    expect(pre).toHaveTextContent('"x": "127.0584"');
  });

  it('H3 - SAP Outbound 탭으로 전환 시 전용 페이지 링크가 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SAP Outbound' }));

    const link = await screen.findByRole('link', { name: /SAP Outbound 페이지로 이동/ });
    expect(link).toHaveAttribute('href', '/admin/tools/sap-outbound');
  });

  it('E1 - 주소 blank 시 "변환" 버튼이 disabled', () => {
    renderPage();
    expect(screen.getByRole('button', { name: '변환' })).toBeDisabled();
  });
});
