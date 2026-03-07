import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import ProLayout from '@ant-design/pro-layout';
import {
  DashboardOutlined,
  BarChartOutlined,
  TeamOutlined,
  ClockCircleOutlined,
  CalendarOutlined,
  ExclamationCircleOutlined,
  BulbOutlined,
  RestOutlined,
  SafetyCertificateOutlined,
  ShoppingOutlined,
  UserOutlined,
  SearchOutlined,
  FileTextOutlined,
  NotificationOutlined,
  ScheduleOutlined,
} from '@ant-design/icons';
import { Button, Space, Typography } from 'antd';
import { useAuthStore } from '@/stores/authStore';

const { Text } = Typography;

const menuRoute = {
  path: '/',
  routes: [
    {
      path: '/',
      name: '대시보드',
      icon: <DashboardOutlined />,
    },
    {
      name: '매출조회',
      icon: <BarChartOutlined />,
      routes: [
        { path: '/sales/monthly', name: '물류배부' },
        { path: '/sales/electronic', name: '전산실적' },
        { path: '/sales/pos', name: 'POS매출' },
      ],
    },
    {
      name: '여사원',
      icon: <TeamOutlined />,
      routes: [
        { path: '/schedule', name: '일정관리' },
        { path: '/deployment', name: '배치' },
      ],
    },
    {
      name: '근무',
      icon: <ClockCircleOutlined />,
      routes: [{ path: '/attendance', name: '등록현황' }],
    },
    { path: '/event-team', name: '전문행사조', icon: <CalendarOutlined /> },
    { path: '/claim', name: '클레임 현황', icon: <ExclamationCircleOutlined /> },
    { path: '/suggestion', name: '제안사항', icon: <BulbOutlined /> },
    { path: '/leave', name: '휴무관리', icon: <RestOutlined /> },
    { path: '/safety-check', name: '안전점검', icon: <SafetyCertificateOutlined /> },
    { path: '/product', name: '제품', icon: <ShoppingOutlined /> },
    { path: '/employee', name: '사원', icon: <UserOutlined /> },
    { path: '/field-inspection', name: '현장점검', icon: <SearchOutlined /> },
    { path: '/report', name: '보고서', icon: <FileTextOutlined /> },
    { path: '/notices', name: '공지사항', icon: <NotificationOutlined /> },
  ],
};

// ProLayout expects a specific icon type; use ScheduleOutlined for menu header
void ScheduleOutlined;

export default function AdminLayout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

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
            {user?.appAuthority ? ` (${user.appAuthority})` : ''}
          </Text>
          <Button type="text" size="small" onClick={handleLogout}>
            로그아웃
          </Button>
        </Space>,
      ]}
      style={{ minHeight: '100vh' }}
      contentStyle={{ margin: 0 }}
    >
      <Outlet />
    </ProLayout>
  );
}
