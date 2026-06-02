import { LeftOutlined, HomeFilled, MenuOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';

/**
 * 하단 퀵 네비게이션 — Heroku 레거시 footer.jsp 의 3버튼(뒤로/홈으로/전체메뉴) 정합.
 * (기존 5탭 구조를 레거시와 동일한 3버튼 퀵메뉴로 교체)
 */
interface NavItem {
  key: string;
  label: string;
  icon: ReactNode;
  onClick: () => void;
}

export default function QuickNav() {
  const navigate = useNavigate();

  const items: NavItem[] = [
    { key: 'back', label: '뒤로', icon: <LeftOutlined />, onClick: () => navigate(-1) },
    { key: 'home', label: '홈으로', icon: <HomeFilled />, onClick: () => navigate('/') },
    { key: 'menu', label: '전체메뉴', icon: <MenuOutlined />, onClick: () => navigate('/menu') },
  ];

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
        borderTop: '1px solid var(--mw-border)',
        display: 'flex',
        zIndex: 20,
      }}
    >
      {items.map((item) => (
        <button
          key={item.key}
          onClick={item.onClick}
          style={{
            flex: 1,
            border: 'none',
            background: 'transparent',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 3,
            cursor: 'pointer',
            color: '#333',
            fontSize: 12,
            fontWeight: 700,
          }}
        >
          <span style={{ fontSize: 19 }}>{item.icon}</span>
          {item.label}
        </button>
      ))}
    </nav>
  );
}
