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

const pushTestMutateAsyncMock = vi.fn();
vi.mock('@/hooks/admin/usePushTest', () => ({
  usePushTest: () => ({
    mutateAsync: pushTestMutateAsyncMock,
    isPending: false,
  }),
}));

const testClaimRegistMock = vi.fn();
const testLogisticsClaimRegistMock = vi.fn();
vi.mock('@/api/claims', () => ({
  testClaimRegist: (...args: unknown[]) => testClaimRegistMock(...args),
  testLogisticsClaimRegist: (...args: unknown[]) =>
    testLogisticsClaimRegistMock(...args),
}));

const INTEGRATION_INFO_ITEMS = [
  {
    key: 'naver-geocode',
    externalSystem: 'Naver Cloud Platform (Maps Geocode)',
    endpoint: 'https://maps.apigw.ntruss.com/map-geocode/v2/geocode',
    httpMethod: 'GET',
    authType: 'NCP API Key',
    note: 'query 파라미터로 주소 전송',
  },
  {
    key: 'claim-regist',
    externalSystem: 'Salesforce (Apex REST)',
    endpoint: 'https://ottogi.my.salesforce.com/services/apexrest/mobile/ClaimRegist',
    httpMethod: 'POST',
    authType: 'OAuth2 Password Grant (Bearer)',
    note: 'Content-Type: application/json',
  },
];

// 각 탭 하단 "이 API 의 최근 호출 이력" 인라인 섹션(ExternalApiLogsTab) 은 테이블 + 필터 +
// 호출 이력 조회 훅까지 딸린 무거운 컴포넌트라, 탭 전환마다 렌더 비용이 누적되어 10초
// testTimeout 을 초과하는 flaky 실패의 원인이 된다. 본 스위트의 검증 대상(탭 구성/각 탭의
// 입력 폼·전송 동작) 이 아니므로 stub 으로 대체한다.
vi.mock('./ExternalApiLogsTab', () => ({
  default: () => <div data-testid="external-api-logs-tab" />,
}));

const fetchIntegrationInfoMock = vi.fn();
vi.mock('@/api/admin/externalApiIntegrationInfo', () => ({
  fetchExternalApiIntegrationInfo: () => fetchIntegrationInfoMock(),
  useExternalApiIntegrationInfo: (apiKey: string) => ({
    info: INTEGRATION_INFO_ITEMS.find((i) => i.key === apiKey),
    isLoading: false,
    isError: false,
  }),
}));

/**
 * 버튼 조회 — 라벨 텍스트로 찾아 감싸는 <button> 을 되짚는다 (antd Button 은 <button><span>라벨).
 *
 * `getByRole('button', { name })` 은 문서 전체를 돌며 접근성 이름 + 가시성(getComputedStyle)
 * 을 계산하므로, 무거운 탭 패널이 마운트된 뒤에는 쿼리 1회에 1.3초 이상 걸려 10초 testTimeout
 * 을 넘기는 flaky 의 주 원인이 된다. 텍스트 조회는 동일 시점에 2ms 수준이다.
 */
function getButton(name: string): HTMLElement {
  const button = screen.getByText(name).closest('button');
  if (!button) throw new Error(`"${name}" 라벨을 가진 버튼이 없습니다`);
  return button;
}

async function findButton(name: string): Promise<HTMLElement> {
  const label = await screen.findByText(name);
  const button = label.closest('button');
  if (!button) throw new Error(`"${name}" 라벨을 가진 버튼이 없습니다`);
  return button;
}

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
    pushTestMutateAsyncMock.mockReset();
    testClaimRegistMock.mockReset();
    testLogisticsClaimRegistMock.mockReset();
    fetchIntegrationInfoMock.mockReset();
    fetchIntegrationInfoMock.mockResolvedValue({ items: INTEGRATION_INFO_ITEMS });
    mutationState.isPending = false;
  });

  it('H1 - 페이지 진입 시 비-SAP 외부 API 탭만 노출 (SAP 탭 없음)', () => {
    renderPage();
    expect(screen.getByRole('tab', { name: 'Naver Geocode' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'SF 클레임 등록' })).toBeInTheDocument();
    expect(
      screen.getByRole('tab', { name: 'SF 물류 클레임 등록' }),
    ).toBeInTheDocument();
    // SAP 테스트는 SAP 연동 페이지로 일원화 — 본 페이지에는 SAP 관련 탭이 없음
    expect(
      screen.queryByRole('tab', { name: 'SAP 연동' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('tab', { name: '여신 한도 조회' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('tab', { name: '전문행사조 마스터' }),
    ).not.toBeInTheDocument();
  });

  it('H4 - SF 클레임 등록 탭 전환 시 클레임 입력 폼과 SF 전송 버튼이 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SF 클레임 등록' }));

    expect(
      await screen.findByPlaceholderText('account.external_key'),
    ).toBeInTheDocument();
    expect(screen.getByPlaceholderText('empcode (SFID 아님)')).toBeInTheDocument();
    expect(getButton('SF 전송')).toBeInTheDocument();
  });

  it('H4b - SF 물류 클레임 등록 탭 전환 시 입력 폼과 payload 미리보기 버튼이 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SF 물류 클레임 등록' }));

    expect(
      await screen.findByPlaceholderText('product.product_code'),
    ).toBeInTheDocument();
    expect(getButton('payload 미리보기')).toBeInTheDocument();
    // SF 미전송 안내가 노출됨
    expect(
      screen.getByText(/payload 미리보기 전용/),
    ).toBeInTheDocument();
  });

  it('H4c - SF 물류 클레임 등록 필수값 미입력 제출 시 API 호출 안 됨 (검증 차단)', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SF 물류 클레임 등록' }));

    // 필수값 미입력 상태로 바로 제출 → Form 검증에 막혀 API 미호출
    await user.click(await findButton('payload 미리보기'));

    // antd Form 의 비동기 validation 메시지는 전체 스위트 병렬 실행 시 렌더 지연이
    // findByText 기본 1초를 넘길 수 있어 timeout 을 5초로 명시해 대기한다.
    expect(
      await screen.findByText('거래처 SAP 코드는 필수입니다', undefined, {
        timeout: 5000,
      }),
    ).toBeInTheDocument();
    expect(testLogisticsClaimRegistMock).not.toHaveBeenCalled();
  });

  it('H5 - 각 탭에 외부 시스템 연동 정보(endpoint/method/인증)가 노출', async () => {
    renderPage();
    // 기본 탭(naver) — endpoint/method
    expect(
      await screen.findByText('https://maps.apigw.ntruss.com/map-geocode/v2/geocode'),
    ).toBeInTheDocument();
    expect(screen.getAllByText('GET').length).toBeGreaterThan(0);

    // SF 클레임 등록 탭 — SF Apex endpoint
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'SF 클레임 등록' }));
    expect(
      await screen.findByText(
        'https://ottogi.my.salesforce.com/services/apexrest/mobile/ClaimRegist',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText('OAuth2 Password Grant (Bearer)'),
    ).toBeInTheDocument();
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
    await user.click(getButton('변환'));

    await waitFor(() => {
      expect(mutateAsyncMock).toHaveBeenCalledWith({
        address: '서울특별시 강남구 테헤란로 123',
      });
    });
    const pre = await screen.findByTestId('naver-geocode-raw-response');
    expect(pre).toHaveTextContent('"status": "OK"');
    expect(pre).toHaveTextContent('"x": "127.0584"');
  });

  it('E1 - 주소 blank 시 "변환" 버튼이 disabled', () => {
    renderPage();
    expect(getButton('변환')).toBeDisabled();
  });

  it('P1 - push 발송 테스트 탭 전환 시 사번/제목/본문 폼과 발송 버튼이 노출', async () => {
    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'push 발송 테스트' }));

    expect(await screen.findByPlaceholderText('예: 00012345')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('알림 제목')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('알림 본문')).toBeInTheDocument();
    expect(getButton('push 발송')).toBeInTheDocument();
    // 실제 발송 경고 안내 노출
    expect(
      screen.getByText(/실제 단말로 FCM push 를 발송합니다/),
    ).toBeInTheDocument();
  });

  it('P2 - push 발송 시 입력값으로 API 호출 + 발송 결과(성공 건수/요약)를 출력', async () => {
    pushTestMutateAsyncMock.mockResolvedValue({
      employeeCode: '00012345',
      employeeName: '홍길동',
      tokenRegistered: true,
      maskedToken: 'abcdefgh…(30자)',
      successCount: 1,
      failureCount: 0,
      message: '발송 성공 (success=1, failure=0)',
    });

    renderPage();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'push 발송 테스트' }));

    await user.type(await screen.findByPlaceholderText('예: 00012345'), '00012345');
    await user.click(getButton('push 발송'));

    await waitFor(() => {
      expect(pushTestMutateAsyncMock).toHaveBeenCalledWith({
        employeeCode: '00012345',
        title: '테스트 알림',
        body: '푸시 발송 테스트입니다.',
      });
    });
    expect(await screen.findByText('발송 성공 (success=1, failure=0)')).toBeInTheDocument();
    expect(screen.getByText('abcdefgh…(30자)')).toBeInTheDocument();
  });
});
