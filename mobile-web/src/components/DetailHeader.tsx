import { LeftOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { ReactNode } from 'react';

/**
 * 상세/하위 페이지용 고정 헤더 (뒤로가기 + 타이틀).
 * 탭바 진입점이 아닌 화면에서 사용. MobileLayout 의 헤더를 덮어쓴다.
 */
export default function DetailHeader({ title, extra }: { title: string; extra?: ReactNode }) {
  const navigate = useNavigate();
  return (
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
        zIndex: 20,
      }}
    >
      <button
        onClick={() => navigate(-1)}
        aria-label="뒤로가기"
        style={{
          border: 'none',
          background: 'transparent',
          height: '100%',
          padding: '0 18px',
          fontSize: 18,
          cursor: 'pointer',
        }}
      >
        <LeftOutlined />
      </button>
      <h1
        style={{
          flex: 1,
          margin: 0,
          fontSize: 19,
          fontWeight: 800,
          letterSpacing: '-0.5px',
          textAlign: 'center',
          overflow: 'hidden',
          textOverflow: 'ellipsis',
          whiteSpace: 'nowrap',
        }}
      >
        {title}
      </h1>
      <div style={{ minWidth: 48, paddingRight: 8, textAlign: 'right' }}>{extra}</div>
    </header>
  );
}
