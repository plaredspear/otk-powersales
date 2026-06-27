import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AgreementWordsPage from './AgreementWordsPage';
import { fetchActiveAgreementWord } from '@/api/agreementWord';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey } from '@/hooks/usePermission';

vi.mock('@/api/agreementWord', async () => {
  const actual = await vi.importActual<typeof import('@/api/agreementWord')>('@/api/agreementWord');
  return {
    ...actual,
    fetchActiveAgreementWord: vi.fn(),
    createAgreementWord: vi.fn(),
  };
});

const mockedFetchActive = vi.mocked(fetchActiveAgreementWord);

function setPermissions(permissions: string[]) {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      isSalesSupport: false,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AgreementWordsPage />
    </QueryClientProvider>,
  );
}

describe('AgreementWordsPage (Spec #658 P2-W)', () => {
  beforeEach(() => {
    mockedFetchActive.mockReset();
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
  });

  it('W1 활성 약관 응답 → 카드에 name / activeDate / afterActiveDate 표시', async () => {
    mockedFetchActive.mockResolvedValue({
      agreementWordId: 10,
      name: 'AGR-2025-002',
      contents: '활성 약관 본문 내용',
      activeDate: '2025-11-01',
      afterActiveDate: '2026-11-01',
    });
    setPermissions([
      entityPermissionKey('agreement_word', 'READ'),
      entityPermissionKey('agreement_word', 'EDIT'),
    ]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('AGR-2025-002')).toBeInTheDocument();
      expect(screen.getByText('2025-11-01')).toBeInTheDocument();
      expect(screen.getByText('2026-11-01')).toBeInTheDocument();
    });
  });

  it('W2 활성 약관 부재 (null) → 안내 문구 표시', async () => {
    mockedFetchActive.mockResolvedValue(null);
    setPermissions([
      entityPermissionKey('agreement_word', 'READ'),
      entityPermissionKey('agreement_word', 'EDIT'),
    ]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/활성 약관 없음/)).toBeInTheDocument();
    });
  });

  it('W3 신규 등록 버튼 클릭 → Modal 열림', async () => {
    mockedFetchActive.mockResolvedValue(null);
    setPermissions([
      entityPermissionKey('agreement_word', 'READ'),
      entityPermissionKey('agreement_word', 'EDIT'),
    ]);

    renderPage();

    await userEvent.click(screen.getByRole('button', { name: /신규 등록/ }));

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('신규 약관 등록')).toBeInTheDocument();
    });
  });

  it('W10 AGREEMENT_WRITE 미보유 → 등록 버튼 disabled', async () => {
    mockedFetchActive.mockResolvedValue(null);
    setPermissions([entityPermissionKey('agreement_word', 'READ')]); // EDIT 미보유

    renderPage();

    expect(screen.getByRole('button', { name: /신규 등록/ })).toBeDisabled();
  });
});
