import { describe, expect, it } from 'vitest';
import {
  collectPlaceholderMappings,
  replacePreviewsWithPlaceholders,
  uniqueKeyFromSrc,
} from './noticeInlineImage';

const KEY = 'uploads/notice/2026/07/30/de9a7d0b-006d-4a95-9d81-5a882ab39763.jpg';
const HOST = 'https://prod-otk-pwrs-storage.s3.ap-northeast-2.amazonaws.com';
const presigned = (sep: '&' | '&amp;') =>
  `${HOST}/${'private/'}${KEY}?X-Amz-Algorithm=AWS4-HMAC-SHA256${sep}X-Amz-Expires=1800${sep}X-Amz-Signature=abc123`;

describe('uniqueKeyFromSrc', () => {
  it('presigned URL 에서 서명 쿼리스트링을 제외한 uniqueKey 를 뽑는다', () => {
    expect(uniqueKeyFromSrc(presigned('&'))).toBe(KEY);
  });

  it('placeholder scheme / 외부 URL 은 null', () => {
    expect(uniqueKeyFromSrc('notice-image://555')).toBeNull();
    expect(uniqueKeyFromSrc('https://example.com/photo.jpg')).toBeNull();
    expect(uniqueKeyFromSrc('file:///C:/Users/tmp/image001.jpg')).toBeNull();
  });
});

describe('replacePreviewsWithPlaceholders', () => {
  const mappings = collectPlaceholderMappings(
    `<p><img src="${presigned('&')}" data-refid="777"></p>`,
  );

  it('매핑을 uniqueKey 기준으로 등록한다', () => {
    expect(mappings.get(KEY)).toBe('<img src="notice-image://777" data-refid="777">');
  });

  // 회귀: 공지 2393 — Quill 이 속성값의 & 를 &amp; 로 이스케이프해 URL 전문 매칭이 실패했고,
  // 30분 만료 presigned URL 이 DB 본문에 그대로 저장돼 이미지가 영구히 깨졌다.
  it('Quill 이 &amp; 로 이스케이프한 본문도 placeholder 로 치환한다', () => {
    const html = `<h1>제목</h1><p><img src="${presigned('&amp;')}"></p>`;

    const result = replacePreviewsWithPlaceholders(html, mappings);

    expect(result).not.toContain('X-Amz-Signature');
    expect(result).toContain('<img src="notice-image://777" data-refid="777">');
  });

  it('서명이 갱신된(재발급) URL 도 같은 uniqueKey 라 치환된다', () => {
    const html = `<p><img src="${HOST}/private/${KEY}?X-Amz-Signature=DIFFERENT&X-Amz-Date=20260731T000000Z"></p>`;

    expect(replacePreviewsWithPlaceholders(html, mappings)).toContain('notice-image://777');
  });

  it('매핑에 없는 이미지(외부 링크 등)와 placeholder 는 원본을 보존한다', () => {
    const html =
      '<p><img src="https://example.com/photo.jpg"></p>' +
      '<p><img src="notice-image://999" data-refid="999"></p>';

    expect(replacePreviewsWithPlaceholders(html, mappings)).toBe(html);
  });
});
