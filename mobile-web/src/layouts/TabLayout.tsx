import { Outlet, useLocation } from 'react-router-dom';
import QuickNav from '@/components/QuickNav';

const TITLES: Record<string, string> = {
  '/': '오뚜기',
  '/notices': '공지사항',
  '/sales': '매출 현황',
  '/claims': '클레임',
  '/menu': '전체 메뉴',
};

/** 상단 헤더(레거시 .header 60px/800) + 하단 퀵네비. */
export default function TabLayout() {
  const { pathname } = useLocation();
  const title = TITLES[pathname] ?? '오뚜기';

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
          borderBottom: '1px solid var(--mw-border)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 20,
        }}
      >
        <h1 style={{ margin: 0, fontSize: 20, fontWeight: 800, letterSpacing: '-0.5px' }}>{title}</h1>
      </header>
      <main className="mw-content">
        <div className="mw-content-inner">
          <Outlet />
        </div>
      </main>
      <QuickNav />
    </>
  );
}
