import { Outlet } from 'react-router-dom';

/**
 * 상세/하위 페이지용 레이아웃 — 탭바 없음.
 * 각 페이지가 상단에 <DetailHeader/> 를 직접 렌더한다(뒤로가기).
 */
export default function SubLayout() {
  return (
    <main className="mw-content" style={{ paddingBottom: 'var(--mw-safe-bottom)' }}>
      <div className="mw-content-inner">
        <Outlet />
      </div>
    </main>
  );
}
