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

const testClaimRegistMock = vi.fn();
vi.mock('@/api/claims', () => ({
  testClaimRegist: (...args: unknown[]) => testClaimRegistMock(...args),
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
    testClaimRegistMock.mockReset();
    mutationState.isPending = false;
  });

  it('H1 - 페이지 진입 시 Naver Geocode + SF 클레임 등록 + SAP 인터페이스별 개별 탭이 모두 노출', () => {
    renderPage();
    expect(screen.getByRole('tab', { name: 'Naver Geocode' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'SF 클레임 등록' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '여신 한도 조회' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '주문 등록' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '전문행사조 마스터' })).toBeInTheDocument();
  });

  it('H4 - SF 클레임 등록 탭 전환 시 클레임 입력 폼과 SF 전송 버튼이 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SF 클레임 등록' }));

    expect(
      await screen.findByPlaceholderText('account.external_key'),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText('empcode (SFID 아님)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'SF 전송' })).toBeInTheDocument();
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

  it('H3 - SAP 인터페이스 탭으로 전환 시 해당 탭에서 직접 미리보기/실송신 가능', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '여신 한도 조회' }));

    expect(await screen.findByRole('button', { name: '미리보기' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '실송신' })).toBeInTheDocument();
    expect(screen.getByPlaceholderText('예: 1032619')).toBeInTheDocument();
  });

  it('E1 - 주소 blank 시 "변환" 버튼이 disabled', () => {
    renderPage();
    expect(screen.getByRole('button', { name: '변환' })).toBeDisabled();
  });
});
