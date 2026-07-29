import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AdminLayout from './AdminLayout';
import { useAuthStore } from '@/stores/authStore';

const SIDER_COLLAPSED_KEY = 'admin.sider.collapsed';

function setUser() {
  useAuthStore.setState({
    user: {
      id: 1,
      employeeCode: 'TEST-001',
      username: 'test@otoki.local',
      name: '테스트',
      orgName: '테스트지점',
      role: null,
      profileName: '1.관리자',
      isSalesSupport: false,
      costCenterCode: null,
      permissions: [],
    },
    accessToken: 'token',
    isAuthenticated: true,
  });
}

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <AdminLayout />
    </MemoryRouter>,
  );
}

function getSearchInput() {
  return screen.getByPlaceholderText('메뉴 검색');
}

describe('AdminLayout 메뉴 검색', () => {
  beforeEach(() => {
    localStorage.clear();
    setUser();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('검색어가 비어 있으면 단축키 키캡을 노출한다', () => {
    renderLayout();
    // 플랫폼에 따라 ⌘K / Ctrl K 로 갈리므로 정규식으로 둘 다 수용한다.
    expect(screen.getByText(/⌘K|Ctrl K/)).toBeInTheDocument();
  });

  it('검색어를 입력하면 키캡을 숨긴다 (clear 아이콘에 suffix 자리를 양보)', async () => {
    const user = userEvent.setup();
    renderLayout();

    await user.type(getSearchInput(), '사용자');

    await waitFor(() => {
      expect(screen.queryByText(/⌘K|Ctrl K/)).not.toBeInTheDocument();
    });
  });

  it('ESC 로 검색어를 초기화한다', async () => {
    const user = userEvent.setup();
    renderLayout();

    const input = getSearchInput();
    await user.type(input, '사용자');
    expect(input).toHaveValue('사용자');

    await user.type(input, '{Escape}');
    expect(input).toHaveValue('');
  });

  it('Ctrl+K 로 검색 입력에 포커스한다', async () => {
    const user = userEvent.setup();
    renderLayout();

    expect(getSearchInput()).not.toHaveFocus();
    await user.keyboard('{Control>}k{/Control}');

    await waitFor(() => {
      expect(getSearchInput()).toHaveFocus();
    });
  });

  it('사이더가 접힌 상태에서 단축키를 누르면 펼친 뒤 포커스한다', async () => {
    const user = userEvent.setup();
    renderLayout();

    // 초기 collapsed 는 antd Sider 의 responsive onCollapse 가 마운트 직후 덮어쓰므로
    // (jsdom matchMedia stub = matches:false) localStorage 선주입이 아니라 실제 조작으로 접는다.
    await user.click(screen.getByRole('button', { name: '사이드 메뉴 접기' }));

    // 접힘 상태에서는 menuExtraRender 가 검색 UI 를 렌더하지 않는다.
    await waitFor(() => {
      expect(screen.queryByPlaceholderText('메뉴 검색')).not.toBeInTheDocument();
    });

    await user.keyboard('{Control>}k{/Control}');

    await waitFor(() => {
      expect(getSearchInput()).toHaveFocus();
    });
    expect(localStorage.getItem(SIDER_COLLAPSED_KEY)).toBe('false');
  });
});
