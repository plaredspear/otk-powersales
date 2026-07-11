import type { CSSProperties, MouseEvent, ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface DetailLinkProps {
  /** 이동할 앱 내부 경로. 예: `/account/123`, `/female-employee/45`. */
  to: string;
  /** 링크 텍스트(또는 임의 노드). */
  children: ReactNode;
  /**
   * 새 창(탭)으로 열지 여부. 기본 true.
   * 테이블 셀의 상세 이동 링크는 목록 화면을 남겨둔 채 새 탭에서 보도록 일괄 새 창을 기본값으로 한다.
   * 같은 탭 이동이 필요한 특수 케이스만 false 로 넘긴다.
   */
  newTab?: boolean;
  style?: CSSProperties;
  className?: string;
}

/**
 * 테이블 셀 등에서 다른 화면(상세)으로 이동하는 공통 링크.
 *
 * react-router 의 <Link>(href 부여) 를 감싸 다음을 일관되게 처리한다.
 * - `target="_blank"` + `rel="noopener noreferrer"` 로 기본 새 창(탭) 열기.
 *   href 가 유지되므로 우클릭/중간클릭/Ctrl(Cmd)+클릭도 정상 동작한다.
 * - 행 클릭(모달·드릴다운) 이 걸린 테이블에서 링크 클릭이 행 핸들러로 전파되지 않도록
 *   `onClick` 에서 stopPropagation.
 *
 * 새 창은 react-router 의 navigate state 를 전달할 수 없으므로, 목록 검색조건을
 * state 로 넘겨 "목록으로" 복귀시키던 기존 방식은 새 창에선 불필요하다(원래 목록 탭이 그대로 남는다).
 *
 * 사용 예:
 *   render: (val, record) =>
 *     record.accountId ? <DetailLink to={`/account/${record.accountId}`}>{val}</DetailLink> : (val ?? '-')
 */
export default function DetailLink({
  to,
  children,
  newTab = true,
  style,
  className,
}: DetailLinkProps) {
  return (
    <Link
      to={to}
      target={newTab ? '_blank' : undefined}
      rel={newTab ? 'noopener noreferrer' : undefined}
      onClick={(e: MouseEvent) => e.stopPropagation()}
      style={style}
      className={className}
    >
      {children}
    </Link>
  );
}
