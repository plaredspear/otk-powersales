import { useMemo } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Breadcrumb, Button, Space, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { menuRoute } from '@/config/menuConfig';

const { Text } = Typography;

interface BreadcrumbEntry {
  title: string;
  path?: string;
}

// Build path → breadcrumb trail mapping from menuRoute
const breadcrumbMap: Record<string, BreadcrumbEntry[]> = {};
for (const item of menuRoute.routes) {
  if (item.routes) {
    for (const child of item.routes) {
      if (child.path) {
        breadcrumbMap[child.path] = [
          { title: item.name, path: item.path },
          { title: child.name },
        ];
      }
    }
  } else if (item.path && item.path !== '/') {
    breadcrumbMap[item.path] = [{ title: item.name }];
  }
}

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const breadcrumbItems = useMemo(() => {
    const { pathname } = location;

    // Exact match from menu routes
    if (breadcrumbMap[pathname]) {
      return breadcrumbMap[pathname];
    }

    // Dynamic routes for notices
    if (pathname === '/notices/new') {
      return [{ title: '공지사항', path: '/notices' }, { title: '새 공지' }];
    }
    if (/^\/notices\/\d+\/edit$/.test(pathname)) {
      return [{ title: '공지사항', path: '/notices' }, { title: '수정' }];
    }
    if (/^\/notices\/\d+$/.test(pathname)) {
      return [{ title: '공지사항', path: '/notices' }, { title: '상세' }];
    }

    return null;
  }, [location.pathname]);

  return (
    <ProLayout
      title="판매여사원관리시스템"
      route={menuRoute}
      location={{ pathname: location.pathname }}
      fixedHeader
      fixSiderbar
      layout="mix"
      onMenuHeaderClick={() => navigate('/')}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && navigate(item.path)}>{dom}</a>
      )}
      actionsRender={() => [
        <Space key="user" align="center">
          <Text>
            {user?.name}
          </Text>
          <Button type="text" size="small" onClick={handleLogout}>
            로그아웃
          </Button>
        </Space>,
      ]}
      style={{ minHeight: '100vh' }}
      contentStyle={{ margin: 0 }}
    >
      {breadcrumbItems && (
        <div style={{ padding: '8px 0 0 24px', fontSize: 12 }}>
          <Breadcrumb
            items={breadcrumbItems.map((entry) => ({
              title: entry.path ? (
                <a onClick={() => navigate(entry.path!)}>{entry.title}</a>
              ) : (
                entry.title
              ),
            }))}
          />
        </div>
      )}
      <Outlet />
    </ProLayout>
  );
}
