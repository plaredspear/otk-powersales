import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Button, Space, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { menuRoute } from '@/config/menuConfig';
import { BreadcrumbProvider } from '@/contexts/BreadcrumbContext';
import AppBreadcrumb from '@/components/AppBreadcrumb';

const { Text } = Typography;

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <BreadcrumbProvider>
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
        <AppBreadcrumb />
        <Outlet />
      </ProLayout>
    </BreadcrumbProvider>
  );
}
