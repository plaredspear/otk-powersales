import { Outlet, useLocation } from 'react-router-dom';
import BottomTabBar from '@/components/BottomTabBar';

const TITLES: Record<string, string> = {
  '/': '오토기 현장',
  '/notices': '공지사항',
  '/sales': '매출 현황',
  '/claims': '클레임',
  '/menu': '전체 메뉴',
};

/** 하단 탭 진입점용 레이아웃 — 고정 헤더 + 탭바. */
export default function TabLayout() {
  const { pathname } = useLocation();
  const title = TITLES[pathname] ?? '오토기 현장';

  return (
    <>
      <header
        style={{
          position: 'fixed',
          top: 0,
          left: '50%',
          transform: 'translateX(-50%)',
          width: '100%',
          maxWidth: 'var(--mw-max-w)',
          height: 'calc(var(--mw-header-h) + var(--mw-safe-top))',
          paddingTop: 'var(--mw-safe-top)',
          background: '#fff',
          borderBottom: '1px solid #f0f0f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 20,
        }}
      >
        <h1 style={{ margin: 0, fontSize: 17, fontWeight: 600 }}>{title}</h1>
      </header>
      <main className="mw-content">
        <div className="mw-content-inner">
          <Outlet />
        </div>
      </main>
      <BottomTabBar />
    </>
  );
}
