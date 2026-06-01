import {
  HomeOutlined,
  SoundOutlined,
  BarChartOutlined,
  WarningOutlined,
  AppstoreOutlined,
} from '@ant-design/icons';
import { useLocation, useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';

interface TabItem {
  key: string;
  label: string;
  icon: ReactNode;
  /** 활성 판정 prefix */
  match: string;
}

const TABS: TabItem[] = [
  { key: '/', label: '홈', icon: <HomeOutlined />, match: '/' },
  { key: '/notices', label: '공지', icon: <SoundOutlined />, match: '/notices' },
  { key: '/sales', label: '매출', icon: <BarChartOutlined />, match: '/sales' },
  { key: '/claims', label: '클레임', icon: <WarningOutlined />, match: '/claims' },
  { key: '/menu', label: '전체', icon: <AppstoreOutlined />, match: '/menu' },
];

export default function BottomTabBar() {
  const navigate = useNavigate();
  const { pathname } = useLocation();

  const isActive = (match: string) =>
    match === '/' ? pathname === '/' : pathname.startsWith(match);

  return (
    <nav
      style={{
        position: 'fixed',
        bottom: 0,
        left: '50%',
        transform: 'translateX(-50%)',
        width: '100%',
        maxWidth: 'var(--mw-max-w)',
        height: 'calc(var(--mw-tabbar-h) + var(--mw-safe-bottom))',
        paddingBottom: 'var(--mw-safe-bottom)',
        background: '#fff',
        borderTop: '1px solid #f0f0f0',
        display: 'flex',
        zIndex: 20,
      }}
    >
      {TABS.map((tab) => {
        const active = isActive(tab.match);
        return (
          <button
            key={tab.key}
            onClick={() => navigate(tab.key)}
            style={{
              flex: 1,
              border: 'none',
              background: 'transparent',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 2,
              cursor: 'pointer',
              color: active ? '#1677ff' : '#8c8c8c',
              fontSize: 11,
            }}
          >
            <span style={{ fontSize: 20 }}>{tab.icon}</span>
            {tab.label}
          </button>
        );
      })}
    </nav>
  );
}
