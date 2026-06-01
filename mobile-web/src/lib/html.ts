import DOMPurify from 'dompurify';

/** 공지/교육 본문 HTML 을 XSS 방지 후 렌더 가능한 형태로 정화. */
export function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } });
}
