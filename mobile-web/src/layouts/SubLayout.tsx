import { Outlet } from 'react-router-dom';
import QuickNav from '@/components/QuickNav';

/**
 * 상세/하위 페이지 레이아웃 — 각 페이지가 상단에 <DetailHeader/> 를 렌더하고,
 * 하단은 레거시 정합 퀵네비(뒤로/홈으로/전체메뉴)를 전역 노출한다.
 */
export default function SubLayout() {
  return (
    <>
      <main className="mw-content">
        <div className="mw-content-inner">
          <Outlet />
        </div>
      </main>
      <QuickNav />
    </>
  );
}
