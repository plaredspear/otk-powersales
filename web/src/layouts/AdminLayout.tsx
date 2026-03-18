import './AdminLayout.css';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import { Button, Space, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';
import { menuRoute } from '@/config/menuConfig';
import queryClient from '@/lib/queryClient';
import { BreadcrumbProvider } from '@/contexts/BreadcrumbContext';
import AppBreadcrumb from '@/components/AppBreadcrumb';

const { Text } = Typography;


export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
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
        fixSiderbar
        layout="side"
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
          <Outlet />
        </div>
      </ProLayout>
    </BreadcrumbProvider>
  );
}
