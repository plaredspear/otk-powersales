import { Card, Typography } from 'antd';
import {
  SoundOutlined,
  ReadOutlined,
  ShopOutlined,
  BarChartOutlined,
  WarningOutlined,
  CarOutlined,
  CalendarOutlined,
  FieldTimeOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuthStore } from '@/stores/authStore';

interface Shortcut {
  to: string;
  label: string;
  icon: ReactNode;
  color: string;
}

const SHORTCUTS: Shortcut[] = [
  { to: '/notices', label: '공지사항', icon: <SoundOutlined />, color: '#1677ff' },
  { to: '/education', label: '교육자료', icon: <ReadOutlined />, color: '#52c41a' },
  { to: '/accounts', label: '내 거래처', icon: <ShopOutlined />, color: '#722ed1' },
  { to: '/sales', label: '월매출', icon: <BarChartOutlined />, color: '#fa8c16' },
  { to: '/promotions', label: '행사매출', icon: <CalendarOutlined />, color: '#eb2f96' },
  { to: '/claims', label: '클레임', icon: <WarningOutlined />, color: '#f5222d' },
  { to: '/logistics-claims', label: '물류클레임', icon: <CarOutlined />, color: '#13c2c2' },
  { to: '/product-expiration', label: '유통기한', icon: <FieldTimeOutlined />, color: '#a0d911' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);

  return (
    <div>
      <Card style={{ marginBottom: 12 }} styles={{ body: { padding: 16 } }}>
        <Typography.Text type="secondary">안녕하세요</Typography.Text>
        <Typography.Title level={4} style={{ margin: '4px 0 0' }}>
          {user?.name ?? '현장 사원'}님
        </Typography.Title>
        {user?.orgName && <Typography.Text type="secondary">{user.orgName}</Typography.Text>}
      </Card>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(4, 1fr)',
          gap: 10,
        }}
      >
        {SHORTCUTS.map((s) => (
          <button
            key={s.to}
            onClick={() => navigate(s.to)}
            style={{
              border: 'none',
              background: '#fff',
              borderRadius: 12,
              padding: '16px 4px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 8,
              cursor: 'pointer',
            }}
          >
            <span style={{ fontSize: 26, color: s.color }}>{s.icon}</span>
            <span style={{ fontSize: 12, color: '#333' }}>{s.label}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
