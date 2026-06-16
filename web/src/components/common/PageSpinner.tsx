import { Spin } from 'antd';

/**
 * 페이지/청크 로딩 중 중앙 정렬 스피너. lazy 라우트의 Suspense fallback 과
 * 청크 자동 reload 직후 로딩 표시에서 공통으로 사용한다.
 */
export default function PageSpinner() {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
      <Spin size="large" />
    </div>
  );
}
