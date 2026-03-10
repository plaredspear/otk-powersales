import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Button, Space, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { menuRoute } from '@/config/menuConfig';
import queryClient from '@/lib/queryClient';
import { BreadcrumbProvider } from '@/contexts/BreadcrumbContext';
import AppBreadcrumb from '@/components/AppBreadcrumb';
import { useBreadcrumb } from '@/hooks/useBreadcrumb';

const { Text } = Typography;

const HEADER_HEIGHT = 56;
const BREADCRUMB_HEIGHT = 40;

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const breadcrumbItems = useBreadcrumb(location.pathname, null);
  const hasBreadcrumb = breadcrumbItems !== null && breadcrumbItems.length > 0;

  const handleLogout = () => {
    queryClient.clear();
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
        token={{
          header: {
            heightLayoutHeader: hasBreadcrumb
              ? HEADER_HEIGHT + BREADCRUMB_HEIGHT
              : HEADER_HEIGHT,
          },
        }}
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
        headerRender={(_props, defaultDom) => (
          <>
            {defaultDom}
            <AppBreadcrumb />
          </>
        )}
        style={{ minHeight: '100vh' }}
        contentStyle={{ margin: 0 }}
      >
        <Outlet />
      </ProLayout>
    </BreadcrumbProvider>
  );
}
