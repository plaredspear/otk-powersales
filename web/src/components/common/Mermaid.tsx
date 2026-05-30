import { useEffect, useId, useRef, useState } from 'react';
import { Alert, Spin } from 'antd';
import mermaid from 'mermaid';

mermaid.initialize({
  startOnLoad: false,
  theme: 'neutral',
  securityLevel: 'strict',
  flowchart: { useMaxWidth: true, htmlLabels: true },
});

interface MermaidProps {
  /** mermaid 다이어그램 정의 문자열 */
  chart: string;
}

/**
 * Mermaid 다이어그램 렌더 컴포넌트.
 * mermaid 는 비동기로 SVG 를 생성하므로 렌더 결과를 상태로 보관한다.
 * securityLevel='strict' 로 다이어그램 내 임의 스크립트/링크를 차단한다 (정적 안내 용도).
 */
export default function Mermaid({ chart }: MermaidProps) {
  const rawId = useId();
  // mermaid render id 는 CSS selector 로 쓰이므로 콜론(:) 등 특수문자를 제거한다.
  const id = `mermaid-${rawId.replace(/[^a-zA-Z0-9]/g, '')}`;
  const [svg, setSvg] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let cancelled = false;
    setError(null);
    mermaid
      .render(id, chart)
      .then(({ svg: rendered }) => {
        if (!cancelled) setSvg(rendered);
      })
      .catch((e: unknown) => {
        if (!cancelled) setError(e instanceof Error ? e.message : '다이어그램 렌더 실패');
      });
    return () => {
      cancelled = true;
    };
  }, [chart, id]);

  if (error) {
    return <Alert type="error" showIcon message="다이어그램 렌더 실패" description={error} />;
  }
  if (!svg) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 32 }}>
        <Spin />
      </div>
    );
  }
  return <div ref={containerRef} dangerouslySetInnerHTML={{ __html: svg }} />;
}
