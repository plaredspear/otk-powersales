import './AdminLayout.css';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Dropdown, Input, Typography, type InputRef, type MenuProps } from 'antd';
import { DownOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined, SearchOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/stores/authStore';
import {
  collectMenuOpenKeys,
  filterMenuByKeyword,
  normalizeMenuKeyword,
  menuRoute,
  withMenuCategoryKeys,
  type MenuItem,
} from '@/config/menuConfig';
import queryClient from '@/lib/queryClient';
import { BreadcrumbProvider } from '@/contexts/BreadcrumbContext';
import AppBreadcrumb from '@/components/AppBreadcrumb';
import ImpersonationBanner from '@/components/ImpersonationBanner';
import { usePermission } from '@/hooks/usePermission';

const { Text } = Typography;


const SIDER_COLLAPSED_KEY = 'admin.sider.collapsed';

/** macOS 계열이면 ⌘, 그 외는 Ctrl 을 단축키 힌트로 표기한다. */
const IS_MAC = typeof navigator !== 'undefined' && /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent);
const MENU_SEARCH_HOTKEY_LABEL = IS_MAC ? '⌘K' : 'Ctrl K';

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const profileName = user?.profileName ?? null;
  const costCenterCode = user?.costCenterCode ?? null;
  const { hasEntityPermission, hasSystemPermission } = usePermission();

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem(SIDER_COLLAPSED_KEY) === 'true';
  });
  const [menuKeyword, setMenuKeyword] = useState('');
  const isSearching = normalizeMenuKeyword(menuKeyword).length > 0;
  const menuSearchRef = useRef<InputRef>(null);

  const handleCollapse = (next: boolean) => {
    setCollapsed(next);
    localStorage.setItem(SIDER_COLLAPSED_KEY, String(next));
  };

  // ⌘K / Ctrl+K 로 메뉴 검색에 포커스. 사이더가 접혀 있으면 먼저 펼친 뒤 포커스한다
  // (접힘 상태에서는 menuExtraRender 가 검색 UI 를 렌더하지 않기 때문).
  useEffect(() => {
    const handleHotkey = (e: KeyboardEvent) => {
      if (e.key.toLowerCase() !== 'k' || !(e.metaKey || e.ctrlKey) || e.altKey) return;
      e.preventDefault();
      setCollapsed((prev) => {
        if (!prev) return prev;
        localStorage.setItem(SIDER_COLLAPSED_KEY, 'false');
        return false;
      });
      // 펼침 렌더 이후에 포커스가 걸리도록 다음 프레임으로 미룬다.
      requestAnimationFrame(() => menuSearchRef.current?.focus({ cursor: 'all' }));
    };
    window.addEventListener('keydown', handleHotkey);
    return () => window.removeEventListener('keydown', handleHotkey);
  }, []);

  const filteredMenuRoute = useMemo(() => {
    const itemAllowed = (item: MenuItem): boolean => {
      // allowedProfileNames 는 추가 가드. 지정되었는데 profileName 미일치면 즉시 차단.
      if (item.allowedProfileNames && (!profileName || !item.allowedProfileNames.includes(profileName))) {
        return false;
      }
      // deniedForCostCenterCodes 는 deny-list. 사용자 costCenterCode 가 포함되면 권한 무관 즉시 차단.
      if (item.deniedForCostCenterCodes && costCenterCode && item.deniedForCostCenterCodes.includes(costCenterCode)) {
        return false;
      }
      const requiresEntity = !!(item.entity && item.operation);
      const requiresSystem = !!item.systemPermission;
      if (!requiresEntity && !requiresSystem) return true;
      if (requiresEntity && hasEntityPermission(item.entity!, item.operation!)) return true;
      if (requiresSystem && hasSystemPermission(item.systemPermission!)) return true;
      return false;
    };
    const filterItems = (items: MenuItem[]): MenuItem[] =>
      items
        .filter(itemAllowed)
        .map((item) => (item.children ? { ...item, children: filterItems(item.children) } : item))
        .filter((item) => !item.children || item.children.length > 0);
    return { ...menuRoute, children: filterItems(menuRoute.children) };
  }, [hasEntityPermission, hasSystemPermission, profileName, costCenterCode]);

  // 권한 필터된 트리를 검색어로 한 번 더 좁힌다. 검색어 없으면 그대로 통과.
  // withMenuCategoryKeys 로 모든 카테고리에 안정 key + placeholder path 를 주입한다.
  // (path 없는 카테고리가 ProLayout 내부에서 path `'/'` 로 collapse 되어 대시보드에서
  //  엉뚱한 카테고리가 열리는 문제 방지 — menuConfig.withMenuCategoryKeys 주석 참조.)
  const searchedMenuRoute = useMemo(
    () => ({
      ...filteredMenuRoute,
      children: withMenuCategoryKeys(filterMenuByKeyword(filteredMenuRoute.children, menuKeyword)),
    }),
    [filteredMenuRoute, menuKeyword],
  );

  // 검색 중에는 매칭 카테고리를 전부 펼쳐 결과(item)가 접힌 채 숨지 않도록 openKeys 를 제어한다.
  // 비검색 시 undefined 로 두어 ProLayout 기본 (현재 경로 자동 펼침) 동작을 보존.
  const menuOpenKeys = useMemo(
    () => (isSearching ? collectMenuOpenKeys(searchedMenuRoute.children) : undefined),
    [isSearching, searchedMenuRoute],
  );

  const handleLogout = () => {
    queryClient.clear();
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <BreadcrumbProvider>
      <ProLayout
        title="판매여사원관리시스템"
        logo="/favicon.svg"
        route={searchedMenuRoute}
        location={{ pathname: location.pathname }}
        fixSiderbar
        layout="side"
        collapsed={collapsed}
        onCollapse={handleCollapse}
        collapsedButtonRender={false}
        menuProps={
          // 검색 중에만 openKeys 를 제어 (매칭 카테고리 전부 펼침). searchedMenuRoute 의 카테고리에
          // 부여한 안정 key 와 동일 규칙으로 산출하므로 ProLayout submenu 와 정확히 매칭된다.
          // 비검색 시 undefined 로 두어 제어 모드를 벗어나 기본 동작 복원.
          menuOpenKeys ? { openKeys: menuOpenKeys } : undefined
        }
        // menuExtraRender 콜백 인자의 collapsed 는 이 ProLayout 버전에서 undefined 로 넘어와
        // 접힘 판정에 쓸 수 없다 (조건이 항상 falsy 가 되어 접어도 검색창이 남았다).
        // 레이아웃이 소유한 collapsed state 를 직접 참조한다.
        menuExtraRender={() =>
          collapsed ? null : (
            <div className="admin-sider-search">
              <Input
                allowClear
                ref={menuSearchRef}
                prefix={<SearchOutlined />}
                // 입력 중에는 allowClear 의 clear 아이콘이 suffix 자리를 쓰므로 키캡은 빈 상태에서만 노출.
                suffix={
                  menuKeyword ? undefined : (
                    <kbd className="admin-sider-search-kbd">{MENU_SEARCH_HOTKEY_LABEL}</kbd>
                  )
                }
                placeholder="메뉴 검색"
                aria-label={`메뉴 검색 (${MENU_SEARCH_HOTKEY_LABEL})`}
                value={menuKeyword}
                onChange={(e) => setMenuKeyword(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Escape') setMenuKeyword('');
                }}
              />
            </div>
          )
        }
        menuFooterRender={() => (
          <div
            className={`admin-sider-collapse-footer${collapsed ? ' admin-sider-collapse-footer--collapsed' : ''}`}
            onClick={() => handleCollapse(!collapsed)}
            role="button"
            tabIndex={0}
            aria-label={collapsed ? '사이드 메뉴 펼치기' : '사이드 메뉴 접기'}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleCollapse(!collapsed);
              }
            }}
          >
            <span className="admin-sider-collapse-footer-icon">
              {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            </span>
            {!collapsed && <span className="admin-sider-collapse-footer-label">메뉴 접기</span>}
          </div>
        )}
        token={{
          header: {
            heightLayoutHeader: 0,
          },
        }}
        onMenuHeaderClick={() => navigate('/')}
        menuItemRender={(item, dom) =>
          item.path ? (
            // Link 는 href 부여 + Cmd/Ctrl/middle-click 분기를 내장하므로
            // 일반 클릭은 SPA navigate, modifier 클릭은 새 탭/창으로 동작한다.
            <Link to={item.path}>{dom}</Link>
          ) : (
            <span>{dom}</span>
          )
        }
        headerRender={false}
        style={{ minHeight: '100vh' }}
        contentStyle={{ margin: 0, padding: 0 }}
      >
        <div className="admin-header">
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '로그아웃',
                  onClick: handleLogout,
                },
              ] satisfies MenuProps['items'],
            }}
            trigger={['click']}
          >
            <a
              onClick={(e) => e.preventDefault()}
              className="admin-header-user"
            >
              <div className="admin-header-user-info">
                <Text className="admin-header-user-name">{user?.name}</Text>
                {user?.orgName && (
                  <Text type="secondary" className="admin-header-user-org">
                    {user.orgName}
                  </Text>
                )}
              </div>
              <DownOutlined className="admin-header-user-caret" />
            </a>
          </Dropdown>
        </div>
        <ImpersonationBanner />
        <AppBreadcrumb />
        <div style={{ padding: 24 }}>
          <Outlet />
        </div>
      </ProLayout>
    </BreadcrumbProvider>
  );
}
