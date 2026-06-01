import { List, Avatar, Button, App as AntdApp } from 'antd';
import {
  SoundOutlined,
  ReadOutlined,
  ShopOutlined,
  BarChartOutlined,
  CalendarOutlined,
  WarningOutlined,
  CarOutlined,
  FieldTimeOutlined,
  SafetyOutlined,
  ScheduleOutlined,
  LockOutlined,
  SettingOutlined,
  LogoutOutlined,
  SearchOutlined,
  FileDoneOutlined,
  AuditOutlined,
  BulbOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuthStore } from '@/stores/authStore';

interface MenuEntry {
  to: string;
  label: string;
  icon: ReactNode;
}

const ENTRIES: MenuEntry[] = [
  { to: '/mypage/schedule', label: '내 일정', icon: <ScheduleOutlined /> },
  { to: '/safety-check', label: '안전점검', icon: <SafetyOutlined /> },
  { to: '/orders', label: '주문', icon: <FileDoneOutlined /> },
  { to: '/products/search', label: '제품 검색', icon: <SearchOutlined /> },
  { to: '/inspections', label: '현장 점검', icon: <AuditOutlined /> },
  { to: '/notices', label: '공지사항', icon: <SoundOutlined /> },
  { to: '/education', label: '교육자료', icon: <ReadOutlined /> },
  { to: '/accounts', label: '내 거래처', icon: <ShopOutlined /> },
  { to: '/sales', label: '매출 현황', icon: <BarChartOutlined /> },
  { to: '/promotions', label: '행사매출', icon: <CalendarOutlined /> },
  { to: '/claims', label: '클레임', icon: <WarningOutlined /> },
  { to: '/logistics-claims', label: '물류클레임', icon: <CarOutlined /> },
  { to: '/suggestions/new', label: '제안하기', icon: <BulbOutlined /> },
  { to: '/product-expiration', label: '유통기한 관리', icon: <FieldTimeOutlined /> },
  { to: '/password/verify', label: '비밀번호 변경', icon: <LockOutlined /> },
  { to: '/settings', label: '앱 정보 / 설정', icon: <SettingOutlined /> },
];

export default function MenuPage() {
  const navigate = useNavigate();
  const { modal } = AntdApp.useApp();
  const logout = useAuthStore((s) => s.logout);

  const onLogout = () => {
    modal.confirm({
      title: '로그아웃 하시겠습니까?',
      okText: '로그아웃',
      cancelText: '취소',
      onOk: () => {
        logout();
        navigate('/login', { replace: true });
      },
    });
  };

  return (
    <div>
      <List
        bordered
        style={{ background: '#fff', borderRadius: 12 }}
        dataSource={ENTRIES}
        renderItem={(item) => (
          <List.Item style={{ cursor: 'pointer' }} onClick={() => navigate(item.to)}>
            <List.Item.Meta
              avatar={<Avatar style={{ background: '#f0f5ff', color: '#1677ff' }} icon={item.icon} />}
              title={item.label}
            />
          </List.Item>
        )}
      />
      <Button danger block icon={<LogoutOutlined />} style={{ marginTop: 16 }} onClick={onLogout}>
        로그아웃
      </Button>
    </div>
  );
}
