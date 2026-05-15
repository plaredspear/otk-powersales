import './AdminLayout.css';
import { useEffect, useMemo, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Button, Space, Typography } from 'antd';
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
  const { hasPermission } = usePermission();

  const [collapsed, setCollapsed] = useState<boolean>(() => {
    return localStorage.getItem(SIDER_COLLAPSED_KEY) === 'true';
  });

  const handleCollapse = (next: boolean) => {
    setCollapsed(next);
    localStorage.setItem(SIDER_COLLAPSED_KEY, String(next));
  };

  const filteredMenuRoute = useMemo(() => {
    const filterItems = (items: MenuItem[]): MenuItem[] =>
      items
        .filter((item) => !item.requiredPermission || hasPermission(item.requiredPermission))
        .map((item) => (item.children ? { ...item, children: filterItems(item.children) } : item))
        .filter((item) => !item.children || item.children.length > 0);
    return { ...menuRoute, children: filterItems(menuRoute.children) };
  }, [hasPermission]);

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
          <Space align="center">
            <Text>{user?.name}</Text>
            <Button type="text" size="small" onClick={handleLogout}>
              로그아웃
            </Button>
          </Space>
        </div>
        <AppBreadcrumb />
        <div style={{ padding: 24 }}>
          {forbidden ? <ForbiddenResult /> : <Outlet />}
        </div>
      </ProLayout>
    </BreadcrumbProvider>
  );
}
