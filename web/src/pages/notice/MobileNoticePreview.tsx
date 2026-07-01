import { useMemo } from 'react';
import DOMPurify from 'dompurify';
import './MobilePreview.css';

/**
 * 공지 작성/수정 화면의 모바일 미리보기 프레임.
 *
 * mobile/lib/presentation/pages/notice_detail_page.dart 의 상세 화면 레이아웃
 * (분류 태그 → 제목 → 등록일 → 구분선 → 본문)을 web 에서 재현한다.
 * 폭 375px + 모바일 디자인 토큰 스타일(MobilePreview.css)로 실제 표현을 근사한다.
 *
 * 본문 HTML 은 에디터가 편집 중인 값을 그대로 받는다 — 저장 전 상태라
 * 인라인 이미지는 presigned previewUrl(만료 전) 로 실제 렌더링된다.
 * (저장 후 조회는 백엔드가 placeholder 를 presigned 로 rewrite → 표현 동일)
 *
 * 주의: 모바일은 flutter_widget_from_html_core 로 태그 기반 렌더링만 하고
 * 인라인 style/CSS class 는 무시하므로, 재현도 태그 기반 스타일로만 맞춘다.
 */
export interface MobileNoticePreviewProps {
  title: string;
  categoryName: string;
  isCompanyCategory: boolean;
  content: string;
}

// 모바일 상세 화면 날짜 포맷(_formatDate)과 동일한 표기: yyyy.MM.dd HH:mm
function formatNow(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

export default function MobileNoticePreview({
  title,
  categoryName,
  isCompanyCategory,
  content,
}: MobileNoticePreviewProps) {
  // data-refid 는 mobile cacheKey 용 식별자이므로 sanitize 시 보존 (NoticeDetailPage 정합).
  const safeHtml = useMemo(
    () => DOMPurify.sanitize(content || '', { ADD_ATTR: ['data-refid'] }),
    [content],
  );

  const hasContent = safeHtml.replace(/<[^>]*>/g, '').trim().length > 0 || safeHtml.includes('<img');

  return (
    <div>
      <p className="mobile-preview-caption">모바일 화면 미리보기</p>
      <div className="mobile-preview-frame">
        <div className="mobile-preview-statusbar" />
        <div className="mobile-preview-scroll">
          <span
            className={`mobile-preview-badge ${
              isCompanyCategory ? 'mobile-preview-badge--company' : 'mobile-preview-badge--other'
            }`}
          >
            {categoryName || '분류'}
          </span>

          <h1 className="mobile-preview-title">{title || '제목을 입력하세요'}</h1>

          <p className="mobile-preview-date">{formatNow()}</p>

          <hr className="mobile-preview-divider" />

          {hasContent ? (
            <div
              className="mobile-preview-body"
              dangerouslySetInnerHTML={{ __html: safeHtml }}
            />
          ) : (
            <p className="mobile-preview-empty">본문 내용이 여기에 표시됩니다</p>
          )}
        </div>
      </div>
    </div>
  );
}
