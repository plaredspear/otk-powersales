import { Outlet } from 'react-router-dom';

/** 앱 셸 — 모바일 폭으로 중앙 정렬된 단일 컬럼. */
export default function RootShell() {
  return (
    <div className="mw-shell">
      <Outlet />
    </div>
  );
}
