import './AdminLayout.css';
import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Dropdown, Space, Typography, type MenuProps } from 'antd';
import { DownOutlined, LogoutOutlined, MenuFoldOutlined, MenuUnfoldOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/stores/authStore';
import { useForbiddenStore } from '@/stores/forbiddenStore';
import { menuRoute, type MenuItem } from '@/config/menuConfig';
import queryClient from '@/lib/queryClient';
import { BreadcrumbProvider } from '@/contexts/BreadcrumbContext';
import AppBreadcrumb from '@/components/AppBreadcrumb';
import ForbiddenResult from '@/components/ForbiddenResult';
import { usePermission } from '@/hooks/usePermission';

const { Text } = Typography;


const SIDER_COLLAPSED_KEY = 'admin.sider.collapsed';

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { forbidden, setForbidden } = useForbiddenStore();
  const { hasEntityPermission, hasSystemPermission } = usePermission();

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem(SIDER_COLLAPSED_KEY) === 'true';
  });

  const handleCollapse = (next: boolean) => {
    setCollapsed(next);
    localStorage.setItem(SIDER_COLLAPSED_KEY, String(next));
  };

  const filteredMenuRoute = useMemo(() => {
    const itemAllowed = (item: MenuItem): boolean => {
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
  }, [hasEntityPermission, hasSystemPermission]);

  useEffect(() => {
    setForbidden(false);
  }, [location.pathname, setForbidden]);
  const handleLogout = () => {
    queryClient.clear();
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <BreadcrumbProvider>
      <ProLayout
        title="판매여사원관리시스템"
        route={filteredMenuRoute}
        location={{ pathname: location.pathname }}
        fixSiderbar
        layout="side"
        collapsed={collapsed}
        onCollapse={handleCollapse}
        collapsedButtonRender={false}
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
        menuItemRender={(item, dom) => (
          <a onClick={() => item.path && navigate(item.path)}>{dom}</a>
        )}
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
              style={{ color: 'inherit', cursor: 'pointer' }}
            >
              <Space align="center" size={4}>
                {user?.orgName && (
                  <Text type="secondary" style={{ fontSize: 12 }}>
                    {user.orgName}
                  </Text>
                )}
                <Text>{user?.name}</Text>
                <DownOutlined style={{ fontSize: 10 }} />
              </Space>
            </a>
          </Dropdown>
        </div>
        <AppBreadcrumb />
        <div style={{ padding: 24 }}>
          {forbidden ? <ForbiddenResult /> : <Outlet />}
        </div>
      </ProLayout>
    </BreadcrumbProvider>
  );
}
