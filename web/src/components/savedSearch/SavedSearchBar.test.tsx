import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { entityPermissionKey } from '@/hooks/usePermission';
import SavedSearchBar from './SavedSearchBar';
import { useSavedSearches } from '@/hooks/savedSearch/useSavedSearches';
import type { SavedSearch } from '@/api/savedSearch';

vi.mock('@/hooks/savedSearch/useSavedSearches', () => ({
  useSavedSearches: vi.fn(),
}));

const mockedUseSavedSearches = vi.mocked(useSavedSearches);

const SHARED: SavedSearch = {
  id: 1,
  resourceKey: 'promotion',
  name: '모든 행사 조회',
  scope: 'SHARED',
  ownerId: 99,
  ownerName: '관리자',
  filters: { promotionType: '시식', startDate: '', endDate: '', keyword: '' },
  sortOrder: 0,
  editable: false,
};

const PRIVATE: SavedSearch = {
  id: 2,
  resourceKey: 'promotion',
  name: '관리자_검색용',
  scope: 'PRIVATE',
  ownerId: 1,
  ownerName: '테스트',
  filters: { promotionType: '', startDate: '2026-06-01', endDate: '', keyword: '라면' },
  sortOrder: 0,
  editable: true,
};

function setPermissions(permissions: string[]) {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: null,
      role: null,
      costCenterCode: null,
      permissions,
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderBar(onApply = vi.fn()) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <SavedSearchBar
        resourceKey="promotion"
        filters={{ promotionType: '', startDate: '', endDate: '', keyword: '' }}
        preview={[]}
        onApply={onApply}
      />
    </QueryClientProvider>,
  );
  return onApply;
}

describe('SavedSearchBar (Spec #852 P2-W)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false });
    mockedUseSavedSearches.mockReturnValue({
      data: [SHARED, PRIVATE],
      isLoading: false,
    } as ReturnType<typeof useSavedSearches>);
  });

  it('저장된 검색 선택 시 해당 filters 로 onApply 가 호출된다', () => {
    setPermissions([]);
    const onApply = renderBar();

    fireEvent.mouseDown(screen.getByRole('combobox'));
    fireEvent.click(screen.getByText('관리자_검색용'));

    expect(onApply).toHaveBeenCalledWith(PRIVATE.filters);
  });

  it('공용/개인 그룹이 드롭다운에 표시된다', () => {
    setPermissions([]);
    renderBar();

    fireEvent.mouseDown(screen.getByRole('combobox'));

    expect(screen.getByText('공용')).toBeInTheDocument();
    expect(screen.getByText('개인')).toBeInTheDocument();
  });

  it('saved_search EDIT 권한 미보유 시 공용 라디오가 비활성된다', () => {
    setPermissions([]);
    renderBar();

    fireEvent.click(screen.getByText('현재 조건 저장'));

    const sharedRadio = screen.getByRole('radio', { name: /공용/ });
    expect(sharedRadio).toBeDisabled();
  });

  it('saved_search EDIT 권한 보유 시 공용 라디오가 활성된다', () => {
    setPermissions([entityPermissionKey('saved_search', 'EDIT')]);
    renderBar();

    fireEvent.click(screen.getByText('현재 조건 저장'));

    const sharedRadio = screen.getByRole('radio', { name: /공용/ });
    expect(sharedRadio).toBeEnabled();
  });
});
